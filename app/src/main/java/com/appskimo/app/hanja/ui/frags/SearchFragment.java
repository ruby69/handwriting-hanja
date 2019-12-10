package com.appskimo.app.hanja.ui.frags;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.event.WordCheck;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.support.EventBusObserver;
import com.appskimo.app.hanja.ui.adapter.WordListRecyclerViewAdapter;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.IntegerRes;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collection;

@EFragment(R.layout.fragment_search)
public class SearchFragment extends Fragment {
    @ViewById(R.id.recyclerView) RecyclerView recyclerView;
    @Bean WordListRecyclerViewAdapter recyclerViewAdapter;
    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @IntegerRes(R.integer.list_uncom_column_count) int columnCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
        recyclerViewAdapter.setCollectionType(Constants.CollectionType.SEARCH);
    }

    @AfterViews
    void afterViews() {
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @UiThread
    void refreshWords(Collection<CategoryWord> categoryWords) {
        recyclerViewAdapter.reset(categoryWords);
        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }
    }

    public void search(String query) {
        if (vocabService != null) {
            vocabService.search(query, new Callback<Collection<CategoryWord>>() {
                @Override
                public void onSuccess(Collection<CategoryWord> categoryWords) {
                    refreshWords(categoryWords);
                }
            });
        }
    }

    @UiThread
    public void clear() {
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.clear();
        }
    }

    @Subscribe
    @UiThread
    public void onEvent(WordCheck event) {
        if (event.getEventDomain().isPad()) {
            recyclerViewAdapter.updateCheckByCategoryWordUid(event.getCategoryWordUid(), event.isChecked());
        }
    }
}
