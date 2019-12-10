package com.appskimo.app.hanja.ui.frags;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.ebanx.swipebtn.OnActiveListener;
import com.ebanx.swipebtn.SwipeButton;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Set;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

@EFragment(R.layout.fragment_lock)
public class LockFragment extends Fragment {
    @ViewById(R.id.wordViewLayer) View wordViewLayer;
    @ViewById(R.id.wordView) TextView wordView;
    @ViewById(R.id.meansView) TextView meansView;
    @ViewById(R.id.menus) View menus;

    @ViewById(R.id.showWordLabel) TextView showWordLabel;
    @ViewById(R.id.showMeansLabel) TextView showMeansLabel;
    @ViewById(R.id.showWord) FloatingActionButton showWordButton;
    @ViewById(R.id.showMeans) FloatingActionButton showMeansButton;

    @ViewById(R.id.swipe) SwipeButton swipe;
    @ViewById(R.id.refresh) View refresh;
    @ViewById(R.id.masterOrLearning) Button masterOrLearning;
    @ViewById(R.id.check) Button check;
    @ViewById(R.id.optionsLayer) View optionsLayer;
    @ViewById(R.id.adLayer2) View adLayer2;
    @ViewById(R.id.adBanner2) AdView adBanner2;

    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;

    @ColorRes(R.color.white) int white;
    @ColorRes(R.color.grey) int grey;
    @ColorRes(R.color.colorAccent) int colorAccent;
    @ColorRes(R.color.grey_light) int greyLight;
    @SystemService PowerManager powerManager;

    private boolean openMenu = false;
    private boolean showMeans = false;
    private boolean showWord = true;
    private Set<String> checkedCategories;
    private CategoryWord categoryWord;
    private Constants.CollectionType collectionType = Constants.CollectionType.ALL;

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
        showWord = prefs.showWord().getOr(true);
        showMeans = prefs.showMeans().getOr(true);
        checkedCategories = prefs.checkedCategories().get();

