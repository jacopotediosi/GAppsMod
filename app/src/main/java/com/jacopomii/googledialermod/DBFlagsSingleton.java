package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.Constants.DIALER_PHENOTYPE_CACHE;
import static com.jacopomii.googledialermod.Constants.PHENOTYPE_DB;
import static com.jacopomii.googledialermod.Utils.byteArrayToHexString;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class DBFlagsSingleton {
    private static DBFlagsSingleton mUniqueInstance = null;
    private final Map<String, Boolean> mDBBooleanFlags = new TreeMap<>();
    private final Map<String, String> mDBStringFlags = new TreeMap<>();
    private final ArrayList<String> mDBUsers = new ArrayList<>();
    private final Context mContext;

    private DBFlagsSingleton(Context context) {
        mContext = context.getApplicationContext();
        reloadDB();
    }

    public static DBFlagsSingleton getInstance(Context context) {
        if (mUniqueInstance == null)
            mUniqueInstance = new DBFlagsSingleton(context);
        return mUniqueInstance;
    }

    public Boolean getDBBooleanFlag(String flag) {
        return mDBBooleanFlags.get(flag);
    }

    public String getDBStringFlag(String flag) {
        return mDBStringFlags.get(flag);
    }

    public Map<String, Boolean> getDBBooleanFlags() {
        return mDBBooleanFlags;
    }

    public Map<String, String> getDBStringFlags() {
        return mDBStringFlags;
    }

    public void reloadDB() {
        reloadDBUsers();
        reloadDBBooleanFlags();
        reloadDBStringFlags();
    }

    private void reloadDBUsers() {
        mDBUsers.clear();
        JSONArray users = execPhenotypeQuery(
                mContext,
                String.format(
                        "SELECT DISTINCT user FROM Flags WHERE packageName = '%s'",
                        DIALER_PACKAGE_NAME
                )
        );
        for (int i=0; i < users.length(); i++) {
            try {
                String user = users.getJSONObject(i).getString("user");
                mDBUsers.add(user);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadDBBooleanFlags() {
        mDBBooleanFlags.clear();
        JSONArray queryResult = execPhenotypeQuery(
                mContext,
                String.format(
                        "SELECT DISTINCT name, boolVal " +
                                "FROM Flags " +
                                "WHERE packageName = '%s' AND user = '' AND boolVal != 'NULL' AND name NOT IN (SELECT name FROM FlagOverrides) " +
                                "UNION ALL " +
                                "SELECT DISTINCT name, boolVal FROM FlagOverrides " +
                                "WHERE packageName = '%s' AND user = '' AND boolVal != 'NULL'",
                        DIALER_PACKAGE_NAME,
                        DIALER_PACKAGE_NAME
                )
        );
        for (int i=0; i < queryResult.length(); i++) {
            try {
                JSONObject flag = queryResult.getJSONObject(i);
                mDBBooleanFlags.put(flag.getString("name"), flag.getInt("boolVal")!=0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void reloadDBStringFlags() {
        mDBStringFlags.clear();
        JSONArray queryResult = execPhenotypeQuery(
                mContext,
                String.format(
                        "SELECT DISTINCT name, stringVal " +
                                "FROM Flags " +
                                "WHERE packageName = '%s' AND user = '' AND stringVal != 'NULL' AND name NOT IN (SELECT name FROM FlagOverrides) " +
                                "UNION ALL " +
                                "SELECT DISTINCT name, stringVal FROM FlagOverrides " +
                                "WHERE packageName = '%s' AND user = '' AND stringVal != 'NULL'",
                        DIALER_PACKAGE_NAME,
                        DIALER_PACKAGE_NAME
                )
        );
        for (int i=0; i < queryResult.length(); i++) {
            try {
                JSONObject flag = queryResult.getJSONObject(i);
                mDBStringFlags.put(flag.getString("name"), flag.getString("stringVal"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateDBFlag(String flag, boolean value) {
        mDBBooleanFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                        DIALER_PACKAGE_NAME,
                        flag.replace("'", "\\'")
                )
        );

        ArrayList<String> queryValues = new ArrayList<>();
        for (String user : mDBUsers)
            queryValues.add(
                    String.format(
                            "('%s', 0, '%s', '%s', '%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            (value ? '1' : '0')
                    )
            );
        execPhenotypeQuery(
                mContext,
                "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, boolVal, committed) VALUES " + String.join(",", queryValues)
        );
    }

    public void updateDBFlag(String flag, String value) {
        mDBStringFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                        DIALER_PACKAGE_NAME,
                        flag.replace("'", "\\'")
                )
        );

        ArrayList<String> queryValues = new ArrayList<>();
        for (String user : mDBUsers)
            queryValues.add(
                    String.format(
                            "('%s', 0, '%s', '%s', '%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            value.replace("'", "\\'")
                    )
            );
        execPhenotypeQuery(
                mContext,
                "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, stringVal, committed) VALUES " + String.join(",", queryValues)
        );
    }

    public void updateDBFlag(String flag, byte[] value) {
        // mDBExtensionFlags.put(flag, value); // Extension flags are only partially supported for now
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                        DIALER_PACKAGE_NAME,
                        flag.replace("'", "\\'")
                )
        );

        ArrayList<String> queryValues = new ArrayList<>();
        for (String user : mDBUsers)
            queryValues.add(
                    String.format(
                            "('%s', 0, '%s', '%s', '%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            byteArrayToHexString(value)
                    )
            );
        execPhenotypeQuery(
                mContext,
                "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, extensionVal, committed) VALUES " + String.join(",", queryValues)
        );
    }

    public void deleteFlagOverrides(String... flags) {
        killDialerAndDeletePhenotypeCache();

        ArrayList<String> queryValues = new ArrayList<>();
        for (String flag : flags)
            queryValues.add("'" + flag.replace("'", "\\'") + "'");

        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name IN (%s)",
                        DIALER_PACKAGE_NAME,
                        String.join(",", queryValues)
                )
        );

        JSONArray queryResult = execPhenotypeQuery(
                mContext,
                String.format(
                        "SELECT name, boolVal, stringVal FROM Flags WHERE packageName = '%s' AND user = '' AND name IN (%s)",
                        DIALER_PACKAGE_NAME,
                        String.join(",", queryValues)
                )
        );
        try {
            for (int i=0; i<queryResult.length(); i++) {
                JSONObject flagValues = queryResult.getJSONObject(i);
                if (!flagValues.isNull("boolVal"))
                    mDBBooleanFlags.put(flagValues.getString("name"), flagValues.getInt("boolVal")==1);
                else if (!flagValues.isNull("stringVal"))
                    mDBStringFlags.put(flagValues.getString("name"), flagValues.getString("stringVal"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean areAllBooleanFlagsTrue(String... flags) {
        for (String flag : flags) {
            Boolean flagValue = getDBBooleanFlag(flag);
            if (flagValue == null || !flagValue)
                return false;
        }
        return true;
    }

    public boolean areAllStringFlagsEmpty(String... flags) {
        for (String flag : flags) {
            String flagValue = getDBStringFlag(flag);
            if (flagValue == null || !flagValue.isEmpty())
                return false;
        }
        return true;
    }

    public void deleteAllFlagOverrides() {
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s'",
                        DIALER_PACKAGE_NAME
                )
        );
        reloadDB();
    }

    public boolean areAllFlagsOverridden(String... flags) {
        ArrayList<String> queryValues = new ArrayList<>();
        for (String flag : flags)
            queryValues.add("'" + flag.replace("'", "\\'") + "'");

        JSONArray queryResult = execPhenotypeQuery(
                mContext,
                String.format(
                        "SELECT DISTINCT name FROM Flags WHERE packageName = '%s' AND name IN (%s)",
                        DIALER_PACKAGE_NAME,
                        String.join(",", queryValues)
                )
        );

        return queryResult.length() == flags.length;
    }

    private JSONArray execPhenotypeQuery(Context context, String query) {
        JSONArray result = null;
        try {
            String query_result = String.join("", Shell.cmd(
                    String.format(
                            "%s/sqlite3 -batch -json %s \"%s;\"",
                            context.getApplicationInfo().dataDir,
                            PHENOTYPE_DB,
                            query
                    )
            ).exec().getOut());
            if (query_result.equals("")) {
                result = new JSONArray("[]");
            } else {
                result = new JSONArray(query_result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void killDialerAndDeletePhenotypeCache() {
        Shell.cmd(
                String.format(
                        "am kill all %s; rm -rf %s",
                        DIALER_PACKAGE_NAME,
                        DIALER_PHENOTYPE_CACHE
                )
        ).exec();
    }
}