package com.jacopomii.gappsmod.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.data.BooleanFlag;
import com.jacopomii.gappsmod.databinding.SwitchCardBinding;
import com.jacopomii.gappsmod.ui.view.ProgrammaticMaterialSwitch;
import com.l4digital.fastscroll.FastScroller;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
public class BooleanModsRecyclerViewAdapter extends RecyclerView.Adapter<BooleanModsRecyclerViewAdapter.ViewHolder> implements Filterable, FastScroller.SectionIndexer {
    private final Context mContext;

    private List<BooleanFlag> mFlagsList = new ArrayList<>();
    private List<BooleanFlag> mFlagsListFiltered = new ArrayList<>();
    private String mPhenotypePackageName = null;
    private CharSequence mLastFilterPerformed = null;

    private final ICoreRootService mCoreRootServiceIpc;

    public BooleanModsRecyclerViewAdapter(Context context, ICoreRootService coreRootServiceIpc) {
        mContext = context;
        mCoreRootServiceIpc = coreRootServiceIpc;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void selectPhenotypePackageName(String phenotypePackageName) {
        mPhenotypePackageName = phenotypePackageName;

        try {
            mFlagsList = new ArrayList<>();
            TreeMap<String, Boolean> map = new TreeMap<String, Boolean>(mCoreRootServiceIpc.phenotypeDBGetBooleanFlagsOrOverridden(phenotypePackageName));
            for (Map.Entry<String, Boolean> flag : map.entrySet())
                mFlagsList.add(new BooleanFlag(flag.getKey(), flag.getValue()));

            if (mLastFilterPerformed != null) {
                getFilter().filter(mLastFilterPerformed);
            } else {
                mFlagsListFiltered = mFlagsList;
                notifyDataSetChanged();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Initialize binding and viewHolder
        SwitchCardBinding binding = SwitchCardBinding.inflate(LayoutInflater.from(mContext), parent, false);
        ViewHolder viewHolder = new ViewHolder(binding);

        // Set setOnCheckedChangeListener on list items
        viewHolder.mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int position = viewHolder.getAdapterPosition();

            mFlagsListFiltered.get(position).setFlagValue(isChecked);
            try {
                mCoreRootServiceIpc.phenotypeDBOverrideBooleanFlag(mPhenotypePackageName, mFlagsListFiltered.get(position).getFlagName(), isChecked);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            notifyItemChanged(position);
        });

        // Return viewHolder
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Update switch text
        holder.mTextView.setText(mFlagsListFiltered.get(position).getFlagName());

        // Update the switch checked status without triggering any existing listener
        holder.mSwitch.setCheckedProgrammatically(mFlagsListFiltered.get(position).getFlagValue());
    }

    @Override
    public int getItemCount() {
        return mFlagsListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                mLastFilterPerformed = charSequence;

                try {
                    JSONObject filterConfig = new JSONObject(charSequence.toString());
                    String key = filterConfig.getString("key");
                    String mode = filterConfig.getString("mode");

                    List<BooleanFlag> flagsListFiltered = new ArrayList<>();
                    for (BooleanFlag booleanFlag : mFlagsList) {
                        if (booleanFlag.getFlagName().toLowerCase().contains(key.toLowerCase())) {
                            boolean flagValue = booleanFlag.getFlagValue();
                            if (mode.equals("all") || (mode.equals("enabled_only") && flagValue) || (mode.equals("disabled_only") && !flagValue))
                                flagsListFiltered.add(booleanFlag);
                        }
                    }
                    mFlagsListFiltered = flagsListFiltered;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFlagsListFiltered;
                filterResults.count = mFlagsListFiltered.size();
                return filterResults;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFlagsListFiltered = (List<BooleanFlag>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public CharSequence getSectionText(int position) {
        return mFlagsListFiltered.get(position).getFlagName().substring(0, 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final ProgrammaticMaterialSwitch mSwitch;

        public ViewHolder(SwitchCardBinding binding) {
            super(binding.getRoot());
            mTextView = binding.switchCardTextview;
            mSwitch = binding.switchCardSwitch;
        }
    }
}
