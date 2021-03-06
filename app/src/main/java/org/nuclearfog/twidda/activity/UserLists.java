package org.nuclearfog.twidda.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.Tab;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.backend.utils.FontTool;
import org.nuclearfog.twidda.database.GlobalSettings;

/**
 * Activity to show user lists of a twitter user
 */
public class UserLists extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    /**
     * request code for {@link ListPopup} OnTabSelectedListener
     */
    public static final int REQ_CREATE_LIST = 1;

    /**
     * return code for {@link ListPopup} if list was created
     */
    public static final int RET_LIST_CREATED = 2;

    /**
     * Key for the ID the list owner
     */
    public static final String KEY_USERLIST_OWNER_ID = "userlist-owner-id";

    /**
     * alternative key for the screen name of the owner
     */
    public static final String KEY_USERLIST_OWNER_NAME = "userlist-owner-name";

    private FragmentAdapter adapter;
    private ViewPager pager;
    private TabLayout mTab;

    private boolean isHome = false;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_list);
        View root = findViewById(R.id.list_view);
        Toolbar toolbar = findViewById(R.id.list_toolbar);
        pager = findViewById(R.id.list_pager);
        mTab = findViewById(R.id.list_tab);

        toolbar.setTitle(R.string.list_appbar);
        setSupportActionBar(toolbar);
        adapter = new FragmentAdapter(getSupportFragmentManager());
        mTab.setupWithViewPager(pager);

        GlobalSettings settings = GlobalSettings.getInstance(this);
        FontTool.setViewFontAndColor(settings, root);
        root.setBackgroundColor(settings.getBackgroundColor());
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(2);
        pager.setAdapter(adapter);
        mTab.setupWithViewPager(pager);
        mTab.setSelectedTabIndicatorColor(settings.getHighlightColor());
        mTab.addOnTabSelectedListener(this);

        Bundle param = getIntent().getExtras();
        if (param != null) {
            if (param.containsKey(KEY_USERLIST_OWNER_ID)) {
                long ownerId = param.getLong(KEY_USERLIST_OWNER_ID);
                isHome = ownerId == settings.getUserId();
                adapter.setupListPage(ownerId, "");
            } else if (param.containsKey(KEY_USERLIST_OWNER_NAME)) {
                String ownerName = param.getString(KEY_USERLIST_OWNER_NAME);
                adapter.setupListPage(-1, ownerName);
            }
            Tab userList = mTab.getTabAt(0);
            Tab userSub = mTab.getTabAt(1);
            if (userList != null && userSub != null) {
                userList.setIcon(R.drawable.list_owner);
                userSub.setIcon(R.drawable.list_subscribe);
            }
        }
    }


    @Override
    protected void onActivityResult(int reqCode, int returnCode, @Nullable Intent intent) {
        super.onActivityResult(reqCode, returnCode, intent);
        if (reqCode == REQ_CREATE_LIST && returnCode == RET_LIST_CREATED) {
            adapter.notifySettingsChanged();
        }
    }


    @Override
    public void onBackPressed() {
        if (mTab.getSelectedTabPosition() > 0) {
            pager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.lists, m);
        m.findItem(R.id.list_create).setVisible(isHome);
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.list_create) {
            Intent createList = new Intent(this, ListPopup.class);
            startActivityForResult(createList, REQ_CREATE_LIST);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
    }


    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }
}