package com.jacopomii.gappsmod.ui.fragment;

import static com.jacopomii.gappsmod.util.Utils.showSelectPackageDialog;

import android.app.Activity;
import android.graphics.Rect;
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
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.databinding.FragmentBooleanModsBinding;
import com.jacopomii.gappsmod.ui.activity.MainActivity;
import com.jacopomii.gappsmod.ui.adapter.BooleanModsRecyclerViewAdapter;
import com.jacopomii.gappsmod.ui.view.FilterableSearchView;
import com.l4digital.fastscroll.FastScrollRecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

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

        // Initialize the selectPackageDialogOpened
        AtomicBoolean selectPackageDialogOpened = new AtomicBoolean(false);

        // Select package onClick
        selectPackage.setOnClickListener(v -> {
            // If the select package dialog isn't already opened
            if (!selectPackageDialogOpened.get()) {
                // Set the selectPackageDialogOpened to true
                selectPackageDialogOpened.set(true);

                // Show the select package dialog
                showSelectPackageDialog(
                        getContext(),
                        mCoreRootServiceIpc,
                        item -> {
                            // The item received by the listener here is the Phenotype package name chosen by the user

                            // Update the select package textview
                            selectPackage.setText((String) item);

                            // Update the selectPackageRecyclerView adapter
                            mFlagsRecyclerViewAdapter.selectPhenotypePackageName((String) item);

                            // Set the selectPackageDialogOpened to false
                            selectPackageDialogOpened.set(false);
                        },
                        dialog -> {
                            // Set the selectPackageDialogOpened to false dismissing the dialog
                            selectPackageDialogOpened.set(false);
                        });
            }
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

        // Set flagsRecyclerView items padding
        flagsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int padding = (int) getResources().getDimension(R.dimen.margin_generic);

                int itemPosition = parent.getChildAdapterPosition(view);

                if (itemPosition == 0)
                    outRect.top = padding;
                else if (itemPosition == mFlagsRecyclerViewAdapter.getItemCount() - 1)
                    outRect.bottom = padding;

                outRect.left = padding;
                outRect.right = padding;
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

                MenuItem menuSearchIcon = menu.findItem(R.id.menu_search_icon);

                FilterableSearchView filterableSearchView = (FilterableSearchView) menuSearchIcon.getActionView();
                filterableSearchView.setQueryHint(getString(R.string.search_by_flag));
                filterableSearchView.setFiltersContainer(mBinding.filtersContainer);

                RadioGroup filtersStatusRadioGroup = mBinding.filtersStatusRadioGroup;

                filtersStatusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    RadioButton radioButtonChecked = mBinding.getRoot().findViewById(checkedId);
                    if (radioButtonChecked.isChecked())
                        applyFlagsFilter(filterableSearchView.getQuery());
                });

                menuSearchIcon.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        filtersStatusRadioGroup.check(mBinding.filtersStatusRadioButtonAll.getId());
                        return true;
                    }
                });
                filterableSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

            int radioGroupSearchCheckedButtonId = mBinding.filtersStatusRadioGroup.getCheckedRadioButtonId();
            if (radioGroupSearchCheckedButtonId == mBinding.filtersStatusRadioButtonEnabled.getId())
                filterConfig.put("mode", "enabled_only");
            else if (radioGroupSearchCheckedButtonId == mBinding.filtersStatusRadioButtonDisabled.getId())
                filterConfig.put("mode", "disabled_only");
            else
                filterConfig.put("mode", "all");

            mFlagsRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}