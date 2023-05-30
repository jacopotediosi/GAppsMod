package com.jacopomii.gappsmod.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.data.BooleanFlag;
import com.jacopomii.gappsmod.databinding.SwitchCardBinding;
import com.jacopomii.gappsmod.ui.view.ProgrammaticMaterialSwitchView;
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
            TreeMap<String, List<Object>> map = new TreeMap<String, List<Object>>(mCoreRootServiceIpc.phenotypeDBGetBooleanFlagsOrOverridden(phenotypePackageName));
            for (Map.Entry<String, List<Object>> flag : map.entrySet()) {
                String flagName = flag.getKey();
                List<Object> flagData = flag.getValue();
                Boolean flagValue = (Boolean) flagData.get(0);
                Boolean flagOverriddenAndChanged = (Boolean) flagData.get(1);
                mFlagsList.add(new BooleanFlag(flagName, flagValue, flagOverriddenAndChanged));
            }

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
            int checkedPosition = viewHolder.getAdapterPosition();
            BooleanFlag checkedBooleanFlag = mFlagsListFiltered.get(checkedPosition);
            String checkedBooleanFlagName = checkedBooleanFlag.getFlagName();

            checkedBooleanFlag.setFlagValue(isChecked);
            try {
                mCoreRootServiceIpc.phenotypeDBOverrideBooleanFlag(mPhenotypePackageName, checkedBooleanFlagName, isChecked);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            checkedBooleanFlag.setFlagOverriddenAndChanged(!checkedBooleanFlag.getFlagOverriddenAndChanged());

            notifyItemChanged(checkedPosition);
        });

        // Return viewHolder
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the boolean flag
        BooleanFlag booleanFlag = mFlagsListFiltered.get(position);

        // Update switch text
        holder.mTextView.setText(booleanFlag.getFlagName());

        // Update the switch checked status without triggering any existing listener
        holder.mSwitch.setCheckedProgrammatically(booleanFlag.getFlagValue());

        // Change background color for cards containing overridden and changed flags
        TypedArray typedArray = mContext.getTheme().obtainStyledAttributes(R.styleable.ViewStyle);
        int colorSurface = typedArray.getColor(R.styleable.ViewStyle_colorSurface, Color.WHITE);
        int colorSurfaceVariant = typedArray.getColor(R.styleable.ViewStyle_colorSecondaryContainer, Color.LTGRAY);
        typedArray.recycle();
        int cardBackgroundColor = booleanFlag.getFlagOverriddenAndChanged() ? colorSurfaceVariant : colorSurface;
        ((MaterialCardView) holder.itemView).setCardBackgroundColor(cardBackgroundColor);
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
                    boolean enabled = filterConfig.getBoolean("enabled");
                    boolean disabled = filterConfig.getBoolean("disabled");
                    boolean changed = filterConfig.getBoolean("changed");
                    boolean unchanged = filterConfig.getBoolean("unchanged");

                    List<BooleanFlag> flagsListFiltered = new ArrayList<>();
                    for (BooleanFlag booleanFlag : mFlagsList) {
                        if (booleanFlag.getFlagName().toLowerCase().contains(key.toLowerCase())) {
                            boolean flagValue = booleanFlag.getFlagValue();
                            boolean flagChanged = booleanFlag.getFlagOverriddenAndChanged();
                            if (((enabled && flagValue) || (disabled && !flagValue)) && ((changed && flagChanged) || (unchanged && !flagChanged)))
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
        private final ProgrammaticMaterialSwitchView mSwitch;

        public ViewHolder(SwitchCardBinding binding) {
            super(binding.getRoot());
            mTextView = binding.switchCardTextview;
            mSwitch = binding.switchCardSwitch;
        }
    }
}
