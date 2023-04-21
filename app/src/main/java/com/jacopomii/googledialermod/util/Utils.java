package com.jacopomii.googledialermod.util;

import static com.jacopomii.googledialermod.data.Constants.VENDING_ANDROID_PACKAGE_NAME;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.jacopomii.googledialermod.BuildConfig;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.data.Version;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static void openGooglePlay(Context context, String googlePlayLink) {
        try {
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayLink));
            appStoreIntent.setPackage(VENDING_ANDROID_PACKAGE_NAME);
            context.startActivity(appStoreIntent);
        } catch (ActivityNotFoundException exception) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayLink)));
        }
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

    /**
     * This method generates strings used for IN queries.
     * It creates string containing "?" characters repeated {@code size} times and separated by ",".
     *
     * @param size size of the items.
     * @return IN query string of the form ?,?,?,?.
     */
    public static String createInQueryString(int size) {
        StringBuilder stringBuilder = new StringBuilder();
        String separator = "";
        for (int i = 0; i < size; i++) {
            stringBuilder.append(separator);
            stringBuilder.append("?");
            separator = ",";
        }
        return stringBuilder.toString();
    }
}
