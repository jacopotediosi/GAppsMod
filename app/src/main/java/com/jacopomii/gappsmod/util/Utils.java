package com.jacopomii.gappsmod.util;

import static com.jacopomii.gappsmod.data.Constants.VENDING_ANDROID_PACKAGE_NAME;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jacopomii.gappsmod.BuildConfig;
import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.data.Version;
import com.jacopomii.gappsmod.databinding.DialogSelectPackageBinding;
import com.jacopomii.gappsmod.ui.adapter.SelectPackageRecyclerViewAdapter;
import com.l4digital.fastscroll.FastScrollRecyclerView;

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

    /**
     * This method returns, given an {@code androidPackageName}, the label of the corresponding
     * application, or a localized string "Unknown" if the application is not installed.
     *
     * @param context            context.
     * @param androidPackageName the Android package name of the application to get the label for.
     * @return the application label if the application exists; The localized string
     * {@link R.string#unknown} otherwise.
     */
    public static String getApplicationLabelOrUnknown(Context context, String androidPackageName) {
        String applicationLabel = context.getString(R.string.unknown);

        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(androidPackageName, 0);
            if (applicationInfo != null)
                applicationLabel = (String) (packageManager.getApplicationLabel(applicationInfo));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return applicationLabel;
    }

    // Static variables for showSelectPackageDialog()
    private static CharSequence lastPackageSearched = null;
    private static Boolean lastPackageSearchedRemember = true;

    /**
     * Show the "Select Package" dialog, a custom view to select package names contained in the
     * Phenotype DB with search and fastscroll features.
     *
     * @param context             context.
     * @param coreRootServiceIpc  a {@code ICoreRootService} instance.
     * @param onItemClickListener an implementation of the {@link OnItemClickListener} interface,
     *                            to perform actions after the user has selected a package.
     *                            The received item is a string containing the selected Phenotype
     *                            (not Android) package name.
     */
    public static void showSelectPackageDialog(Context context, ICoreRootService coreRootServiceIpc, OnItemClickListener onItemClickListener, DialogInterface.OnDismissListener onDismissListener) {
        // Dialog builder
        MaterialAlertDialogBuilder selectPackageDialogBuilder = new MaterialAlertDialogBuilder(context);

        // Inflate dialog layout
        DialogSelectPackageBinding dialogSelectPackageBinding = DialogSelectPackageBinding.inflate(LayoutInflater.from(context));
        selectPackageDialogBuilder.setView(dialogSelectPackageBinding.getRoot());

        // Create dialog
        AlertDialog selectPackageDialog = selectPackageDialogBuilder.create();

        // Set dialog custom height and width
        selectPackageDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        selectPackageDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Set dialog onDismissListener
        selectPackageDialog.setOnDismissListener(onDismissListener);

        // Dialog components
        SearchView selectPackageSearchView = dialogSelectPackageBinding.searchview;
        FastScrollRecyclerView selectPackageRecyclerView = dialogSelectPackageBinding.recyclerview;
        CheckBox SelectPackageRememberCheckbox = dialogSelectPackageBinding.remembercheckbox;

        // Initialize the dialog adapter
        SelectPackageRecyclerViewAdapter selectPackageRecyclerViewAdapter = new SelectPackageRecyclerViewAdapter(context, coreRootServiceIpc, item -> {
            // Pass the received item to the caller onItemClickListener
            onItemClickListener.onItemClick(item);

            // Dismiss dialog
            selectPackageDialog.dismiss();
        });

        // Disable fast scroll if the selectPackageRecyclerView is empty or changes to empty
        selectPackageRecyclerView.setFastScrollEnabled(selectPackageRecyclerViewAdapter.getItemCount() != 0);
        selectPackageRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                selectPackageRecyclerView.setFastScrollEnabled(selectPackageRecyclerViewAdapter.getItemCount() != 0);
            }
        });

        // Set the dialog selectPackageRecyclerView LayoutManager and Adapter
        selectPackageRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        selectPackageRecyclerView.setAdapter(selectPackageRecyclerViewAdapter);

        // Add list dividers to the selectPackageRecyclerView
        selectPackageRecyclerView.addItemDecoration(new DividerItemDecoration(selectPackageRecyclerView.getContext(), DividerItemDecoration.VERTICAL));

        // Dialog filter
        selectPackageSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastPackageSearched = newText;
                selectPackageRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });

        // Remember last package searched
        SelectPackageRememberCheckbox.setChecked(lastPackageSearchedRemember);
        SelectPackageRememberCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            lastPackageSearchedRemember = isChecked;
            lastPackageSearched = selectPackageSearchView.getQuery();
        });
        if (lastPackageSearched != null && lastPackageSearchedRemember)
            selectPackageSearchView.setQuery(lastPackageSearched, true);

        // Show dialog
        selectPackageDialog.show();
    }
}
