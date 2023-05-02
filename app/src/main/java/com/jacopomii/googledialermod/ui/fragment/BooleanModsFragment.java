package com.jacopomii.googledialermod.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.DialogSelectPackageBinding;
import com.jacopomii.googledialermod.databinding.FragmentBooleanModsBinding;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.jacopomii.googledialermod.ui.adapter.BooleanModsRecyclerViewAdapter;
import com.jacopomii.googledialermod.ui.adapter.SelectPackageRecyclerViewAdapter;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

public class BooleanModsFragment extends Fragment {
    private FragmentBooleanModsBinding mBinding;

    private BooleanModsRecyclerViewAdapter mFlagsRecyclerViewAdapter;

    private ICoreRootService mCoreRootServiceIpc;

    public BooleanModsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity)
            mCoreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
        else
            throw new RuntimeException("SuggestedModsFragment can be attached only to the MainActivity");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // View bindings
        mBinding = FragmentBooleanModsBinding.inflate(getLayoutInflater());


        // Setup menu
        setupMenu();


        // Select package
        TextView selectPackage = mBinding.selectPackage;

        // Select package OnClick
        selectPackage.setOnClickListener(v -> {
            // Dialog builder
            MaterialAlertDialogBuilder selectPackageDialogBuilder = new MaterialAlertDialogBuilder(requireContext());

            // Inflate dialog layout
            DialogSelectPackageBinding dialogSelectPackageBinding = DialogSelectPackageBinding.inflate(getLayoutInflater());
            selectPackageDialogBuilder.setView(dialogSelectPackageBinding.getRoot());

            // Create dialog
            AlertDialog selectPackageDialog = selectPackageDialogBuilder.create();

            // Set dialog custom height and width
            selectPackageDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            // Dialog components
            SearchView selectPackageSearchView = dialogSelectPackageBinding.searchview;
            FastScrollRecyclerView selectPackageRecyclerView = dialogSelectPackageBinding.recyclerview;

            // Initialize the dialog adapter
            SelectPackageRecyclerViewAdapter selectPackageRecyclerViewAdapter = new SelectPackageRecyclerViewAdapter(
                    getContext(),
                    mCoreRootServiceIpc,
                    item -> {
                        // Dialog onItemClickListener
                        // The item received by the listener here is the Phenotype package name chosen by the user

                        // Update the select package textview
                        selectPackage.setText((String) item);

                        // Update the selectPackageRecyclerView adapter
                        mFlagsRecyclerViewAdapter.selectPhenotypePackageName((String) item);

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
            selectPackageRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
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
                    selectPackageRecyclerViewAdapter.getFilter().filter(newText);
                    return false;
                }
            });

            // Show dialog
            selectPackageDialog.show();
        });


        // Flags recyclerview
        FastScrollRecyclerView flagsRecyclerView = mBinding.recyclerview;

        // Initialize the flagsRecyclerView adapter
        mFlagsRecyclerViewAdapter = new BooleanModsRecyclerViewAdapter(getContext(), mCoreRootServiceIpc);

        // Disable fast scroll if the flagsRecyclerView is empty or changes to empty
        flagsRecyclerView.setFastScrollEnabled(mFlagsRecyclerViewAdapter.getItemCount() != 0);
        mFlagsRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                flagsRecyclerView.setFastScrollEnabled(mFlagsRecyclerViewAdapter.getItemCount() != 0);
            }
        });

        // Set the flagsRecyclerView LayoutManager and Adapter
        flagsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        flagsRecyclerView.setAdapter(mFlagsRecyclerViewAdapter);


        // Return the fragment view
        return mBinding.getRoot();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.search_menu, menu);

                MenuItem searchIcon = menu.findItem(R.id.menu_search_icon);

                SearchView searchView = (SearchView) searchIcon.getActionView();
                searchView.setQueryHint(getString(R.string.search_by_flag));

                RadioGroup radioGroupSearch = mBinding.radioGroupSearch;

                radioGroupSearch.setOnCheckedChangeListener((group, checkedId) -> {
                    RadioButton radioButtonChecked = mBinding.getRoot().findViewById(checkedId);
                    if (radioButtonChecked.isChecked())
                        applyFlagsFilter(searchView.getQuery());
                });

                searchIcon.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        for (int i = 0; i < menu.size(); i++) {
                            MenuItem itemToHide = menu.getItem(i);
                            if (itemToHide.getItemId() != R.id.menu_search_icon)
                                itemToHide.setVisible(false);
                        }
                        radioGroupSearch.setVisibility(View.VISIBLE);
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        radioGroupSearch.check(R.id.radiobutton_all);
                        radioGroupSearch.setVisibility(View.GONE);
                        requireActivity().invalidateOptionsMenu();
                        return true;
                    }
                });
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        applyFlagsFilter(newText);
                        return false;
                    }
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void applyFlagsFilter(CharSequence query) {
        try {
            JSONObject filterConfig = new JSONObject();

            filterConfig.put("key", query);

            int radioGroupSearchCheckedButtonId = mBinding.radioGroupSearch.getCheckedRadioButtonId();
            if (radioGroupSearchCheckedButtonId == R.id.radiobutton_enabled)
                filterConfig.put("mode", "enabled_only");
            else if (radioGroupSearchCheckedButtonId == R.id.radiobutton_disabled)
                filterConfig.put("mode", "disabled_only");
            else
                filterConfig.put("mode", "all");

            mFlagsRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}