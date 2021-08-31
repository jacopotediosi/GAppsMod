package com.jacopomii.googledialermod;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllSwitchesFragment extends Fragment {
    View mView;
    private RecyclerView mRecyclerView;
    private AllSwitchesRecyclerViewAdapter mAllSwitchesRecyclerViewAdapter;
    private List<SwitchRowItem> mLstSwitch = new ArrayList<>();

    public AllSwitchesFragment() {}

    // TODO filter all / only enabled / only disabled

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

        MenuItem deleteIcon = menu.findItem(R.id.delete_icon);

        MenuItem searchIcon = menu.findItem(R.id.search_icon);
        SearchView searchView = (SearchView) searchIcon.getActionView();
        searchIcon.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                deleteIcon.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getActivity().invalidateOptionsMenu();
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
                mAllSwitchesRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        DBFlagsSingleton.getInstance(getContext()).deleteAllFlagOverrides();
                        refreshAdapter();
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshAdapter() {
        mLstSwitch.clear();

        for (Map.Entry<String, Boolean> flag : DBFlagsSingleton.getInstance(getActivity()).getDBFlags().entrySet()) {
            mLstSwitch.add(new SwitchRowItem(flag.getKey(), flag.getValue()));
        }

        if (mAllSwitchesRecyclerViewAdapter != null) {
            mAllSwitchesRecyclerViewAdapter.notifyDataSetChanged();
        }
    }
}