package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.Constants.DIALER_DATA_DATA;
import static com.jacopomii.googledialermod.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.Constants.DIALER_PHENOTYPE_CACHE;
import static com.jacopomii.googledialermod.Constants.PHENOTYPE_DB;

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
import java.util.Formatter;

public class Utils {
    private static final String TAG = "Utils";

    public static boolean checkIsDialerInstalled(Context context) {
        try {
            context.getPackageManager().getApplicationInfo(DIALER_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return Shell.cmd(String.format("test -d %s", DIALER_DATA_DATA)).exec().isSuccess();
    }

    public static boolean checkIsPhenotypeDBInstalled() {
        return Shell.cmd(String.format("test -f %s", PHENOTYPE_DB)).exec().isSuccess();
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

    public static String byteArrayToHexString(byte[] byteArray) {
        Formatter formatter = new Formatter();
        for (byte b : byteArray)
            formatter.format("%02x", b);
        return formatter.toString();
    }

    public static JSONArray execPhenotypeQuery(Context context, String query) {
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

    public static void killDialerAndDeletePhenotypeCache() {
        Shell.cmd(
                String.format(
                        "am kill all com.google.android.dialer; rm -rf %s",
                        DIALER_PHENOTYPE_CACHE
                )
        ).exec();
    }

    public static void deleteCallrecordingpromptFolder() {
        Shell.cmd(
                String.format(
                        "rm -rf %s",
                        DIALER_CALLRECORDINGPROMPT
                )
        ).exec();
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
