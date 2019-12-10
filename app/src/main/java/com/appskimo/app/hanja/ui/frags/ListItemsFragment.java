package com.appskimo.app.hanja.ui.frags;

import android.os.Bundle;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.More;
import com.appskimo.app.hanja.event.CategorySelect;
import com.appskimo.app.hanja.event.WordCheck;
import com.appskimo.app.hanja.event.WordComplete;
import com.appskimo.app.hanja.event.WordsMore;
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
import org.greenrobot.eventbus.Subscribe;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

@EFragment(R.layout.fragment_list_items)
public class ListItemsFragment extends Fragment {
    @ViewById(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;
    @ViewById(R.id.recyclerView) RecyclerView recyclerView;

    @Bean WordListRecyclerViewAdapter recyclerViewAdapter;
    @Bean VocabService vocabService;
    @Bean MiscService miscService;

    @IntegerRes(R.integer.list_comp_column_count) int completeColumnCount;
    @IntegerRes(R.integer.list_uncom_column_count) int uncompleteColumnCount;
    @IntegerRes(R.integer.list_check_column_count) int checkColumnCount;

    private More currentMore;
    private Constants.CollectionType collectionType = Constants.CollectionType.LEARNING;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
        collectionType = (Constants.CollectionType) getArguments().getSerializable("type");
        recyclerViewAdapter.setCollectionType(collectionType);
    }

    @AfterViews
    void afterViews() {
        int columnCount = uncompleteColumnCount;
        if (collectionType.isChecked()) {
            columnCount = checkColumnCount;
        } else if (collectionType.isMastered()) {
            columnCount = completeColumnCount;
        }

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setAdapter(recyclerViewAdapter);

        refreshLayout.setOnRefreshListener(refreshListener);
        refreshLayout.setColorSchemeResources(R.color.red, R.color.green, R.color.blue, R.color.yellow);
    }

    @Subscribe
    @UiThread
    public void onEvent(WordsMore event) {
        if (vocabService != null && event.getEventDomain().isList() && collectionType == event.getCollectionType() && currentMore.isHasMore()) {
            vocabService.retrieve(currentMore, collectionType, new Callback<More>() {
                @Override
                public void onSuccess(More more) {
                    currentMore = more;
                    recyclerViewAdapter.add(more.getContent());
                }
            });
        }
    }

    @Subscribe
    public void onEvent(CategorySelect event) {
        if (vocabService != null) {
            vocabService.retrieve(new More(), collectionType, new Callback<More>() {
                @Override
                public void onSuccess(More more) {
                    refreshWords(more);
                }
            });
        }
    }

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (refreshLayout != null) {
                refreshLayout.setRefreshing(true);
            }

            if (vocabService != null) {
                vocabService.retrieve(new More(), collectionType, new Callback<More>() {
                    @Override
                    public void onSuccess(More more) {
                        currentMore = more;
                        refreshWords(more);
                    }
                });
            }
        }
    };

    public void refresh() {
        refreshListener.onRefresh();
    }

    @UiThread
    void refreshWords(More more) {
        currentMore = more;
        recyclerViewAdapter.reset(more.getContent());

        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }

        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
    }

    @Subscribe
    @UiThread
    public void onEvent(WordCheck event) {
        if (event.getEventDomain().isNotList()) {
            if (collectionType.isChecked()) {
                refreshListener.onRefresh();
            } else {
                recyclerViewAdapter.updateCheckByCategoryWordUid(event.getCategoryWordUid(), event.isChecked());
            }
        } else {
            if (collectionType.isChecked()) {
                if (getUserVisibleHint()) {
                    recyclerViewAdapter.removeByCategoryWordUid(event.getCategoryWordUid());
                }
            }
        }
    }

    @Subscribe
    @UiThread
    public void onEvent(WordComplete event) {
        if (collectionType.isChecked()) {
            recyclerViewAdapter.updateCompleteByCategoryWordUid(event.getCategoryWordUid(), event.isCompleted());
        } else {
            if (getUserVisibleHint()) {
                recyclerViewAdapter.removeByCategoryWordUid(event.getCategoryWordUid());
            }
        }
    }
}
