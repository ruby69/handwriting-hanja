package com.appskimo.app.hanja.ui.frags;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.event.GameNextAction;
import com.appskimo.app.hanja.service.GameService;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.appskimo.app.hanja.ui.dialog.GameResultDialog;
import com.appskimo.app.hanja.ui.dialog.GameResultDialog_;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.ViewsById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

@EFragment(R.layout.fragment_game_play)
public class GamePlayFragment extends Fragment {
    ViewPager viewPager;
    @ViewById(R.id.meansView1) TextView meansView1;
    @ViewById(R.id.meansView2) TextView meansView2;
    @ViewById(R.id.meansView3) TextView meansView3;
    @ViewById(R.id.scoreView) TextView scoreView;
    @ViewById(R.id.chronometer) Chronometer chronometer;
    @ViewById(R.id.countDown) TextView countDown;

    @ViewsById({R.id.bt01, R.id.bt02, R.id.bt03, R.id.bt04, R.id.bt05, R.id.bt06, R.id.bt07, R.id.bt08, R.id.bt09, R.id.bt10, R.id.bt11, R.id.bt12, R.id.bt13, R.id.bt14, R.id.bt15, R.id.bt16, R.id.bt17, R.id.bt18, R.id.bt19, R.id.bt20, R.id.bt21, R.id.bt22, R.id.bt23, R.id.bt24, R.id.bt25})
    List<TextView> buttons;

    @Bean GameService gameService;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;

    @ColorRes(R.color.white) int white;
    @ColorRes(R.color.white_trans00) int whiteTrans;

