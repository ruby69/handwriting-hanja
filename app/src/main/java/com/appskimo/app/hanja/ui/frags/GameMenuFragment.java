package com.appskimo.app.hanja.ui.frags;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.event.CategoryGameSelect;
import com.appskimo.app.hanja.event.GameNextAction;
import com.appskimo.app.hanja.service.GameService;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.appskimo.app.hanja.ui.view.CategoryItemView;
import com.appskimo.app.hanja.ui.view.CategoryItemView_;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

@EFragment(R.layout.fragment_game_menu)
public class GameMenuFragment extends Fragment {
    ViewPager viewPager;
    @ViewById(R.id.categoryLabel) TextView categoryLabel;
    @ViewById(R.id.categorySelection) View categorySelection;
    @ViewById(R.id.categoriesLayer) LinearLayout categoriesLayer;
    @ViewById(R.id.countLabel) TextView countLabel;
    @ViewById(R.id.hintDelayLabel) TextView hintDelayLabel;

    @Bean GameService gameService;
    @Bean MiscService miscService;
    @StringRes(R.string.label_game_select_category) String selectCategoryLabel;
    @StringRes(R.string.label_game_select_count) String selectCountLabel;
    @StringRes(R.string.label_game_select_hint_delay) String selectHintDelayLabel;

    String [] titles;

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
        List<Category> categories = gameService.getCategories();
        titles = new String[categories.size()];
        for(int i = 0; i< categories.size(); i++) {
            titles[i] = categories.get(i).getName();
        }
    }

    @AfterViews
    void afterViews() {
        for(int i = 0; i < titles.length; i++) {
            CategoryItemView view = CategoryItemView_.build(getActivity());
            view.setTitle(i, titles[i]);
            categoriesLayer.addView(view);
        }
        initializeOptions();
    }

    private void initializeOptions() {
        int categoryPosition = getActivity().getIntent().getIntExtra("selectCategoryPosition", 0);
        int count = getActivity().getIntent().getIntExtra("selectCount", 10);
        long hintDelay = getActivity().getIntent().getLongExtra("selectHintDelay", 5000L);

        onEvent(new CategoryGameSelect(categoryPosition));
        putCount(count);
        putHintDelay(hintDelay);
    }

    @Subscribe
    public void onEvent(CategoryGameSelect event) {
        int position = event.getPosition();
        List<Category> categories = gameService.getCategories();
        if (categories != null && !categories.isEmpty() && categories.size() > position) {
            Category category = categories.get(position);
            categoryLabel.setText(selectCategoryLabel + " : " + category.getName());
            getActivity().getIntent().putExtra("selectCategoryPosition", position);
        }
    }

    @Click(R.id.play)
    void onClickPlay() {
        if(gameService.hasStopped()) {
            viewPager.setCurrentItem(2, true);
        }
    }

    @Click(R.id.histories)
    void onClickHistories() {
        viewPager.setCurrentItem(GameNextAction.Kind.HISTORY.ordinal(), false);
    }

    @Click({R.id.count1, R.id.count2, R.id.count3, R.id.count4, R.id.count5})
    void onClickCount(View view) {
        putCount(Integer.parseInt((String) view.getTag()));
    }

    @Click({R.id.hintDelay1, R.id.hintDelay2, R.id.hintDelay3})
    void onClickHintDelay(View view) {
        putHintDelay(Long.parseLong((String) view.getTag()));
    }

    private void putCount(int count) {
        if(count != -1) {
            countLabel.setText(selectCountLabel + " : " + count);
        } else {
            countLabel.setText(selectCountLabel + " : ALL");
        }
        getActivity().getIntent().putExtra("selectCount", count);
    }

    private void putHintDelay(long hintDelay) {
        hintDelayLabel.setText(selectHintDelayLabel + " : " + (hintDelay / 1000L));
        getActivity().getIntent().putExtra("selectHintDelay", hintDelay);
    }
}
