package com.appskimo.app.hanja.ui.frags;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.event.CategorySelect;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.google.android.material.tabs.TabLayout;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;

@EFragment(R.layout.fragment_list)
public class ListFragment extends Fragment {
    @ViewById(R.id.subViewPager) ViewPager subViewPager;
    @ViewById(R.id.tabLayout) TabLayout tabLayout;

    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;

    private PagerAdapter pagerAdapter;
    private ListItemsFragment completedFragment = ListItemsFragment_.builder().arg("type", Constants.CollectionType.MASTERED).build();
    private ListItemsFragment uncompletedFragment = ListItemsFragment_.builder().arg("type", Constants.CollectionType.LEARNING).build();
    private ListItemsFragment checkFragment = ListItemsFragment_.builder().arg("type", Constants.CollectionType.CHECKED).build();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pagerAdapter = new PagerAdapter(getChildFragmentManager());
    }

    @Override
    public void onStart() {
        super.onStart();
        pagerAdapter.refreshAll();
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
    }

    @AfterViews
    void afterViews() {
        subViewPager.setAdapter(pagerAdapter);
        subViewPager.setCurrentItem(1);
        tabLayout.setupWithViewPager(subViewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pagerAdapter.refresh(tab.getPosition());
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Click(R.id.reset)
    void onClickReset() {
        miscService.showDialog(getActivity(), R.string.label_reset_confirm, (dialog, i) -> {
            if (vocabService != null) {
                vocabService.reset(new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        prefs.categoryWordUid().put(0);
                        prefs.collectionType().put(0);
                        EventBus.getDefault().post(new CategorySelect());
                    }
                });
            }
        });
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        private final List<ListItemsFragment> items = Arrays.asList(completedFragment, uncompletedFragment, checkFragment);
        private final int[] TITLES_ID = {R.string.tab_title_mastered, R.string.tab_title_learning, R.string.tab_title_checked};

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public ListItemsFragment getItem(int position) {
            return items.get(position);
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getText(TITLES_ID[position]);
        }

        public void refresh(int position) {
            getItem(position).refresh();
        }

        public void refreshAll() {
            for (var item : items) {
                item.refresh();
            }
        }
    }

}
