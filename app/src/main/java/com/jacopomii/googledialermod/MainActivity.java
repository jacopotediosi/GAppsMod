package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Utils.revertAllMods;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        mTabLayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.viewpager);

        setSupportActionBar(mToolbar);

        mViewPagerAdapter = new ViewPagerAdapter(this);

        mViewPager.setAdapter(mViewPagerAdapter);

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                RadioGroup radioGroupSearch = findViewById(R.id.radio_group_search);
                radioGroupSearch.setVisibility(View.GONE);
                radioGroupSearch.check(R.id.radiobutton_all);
            }
        });

        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
            if (position == 1) {
                tab.setText(R.string.boolean_mods);
            } else {
                tab.setText(R.string.suggested_mods);
            }
        }).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_info_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(android.R.string.ok, null)
                    .setView(R.layout.information_dialog);
            AlertDialog alert = builder.create();
            alert.show();

            // Links aren't clickable workaround
            TextView whatIsItExplanation = alert.findViewById(R.id.what_is_it_explanation);
            if (whatIsItExplanation != null)
                whatIsItExplanation.setMovementMethod(LinkMovementMethod.getInstance());
            TextView madeWithLove = alert.findViewById(R.id.made_with_love_by_jacopo_tediosi);
            if (madeWithLove != null)
                madeWithLove.setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        } else if (item.getItemId() == R.id.menu_delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        revertAllMods(this);
                        // Restart the activity to be sure UI is updated
                        finish();
                        startActivity(getIntent());
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}