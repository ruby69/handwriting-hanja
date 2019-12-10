package com.appskimo.app.hanja.ui.frags;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.service.GameService;
import com.appskimo.app.hanja.ui.adapter.GameRecordRecyclerViewAdapter;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.IntegerRes;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

@EFragment(R.layout.fragment_game_history)
public class GameHistoryFragment extends Fragment {
    @ViewById(R.id.recyclerView) RecyclerView recyclerView;

    @Bean GameRecordRecyclerViewAdapter recyclerViewAdapter;
    @Bean GameService gameService;
    @IntegerRes(R.integer.rank_column_count) int columnCount;

    @AfterViews
    void afterViews() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.reset(gameService.getTopRank());
    }
}
