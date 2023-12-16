package com.appskimo.app.hanja;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.appskimo.app.hanja.event.OnLockActivity;
import com.appskimo.app.hanja.event.OnMainActivity;
import com.appskimo.app.hanja.service.GameService;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.appskimo.app.hanja.ui.frags.GameHistoryFragment;
import com.appskimo.app.hanja.ui.frags.GameHistoryFragment_;
import com.appskimo.app.hanja.ui.frags.GameMenuFragment;
import com.appskimo.app.hanja.ui.frags.GameMenuFragment_;
import com.appskimo.app.hanja.ui.frags.GamePlayFragment;
import com.appskimo.app.hanja.ui.frags.GamePlayFragment_;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

@Fullscreen
@EActivity(R.layout.activity_game)
public class GameActivity extends AppCompatActivity {
    @ViewById(R.id.viewPager) ViewPager viewPager;

    @Pref PrefsService_ prefs;

    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @Bean GameService gameService;

    private PagerAdapter pagerAdapter;

    private GameMenuFragment gameMenuFragment = GameMenuFragment_.builder().build();
    private GamePlayFragment gamePlayFragment = GamePlayFragment_.builder().build();
    private GameHistoryFragment gameHistoryFragment = GameHistoryFragment_.builder().build();

    private DialogInterface.OnClickListener quitListener = (dialog, i) -> {
        gamePlayFragment.stopSession();
        viewPager.setCurrentItem(0, true);
        dialog.dismiss();
    };
    private DialogInterface.OnClickListener resumeListener = (dialog, i) -> {
        gamePlayFragment.resumeSession();
        dialog.dismiss();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!BuildConfig.DEBUG) {
            FirebaseAnalytics.getInstance(this);
        }
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(this);
    }

    @AfterViews
    void afterViews() {
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
    }

    @Subscribe
    @UiThread
    public void onEvent(OnMainActivity event) {
        finish();
    }

    @Subscribe
    @UiThread
    public void onEvent(OnLockActivity event) {
        finish();
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> items = new ArrayList<>();

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            items.add(gameMenuFragment);
            items.add(new Fragment());
            items.add(gamePlayFragment);
            items.add(new Fragment());
            items.add(gameHistoryFragment);
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
        var fragment = pagerAdapter.getItem(viewPager.getCurrentItem());
        if (fragment instanceof GamePlayFragment) {
            if (gameService.isReady()) {
                gameService.cancelSession();
                viewPager.setCurrentItem(0, true);

            } else if (gameService.isPlaying()) {
                gamePlayFragment.pauseSession();
                miscService.showDialog(this, R.string.label_game_pause, R.string.label_game_quit, quitListener, R.string.label_game_resume, resumeListener);
            }
        } else if (fragment instanceof GameHistoryFragment) {
            viewPager.setCurrentItem(0, false);

        } else {
            miscService.showDialog(this, R.string.label_game_finish, (dialog, i) -> finish());
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }
}
