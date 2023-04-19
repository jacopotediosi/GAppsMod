package com.jacopomii.googledialermod.ui.adapter;

import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.jacopomii.googledialermod.ui.viewmodel.SwitchCardViewModel;
import com.l4digital.fastscroll.FastScroller;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BooleanModsRecyclerViewAdapter extends RecyclerView.Adapter<BooleanModsRecyclerViewAdapter.BooleanModsViewHolder> implements Filterable, FastScroller.SectionIndexer {
    private final Context mContext;
    private final List<SwitchCardViewModel> mData;
    private List<SwitchCardViewModel> mDataFiltered;

    public BooleanModsRecyclerViewAdapter(Context context, List<SwitchCardViewModel> data) {
        if (context instanceof MainActivity) {
            mContext = context;
            mData = data;
            mDataFiltered = data;
        } else {
            throw new RuntimeException("BooleanModsRecyclerViewAdapter can be attached only to the MainActivity");
        }
    }

    @NonNull
    @Override
    public BooleanModsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.switch_card, parent, false);
        return new BooleanModsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BooleanModsViewHolder holder, int position) {
        holder.mT.setText(mDataFiltered.get(position).getSwitchText());

        holder.mS.setOnCheckedChangeListener(null); // Remove any existing listener from recycled view

        holder.mS.setChecked(mDataFiltered.get(position).getSwitchChecked());

        holder.mS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mDataFiltered.get(position).setSwitchChecked(isChecked);
            try {
                ((MainActivity) mContext).getCoreRootServiceIpc().phenotypeDBOverrideBooleanFlag(DIALER_PACKAGE_NAME, holder.mT.getText().toString(), isChecked);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return mDataFiltered.size();
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

                    List<SwitchCardViewModel> lstFiltered = new ArrayList<>();
                    for (SwitchCardViewModel row : mData) {
                        if (row.getSwitchText().toLowerCase().contains(key.toLowerCase())) {
                            boolean switchStatus = row.getSwitchChecked();
                            if (mode.equals("all") || (mode.equals("enabled_only") && switchStatus) || (mode.equals("disabled_only") && !switchStatus))
                                lstFiltered.add(row);
                        }
                    }
                    mDataFiltered = lstFiltered;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mDataFiltered;
                filterResults.count = mDataFiltered.size();
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mDataFiltered = (List<SwitchCardViewModel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public CharSequence getSectionText(int position) {
        return mDataFiltered.get(position).getSwitchText().substring(0, 1);
    }

    public static class BooleanModsViewHolder extends RecyclerView.ViewHolder {
        private final TextView mT;
        private final MaterialSwitch mS;

        public BooleanModsViewHolder(View itemView) {
            super(itemView);
            mT = itemView.findViewById(R.id.switch_card_textview);
            mS = itemView.findViewById(R.id.switch_card_switch);
        }
    }
}
