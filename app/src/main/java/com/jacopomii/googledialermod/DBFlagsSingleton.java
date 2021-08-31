package com.jacopomii.googledialermod;

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
    private Map<String, Boolean> mDBFlags = new TreeMap<>();
    ArrayList<String> mDBUsers = new ArrayList<>();
    private Context mContext;

    private DBFlagsSingleton(Context context) {
        mContext = context;
        reloadDB();
    }

    public static DBFlagsSingleton getInstance(Context context) {
        if (mUniqueInstance == null)
            mUniqueInstance = new DBFlagsSingleton(context);
        return mUniqueInstance;
    }

    public boolean getDBFlag(String flag) {
        return mDBFlags.get(flag);
    }

    public Map<String, Boolean> getDBFlags() {
        return mDBFlags;
    }

    public void reloadDB() {
        reloadDBUsers();
        reloadDBFlags();
    }

    private void reloadDBUsers() {
        mDBUsers.clear();
        JSONArray users = execPhenotypeQuery(mContext, "SELECT DISTINCT user FROM Flags WHERE packageName = 'com.google.android.dialer'");
        for (int i=0; i < users.length(); i++) {
            try {
                String user = users.getJSONObject(i).getString("user");
                mDBUsers.add(user);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void reloadDBFlags() {
        mDBFlags.clear();
        String[] tables = {"Flags", "FlagOverrides"};
        for (String table : tables) {
            JSONArray query_result = execPhenotypeQuery(mContext, "SELECT DISTINCT name,boolVal FROM " + table + " WHERE packageName = 'com.google.android.dialer' AND user = '' AND boolVal != 'NULL'");
            for (int i=0; i < query_result.length(); i++) {
                try {
                    JSONObject flag = query_result.getJSONObject(i);
                    mDBFlags.put(flag.getString("name"), flag.getInt("boolVal")!=0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateDBFlag(String flag, boolean value) {
        mDBFlags.put(flag, value);
        killDialerAndDeletePhenotypeCache();
        execPhenotypeQuery(mContext, "DELETE FROM FlagOverrides WHERE packageName = 'com.google.android.dialer' AND name = '" + flag.replace("'", "\\'") + "'");
        for (String user : mDBUsers)
            execPhenotypeQuery(mContext, "INSERT OR REPLACE INTO FlagOverrides (packageName, flagType, name, user, boolVal, committed) VALUES ('com.google.android.dialer', 0, '" + flag.replace("'", "\\'") + "', '" + user.replace("'", "\\'") + "', " + (value ? 1 : 0) + ", 0)");
    }

    public boolean areAllFlagsTrue(String... flags) {
        for (String flag : flags)
            if (!getDBFlag(flag))
                return false;
        return true;
    }

    public void deleteAllFlagOverrides() {
        execPhenotypeQuery(mContext, "DELETE FROM FlagOverrides WHERE packageName = 'com.google.android.dialer'");
        reloadDB();
    }
}