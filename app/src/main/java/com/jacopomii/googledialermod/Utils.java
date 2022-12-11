package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.Constants.VENDING_PACKAGE_NAME;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;

public class Utils {
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

    public static void openGooglePlay(Context context, String googlePlayLink) {
        try {
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayLink));
            appStoreIntent.setPackage(VENDING_PACKAGE_NAME);
            context.startActivity(appStoreIntent);
        } catch (ActivityNotFoundException exception) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayLink)));
        }
    }

    public static void revertAllMods(Context context) {
        DBFlagsSingleton.getInstance(context).deleteAllFlagOverrides();
        deleteCallrecordingpromptFolder();
    }

    public static void deleteCallrecordingpromptFolder() {
        Shell.cmd(
                String.format(
                        "rm -rf %s",
                        DIALER_CALLRECORDINGPROMPT
                )
        ).exec();
    }

    public static boolean checkUpdateAvailable(Context context) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        requestQueue.add(
                new JsonObjectRequest(
                        Request.Method.GET,
                        context.getString(R.string.github_api_link) + "/releases/latest",
                        null,
                        future,
                        future
                )
        );

        try {
            JSONObject response = future.get();
            Version actualVersion = new Version(BuildConfig.VERSION_NAME);
            Version fetchedVersion = new Version(response.getString("tag_name").substring(1));
            if (actualVersion.compareTo(fetchedVersion) < 0)
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
