package com.appskimo.app.hanja;

import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.event.ChangeFontScale;
import com.appskimo.app.hanja.event.Link;
import com.appskimo.app.hanja.event.OnLockActivity;
import com.appskimo.app.hanja.event.OnMainActivity;
import com.appskimo.app.hanja.event.WordSelect;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.appskimo.app.hanja.support.ScreenOffService_;
import com.appskimo.app.hanja.support.UnconsciousService_;
import com.appskimo.app.hanja.ui.dialog.LinkDialog;
import com.appskimo.app.hanja.ui.dialog.NoticeKanjiDialog;
import com.appskimo.app.hanja.ui.dialog.NoticeKanjiDialog_;
import com.appskimo.app.hanja.ui.frags.ListFragment_;
import com.appskimo.app.hanja.ui.frags.LockSettingsFragment_;
import com.appskimo.app.hanja.ui.frags.SearchFragment;
import com.appskimo.app.hanja.ui.frags.SearchFragment_;
import com.appskimo.app.hanja.ui.view.FontScaleView_;
import com.appskimo.app.hanja.ui.view.WritingPadView;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

@Fullscreen
@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int POSITION_LIST = 0;
    private static final int POSITION_SEARCH = 1;
    private static final int POSITION_LOCK_SETTINGS = 2;
    private int prevPosition = POSITION_LIST;

    @ViewById(R.id.appBarLayout) AppBarLayout appBarLayout;
    @ViewById(R.id.toolbar) Toolbar toolbar;
    @ViewById(R.id.drawerLayout) DrawerLayout drawerLayout;
    @ViewById(R.id.navigationView) NavigationView navigationView;
    @ViewById(R.id.progressView) View progressView;
    private Spinner spinner;

    @ViewById(R.id.mainViewPager) ViewPager mainViewPager;
    @ViewById(R.id.writingPad) WritingPadView writingPad;

    @Pref PrefsService_ prefs;
    @Bean MiscService miscService;
    @Bean VocabService vocabService;
    @SystemService AudioManager audioManager;

    private PagerAdapter pagerAdapter;
    private BottomSheetBehavior bottomSheetBehavior;

    private NoticeKanjiDialog noticeKanjiDialog = NoticeKanjiDialog_.builder().build();
    private LinkDialog linkDialog = new LinkDialog();

    private String currentQuery = "";
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!BuildConfig.DEBUG) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Fabric.with(this, new Crashlytics());
        }
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        UnconsciousService_.intent(this).stop();
        if (prefs.useLockScreen().getOr(false)) {
            ScreenOffService_.start(this);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        prefs.lockOffTime().put(0L);
        EventBus.getDefault().post(new OnMainActivity());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(this);
    }

    @AfterViews
    void afterViews() {
        initializeViews();
        initCheckKanjiNotice();
    }

    private void initializeViews() {
        initNavigationDrawer();

        mainViewPager.setAdapter(pagerAdapter);
        initBottomSheet();
        miscService.initializeMobileAds();
    }

    private void initCheckKanjiNotice() {
        if (!prefs.checkedKanjiNotice().getOr(false)) {
            noticeKanjiDialog.show(getSupportFragmentManager(), NoticeKanjiDialog.TAG);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);

        vocabService.loadCategories(new Callback<List<Category>>() {
            @Override
            public void onSuccess(final List<Category> categories) {
                super.onSuccess(categories);
                initSpinner(menu, categories);
            }
        });

        initSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @UiThread
    void initSpinner(final Menu menu, final List<Category> categories) {
        String [] titles = new String[categories.size()];
        int index = 0;
        for(Category category : categories) {
            titles[index++] = category.getName();
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(MainActivity.this, R.layout.view_spinner, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.spinner));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(spinnerSelectedListener);
    }

    private AdapterView.OnItemSelectedListener spinnerSelectedListener = new AdapterView.OnItemSelectedListener() {
        int count = 0;

        @Override
        public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
            if (count++ > 2) {
                miscService.showAdDialog(MainActivity.this, R.string.label_keep_going, (dialog, i) -> {});
            }
            vocabService.selectCategory(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };


    private void initSearchView(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.search);
        final MenuItem spinnerItem = menu.findItem(R.id.spinner);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                spinnerItem.setVisible(false);
                searchFragment.clear();
                mainViewPager.setCurrentItem(POSITION_SEARCH, false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                    setVisibleWritingPad(false);
                    return false;

                } else {
                    mainViewPager.setCurrentItem(prevPosition, false);
                    spinnerItem.setVisible(true);
                    currentQuery = "";
                    searchFragment.clear();
                    return true;
                }
            }
        });

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setVisibleWritingPad(false);
                if (query != null && query.trim().length() > 0 && !currentQuery.equals(query)) {
                    searchFragment.search(query);
                    currentQuery = query;
                    searchView.clearFocus();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setVisibleWritingPad(false);
                return false;
            }
        });
    }

    private void initNavigationDrawer() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(writingPad);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        writingPad.setFragmentManager(getSupportFragmentManager());
    }

    public void setVisibleWritingPad(boolean visible) {
        appBarLayout.setExpanded(!visible);
        bottomSheetBehavior.setState(visible ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menuList) {
            mainViewPager.setCurrentItem(POSITION_LIST, false);
            drawerLayout.closeDrawer(GravityCompat.START);
            setVisibleWritingPad(false);
            prevPosition = POSITION_LIST;

        } else if (id == R.id.menuLockscreen) {
            boolean useLockscreen = prefs.useLockScreen().get();
            if (useLockscreen) {
                miscService.showAdDialog(this, R.string.label_onlock_start, (dialog, i) -> OnActivity_.intent(this).start());
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.message_confirm_lockscreen)
                        .setPositiveButton(R.string.label_confirm, (dialogInterface, i) -> {
                            prefs.useLockScreen().put(true);
                            OnActivity_.intent(MainActivity.this).start();
                            ScreenOffService_.start(MainActivity.this);
                        }).create();

                if (!this.isFinishing()) {
                    alertDialog.show();
                }
            }

        } else if (id == R.id.menuLockscreenSettings) {
            mainViewPager.setCurrentItem(POSITION_LOCK_SETTINGS, false);
            setVisibleWritingPad(false);
            prevPosition = POSITION_LOCK_SETTINGS;

            drawerLayout.closeDrawer(GravityCompat.START);

        } else if (id == R.id.menuGames) {
            miscService.showAdDialog(this, R.string.label_game, (dialog, i) -> GameActivity_.intent(this).start());

        } else if (id == R.id.fontScale) {
            fontScaleDialog = new AlertDialog.Builder(this).setTitle(R.string.label_font_scale).setView(FontScaleView_.build(this)).create();
            fontScaleDialog.show();
        }
        return true;
    }

    @Subscribe
    public void onEvent(WordSelect event) {
        setVisibleWritingPad(true);
        writingPad.retrieveAndManipulate(event.getCollectionType(), event.getCategoryWordUid());
    }

    @Subscribe
    public void onEvent(Link event) {
        linkDialog.setUrl(event.getUrl()).show(getSupportFragmentManager(), LinkDialog.TAG);
    }

    private AlertDialog fontScaleDialog;

    @Subscribe
    public void onEvent(ChangeFontScale event) {
        if (fontScaleDialog != null && fontScaleDialog.isShowing()) {
            fontScaleDialog.dismiss();
        }
        LauncherActivity_.intent(this).start();
        finish();
    }

    @Subscribe
    @UiThread
    public void onEvent(OnLockActivity event) {
        finish();
    }

    private SearchFragment searchFragment = SearchFragment_.builder().build();

    private class PagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> items = new ArrayList<>();

        public PagerAdapter(FragmentManager fm) {
            super(fm);

            items.add(ListFragment_.builder().build());
            items.add(searchFragment);
            items.add(LockSettingsFragment_.builder().build());
        }

        @Override
        public Fragment getItem(int position) {
            return items.get(position);
        }

        @Override
        public int getCount() {
            return items.size();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED){
            setVisibleWritingPad(false);
            return;
        }

        miscService.showAdDialog(this, R.string.label_finish, (dialog, i) -> finish());
    }

}
