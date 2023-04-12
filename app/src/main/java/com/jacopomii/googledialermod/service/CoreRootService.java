package com.jacopomii.googledialermod.service;

import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PHENOTYPE_CACHE;
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

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        public Map<String, Boolean> phenotypeDBGetBooleanFlags(String packageName) {
            HashMap<String, Boolean> map = new HashMap<>();

            String sql = "SELECT DISTINCT name,boolVal " +
                    "FROM Flags " +
                    "WHERE packageName=? AND name NOT IN (SELECT name FROM FlagOverrides) AND user='' AND boolVal!='NULL' " +
                    "UNION ALL " +
                    "SELECT DISTINCT name,boolVal FROM FlagOverrides " +
                    "WHERE packageName=? AND user='' AND boolVal!='NULL'";

            String[] selectionArgs = {packageName, packageName};

            Cursor cursor = phenotypeDB.rawQuery(sql, selectionArgs);

            while(cursor.moveToNext())
                map.put(cursor.getString(0), cursor.getInt(1)!=0);

            return map;
        }

        @Override
        public boolean phenotypeDBAreAllBooleanFlagsTrue(String packageName, String[] flags) {
            String sql = "SELECT DISTINCT name,boolVal " +
                "FROM Flags " +
                "WHERE packageName=? AND name IN ("+createInQueryString(flags.length)+") AND name NOT IN (SELECT name FROM FlagOverrides) AND user='' AND boolVal='1' " +
                "UNION ALL " +
                "SELECT DISTINCT name,boolVal FROM FlagOverrides " +
                "WHERE packageName=? AND name IN ("+createInQueryString(flags.length)+") AND user='' AND boolVal='1'";

            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(packageName);
            selectionArgs.addAll(Arrays.asList(flags));
            selectionArgs.add(packageName);
            selectionArgs.addAll(Arrays.asList(flags));

            return phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0])).getCount() == flags.length;
        }

        @Override
        public boolean phenotypeDBAreAllFlagsOverridden(String packageName, String[] flags) {
            String sql = "SELECT DISTINCT name FROM FlagOverrides WHERE packageName=? AND name IN ("+createInQueryString(flags.length)+")";

            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(packageName);
            selectionArgs.addAll(Arrays.asList(flags));

            return phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0])).getCount() == flags.length;
        }

        @Override
        public boolean phenotypeDBAreAllStringFlagsEmpty(String packageName, String[] flags) {
            String sql = "SELECT DISTINCT name,stringVal " +
                    "FROM Flags " +
                    "WHERE packageName=? AND name IN ("+createInQueryString(flags.length)+") AND name NOT IN (SELECT name FROM FlagOverrides) AND user='' AND stringVal='' " +
                    "UNION ALL " +
                    "SELECT DISTINCT name,stringVal FROM FlagOverrides " +
                    "WHERE packageName=? AND name IN ("+createInQueryString(flags.length)+") AND user='' AND stringVal=''";

            List<String> selectionArgs = new ArrayList<>();
            selectionArgs.add(packageName);
            selectionArgs.addAll(Arrays.asList(flags));
            selectionArgs.add(packageName);
            selectionArgs.addAll(Arrays.asList(flags));

            return phenotypeDB.rawQuery(sql, selectionArgs.toArray(new String[0])).getCount() == flags.length;
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverrides() {
            killDialerAndDeletePhenotypeCache();

            phenotypeDB.delete("FlagOverrides", null, null);
        }

        @Override
        public void phenotypeDBDeleteAllFlagOverridesByPackageName(String packageName) {
            killDialerAndDeletePhenotypeCache();

            phenotypeDB.delete("FlagOverrides", "packageName=?", new String[]{packageName});
        }

        @Override
        public void phenotypeDBDeleteFlagOverrides(String packageName, String[] flags) {
            killDialerAndDeletePhenotypeCache();

            String whereClause = "packageName=? AND name IN ("+createInQueryString(flags.length)+")";
            List<String> whereArgs = new ArrayList<>();
            whereArgs.add(packageName);
            whereArgs.addAll(Arrays.asList(flags));
            phenotypeDB.delete("FlagOverrides", whereClause, whereArgs.toArray(new String[0]));
        }

        @Override
        public void phenotypeDBUpdateBooleanFlag(String packageName, String flag, boolean value) {
            phenotypeDBDeleteFlagOverrides(packageName, new String[]{flag});

            Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

            while(cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", packageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("boolVal", (value ? "1" : "0"));
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        public void phenotypeDBUpdateExtensionFlag(String packageName, String flag, byte[] value) {
            phenotypeDBDeleteFlagOverrides(packageName, new String[]{flag});

            Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

            while(cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", packageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("extensionVal", value);
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }

        public void phenotypeDBUpdateStringFlag(String packageName, String flag, String value) {
            phenotypeDBDeleteFlagOverrides(packageName, new String[]{flag});

            Cursor cursor = phenotypeDB.rawQuery("SELECT DISTINCT user FROM Flags WHERE packageName = ?", new String[]{packageName});

            while(cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("packageName", packageName);
                contentValues.put("flagType", 0);
                contentValues.put("name", flag);
                contentValues.put("user", cursor.getString(0));
                contentValues.put("stringVal", value);
                contentValues.put("committed", 0);
                phenotypeDB.insertWithOnConflict("FlagOverrides", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    private void killDialerAndDeletePhenotypeCache() {
        Shell.cmd("am kill all " + DIALER_PACKAGE_NAME).exec();
        ExtendedFile dialerPhenotypeCache = FileSystemManager.getLocal().getFile(DIALER_PHENOTYPE_CACHE);
        if (dialerPhenotypeCache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dialerPhenotypeCache.delete();
        }
    }
}