        int collectionTypeValue = prefs.collectionType().getOr(0);
        if (collectionTypeValue == 3) {
            collectionType = Constants.CollectionType.CHECKED;
        } else if (collectionTypeValue == 2) {
            collectionType = Constants.CollectionType.MASTERED;
        } else if (collectionTypeValue == 1) {
            collectionType = Constants.CollectionType.LEARNING;
        } else {
            collectionType = Constants.CollectionType.ALL;
        }
    }

    @AfterViews
    void afterViews() {
        YoYo.with(Techniques.FadeOut).duration(0).playOn(menus);
        options(showWord, showWordButton, showWordLabel, R.drawable.ic_assignment_white_24dp, R.drawable.ic_assignment_black_24dp);
        options(showMeans, showMeansButton, showMeansLabel, R.drawable.ic_description_white_24dp, R.drawable.ic_description_black_24dp);

        swipe.setOnActiveListener(new OnActiveListener() {
            @Override
            public void onActive() {
                YoYo.with(Techniques.FadeOut).duration(300).playOn(swipe);
                optionsLayer.setVisibility(View.VISIBLE);
                if (!showWord) {
                    showOrHide(true, wordViewLayer, Techniques.SlideInRight, 500L);
                }
                if (!showMeans) {
                    showOrHide(true, meansView, Techniques.FadeInLeft, 1000L);
                }

                if (adLayer2.getVisibility() != View.GONE) {
                    adLayer2.setVisibility(View.GONE);
                }
            }
        });
        miscService.loadBannerAdView(adBanner2);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (categoryWord == null) {
            vocabService.findCategoryWord(checkedCategories, prefs.categoryWordUid().getOr(0), collectionType, manipulateCallback);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private Callback<CategoryWord> manipulateCallback = new Callback<CategoryWord>() {
        @Override
        public void onSuccess(CategoryWord categoryWord) {
            manipulate(categoryWord);
        }
    };

    @UiThread
    void manipulate(CategoryWord categoryWord) {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null && wordView != null && meansView != null) {
            this.categoryWord = categoryWord;

            prefs.categoryWordUid().put(categoryWord.getCategoryWordUid());
            wordView.setText(word.getWord());
            meansView.setText(word.getMeansForCard());

            checking();
            showOrHide(showWord, wordViewLayer, Techniques.SlideInRight, 500L);
            showOrHide(showMeans, meansView, Techniques.FadeInLeft, 1000L);
            showHideSwipe();
            showMasterOrLearning();
            checkAd();
        } else {
            prefs.categoryWordUid().put(0);
            FragmentActivity activity = getActivity();
            if(activity != null) {
                activity.finish();
            }
        }
    }

    private int touchCount = 0;
    private int checkCount = 4;
    private void checkAd() {
        if (!showMeans && showWord && adLayer2.getVisibility() != View.VISIBLE && countForAd()) {
            adLayer2.setVisibility(View.VISIBLE);
        }
    }

    private boolean countForAd() {
        boolean b = touchCount++ % checkCount == 0 && touchCount > 1;
        if (b) {
            touchCount = 0;
            checkCount++;
        }
        return b;
    }

    @UiThread
    void showMasterOrLearning() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null && masterOrLearning != null) {
            masterOrLearning.setText(word.isCompleted() ? getString(R.string.label_learning) : getString(R.string.label_mastered));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// bottom navigation actions

    @Click(R.id.prev)
    void onClickPrev() {
        vocabService.findPrevOrNext(checkedCategories, categoryWord, false, collectionType, manipulateCallback);
    }

    @Click(R.id.next)
    void onClickNext() {
        vocabService.findPrevOrNext(checkedCategories, categoryWord, true, collectionType, manipulateCallback);
    }

    @Click(R.id.random)
    void onClickRandom() {
        vocabService.findRandomize(checkedCategories, categoryWord, collectionType, manipulateCallback);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// menu & option actions

    @Click(R.id.optionsMenu)
    void onClickOptionsMenu() {
        if (openMenu) {
            YoYo.with(Techniques.FadeOutDown).duration(100).playOn(menus);
            openMenu = false;
        } else {
            YoYo.with(Techniques.FadeInUp).interpolate(new OvershootInterpolator()).duration(300).playOn(menus);
            openMenu = true;
        }
    }

    @Click(R.id.showWord)
    void onClickShowWord() {
        showWord = !showWord;
        prefs.showWord().put(showWord);
        options(showWord, showWordButton, showWordLabel, R.drawable.ic_assignment_white_24dp, R.drawable.ic_assignment_black_24dp);
        showOrHide(showWord, wordViewLayer);
        showHideSwipe();
        adLayer2.setVisibility(View.GONE);
    }

    @Click(R.id.showMeans)
    void onClickShowMeans() {
        showMeans = !showMeans;
        prefs.showMeans().put(showMeans);
        options(showMeans, showMeansButton, showMeansLabel, R.drawable.ic_description_white_24dp, R.drawable.ic_description_black_24dp);
        showOrHide(showMeans, meansView);
        showHideSwipe();
        adLayer2.setVisibility(View.GONE);
    }

    private void options(boolean selected, FloatingActionButton fab, TextView labelView, int selectedResId, int unSelectedResId) {
        if (selected) {
            labelView.setTextColor(greyLight);
            labelView.setBackgroundResource(R.drawable.options_label_bg_sel);
            fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryLight));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageDrawable(getResources().getDrawable(selectedResId, getActivity().getTheme()));
            } else {
                fab.setImageDrawable(getResources().getDrawable(selectedResId));
            }

        } else {
            labelView.setTextColor(grey);
            labelView.setBackgroundResource(R.drawable.options_label_bg_unsel);
            fab.setBackgroundTintList(getResources().getColorStateList(R.color.grey_light));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fab.setImageDrawable(getResources().getDrawable(unSelectedResId, getActivity().getTheme()));
            } else {
                fab.setImageDrawable(getResources().getDrawable(unSelectedResId));
            }
        }
    }

    private void showOrHide(boolean show, View view) {
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void showOrHide(boolean show, View view, Techniques techniques, long duration) {
        if (show) {
            view.setVisibility(View.VISIBLE);
            YoYo.with(techniques).duration(duration).playOn(view);
        } else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void showHideSwipe() {
        boolean hide = showMeans && showWord;
        if (hide) {
            YoYo.with(Techniques.FadeOut).duration(0).playOn(swipe);
        } else {
            YoYo.with(Techniques.FadeIn).duration(0).playOn(swipe);
            if (swipe.isActive()) {
                swipe.toggleState();
            }
        }
        optionsLayer.setVisibility(hide ? View.VISIBLE : View.GONE);
        refresh.setVisibility(hide ? View.GONE : View.VISIBLE);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// learning actions

    @Click(R.id.refresh)
    void onClickRefresh() {
        manipulate(categoryWord);
    }

    @Click(R.id.check)
    void onClickCheck() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null) {
            word.setChecked(!word.isChecked());
            vocabService.update(word, new Callback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    checking();
                }
            });
        }
    }

    @Click(R.id.masterOrLearning)
    void onClickMasterOrLearning() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null) {
            word.setCompleted(!word.isCompleted());
            vocabService.update(word, new Callback<Void>() {
                @Override
                public void before() {
                    if (vocabService.countOf(checkedCategories, collectionType) < 2L) {
                        collectionType = Constants.CollectionType.ALL;
                        vocabService.findCategoryWord(checkedCategories, 0, collectionType, manipulateCallback);
                    } else {
                        onClickNext();
                    }
                }
            });
        }
    }

    @UiThread
    void checking() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null && check != null) {
            check.setText(word.isChecked() ? getString(R.string.label_check_unset) : getString(R.string.label_check_set));
            setTextViewDrawableColor(check, word.isChecked() ? colorAccent : greyLight);
        }
    }

    private void setTextViewDrawableColor(TextView textView, int color) {
        for (Drawable drawable : textView.getCompoundDrawables()) {
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            }
        }
    }
}
