package com.jacopomii.gappsmod.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.view.CollapsibleActionView;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;

import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.databinding.FilterableSearchviewBinding;

/**
 * A View, containing a {@link SearchView}, to which an additional View can optionally be connected
 * as a container to contain components for filtering the search, via the
 * {@link #setFiltersContainer} method. When a filtersContainer is set, a button appears next to the
 * SearchView that allows the user to manually show / hide the filtersContainer.
 */
// Here I use the deprecated CollapsibleActionView interface because otherwise the
// onActionViewExpanded and onActionViewCollapsed methods are never called, idk why
@SuppressWarnings("deprecation")
public class FilterableSearchView extends LinearLayout implements CollapsibleActionView {
    FilterableSearchviewBinding mBinding;
    private final Context mContext;

    private View mFiltersContainer;
    private boolean mIsFiltersContainerVisible;

    public FilterableSearchView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public FilterableSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mBinding = FilterableSearchviewBinding.inflate(LayoutInflater.from(mContext), this, true);
        mBinding.collapseFiltersButton.setOnClickListener(v -> {
            if (mFiltersContainer != null) setFiltersVisibility(!mIsFiltersContainerVisible);
        });
    }

    /**
     * Connects a filters container to the SearchView, which can be shown / hidden by the user
     * using a special button.
     *
     * @param filtersContainer the filters container to attach.
     */
    public void setFiltersContainer(View filtersContainer) {
        this.mFiltersContainer = filtersContainer;
        mBinding.collapseFiltersButton.setVisibility(VISIBLE);
    }

    /**
     * Sets the hint text to display in the query text field of the SearchView.
     *
     * @param hint the hint text to display or {@code null}.
     */
    public void setQueryHint(CharSequence hint) {
        mBinding.searchView.setQueryHint(hint);
    }

    /**
     * Returns the query string currently in the text field of the SearchView.
     *
     * @return the query string currently in the text field of the SearchView.
     */
    public CharSequence getQuery() {
        return mBinding.searchView.getQuery();
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs actions
     *                 in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(SearchView.OnQueryTextListener listener) {
        mBinding.searchView.setOnQueryTextListener(listener);
    }

    @Override
    public void onActionViewExpanded() {
        if (mFiltersContainer != null) setFiltersVisibility(true);
        mBinding.searchView.onActionViewExpanded();
    }

    @Override
    public void onActionViewCollapsed() {
        if (mFiltersContainer != null) setFiltersVisibility(false);
        mBinding.searchView.onActionViewCollapsed();
    }

    private void setFiltersVisibility(boolean visible) {
        mIsFiltersContainerVisible = visible;

        int newFiltersViewVisibility;
        int newCollapseFiltersButtonDrawableID;

        if (visible) {
            newFiltersViewVisibility = View.VISIBLE;
            newCollapseFiltersButtonDrawableID = R.drawable.ic_arrow_up_24;
        } else {
            newFiltersViewVisibility = View.GONE;
            newCollapseFiltersButtonDrawableID = R.drawable.ic_arrow_down_24;
        }

        mFiltersContainer.setVisibility(newFiltersViewVisibility);

        Drawable newCollapseFiltersButtonDrawable = ResourcesCompat.getDrawable(getResources(), newCollapseFiltersButtonDrawableID, null);
        mBinding.collapseFiltersButton.setImageDrawable(newCollapseFiltersButtonDrawable);
    }
}
