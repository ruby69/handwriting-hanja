package com.appskimo.app.hanja.ui.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.UnconsciousTheme;
import com.appskimo.app.hanja.service.PrefsService_;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

@EViewGroup(R.layout.view_unconscious)
public class UnconsciousView extends FrameLayout {
    @SystemService WindowManager windowManager;

    @ViewById(R.id.unconsciousLayer) View unconsciousLayer;
    @ViewById(R.id.textView) TextView textView;
    @ViewById(R.id.meansView) TextView meaningView;

    @Pref PrefsService_ prefs;

    public UnconsciousView(Context context) {
        super(context);
    }

    public UnconsciousView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @AfterViews
    void afterViews() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT;

        if(prefs.unconsciousPosX().getOr(-1) != -1) {
            params.x = prefs.unconsciousPosX().get();
            params.y = prefs.unconsciousPosY().get();
        }

        windowManager.addView(this, params);

        unconsciousLayer.setBackgroundResource(prefs.bgResId().getOr(R.drawable.unconscious_grey));
        unconsciousLayer.setOnTouchListener(new OnTouchListener() {
            private int initX, initY;
            private int initTouchX, initTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        initTouchX = x;
                        initTouchY = y;
                        return true;

                    case MotionEvent.ACTION_UP:
                        prefs.unconsciousPosX().put(params.x);
                        prefs.unconsciousPosY().put(params.y);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initX + (x - initTouchX);
                        params.y = initY + (y - initTouchY);

                        windowManager.updateViewLayout(UnconsciousView.this, params);
                        return true;
                }
                return false;
            }
        });
    }

    public void destroy() {
        windowManager.removeView(this);
    }

    @UiThread
    public void populate(CategoryWord categoryWord) {
        if (categoryWord != null && categoryWord.getWord() != null) {
            unconsciousLayer.setVisibility(View.VISIBLE);
            setWord(categoryWord.getWord());
            meaningView.setText(categoryWord.getWord().getMeansForList());
            YoYo.with(Techniques.Pulse).duration(750).playOn(unconsciousLayer);
        } else {
            unconsciousLayer.setVisibility(View.GONE);
        }
    }

    @Subscribe
    public void onEvent(UnconsciousTheme event) {
        unconsciousLayer.setBackgroundResource(event.getResId());
    }

    private void setWord(Word word) {
        if (word != null) {
            String str = word.getWord();

            if (str == null || "null".equals(str)) {
                textView.setText(null);
            } else {
                textView.setText(str);
            }
        } else {
            textView.setText(null);
        }
    }
}
