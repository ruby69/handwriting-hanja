package com.appskimo.app.hanja.ui.adapter;

import android.view.View;
import android.view.ViewGroup;

import com.appskimo.app.hanja.domain.GameRecord;
import com.appskimo.app.hanja.ui.view.GameRecordItemView;
import com.appskimo.app.hanja.ui.view.GameRecordItemView_;

import org.androidannotations.annotations.EBean;

import androidx.recyclerview.widget.RecyclerView;

@EBean
public class GameRecordRecyclerViewAdapter extends CommonRecyclerViewAdapter<GameRecord, GameRecordRecyclerViewAdapter.ViewHolder> {
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.gameRecordItemView.setRecord(position, items.get(position));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new ViewHolder(GameRecordItemView_.build(viewGroup.getContext()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        GameRecordItemView gameRecordItemView;
        public ViewHolder(View itemView) {
            super(itemView);
            gameRecordItemView = (GameRecordItemView) itemView;
        }
    }
}