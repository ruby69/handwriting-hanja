package com.appskimo.app.hanja;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import android.app.PendingIntent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.appskimo.app.hanja.event.OnFinishLockscreenActivityAll;
import com.appskimo.app.hanja.event.OnLockActivity;
import com.appskimo.app.hanja.event.OnMainActivity;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Date;

@Fullscreen
@EActivity(R.layout.activity_lock)
public class LockActivity extends AppCompatActivity {
    @ViewById(R.id.containerLayer) View containerLayer;
    @ViewById(R.id.clock) View clock;

    @Pref PrefsService_ prefs;
    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @SystemService AudioManager audioManager;
    @SystemService TelephonyManager telephonyManager;

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                finish();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private TelephonyCallback telephonyCallback = new TelephoneCallStateListener();

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class TelephoneCallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!BuildConfig.DEBUG) {
            FirebaseAnalytics.getInstance(this);
        }

        registerTelephonyCallback();
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
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

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        EventBus.getDefault().post(new OnLockActivity());
    }

    private void registerTelephonyCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(directExecutor(), telephonyCallback);
        } else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(this);
    }

    @AfterViews
    void afterViews() {
        initClock();
    }

    private void initClock() {
        if(prefs.useClock().getOr(true)) {
            final var params = (RelativeLayout.LayoutParams) clock.getLayoutParams();

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

    @Click(R.id.menu)
    void onClickMenu() {
        LauncherActivity_.intent(this).flags(PendingIntent.FLAG_IMMUTABLE).start();
    }

    @Click(R.id.lockOff)
    void onClickLockOff() {
        miscService.showDialog(this, R.string.label_lock_off, (dialog, i) -> {
            prefs.lockOffTime().put(new Date().getTime());
            finish();
        });
    }

    @Subscribe
    @UiThread
    public void onEvent(OnMainActivity event) {
        finish();
    }

    @Subscribe
    public void onEvent(OnFinishLockscreenActivityAll event) {
        finish();
    }

}
