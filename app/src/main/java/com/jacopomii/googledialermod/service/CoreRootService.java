package com.jacopomii.googledialermod.service;

import static com.jacopomii.googledialermod.data.Constants.DATA_DATA_PREFIX;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PHENOTYPE_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.MESSAGES_PHENOTYPE_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.PHENOTYPE_DB;
import static com.jacopomii.googledialermod.util.Utils.createInQueryString;
import static org.sqlite.database.sqlite.SQLiteDatabase.OPEN_READWRITE;
import static org.sqlite.database.sqlite.SQLiteDatabase.openDatabase;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Process;

import androidx.annotation.NonNull;

import com.jacopomii.googledialermod.ICoreRootService;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.FileUtils;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SameParameterValue")
public class CoreRootService extends RootService {
    static {
        // Only load the library when this class is loaded in a root process.
        // The classloader will load this class (and call this static block) in the non-root process because we accessed it when constructing the Intent to send.
        // Add this check so we don't unnecessarily load native code that'll never be used.
        if (Process.myUid() == 0)
            System.loadLibrary("sqliteX");
    }

    private SQLiteDatabase phenotypeDB;

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        phenotypeDB = openDatabase(PHENOTYPE_DB, null, OPEN_READWRITE);
        return new CoreRootServiceIPC();
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        if (phenotypeDB.isOpen())
            phenotypeDB.close();
        super.onUnbind(intent);
        return false;
    }

    @Override
    public void onDestroy() {
        if (phenotypeDB.isOpen())
            phenotypeDB.close();
        super.onDestroy();
    }

    private class CoreRootServiceIPC extends ICoreRootService.Stub {
        @Override
        public IBinder getFileSystemService() {
            return FileSystemManager.getService();
        }

        @Override
        public Map<String, String> phenotypeDBGetAllPackageNames() {
            return CoreRootService.this.phenotypeDBGetAllPackageNames();
        }

        @Override
        public String phenotypeDBGetAndroidPackageNameByPhenotypePackageName(String phenotypePackageName) {
            return CoreRootService.this.phenotypeDBGetAndroidPackageNameByPhenotypePackageName(phenotypePackageName);
        }

        @Override
        public Map<String, Boolean> phenotypeDBGetBooleanFlagsOrOverridden(String phenotypePackageName) {
            return CoreRootService.this.phenotypeDBGetBooleanFlagsOrOverridden(phenotypePackageName);
        }

        @Override
        public boolean phenotypeDBAreAllFlagsOverridden(String phenotypePackageName, List<String> flags) {
            return CoreRootService.this.phenotypeDBAreAllFlagsOverridden(phenotypePackageName, flags);
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverrides() {
            CoreRootService.this.phenotypeDBDeleteAllFlagOverrides(true);
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(String phenotypePackageName) {
            CoreRootService.this.phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(phenotypePackageName, true);
        }

        @Override
        public void phenotypeDBDeleteFlagOverrides(String phenotypePackageName, List<String> flags) {
            CoreRootService.this.phenotypeDBDeleteFlagOverrides(phenotypePackageName, flags, true);
        }

        @Override
        public void phenotypeDBOverrideBooleanFlag(String phenotypePackageName, String flag, boolean value) {
            CoreRootService.this.phenotypeDBOverrideBooleanFlag(phenotypePackageName, flag, value, true);
        }

        @Override
        public void phenotypeDBOverrideExtensionFlag(String phenotypePackageName, String flag, byte[] value) {
            CoreRootService.this.phenotypeDBOverrideExtensionFlag(phenotypePackageName, flag, value, true);
        }

        @Override
        public void phenotypeDBOverrideStringFlag(String phenotypePackageName, String flag, String value) {
            CoreRootService.this.phenotypeDBOverrideStringFlag(phenotypePackageName, flag, value, true);
        }
    }


    public Map<String, String> phenotypeDBGetAllPackageNames() {
        HashMap<String, String> map = new HashMap<>();

        String sql = "SELECT Flags.packageName as phenotypePackageName, Packages.androidPackageName " +
                "FROM Flags, Packages " +
                "WHERE phenotypePackageName=Packages.packageName " +
                "GROUP BY phenotypePackageName " +
                "ORDER BY phenotypePackageName ASC";

        try (Cursor cursor = phenotypeDB.rawQuery(sql, null)) {
            while (cursor.moveToNext())
                map.put(cursor.getString(0), cursor.getString(1));
        }

        return map;
    }

    public String phenotypeDBGetAndroidPackageNameByPhenotypePackageName(String phenotypePackageName) {
        String androidPackageName = "";

        String sql = "SELECT androidPackageName FROM Packages WHERE packageName=? LIMIT 1";
        String[] selectionArgs = {phenotypePackageName};

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
            if (cursor.moveToFirst()) androidPackageName = cursor.getString(0);
        }

        return androidPackageName;
    }

    private Map<String, Boolean> phenotypeDBGetBooleanFlagsOrOverridden(String phenotypePackageName) {
        HashMap<String, Boolean> map = new HashMap<>();

        String sql = "SELECT DISTINCT name,boolVal " +
                "FROM Flags " +
                "WHERE packageName=? AND name NOT IN (SELECT name FROM FlagOverrides) AND user='' AND boolVal!='NULL' " +
                "UNION ALL " +
                "SELECT DISTINCT name,boolVal FROM FlagOverrides " +
                "WHERE packageName=? AND user='' AND boolVal!='NULL'";
        String[] selectionArgs = {phenotypePackageName, phenotypePackageName};

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
            while (cursor.moveToNext())
                map.put(cursor.getString(0), cursor.getInt(1) != 0);
        }

        return map;
    }

    private boolean phenotypeDBAreAllFlagsOverridden(String phenotypePackageName, List<String> flags) {
        boolean areAllFlagsOverridden;

        String sql = "SELECT DISTINCT name FROM FlagOverrides WHERE packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(phenotypePackageName);
        selectionArgs.addAll(flags);

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0]))) {
            areAllFlagsOverridden = cursor.getCount() == flags.size();
        }

        return areAllFlagsOverridden;
    }

    private void phenotypeDBDeleteAllFlagOverrides(boolean deleteSuggestedPackagesPhenotypeCache) {
        phenotypeDB.delete("FlagOverrides", null, null);

        if (deleteSuggestedPackagesPhenotypeCache)
            killSuggestedPackagesAndDeletePhenotypeCaches();
    }

    private void phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(String phenotypePackageName, boolean deletePackagePhenotypeCache) {
        phenotypeDB.delete("FlagOverrides", "packageName=?", new String[]{phenotypePackageName});

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBDeleteFlagOverrides(String phenotypePackageName, List<String> flags, boolean deletePackagePhenotypeCache) {
        String whereClause = "packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";
        List<String> whereArgs = new ArrayList<>();
        whereArgs.add(phenotypePackageName);
        whereArgs.addAll(flags);
        phenotypeDB.delete("FlagOverrides", whereClause, whereArgs.toArray(new String[0]));

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideBooleanFlag(String phenotypePackageName, String flag, boolean value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
            while (cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", phenotypePackageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("boolVal", (value ? "1" : "0"));
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideExtensionFlag(String phenotypePackageName, String flag, byte[] value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
            while (cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", phenotypePackageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("extensionVal", value);
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideStringFlag(String phenotypePackageName, String flag, String value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
            while (cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", phenotypePackageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("stringVal", value);
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void killPackageAndDeletePhenotypeCache(String phenotypePackageName) {
        String androidPackageName = phenotypeDBGetAndroidPackageNameByPhenotypePackageName(phenotypePackageName);

        Shell.cmd("am kill all " + androidPackageName).exec();
        ExtendedFile phenotypeCache = FileSystemManager.getLocal().getFile(DATA_DATA_PREFIX + androidPackageName + "/files/phenotype");
        if (phenotypeCache.exists()) {
            try {
                FileUtils.deleteDirectory(phenotypeCache);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void killSuggestedPackagesAndDeletePhenotypeCaches() {
        String[] suggestedPhenotypePackageNames = {DIALER_PHENOTYPE_PACKAGE_NAME, MESSAGES_PHENOTYPE_PACKAGE_NAME};
        for (String phenotypePackageName : suggestedPhenotypePackageNames) {
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
        }
    }
}
