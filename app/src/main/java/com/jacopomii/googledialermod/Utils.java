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

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = "Utils";

    public static boolean checkIsDeviceRooted() {
        return runSuWithCmd("echo 1").getInputStreamLog().equals("1");
    }

    public static boolean checkIsDialerInstalled(Context context) {
        try {
            context.getPackageManager().getApplicationInfo("com.google.android.dialer", 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return runSuWithCmd("test -d /data/data/com.google.android.dialer; echo $?").getInputStreamLog().equals("0");
    }

    public static boolean checkIsPhenotypeDBInstalled() {
        return runSuWithCmd("test -f /data/data/com.google.android.gms/databases/phenotype.db; echo $?").getInputStreamLog().equals("0");
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
            String query_result = runSuWithCmd(
                    context.getApplicationInfo().dataDir +
                            "/sqlite3 -batch -json /data/data/com.google.android.gms/databases/phenotype.db " +
                            "\"" + query + ";\"").getInputStreamLog();
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
        runSuWithCmd("am kill all com.google.android.dialer; rm -r /data/data/com.google.android.dialer/files/phenotype");
    }

    public static void deleteCallrecordingpromptFolder() {
        runSuWithCmd("rm -r /data/data/com.google.android.dialer/files/callrecordingprompt");
    }

    public static void revertAllMods(Context context) {
        DBFlagsSingleton.getInstance(context).deleteAllFlagOverrides();
        deleteCallrecordingpromptFolder();
    }

    public static StreamLogs runSuWithCmd(String cmd) {
        DataOutputStream outputStream;
        InputStream inputStream;
        InputStream errorStream;

        StreamLogs streamLogs = new StreamLogs();
        streamLogs.setOutputStreamLog(cmd);

        try {
            Process su = Runtime.getRuntime().exec("su --mount-master");
            outputStream = new DataOutputStream(su.getOutputStream());
            inputStream = su.getInputStream();
            errorStream = su.getErrorStream();
            outputStream.writeBytes(cmd + "\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            streamLogs.setInputStreamLog(readFully(inputStream));
            streamLogs.setErrorStreamLog(readFully(errorStream));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "runSuWithCmd: " + streamLogs.getStreamLogsWithLabels());

        return streamLogs;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
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
