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
 * {@link #setFilterContainer} method. When a filterContainer is set, a button appears next to the
 * SearchView that allows the user to manually show / hide the filterContainer.
 */
// Here I use the deprecated CollapsibleActionView interface because otherwise the
// onActionViewExpanded and onActionViewCollapsed methods are never called, idk why
@SuppressWarnings("deprecation")
public class FilterableSearchView extends LinearLayout implements CollapsibleActionView {
    FilterableSearchviewBinding mBinding;
    private final Context mContext;

    private View mFilterContainer;
    private boolean mIsFilterContainerVisible;
    private boolean mFilterContainerAutoExpand;

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
        mBinding.collapseFilterContainerButton.setOnClickListener(v -> {
            if (mFilterContainer != null) setFilterContainerVisibility(!mIsFilterContainerVisible);
        });
    }

    /**
     * Connects a filter container to the SearchView, which can be shown / hidden by the user
     * using a special button.
     *
     * @param filterContainer           the filter container to attach.
     * @param filterContainerAutoExpand whether the filter container should open itself when the
     *                                  SearchView is expanded.
     */
    public void setFilterContainer(View filterContainer, boolean filterContainerAutoExpand) {
        mFilterContainer = filterContainer;
        mFilterContainerAutoExpand = filterContainerAutoExpand;
        mBinding.collapseFilterContainerButton.setVisibility(VISIBLE);
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
        if (mFilterContainer != null && mFilterContainerAutoExpand)
            setFilterContainerVisibility(true);
        mBinding.searchView.onActionViewExpanded();
    }

    @Override
    public void onActionViewCollapsed() {
        if (mFilterContainer != null) setFilterContainerVisibility(false);
        mBinding.searchView.onActionViewCollapsed();
    }

    private void setFilterContainerVisibility(boolean visible) {
        mIsFilterContainerVisible = visible;

        int newFilterContainerVisibility;
        int newCollapseFilterButtonDrawableID;

        if (visible) {
            newFilterContainerVisibility = View.VISIBLE;
            newCollapseFilterButtonDrawableID = R.drawable.ic_arrow_up_24;
        } else {
            newFilterContainerVisibility = View.GONE;
            newCollapseFilterButtonDrawableID = R.drawable.ic_arrow_down_24;
        }

        mFilterContainer.setVisibility(newFilterContainerVisibility);

        Drawable newCollapseFilterButtonDrawable = ResourcesCompat.getDrawable(getResources(), newCollapseFilterButtonDrawableID, null);
        mBinding.collapseFilterContainerButton.setImageDrawable(newCollapseFilterButtonDrawable);
    }
}
