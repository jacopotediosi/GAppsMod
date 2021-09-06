package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Utils.revertAllMods;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllSwitchesFragment extends Fragment {
    View mView;
    private RecyclerView mRecyclerView;
    private AllSwitchesRecyclerViewAdapter mAllSwitchesRecyclerViewAdapter;
    private final List<SwitchRowItem> mLstSwitch = new ArrayList<>();

    public AllSwitchesFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshAdapter();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.all_switches_fragment, container, false);
        mRecyclerView = mView.findViewById(R.id.recyclerView);
        mAllSwitchesRecyclerViewAdapter = new AllSwitchesRecyclerViewAdapter(getContext(), mLstSwitch);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAllSwitchesRecyclerViewAdapter);
        return mView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.all_switches_menu, menu);

        FragmentActivity parentActivity = requireActivity();
        RadioGroup radioGroupSearch = parentActivity.findViewById(R.id.radiogroup_search);

        MenuItem infoIcon = menu.findItem(R.id.menu_info_icon);
        MenuItem deleteIcon = menu.findItem(R.id.menu_delete_icon);
        MenuItem searchIcon = menu.findItem(R.id.menu_search_icon);

        SearchView searchView = (SearchView) searchIcon.getActionView();

        radioGroupSearch.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButtonChecked = parentActivity.findViewById(checkedId);
            if (radioButtonChecked.isChecked()) {
                try {
                    JSONObject filterConfig = new JSONObject();

                    filterConfig.put("key", searchView.getQuery().toString());

                    int radioGroupSearchCheckedButtonId = radioButtonChecked.getId();
                    if (radioGroupSearchCheckedButtonId == R.id.radiobutton_enabled)
                        filterConfig.put("mode", "enabled_only");
                    else if (radioGroupSearchCheckedButtonId == R.id.radiobutton_disabled)
                        filterConfig.put("mode", "disabled_only");
                    else
                        filterConfig.put("mode", "all");

                    mAllSwitchesRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        searchIcon.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                infoIcon.setVisible(false);
                deleteIcon.setVisible(false);
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
                try {
                    JSONObject filterConfig = new JSONObject();

                    filterConfig.put("key", newText);

                    int radioGroupSearchCheckedButtonId = radioGroupSearch.getCheckedRadioButtonId();
                    if (radioGroupSearchCheckedButtonId == R.id.radiobutton_enabled)
                        filterConfig.put("mode", "enabled_only");
                    else if (radioGroupSearchCheckedButtonId == R.id.radiobutton_disabled)
                        filterConfig.put("mode", "disabled_only");
                    else
                        filterConfig.put("mode", "all");

                    mAllSwitchesRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_info_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setPositiveButton(android.R.string.ok, null)
                    .setView(R.layout.information_dialog);
            AlertDialog alert = builder.create();
            alert.show();

            // Links aren't clickable workaround
            TextView whatIsItExplanation = alert.findViewById(R.id.what_is_it_explanation);
            if (whatIsItExplanation != null)
                whatIsItExplanation.setMovementMethod(LinkMovementMethod.getInstance());
            TextView madeWithLove = alert.findViewById(R.id.made_with_love_by_jacopo_tediosi);
            if (madeWithLove != null)
                madeWithLove.setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        } else if (item.getItemId() == R.id.menu_delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        revertAllMods(requireContext());
                        refreshAdapter();
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void refreshAdapter() {
        mLstSwitch.clear();

        for (Map.Entry<String, Boolean> flag : DBFlagsSingleton.getInstance(getActivity()).getDBBooleanFlags().entrySet()) {
            mLstSwitch.add(new SwitchRowItem(flag.getKey(), flag.getValue()));
        }

        mAllSwitchesRecyclerViewAdapter = new AllSwitchesRecyclerViewAdapter(getContext(), mLstSwitch);

        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mAllSwitchesRecyclerViewAdapter);
        }
    }
}