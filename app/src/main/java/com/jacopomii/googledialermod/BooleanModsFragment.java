package com.jacopomii.googledialermod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class BooleanModsFragment extends Fragment {
    View mView;
    private RecyclerView mRecyclerView;
    private BooleanModsRecyclerViewAdapter mBooleanModsRecyclerViewAdapter;
    private final List<SwitchRowItem> mLstSwitch = new ArrayList<>();

    public BooleanModsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.boolean_mods_fragment, container, false);
        mRecyclerView = mView.findViewById(R.id.recyclerView);
        mBooleanModsRecyclerViewAdapter = new BooleanModsRecyclerViewAdapter(getContext(), mLstSwitch);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mBooleanModsRecyclerViewAdapter);
        return mView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);

        FragmentActivity parentActivity = requireActivity();
        RadioGroup radioGroupSearch = parentActivity.findViewById(R.id.radio_group_search);

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

                    mBooleanModsRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        searchIcon.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                for (int i=0; i<menu.size(); i++) {
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

                    mBooleanModsRecyclerViewAdapter.getFilter().filter(filterConfig.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAdapter();
    }

    public void refreshAdapter() {
        mLstSwitch.clear();

        for (Map.Entry<String, Boolean> flag : DBFlagsSingleton.getInstance(getActivity()).getDBBooleanFlags().entrySet()) {
            mLstSwitch.add(new SwitchRowItem(flag.getKey(), flag.getValue()));
        }

        mBooleanModsRecyclerViewAdapter = new BooleanModsRecyclerViewAdapter(getContext(), mLstSwitch);

        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mBooleanModsRecyclerViewAdapter);
        }
    }
}