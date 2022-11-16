package com.jacopomii.googledialermod;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.topjohnwu.superuser.Shell;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = "Utils";

    public static boolean checkIsDialerInstalled(Context context) {
        try {
            context.getPackageManager().getApplicationInfo("com.google.android.dialer", 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return Shell.cmd("test -d /data/data/com.google.android.dialer").exec().isSuccess();
    }

    public static boolean checkIsPhenotypeDBInstalled() {
        return Shell.cmd("test -f /data/data/com.google.android.gms/databases/phenotype.db").exec().isSuccess();
    }

    public static void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        inputStream.close();
        outputStream.flush();
        outputStream.close();
    }

    public static JSONArray execPhenotypeQuery(Context context, String query) {
        JSONArray result = null;
        try {
            String query_result = String.join("", Shell.cmd(
                    context.getApplicationInfo().dataDir +
                            "/sqlite3 -batch -json /data/data/com.google.android.gms/databases/phenotype.db " +
                            "\"" + query + ";\"").exec().getOut());
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

    public static void killDialerAndDeletePhenotypeCache() {
        Shell.cmd("am kill all com.google.android.dialer; rm -rf /data/data/com.google.android.dialer/files/phenotype").exec();
    }

    public static void deleteCallrecordingpromptFolder() {
        Shell.cmd("rm -rf /data/data/com.google.android.dialer/files/callrecordingprompt").exec();
    }

    public static void revertAllMods(Context context) {
        DBFlagsSingleton.getInstance(context).deleteAllFlagOverrides();
        deleteCallrecordingpromptFolder();
    }

    public static void checkIsLatestGithubVersion(Context context) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(
            new JsonObjectRequest(
                Request.Method.GET,
                context.getString(R.string.github_api_link) + "/releases/latest",
                null,
                response -> {
                    try {
                        Version actualVersion = new Version(BuildConfig.VERSION_NAME);
                        Version fetchedVersion = new Version(response.getString("tag_name").substring(1));

                        if (actualVersion.compareTo(fetchedVersion) < 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);

                            builder.setMessage(R.string.new_version_alert)
                                    .setNeutralButton(R.string.github, null)
                                    .setPositiveButton(android.R.string.ok, null);

                            AlertDialog alert = builder.create();

                            alert.setOnShowListener(dialogInterface -> alert.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                                    view ->
                                            context.startActivity(
                                                    new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_link)+"/releases"))
                                            )
                                    )
                            );

                            alert.show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Utils", "checkIsLatestGithubVersion: HTTP request failed")
            )
        );
    }
}
