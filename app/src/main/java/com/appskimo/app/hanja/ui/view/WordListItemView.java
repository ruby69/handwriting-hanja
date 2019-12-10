package com.appskimo.app.hanja.ui.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.WordCheck;
import com.appskimo.app.hanja.event.WordComplete;
import com.appskimo.app.hanja.event.WordSelect;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.DimensionRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;

@EViewGroup(R.layout.view_word_list_item)
public class WordListItemView extends RelativeLayout {
    @ViewById(R.id.bgLayer) View bgLayer;
    @ViewById(R.id.wordView) TextView wordView;
    @ViewById(R.id.meansView) TextView meansView;
    @ViewById(R.id.categoryView) TextView categoryView;
    @ViewById(R.id.check) ImageView check;
    @ViewById(R.id.checkLayer) View checkLayer;
    @ViewById(R.id.swapLayer) View swapLayer;

    @Bean VocabService vocabService;
    @Pref PrefsService_ prefs;

    @ColorRes(R.color.pink) int pink;
    @ColorRes(R.color.bluegrey_light) int blueGreyLight;

    @DimensionRes(R.dimen.text_size_character_max) float maxTextSize;
    @DimensionRes(R.dimen.text_size_character_min) float minTextSize;
    @DimensionRes(R.dimen.text_size_character_default) float defaultTextSize;

    private int position;
    private Constants.CollectionType collectionType;
    private CategoryWord categoryWord;

    public WordListItemView(Context context) {
        super(context);
    }

    public WordListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRecord(int position, Constants.CollectionType collectionType, CategoryWord categoryWord) {
        this.position = position;
        this.collectionType = collectionType;
        this.categoryWord = categoryWord;
        adjustWordTextSize();

        bgLayer.setBackgroundResource(R.color.white);

        Word word = null;
        if (categoryWord != null && (word = categoryWord.getWord()) != null) {
            wordView.setText(word.getWord());
            meansView.setText(word.getMeansForList());
            checkLayer.setVisibility(View.VISIBLE);
            swapLayer.setVisibility(collectionType.isSearch() ? View.INVISIBLE : View.VISIBLE);
            checking();

            if(collectionType.isSearch()) {
                categoryView.setVisibility(VISIBLE);
                categoryView.setText(categoryWord.getCategory().getName());
            } else {
                categoryView.setVisibility(GONE);
                bgLayer.setBackgroundResource(word.isCompleted() ? R.color.black_trans20 : R.color.white);
            }
        } else {
            wordView.setText(null);
            meansView.setText(null);
            checkLayer.setVisibility(View.INVISIBLE);
            swapLayer.setVisibility(View.INVISIBLE);
            categoryView.setVisibility(GONE);
        }
    }

    @UiThread
    void adjustWordTextSize() {
        if (collectionType.isChecked()) {
            wordView.setTextSize(TypedValue.COMPLEX_UNIT_PX, maxTextSize);
        } else if (collectionType.isMastered()) {
            wordView.setTextSize(TypedValue.COMPLEX_UNIT_PX, minTextSize);
        } else {
            wordView.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize);
        }
    }

    @UiThread
    void checking() {
        if (categoryWord.getWord().isChecked()) {
            check.setColorFilter(pink, PorterDuff.Mode.SRC_ATOP);
        } else {
            check.setColorFilter(blueGreyLight, PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Click({R.id.checkLayer, R.id.check})
    void onClickCheck() {
        final Word word = categoryWord.getWord();
        word.setChecked(!word.isChecked());
        vocabService.update(word, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                checking();
                EventBus.getDefault().post(new WordCheck(categoryWord.getCategoryWordUid(), collectionType == Constants.CollectionType.SEARCH ? Constants.EventDomain.SEARCH : Constants.EventDomain.LIST, word.isChecked()));
            }
        });
    }

    @Click({R.id.selectorLayer, R.id.wordView, R.id.wordView})
    void onClickWord() {
        if (categoryWord != null) {
            EventBus.getDefault().post(new WordSelect(categoryWord.getCategoryWordUid(), collectionType));
        }
    }

    @Click({R.id.swapLayer, R.id.swap})
    void onClickSwap() {
        final Word word = categoryWord.getWord();
        word.setCompleted(!word.isCompleted());
        vocabService.update(word, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                EventBus.getDefault().post(new WordComplete(categoryWord.getCategoryWordUid(), Constants.EventDomain.LIST, word.isCompleted()));
            }
        });
    }
}