    private static final String [] READY_MESSAGE = {"2", "1", "Ready", "Start!!"};
    private GameResultDialog resultDialog = GameResultDialog_.builder().build();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new EventBusObserver.AtStartStop(this));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewPager = getActivity().findViewById(R.id.viewPager);
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
    }

    @AfterViews
    void afterViews() {
        initialize();
    }

    private void initialize() {
        gameService.initializeSession(getActivity().getIntent().getIntExtra("selectCategoryPosition", 1), getActivity().getIntent().getIntExtra("selectCount", 10), getActivity().getIntent().getLongExtra("selectHintDelay", 5000L));
        initializeBoard();
        readySession();
    }

    private void initializeBoard() {
        cleanButtons();
        setButtonsTextColor(white);
        Collections.shuffle(buttons);

        scoreView.setText(String.format("%08d", 0));
        gameService.initBoard(buttons);

        populateMeanings();
        YoYo.with(Techniques.FadeIn).duration(0).playOn(meansView1);
    }

    @Background
    void readySession() {
        gameService.readySession();

        setVisible(countDown, View.VISIBLE);
        for(final String message : READY_MESSAGE) {
            if(gameService.hasCanceled()) {
                break;
            }
            populateCountDownMessage(message);
            try{Thread.sleep(1000);}catch(Exception e){}
        }
        setVisible(countDown, View.INVISIBLE);

        if(gameService.hasCanceled()) {
            gameService.stopSession();
            return;
        }

        gameService.startSession();
        startChronometer();
        startHintObserver();
    }

    @UiThread
    void setVisible(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    @UiThread
    void populateCountDownMessage(String message) {
        countDown.setText(message);
        YoYo.with(Techniques.ZoomInDown).duration(800).playOn(countDown);
    }

    @UiThread
    void startChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    @Background
    void startHintObserver() {
        while(true){
            if(gameService.hasStopped() || gameService.hasCanceled()) {
                break;
            }

            try{Thread.sleep(1000);}catch(Exception e){}

            if(gameService.availableHint()){
                showHintButton();
            }
        }
    }

    @UiThread
    void showHintButton() {
        TextView button = findButton();
        if(button != null) {
            button.setBackgroundResource(R.drawable.bg_button_game_hint);
        }
    }

    @Click({R.id.bt01, R.id.bt02, R.id.bt03, R.id.bt04, R.id.bt05, R.id.bt06, R.id.bt07, R.id.bt08, R.id.bt09, R.id.bt10, R.id.bt11, R.id.bt12, R.id.bt13, R.id.bt14, R.id.bt15, R.id.bt16, R.id.bt17, R.id.bt18, R.id.bt19, R.id.bt20, R.id.bt21, R.id.bt22, R.id.bt23, R.id.bt24, R.id.bt25})
    void onClickButton(View view) {
        if(!gameService.hasLockedTouch()) {
            TextView button = (TextView) view;
            CategoryWord taggedWord = (CategoryWord)button.getTag();

            if(taggedWord == gameService.getCurrentWord()) {
                long currentTouchedTime = SystemClock.elapsedRealtime();

                gameService.processCombo(currentTouchedTime);
                gameService.processScore(currentTouchedTime);
                gameService.setLastTouchedTime(currentTouchedTime);

                scoreView.setText(String.format("%08d", gameService.getScore()));
                YoYo.with(Techniques.RubberBand).duration(750).playOn(scoreView);

                button.setText(null);
                button.setTag(null);
                if(gameService.emptyWordsInSession()) {
                    finishSession(false);
                } else {
                    populateNextWord(button);
                }

                view.setBackgroundResource(R.drawable.bg_button_game);
            }
        }
    }

    private void populateNextWord(TextView button) {
        if(!gameService.isEmptyWords()) {
            populateMeanings();
            YoYo.with(Techniques.Landing).duration(750).playOn(meansView1);
        }

        if(!gameService.isEmptyRemain()) {
            CategoryWord remainWord = gameService.getRemovedRemainWord();
            button.setText(remainWord.getWord().getWord());
            button.setTag(remainWord);
            YoYo.with(Techniques.FadeIn).duration(1500).playOn(button);
        }
    }

    private void populateMeanings() {
        CategoryWord currentWord = gameService.getRemovedCurrentWord();
        CategoryWord next2 = gameService.getWord(0);
        CategoryWord next3 = gameService.getWord(1);

        meansView1.setText(currentWord.getWord().getMeansForGame());
        meansView2.setText(next2 != null ? next2.getWord().getMeansForGame() : null);
        meansView3.setText(next3 != null ? next3.getWord().getMeansForGame() : null);
    }

    private void finishSession(boolean force) {
        gameService.stopSession();

        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());

        if(!force) {
            YoYo.with(Techniques.TakingOff).duration(750).playOn(meansView1);
            gameService.setElapsedTime(elapsedMillis);
            gameService.createGameRecord();

            resultDialog.setCancelable(false);
            resultDialog.setRecord(gameService.getScore(), elapsedMillis, gameService.getMaxComboCount()).show(getChildFragmentManager(), GameResultDialog.TAG);
        }
    }

    public void pauseSession() {
        gameService.pauseSession();
        setButtonsTextColor(whiteTrans);
        chronometer.stop();
    }

    public void stopSession() {
        finishSession(true);
    }

    public void resumeSession() {
        chronometer.setBase(chronometer.getBase() + (System.currentTimeMillis() - gameService.getPausedTime()));
        chronometer.start();

        gameService.resumeSession();
        setButtonsTextColor(white);
    }

    private TextView findButton(){
        for(TextView button : buttons) {
            CategoryWord taggedWord = (CategoryWord) button.getTag();
            if(taggedWord != null && gameService.getCurrentWord() == taggedWord) {
                return button;
            }
        }

        return null;
    }

    private void cleanButtons() {
        for(TextView button : buttons) {
            button.setBackgroundResource(R.drawable.bg_button_game);
            button.setText(null);
            button.setTag(null);
        }
    }

    private void setButtonsTextColor(int color) {
        for(TextView button : buttons) {
            button.setTextColor(color);
        }
    }

    @Subscribe
    public void onEvent(GameNextAction event) {
        if(event.getKind() == GameNextAction.Kind.PLAY) {
            initialize();
        } else {
            viewPager.setCurrentItem(event.getKind().ordinal(), true);
        }
    }
}
