package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.Utils.byteArrayToHexString;
import static com.jacopomii.googledialermod.Utils.execPhenotypeQuery;
import static com.jacopomii.googledialermod.Utils.killDialerAndDeletePhenotypeCache;

import android.content.Context;

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

    public void reloadDBBooleanFlags() {
        mDBBooleanFlags.clear();
        String[] tables = {"Flags", "FlagOverrides"};
        for (String table : tables) {
            JSONArray queryResult = execPhenotypeQuery(
                    mContext,
                    String.format(
                            "SELECT DISTINCT name, boolVal FROM %s WHERE packageName = '%s' AND user = '' AND boolVal != 'NULL'",
                            table,
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
    }

    public void reloadDBStringFlags() {
        mDBStringFlags.clear();
        String[] tables = {"Flags", "FlagOverrides"};
        for (String table : tables) {
            JSONArray queryResult = execPhenotypeQuery(
                    mContext,
                    String.format(
                            "SELECT DISTINCT name, stringVal FROM %s WHERE packageName = '%s' AND user = '' AND stringVal != 'NULL'",
                            table,
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
        for (String user : mDBUsers)
            execPhenotypeQuery(
                    mContext,
                    String.format(
                            "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, boolVal, committed) VALUES ('%s', 0, '%s', '%s', '%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            (value ? '1' : '0')
                    )
            );
    }

    public void updateDBFlag(String flag, String value) {
        mDBStringFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(
                mContext,
                String.format(
                        "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                        DIALER_PACKAGE_NAME,flag.replace("'", "\\'")
                )
        );
        for (String user : mDBUsers)
            execPhenotypeQuery(
                    mContext,
                    String.format(
                            "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, stringVal, committed) VALUES ('%s', 0, '%s', '%s', '%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            value.replace("'", "\\'")
                    )
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
        for (String user : mDBUsers)
            execPhenotypeQuery(
                    mContext,
                    String.format(
                            "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, extensionVal, committed) VALUES ('%s', 0, '%s', '%s', X'%s', 0)",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'"),
                            user.replace("'", "\\'"),
                            byteArrayToHexString(value)
                    )
            );
    }

    public void deleteFlagOverrides(String... flags) {
        for (String flag : flags) {
            execPhenotypeQuery(
                    mContext,
                    String.format(
                            "DELETE FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'")
                    )
            );
            // Updating internal singleton cached flags
            try {
                JSONArray queryResult = execPhenotypeQuery(
                        mContext,
                        String.format(
                                "SELECT boolVal, stringVal FROM Flags WHERE packageName = '%s' AND user = '' AND name = '%s'",
                                DIALER_PACKAGE_NAME,
                                flag.replace("'", "\\'")
                        )
                );
                if (queryResult.length() > 0) {
                    JSONObject flagValues = queryResult.getJSONObject(0);
                    if (!flagValues.isNull("boolVal"))
                        mDBBooleanFlags.put(flag, flagValues.getBoolean(flag));
                    else if (!flagValues.isNull("stringVal"))
                        mDBStringFlags.put(flag, flagValues.getString(flag));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
        for (String flag : flags) {
            JSONArray queryResult = execPhenotypeQuery(
                    mContext,
                    String.format(
                            "SELECT name FROM FlagOverrides WHERE packageName = '%s' AND name = '%s'",
                            DIALER_PACKAGE_NAME,
                            flag.replace("'", "\\'")
                    )
            );
            if (queryResult.length() < 1)
                return false;
        }
        return true;
    }
}