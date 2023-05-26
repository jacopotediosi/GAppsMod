package com.jacopomii.gappsmod.service;

import static com.jacopomii.gappsmod.data.Constants.DATA_DATA_PREFIX;
import static com.jacopomii.gappsmod.data.Constants.GMS_PHENOTYPE_DB;
import static com.jacopomii.gappsmod.data.Constants.VENDING_ANDROID_PACKAGE_NAME;
import static com.jacopomii.gappsmod.data.Constants.VENDING_PHENOTYPE_DB;
import static com.jacopomii.gappsmod.util.Utils.createInQueryString;
import static org.sqlite.database.sqlite.SQLiteDatabase.OPEN_READWRITE;
import static org.sqlite.database.sqlite.SQLiteDatabase.openDatabase;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Process;

import androidx.annotation.NonNull;

import com.jacopomii.gappsmod.ICoreRootService;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.FileUtils;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SameParameterValue")
public class CoreRootService extends RootService {
    static {
        // Only load the library when this class is loaded in a root process.
        // The classloader will load this class (and call this static block) in the non-root process because we accessed it when constructing the Intent to send.
        // Add this check so we don't unnecessarily load native code that'll never be used.
        if (Process.myUid() == 0)
            System.loadLibrary("sqliteX");
    }

    private SQLiteDatabase GMSPhenotypeDB = null;
    private SQLiteDatabase VendingPhenotypeDB = null;

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        try {
            GMSPhenotypeDB = openDatabase(GMS_PHENOTYPE_DB, null, OPEN_READWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            VendingPhenotypeDB = openDatabase(VENDING_PHENOTYPE_DB, null, OPEN_READWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new CoreRootServiceIPC();
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        if (GMSPhenotypeDB != null && GMSPhenotypeDB.isOpen()) GMSPhenotypeDB.close();
        if (VendingPhenotypeDB != null && VendingPhenotypeDB.isOpen()) VendingPhenotypeDB.close();
        super.onUnbind(intent);
        return false;
    }

    @Override
    public void onDestroy() {
        if (GMSPhenotypeDB != null && GMSPhenotypeDB.isOpen()) GMSPhenotypeDB.close();
        if (VendingPhenotypeDB != null && VendingPhenotypeDB.isOpen()) VendingPhenotypeDB.close();
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
        public Map<String, String> phenotypeDBGetAllOverriddenPackageNames() {
            return CoreRootService.this.phenotypeDBGetAllOverriddenPackageNames();
        }

        @Override
        public String phenotypeDBGetAndroidPackageNameByPhenotypePackageName(String phenotypePackageName) {
            return CoreRootService.this.phenotypeDBGetAndroidPackageNameByPhenotypePackageName(phenotypePackageName);
        }

        @Override
        public Map<String, List<Object>> phenotypeDBGetBooleanFlagsOrOverridden(String phenotypePackageName) {
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


    private Map<String, String> phenotypeDBGetAllPackageNames() {
        HashMap<String, String> map = new HashMap<>();

        String sql = "SELECT Flags.packageName as phenotypePackageName, Packages.androidPackageName " +
                "FROM Flags, Packages " +
                "WHERE phenotypePackageName=Packages.packageName " +
                "GROUP BY phenotypePackageName " +
                "ORDER BY phenotypePackageName ASC";

        try {
            try (Cursor cursor = GMSPhenotypeDB.rawQuery(sql, null)) {
                while (cursor.moveToNext())
                    map.put(cursor.getString(0), cursor.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            try (Cursor cursor = VendingPhenotypeDB.rawQuery(sql, null)) {
                while (cursor.moveToNext())
                    map.put(cursor.getString(0), cursor.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    private Map<String, String> phenotypeDBGetAllOverriddenPackageNames() {
        HashMap<String, String> map = new HashMap<>();

        String sql = "SELECT FlagOverrides.packageName as phenotypePackageName, Packages.androidPackageName " +
                "FROM FlagOverrides, Packages " +
                "WHERE phenotypePackageName=Packages.packageName " +
                "GROUP BY phenotypePackageName " +
                "ORDER BY phenotypePackageName ASC";

        try {
            try (Cursor cursor = GMSPhenotypeDB.rawQuery(sql, null)) {
                while (cursor.moveToNext())
                    map.put(cursor.getString(0), cursor.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            try (Cursor cursor = VendingPhenotypeDB.rawQuery(sql, null)) {
                while (cursor.moveToNext())
                    map.put(cursor.getString(0), cursor.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    private String phenotypeDBGetAndroidPackageNameByPhenotypePackageName(String phenotypePackageName) {
        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT androidPackageName FROM Packages WHERE packageName=? LIMIT 1";
        String[] selectionArgs = {phenotypePackageName};

        try {
            try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
                if (cursor.moveToFirst()) return cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private Map<String, List<Object>> phenotypeDBGetBooleanFlagsOrOverridden(String phenotypePackageName) {
        Map<String, List<Object>> map = new HashMap<>();

        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT DISTINCT f.name AS name, COALESCE(fo.boolVal, f.boolVal) AS boolVal, CASE WHEN fo.boolVal != f.boolVal THEN 1 ELSE 0 END AS changed " +
                "FROM Flags f " +
                "LEFT JOIN FlagOverrides fo ON f.packageName = fo.packageName AND f.name = fo.name " +
                "WHERE f.packageName = ? AND f.user = '' AND (f.boolVal IS NOT NULL OR fo.boolVal IS NOT NULL)" +
                "UNION ALL " +
                "SELECT DISTINCT fo.name AS name, fo.boolVal, 1 AS changed " +
                "FROM FlagOverrides fo " +
                "LEFT JOIN Flags f ON f.packageName = fo.packageName AND f.name = fo.name " +
                "WHERE fo.packageName = ? AND f.name IS NULL AND fo.user = '' AND fo.boolVal IS NOT NULL";
        String[] selectionArgs = {phenotypePackageName, phenotypePackageName};

        try {
            try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs)) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    Boolean boolVal = cursor.getInt(1) != 0;
                    Boolean changed = cursor.getInt(2) != 0;
                    map.put(name, Arrays.asList(boolVal, changed));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    private boolean phenotypeDBAreAllFlagsOverridden(String phenotypePackageName, List<String> flags) {
        boolean areAllFlagsOverridden = false;

        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT DISTINCT name FROM FlagOverrides WHERE packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(phenotypePackageName);
        selectionArgs.addAll(flags);

        try {
            try (Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0]))) {
                areAllFlagsOverridden = cursor.getCount() == flags.size();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return areAllFlagsOverridden;
    }

    private void phenotypeDBDeleteAllFlagOverrides(boolean deletePackagePhenotypeCache) {
        Set<String> overriddenPhenotypePackageNames = phenotypeDBGetAllOverriddenPackageNames().keySet();

        try {
            GMSPhenotypeDB.delete("FlagOverrides", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            VendingPhenotypeDB.delete("FlagOverrides", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache) {
            for (String phenotypePackageName : overriddenPhenotypePackageNames) {
                killPackageAndDeletePhenotypeCache(phenotypePackageName);
            }
        }
    }

    private void phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(String phenotypePackageName, boolean deletePackagePhenotypeCache) {
        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);

        try {
            phenotypeDB.delete("FlagOverrides", "packageName=?", new String[]{phenotypePackageName});
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBDeleteFlagOverrides(String phenotypePackageName, List<String> flags, boolean deletePackagePhenotypeCache) {
        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String whereClause = "packageName=? AND name IN (" + createInQueryString(flags.size()) + ")";
        List<String> whereArgs = new ArrayList<>();
        whereArgs.add(phenotypePackageName);
        whereArgs.addAll(flags);

        try {
            phenotypeDB.delete("FlagOverrides", whereClause, whereArgs.toArray(new String[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideBooleanFlag(String phenotypePackageName, String flag, boolean value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideExtensionFlag(String phenotypePackageName, String flag, byte[] value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    private void phenotypeDBOverrideStringFlag(String phenotypePackageName, String flag, String value, boolean deletePackagePhenotypeCache) {
        phenotypeDBDeleteFlagOverrides(phenotypePackageName, Collections.singletonList(flag), false);

        SQLiteDatabase phenotypeDB = getPhenotypeDBByPhenotypePackageName(phenotypePackageName);
        String sql = "SELECT DISTINCT user FROM Flags WHERE packageName = ?";
        String[] selectionArgs = {phenotypePackageName};

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deletePackagePhenotypeCache)
            killPackageAndDeletePhenotypeCache(phenotypePackageName);
    }

    /**
     * Get the correct database (GMS or Vending) based on the {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name for which to get the
     *                             Phenotype DB.
     * @return a {@code SQLiteDatabase} reference to the GMS or Vending Phenotype DB.
     * The returned object may be {@code null} if there was an error opening the database.
     */
    private SQLiteDatabase getPhenotypeDBByPhenotypePackageName(String phenotypePackageName) {
        if (phenotypePackageName.equals("com.google.android.finsky.regular") || phenotypePackageName.equals("com.google.android.finsky.stable")) {
            return VendingPhenotypeDB;
        } else {
            return GMSPhenotypeDB;
        }
    }

    /**
     * Kill and delete the Phenotype cache files of the Android application corresponding to the
     * given {@code phenotypePackageName}.
     *
     * @param phenotypePackageName the Phenotype (not Android) package name to kill and to delete
     *                             the Phenotype cache files for.
     */
    private void killPackageAndDeletePhenotypeCache(String phenotypePackageName) {
        String androidPackageName = phenotypeDBGetAndroidPackageNameByPhenotypePackageName(phenotypePackageName);

        // Kill the android application corresponding to the phenotypePackageName
        Shell.cmd("am kill all " + androidPackageName).exec();

        // Delete application phenotype Cache
        ExtendedFile phenotypeCache = FileSystemManager.getLocal().getFile(DATA_DATA_PREFIX + androidPackageName + "/files/phenotype");
        if (phenotypeCache.exists()) {
            try {
                FileUtils.deleteDirectory(phenotypeCache);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // If the application is Vending, additional cache files need to be deleted
        if (androidPackageName.equals(VENDING_ANDROID_PACKAGE_NAME)) {
            ExtendedFile[] VendingFiles = FileSystemManager.getLocal().getFile(DATA_DATA_PREFIX + androidPackageName + "/files").listFiles();
            if (VendingFiles != null) {
                for (ExtendedFile file : VendingFiles) {
                    if (file.getName().startsWith("experiment-flags")) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            }
        }
    }
}
