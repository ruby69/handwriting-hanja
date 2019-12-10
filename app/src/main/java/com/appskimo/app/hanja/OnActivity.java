package com.appskimo.app.hanja;

import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

@Fullscreen
@EActivity(R.layout.activity_lock)
public class OnActivity extends AppCompatActivity {
    @ViewById(R.id.containerLayer) View containerLayer;
    @ViewById(R.id.lockOff) View lockOff;
    @ViewById(R.id.menu) View menu;
    @ViewById(R.id.clock) View clock;

    @Pref PrefsService_ prefs;
    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @SystemService AudioManager audioManager;

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
        initClock();
        menu.setVisibility(View.GONE);
        lockOff.setVisibility(View.GONE);
    }

    private void initClock() {
        if(prefs.useClock().getOr(true)) {
            final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) clock.getLayoutParams();

            params.leftMargin = prefs.clockPosX().getOr(0);
            params.topMargin = prefs.clockPosY().getOr(0);

            clock.setLayoutParams(params);
            containerLayer.invalidate();

            clock.setOnTouchListener(new View.OnTouchListener() {
                private int mXDelta;
                private int mYDelta;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int x = (int) event.getRawX();
                    final int y = (int) event.getRawY();
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            mXDelta = x - params.leftMargin;
                            mYDelta = y - params.topMargin;
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_DOWN:
                        case MotionEvent.ACTION_POINTER_UP:
                            break;

                        case MotionEvent.ACTION_MOVE:
                            params.leftMargin = x - mXDelta;
                            params.topMargin = y - mYDelta;
                            prefs.clockPosX().put(params.leftMargin);
                            prefs.clockPosY().put(params.topMargin);
                            clock.setLayoutParams(params);
                            break;
                    }

                    containerLayer.invalidate();
                    return true;
                }
            });

            clock.setVisibility(View.VISIBLE);
        } else {
            clock.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        doFinish();
    }

    private void doFinish() {
        miscService.showAdDialog(this, R.string.label_onlock_finish, (dialog, i) -> finish());
    }

}
