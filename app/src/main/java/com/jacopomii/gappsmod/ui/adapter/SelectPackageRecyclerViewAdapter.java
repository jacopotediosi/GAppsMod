package com.jacopomii.gappsmod.ui.adapter;

import static com.jacopomii.gappsmod.util.Utils.getApplicationLabelOrUnknown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.data.PhenotypeDBPackageName;
import com.jacopomii.gappsmod.databinding.PackageRowBinding;
import com.jacopomii.gappsmod.util.OnItemClickListener;
import com.l4digital.fastscroll.FastScroller;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
public class SelectPackageRecyclerViewAdapter extends RecyclerView.Adapter<SelectPackageRecyclerViewAdapter.ViewHolder> implements Filterable, FastScroller.SectionIndexer {
    private final Context mContext;

    private List<PhenotypeDBPackageName> mPackageList = new ArrayList<>();
    private List<PhenotypeDBPackageName> mPackageListFiltered = new ArrayList<>();

    private final ICoreRootService mCoreRootServiceIpc;

    private final OnItemClickListener mOnItemClickListener;

    @SuppressLint("NotifyDataSetChanged")
    public SelectPackageRecyclerViewAdapter(Context context, ICoreRootService coreRootServiceIpc, OnItemClickListener onItemClickListener) {
        mContext = context;
        mCoreRootServiceIpc = coreRootServiceIpc;
        mOnItemClickListener = onItemClickListener;

        try {
            mPackageList = new ArrayList<>();
            TreeMap<String, String> map = new TreeMap<String, String>(mCoreRootServiceIpc.phenotypeDBGetAllPackageNames());
            for (Map.Entry<String, String> packageName : map.entrySet())
                mPackageList.add(new PhenotypeDBPackageName(packageName.getKey(), packageName.getValue()));

            mPackageListFiltered = mPackageList;

            notifyDataSetChanged();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Initialize binding and viewHolder
        PackageRowBinding binding = PackageRowBinding.inflate(LayoutInflater.from(mContext), parent, false);
        ViewHolder viewHolder = new ViewHolder(binding);

        // Set onClickListener on list rows
        viewHolder.mRow.setOnClickListener(v -> {
            int position = viewHolder.getAdapterPosition();
            mOnItemClickListener.onItemClick(mPackageListFiltered.get(position).getPhenotypePackageName());
        });

        // Return viewHolder
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String phenotypePackageName = mPackageListFiltered.get(position).getPhenotypePackageName();
        String androidPackageName = mPackageListFiltered.get(position).getAndroidPackageName();

        PackageManager packageManager = mContext.getPackageManager();

        Drawable packageIcon;
        try {
            packageIcon = packageManager.getApplicationIcon(androidPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            packageIcon = AppCompatResources.getDrawable(mContext, R.drawable.ic_error_24);
            if (packageIcon != null) {
                TypedArray typedArray = mContext.getTheme().obtainStyledAttributes(R.styleable.ViewStyle);
                int colorError = typedArray.getColor(R.styleable.ViewStyle_colorError, Color.RED);
                typedArray.recycle();
                packageIcon.mutate().setTint(colorError);
            }
        }
        holder.mPackageIcon.setImageDrawable(packageIcon);

        String appName = getApplicationLabelOrUnknown(mContext, androidPackageName);
        holder.mAppName.setText(appName);

        holder.mPhenotypePackageName.setText(phenotypePackageName);
    }

    @Override
    public int getItemCount() {
        return mPackageListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String keyLowercase = charSequence.toString().toLowerCase();

                List<PhenotypeDBPackageName> packageListFiltered = new ArrayList<>();
                for (PhenotypeDBPackageName phenotypeDBPackageName : mPackageList) {
                    String phenotypePackageNameLowercase = phenotypeDBPackageName.getPhenotypePackageName().toLowerCase();
                    String appNameLowercase = getApplicationLabelOrUnknown(mContext, phenotypeDBPackageName.getAndroidPackageName()).toLowerCase();
                    if (phenotypePackageNameLowercase.contains(keyLowercase) || appNameLowercase.contains(keyLowercase))
                        packageListFiltered.add(phenotypeDBPackageName);
                }
                mPackageListFiltered = packageListFiltered;

                FilterResults filterResults = new FilterResults();
                filterResults.values = mPackageListFiltered;
                filterResults.count = mPackageListFiltered.size();
                return filterResults;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mPackageListFiltered = (List<PhenotypeDBPackageName>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public CharSequence getSectionText(int position) {
        // Get raw Phenotype package name
        String phenotypePackageName = mPackageListFiltered.get(position).getPhenotypePackageName();

        // Initialize indexEnd
        int indexEnd = 0;

        // Try to split package name by dot character: look for the first 4 parts, if there aren't try 3, 2 and so on
        for (int i = 4; i >= 0; i--) {
            int indexEndTmp = StringUtils.ordinalIndexOf(phenotypePackageName, ".", i);
            if (indexEndTmp != -1) {
                indexEnd = indexEndTmp;
                break;
            }
        }

        // Increment indexEnd by 2 to show two more characters beyond the found parts
        if (indexEnd + 2 <= phenotypePackageName.length())
            indexEnd += 2;
        else
            indexEnd = phenotypePackageName.length();

        // Return the phenotypePackageName parsed substring
        return phenotypePackageName.substring(0, indexEnd);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout mRow;
        private final ImageView mPackageIcon;
        private final TextView mAppName;
        private final TextView mPhenotypePackageName;

        public ViewHolder(PackageRowBinding binding) {
            super(binding.getRoot());
            mRow = binding.row;
            mPackageIcon = binding.packageIcon;
            mAppName = binding.appName;
            mPhenotypePackageName = binding.phenotypePackageName;
        }
    }
}
