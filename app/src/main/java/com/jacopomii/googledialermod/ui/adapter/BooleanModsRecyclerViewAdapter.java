package com.jacopomii.googledialermod.ui.adapter;

import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;

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

import com.jacopomii.googledialermod.data.BooleanFlag;
import com.jacopomii.googledialermod.databinding.SwitchCardBinding;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.jacopomii.googledialermod.ui.view.ProgrammaticMaterialSwitch;
import com.l4digital.fastscroll.FastScroller;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BooleanModsRecyclerViewAdapter extends RecyclerView.Adapter<BooleanModsRecyclerViewAdapter.BooleanModsViewHolder> implements Filterable, FastScroller.SectionIndexer {
    private final Context mContext;
    private final List<BooleanFlag> mFlagsList;
    private List<BooleanFlag> mFlagsListFiltered;

    public BooleanModsRecyclerViewAdapter(Context context, List<BooleanFlag> flagList) {
        if (context instanceof MainActivity) {
            mContext = context;
            mFlagsList = flagList;
            mFlagsListFiltered = flagList;
        } else {
            throw new RuntimeException("BooleanModsRecyclerViewAdapter can be attached only to the MainActivity");
        }
    }

    @NonNull
    @Override
    public BooleanModsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SwitchCardBinding binding = SwitchCardBinding.inflate(LayoutInflater.from(mContext), parent, false);
        return new BooleanModsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BooleanModsViewHolder holder, int position) {
        // Update switch text
        holder.mTextView.setText(mFlagsListFiltered.get(position).getFlagName());

        // Update the switch checked status without triggering any existing listener
        holder.mSwitch.setCheckedProgrammatically(mFlagsListFiltered.get(position).getFlagValue());

        // Set the new onCheckedChange listener
        holder.mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mFlagsListFiltered.get(position).setFlagValue(isChecked);
            try {
                ((MainActivity) mContext).getCoreRootServiceIpc().phenotypeDBOverrideBooleanFlag(DIALER_PACKAGE_NAME, mFlagsListFiltered.get(holder.getAdapterPosition()).getFlagName(), isChecked);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            notifyItemChanged(position);
        });
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

            @SuppressWarnings("unchecked")
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

    public static class BooleanModsViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final ProgrammaticMaterialSwitch mSwitch;

        public BooleanModsViewHolder(SwitchCardBinding binding) {
            super(binding.getRoot());
            mTextView = binding.switchCardTextview;
            mSwitch = binding.switchCardSwitch;
        }
    }
}
