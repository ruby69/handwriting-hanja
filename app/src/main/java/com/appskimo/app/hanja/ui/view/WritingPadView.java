package com.appskimo.app.hanja.ui.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.WordCheck;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.ui.dialog.StrokeAnimationTuneDialog;
import com.appskimo.app.hanja.ui.dialog.StrokeAnimationTuneDialog_;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.ebanx.swipebtn.OnActiveListener;
import com.ebanx.swipebtn.SwipeButton;
import com.eftimoff.androipathview.PathView;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.larvalabs.svgandroid.SVGParser;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.IntegerRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

@EViewGroup(R.layout.view_writing_pad)
public class WritingPadView extends RelativeLayout {
    @ViewById(R.id.guideView1) PathView guideView1;
    @ViewById(R.id.guideView2) TextView guideView2;
    @ViewById(R.id.strokeView) PathView strokeView;
    @ViewById(R.id.strokeOrderView) StrokeOrderView strokeOrderView;
    @ViewById(R.id.signaturePad) SignaturePad signaturePad;

    @ViewById(R.id.prev) View prev;
    @ViewById(R.id.random) View random;
    @ViewById(R.id.next) View next;

    @ViewById(R.id.meansView) TextView meansView;
    @ViewById(R.id.infoView) TextView infoView;
    @ViewById(R.id.menus) View menus;

    @ViewById(R.id.showWordLabel) TextView showWordLabel;
    @ViewById(R.id.showMeansLabel) TextView showMeansLabel;
    @ViewById(R.id.showStrokeOrderLabel) TextView showStrokeOrderLabel;
    @ViewById(R.id.showWord) FloatingActionButton showWordButton;
    @ViewById(R.id.showMeans) FloatingActionButton showMeansButton;
    @ViewById(R.id.showStrokeOrder) FloatingActionButton showStrokeOrderButton;

    @ViewById(R.id.swipe) SwipeButton swipe;
    @ViewById(R.id.refresh) View refresh;
    @ViewById(R.id.masterOrLearning) Button masterOrLearning;
    @ViewById(R.id.check) Button check;
    @ViewById(R.id.optionsLayer) View optionsLayer;
    @ViewById(R.id.stroke) FloatingActionButton strokeButton;

    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;

    @IntegerRes(R.integer.max_line_width) int maxLineWidth;
    @IntegerRes(R.integer.min_line_width) int minLineWidth;

    @ColorRes(R.color.grey) int grey;
    @ColorRes(R.color.colorAccent) int colorAccent;
    @ColorRes(R.color.grey_light) int greyLight;

    private final Matrix guideScaleMatrix = new Matrix();
    private final Matrix strokeScaleMatrix = new Matrix();
    private final Matrix translateMatrix = new Matrix();

    private static final int STROKE_BASE_DURATION = 100;
    private ObjectAnimator strokeAnimator;
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private StrokeAnimationTuneDialog strokeAnimationTuneDialog = StrokeAnimationTuneDialog_.builder().build();

    private final RectF rectF = new RectF(0, 0, 0, 0);
    private boolean openMenu = false;
    private boolean showMeans = true;
    private boolean showWord = true;
    private boolean showStrokeOrder = true;
    private CategoryWord categoryWord;
    private Constants.CollectionType collectionType = Constants.CollectionType.ALL;

    private FragmentManager fragmentManager;

    public WritingPadView(Context context) {
        super(context);
    }

    public WritingPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @AfterInject
    void afterInject() {
        showWord = prefs.showWord().getOr(true);
        showMeans = prefs.showMeans().getOr(true);

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
        initGuideView();
        YoYo.with(Techniques.FadeOut).duration(0).playOn(menus);
        options(showWord, showWordButton, showWordLabel, R.drawable.ic_assignment_white_24dp, R.drawable.ic_assignment_black_24dp);
        options(showMeans, showMeansButton, showMeansLabel, R.drawable.ic_description_white_24dp, R.drawable.ic_description_black_24dp);
        options(showStrokeOrder, showStrokeOrderButton, showStrokeOrderLabel, R.drawable.ic_filter_1_white_24dp, R.drawable.ic_filter_1_black_24dp);
        masterOrLearning.setVisibility(View.GONE);

        swipe.setOnActiveListener(new OnActiveListener() {
            @Override
            public void onActive() {
                YoYo.with(Techniques.FadeOut).duration(300).playOn(swipe);
                optionsLayer.setVisibility(View.VISIBLE);
                meansView.setVisibility(View.VISIBLE);
                infoView.setVisibility(View.VISIBLE);
                if (hasPath()) {
                    showOrHide(true, guideView1);
                    showOrHide(true, strokeView);
                    showOrHide(true, strokeButton);
                } else {
                    showOrHide(true, guideView2);
                }
            }
        });
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
        if (categoryWord != null && (word = categoryWord.getWord()) != null && meansView != null) {
            this.categoryWord = categoryWord;
            cancelStroke();
            clear();

            meansView.setText(word.getMeansForNote());
            infoView.setText(word.getInfo());
            String path = word.getPath();
            if (path == null || "null".equals(path)) {
                clearGuideView1();
                guideView2.setText(word.getWord());
            } else {
                setPathToGuideView1();
                guideView2.setText(null);
            }

            if (hasPath()) {
                showOrHide(showWord, guideView1);
                showOrHide(showWord, strokeView);
                showOrHide(showWord, strokeButton);
                showOrHide(false, guideView2);
            } else {
                showOrHide(showWord, guideView2);
                showOrHide(false, guideView1);
                showOrHide(false, strokeView);
                showOrHide(false, strokeButton);
            }

            checking();
            showOrHide(showMeans, meansView);
            showOrHide(showMeans, infoView);
            showHideSwipe();
        }
    }

