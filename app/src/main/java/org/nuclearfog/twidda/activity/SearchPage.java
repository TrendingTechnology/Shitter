package org.nuclearfog.twidda.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.database.GlobalSettings;

import static org.nuclearfog.twidda.activity.TweetPopup.KEY_TWEETPOPUP_TEXT;

/**
 * Twitter search Activity
 */
public class SearchPage extends AppCompatActivity implements OnTabSelectedListener, OnQueryTextListener {

    /**
     * Key for the search query, required
     */
    public static final String KEY_SEARCH_QUERY = "search_query";

    private FragmentAdapter adapter;
    private TabLayout tabLayout;
    private ViewPager pager;

    private String search = "";

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_search);
        View root = findViewById(R.id.search_layout);
        Toolbar tool = findViewById(R.id.search_toolbar);
        tabLayout = findViewById(R.id.search_tab);
        pager = findViewById(R.id.search_pager);

        tool.setTitle("");
        setSupportActionBar(tool);

        GlobalSettings settings = GlobalSettings.getInstance(this);
        root.setBackgroundColor(settings.getBackgroundColor());

        adapter = new FragmentAdapter(getSupportFragmentManager());
        tabLayout.setSelectedTabIndicatorColor(settings.getHighlightColor());
        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(this);
        pager.setAdapter(adapter);

        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_SEARCH_QUERY)) {
            search = param.getString(KEY_SEARCH_QUERY);
            adapter.setupSearchPage(search);
            Tab twtSearch = tabLayout.getTabAt(0);
            Tab usrSearch = tabLayout.getTabAt(1);
            if (twtSearch != null && usrSearch != null) {
                twtSearch.setIcon(R.drawable.search);
                usrSearch.setIcon(R.drawable.user);
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (tabLayout.getSelectedTabPosition() > 0) {
            pager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.search, m);
        MenuItem searchItem = m.findItem(R.id.new_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(search);
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.search_tweet) {
            Intent intent = new Intent(this, TweetPopup.class);
            if (search.startsWith("#"))
                intent.putExtra(KEY_TWEETPOPUP_TEXT, search + " ");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        Intent intent = new Intent(this, SearchPage.class);
        intent.putExtra(KEY_SEARCH_QUERY, s);
        startActivity(intent);
        return true;
    }


    @Override
    public boolean onQueryTextChange(String s) {
        return false;
    }


    @Override
    public void onTabSelected(Tab tab) {
        invalidateOptionsMenu();
    }


    @Override
    public void onTabUnselected(Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }
}