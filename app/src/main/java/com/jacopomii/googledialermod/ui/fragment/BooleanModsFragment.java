package com.jacopomii.googledialermod.ui.fragment;

import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.FragmentBooleanModsBinding;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.jacopomii.googledialermod.ui.adapter.BooleanModsRecyclerViewAdapter;
import com.jacopomii.googledialermod.ui.viewmodel.SwitchCardViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"unchecked"})
public class BooleanModsFragment extends Fragment {
    private BooleanModsRecyclerViewAdapter mBooleanModsRecyclerViewAdapter;
    private final List<SwitchCardViewModel> mLstSwitch = new ArrayList<>();
    private FragmentBooleanModsBinding binding;

    private ICoreRootService coreRootServiceIpc;

    public BooleanModsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity)
            coreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
        else
            throw new RuntimeException("SuggestedModsFragment can be attached only to the MainActivity");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBooleanModsBinding.inflate(getLayoutInflater());

        try {
            TreeMap<String, Boolean> map = new TreeMap<String, Boolean>(coreRootServiceIpc.phenotypeDBGetBooleanFlags(DIALER_PACKAGE_NAME));
            for (Map.Entry<String, Boolean> flag : map.entrySet())
                mLstSwitch.add(new SwitchCardViewModel(flag.getKey(), flag.getValue()));

            mBooleanModsRecyclerViewAdapter = new BooleanModsRecyclerViewAdapter(getActivity(), mLstSwitch);

            RecyclerView recyclerView = binding.recyclerView;
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(mBooleanModsRecyclerViewAdapter);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);

        RadioGroup radioGroupSearch = binding.radioGroupSearch;

        MenuItem searchIcon = menu.findItem(R.id.menu_search_icon);

        SearchView searchView = (SearchView) searchIcon.getActionView();
        searchView.setQueryHint(getString(R.string.search));

        radioGroupSearch.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButtonChecked = binding.getRoot().findViewById(checkedId);
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
}