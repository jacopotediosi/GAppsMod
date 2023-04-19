package com.jacopomii.googledialermod.service;

import static com.jacopomii.googledialermod.data.Constants.DATA_DATA_PREFIX;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.MESSAGES_PACKAGE_NAME;
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
        public Map<String, Boolean> phenotypeDBGetBooleanFlagsOrOverridden(String packageName) {
            return CoreRootService.this.phenotypeDBGetBooleanFlagsOrOverridden(packageName);
        }

        @Override
        public boolean phenotypeDBAreAllFlagsOverridden(String packageName, List<String> flags) {
            return CoreRootService.this.phenotypeDBAreAllFlagsOverridden(packageName, flags);
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverrides() {
            CoreRootService.this.phenotypeDBDeleteAllFlagOverrides(true);
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverridesByPackageName(String packageName) {
            CoreRootService.this.phenotypeDBDeleteAllFlagOverridesByPackageName(packageName, true);
        }

        @Override
        public void phenotypeDBDeleteFlagOverrides(String packageName, List<String> flags) {
            CoreRootService.this.phenotypeDBDeleteFlagOverrides(packageName, flags, true);
        }

        @Override
        public void phenotypeDBOverrideBooleanFlag(String packageName, String flag, boolean value) {
            CoreRootService.this.phenotypeDBOverrideBooleanFlag(packageName, flag, value, true);
        }

        @Override
        public void phenotypeDBOverrideExtensionFlag(String packageName, String flag, byte[] value) {
            CoreRootService.this.phenotypeDBOverrideExtensionFlag(packageName, flag, value, true);
        }

        @Override
        public void phenotypeDBOverrideStringFlag(String packageName, String flag, String value) {
            CoreRootService.this.phenotypeDBOverrideStringFlag(packageName, flag, value, true);
        }
    }


    private Map<String, Boolean> phenotypeDBGetBooleanFlagsOrOverridden(String packageName) {
        HashMap<String, Boolean> map = new HashMap<>();

        String sql = "SELECT DISTINCT name,boolVal " +
                "FROM Flags " +
                "WHERE packageName=? AND name NOT IN (SELECT name FROM FlagOverrides) AND user='' AND boolVal!='NULL' " +
                "UNION ALL " +
                "SELECT DISTINCT name,boolVal FROM FlagOverrides " +
                "WHERE packageName=? AND user='' AND boolVal!='NULL'";

        String[] selectionArgs = {packageName, packageName};

        Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs);

        while (cursor.moveToNext())
            map.put(cursor.getString(0), cursor.getInt(1) != 0);

        return map;
    }

    private boolean phenotypeDBAreAllFlagsOverridden(String packageName, List<String> flags) {
        String sql = "SELECT DISTINCT name FROM FlagOverrides WHERE packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";

        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(packageName);
        selectionArgs.addAll(flags);

        return phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0])).getCount() == flags.size();
    }

    private void phenotypeDBDeleteAllFlagOverrides(boolean deleteSuggestedPackagesPhenotypeCache) {
        phenotypeDB.delete("FlagOverrides", null, null);

        if (deleteSuggestedPackagesPhenotypeCache)
            killSuggestedPackagesAndDeletePhenotypeCaches();
    }

    private void phenotypeDBDeleteAllFlagOverridesByPackageName(String packageName, boolean deletePackagePhenotypeCache) {
        phenotypeDB.delete("FlagOverrides", "packageName=?", new String[]{packageName});

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(packageName);
    }

    private void phenotypeDBDeleteFlagOverrides(String packageName, List<String> flags, boolean deletePackagePhenotypeCache) {
        String whereClause = "packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";
        List<String> whereArgs = new ArrayList<>();
        whereArgs.add(packageName);
        whereArgs.addAll(flags);
        phenotypeDB.delete("FlagOverrides", whereClause, whereArgs.toArray(new String[0]));

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(packageName);
    }

    private void phenotypeDBOverrideBooleanFlag(String packageName, String flag, boolean value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(packageName, Collections.singletonList(flag), false);

        Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

        while (cursor.moveToNext()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("packageName", packageName);
            contentValues.put("flagType", 0);
            contentValues.put("name", flag);
            contentValues.put("user", cursor.getString(0));
            contentValues.put("boolVal", (value ? "1" : "0"));
            contentValues.put("committed", 0);
            phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(packageName);
    }

    private void phenotypeDBOverrideExtensionFlag(String packageName, String flag, byte[] value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(packageName, Collections.singletonList(flag), false);

        Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

        while (cursor.moveToNext()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("packageName", packageName);
            contentValues.put("flagType", 0);
            contentValues.put("name", flag);
            contentValues.put("user", cursor.getString(0));
            contentValues.put("extensionVal", value);
            contentValues.put("committed", 0);
            phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(packageName);
    }

    private void phenotypeDBOverrideStringFlag(String packageName, String flag, String value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(packageName, Collections.singletonList(flag), false);

        Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

        while (cursor.moveToNext()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("packageName", packageName);
            contentValues.put("flagType", 0);
            contentValues.put("name", flag);
            contentValues.put("user", cursor.getString(0));
            contentValues.put("stringVal", value);
            contentValues.put("committed", 0);
            phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(packageName);
    }

    private void killPackageAndDeletePhenotypeCache(String packageName) {
        Shell.cmd("am kill all " + packageName).exec();
        ExtendedFile phenotypeCache = FileSystemManager.getLocal().getFile(DATA_DATA_PREFIX + packageName + "/files/phenotype");
        if (phenotypeCache.exists()) {
            try {
                FileUtils.deleteDirectory(phenotypeCache);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void killSuggestedPackagesAndDeletePhenotypeCaches() {
        String[] suggestedPackageNames = {DIALER_PACKAGE_NAME, MESSAGES_PACKAGE_NAME};
        for (String packageName : suggestedPackageNames) {
            killPackageAndDeletePhenotypeCache(packageName);
        }
    }
}