    private boolean hasPath() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null && meansView != null) {
            String path = word.getPath();
            return !(path == null || "null".equals(path));
        } else {
            return false;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// bottom navigation actions

    @Click(R.id.prev)
    void onClickPrev() {
        vocabService.findPrevOrNext(collectionType, categoryWord, false, manipulateCallback);
    }

    @Click(R.id.next)
    void onClickNext() {
        vocabService.findPrevOrNext(collectionType, categoryWord, true, manipulateCallback);
    }

    @Click(R.id.random)
    void onClickRandom() {
        vocabService.findRandomize(collectionType, categoryWord, manipulateCallback);
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

        if (hasPath()) {
            cancelStroke();
            showOrHide(showWord, guideView1);
            showOrHide(showWord, strokeView);
            showOrHide(showWord, strokeButton);
            showOrHide(false, guideView2);
        } else {
            showOrHide(showWord, guideView2);
            showOrHide(false, guideView1);
            showOrHide(false, strokeView);
            showOrHide(false, strokeButton);
        }
        showHideSwipe();
    }

    @Click(R.id.showMeans)
    void onClickShowMeans() {
        showMeans = !showMeans;
        prefs.showMeans().put(showMeans);
        options(showMeans, showMeansButton, showMeansLabel, R.drawable.ic_description_white_24dp, R.drawable.ic_description_black_24dp);
        showOrHide(showMeans, meansView);
        showOrHide(showMeans, infoView);
        showHideSwipe();
    }

    @Click(R.id.showStrokeOrder)
    void onClickShowStrokeOrder() {
        showStrokeOrder = !showStrokeOrder;
        options(showStrokeOrder, showStrokeOrderButton, showStrokeOrderLabel, R.drawable.ic_filter_1_white_24dp, R.drawable.ic_filter_1_black_24dp);
        showOrHide(showStrokeOrder, strokeOrderView);
    }

    @Click(R.id.tunePlay)
    void onClickTunePlay() {
        cancelStroke();
        strokeAnimationTuneDialog.show(fragmentManager, StrokeAnimationTuneDialog.TAG);
        onClickOptionsMenu();
    }

    private void options(boolean selected, FloatingActionButton fab, TextView labelView, int selectedResId, int unSelectedResId) {
        if (selected) {
            labelView.setTextColor(greyLight);
            labelView.setBackgroundResource(R.drawable.options_label_bg_sel);
            fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryLight, getContext().getTheme()));
            fab.setImageDrawable(getResources().getDrawable(selectedResId, getContext().getTheme()));

        } else {
            labelView.setTextColor(grey);
            labelView.setBackgroundResource(R.drawable.options_label_bg_unsel);
            fab.setBackgroundTintList(getResources().getColorStateList(R.color.grey_light, getContext().getTheme()));
            fab.setImageDrawable(getResources().getDrawable(unSelectedResId, getContext().getTheme()));
        }
    }

    private void showOrHide(boolean show, View view) {
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
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

    @Click(R.id.clear)
    void onClickClear() {
        signaturePad.clear();
    }

    @Click(R.id.stroke)
    void onClickStroke() {
        if (strokeAnimator != null) {
            strokeAnimator.cancel();
            strokeAnimator = null;
        }

        List<String> pathArrayList = getPathList();
        if (pathArrayList != null) {
            int duration = prefs.strokeSpeed().getOr(3) * STROKE_BASE_DURATION * pathArrayList.size();
            strokeAnimator = strokeView.getPathAnimator().duration(duration).build();
            strokeAnimator.setRepeatCount(prefs.strokeRepeatCount().getOr(2));
            strokeAnimator.setInterpolator(decelerateInterpolator);
            strokeAnimator.start();
        }
    }

    @Click(R.id.refresh)
    void onClickRefresh() {
        manipulate(categoryWord);
    }

    @Click(R.id.check)
    void onClickCheck() {
        if (categoryWord != null) {
            final Word word = categoryWord.getWord();
            if(word != null) {
                word.setChecked(!word.isChecked());
                vocabService.update(word, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        checking();
                        EventBus.getDefault().post(new WordCheck(categoryWord.getCategoryWordUid(), Constants.EventDomain.PAD, word.isChecked()));
                    }
                });
            }
        }
    }

    @UiThread
    void checking() {
        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null && check != null) {
            check.setText(word.isChecked() ? getContext().getString(R.string.label_check_unset) : getContext().getString(R.string.label_check_set));
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

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @UiThread
    void initGuideView() {
        if (guideView1 != null) {
            guideView1.setPathWidth(prefs.guideLineStrokeWidth().get());
        }

        float guideScaleUnit = vocabService.getGuideScaleUnit();
        float density = vocabService.getDensity();
        guideScaleMatrix.setScale(guideScaleUnit, guideScaleUnit);
        strokeScaleMatrix.setScale(density, density);
    }

    private List<String> getPathList() {
        try {
            return categoryWord.getWord().getPathList();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Word.OrderPoint> getOrderList() {
        try {
            return categoryWord.getWord().getOrderList();
        } catch (Exception e) {
            return null;
        }
    }

    private void clear() {
        signaturePad.clear();
        guideView1.clear();
        strokeOrderView.clear();
        strokeView.clear();
    }

    private void clearGuideView1() {
        signaturePad.clear();
        guideView1.clear();
    }

    private void setPathToGuideView1() {
        guideView1.clear();
        strokeView.clear();

        List<String> pathStrings = getPathList();
        if (pathStrings != null) {
            computeBounds(pathStrings, guideScaleMatrix);   // must call at first.
            drawGuideStroke(pathStrings);                   // must call at second.
            drawGuideStrokeOrder(getOrderList());           // must call at 3rd.
            drawStroke(pathStrings);                        // must call finally.
        }
    }

    private void drawGuideStroke(List<String> pathStrings) {
        guideView1.setPaths(getTransformedPaths(pathStrings, guideScaleMatrix, true));
        guideView1.setFillAfter(false);
        guideView1.setPercentage(1.0F);
    }

    private boolean flag = false;
    private void drawGuideStrokeOrder(List<Word.OrderPoint> list) {
        float translateX = (strokeOrderView.getWidth() / 2F) - rectF.centerX();
        float translateY = (strokeOrderView.getHeight() / 2F) - rectF.centerY();
        translateMatrix.setTranslate(translateX, translateY);
        if (!flag) {
            strokeOrderView.drawOrder(null, guideScaleMatrix, translateMatrix);
            flag = true;
        }
        strokeOrderView.drawOrder(list, guideScaleMatrix, translateMatrix);
    }

    private void drawStroke(List<String> pathStrings) {
        strokeView.setPaths(getTransformedPaths(pathStrings, strokeScaleMatrix, false));
        strokeView.setFillAfter(false);
    }

    private void computeBounds(List<String> pathStrings, Matrix scaleMatrix) {
        Path tempPath = new Path();
        for (String pathString : pathStrings) {
            Path path = SVGParser.parsePath(pathString);
            path.transform(scaleMatrix);
            tempPath.addPath(path);
        }
        tempPath.computeBounds(rectF, true);
    }

    private List<Path> getTransformedPaths(List<String> pathStrings, Matrix scaleMatrix, boolean center) {
        float translateX = (guideView1.getWidth() / 2F) - rectF.centerX();
        float translateY = (guideView1.getHeight() / 2F) - rectF.centerY();
        List<Path> list = new ArrayList<>();
        for (String pathString : pathStrings) {
            Path path = SVGParser.parsePath(pathString);
            path.transform(scaleMatrix);
            if (center) {
                translateMatrix.setTranslate(translateX, translateY);
                path.transform(translateMatrix);
            }
            list.add(path);
        }

        return list;
    }

    public void cancelStroke() {
        if (strokeAnimator != null) {
            strokeAnimator.cancel();
        }
    }

    public void retrieveAndManipulate(Constants.CollectionType collectionType, int categoryWordUid) {
        this.collectionType = collectionType;
        showAndHideNavigator(collectionType != Constants.CollectionType.SEARCH);
        vocabService.findCategoryWord(collectionType, categoryWordUid, manipulateCallback);
    }

    @UiThread
    void showAndHideNavigator(boolean show) {
        if (show) {
            prev.setVisibility(View.VISIBLE);
            random.setVisibility(View.VISIBLE);
            next.setVisibility(View.VISIBLE);
        } else {
            prev.setVisibility(View.INVISIBLE);
            random.setVisibility(View.INVISIBLE);
            next.setVisibility(View.INVISIBLE);
        }
    }
}