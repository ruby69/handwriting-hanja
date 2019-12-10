package com.appskimo.app.hanja.ui.adapter;

import android.view.View;
import android.view.ViewGroup;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.WordsMore;
import com.appskimo.app.hanja.ui.view.WordListItemView;
import com.appskimo.app.hanja.ui.view.WordListItemView_;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.greenrobot.eventbus.EventBus;

import java.util.Collections;

import androidx.recyclerview.widget.RecyclerView;
import lombok.Setter;

@EBean
public class WordListRecyclerViewAdapter extends CommonRecyclerViewAdapter<CategoryWord, WordListRecyclerViewAdapter.ViewHolder> {
    @Setter private Constants.CollectionType collectionType;

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if(position == this.getItemCount() - 10) {
            EventBus.getDefault().post(new WordsMore(Constants.EventDomain.LIST, collectionType));
        }
        viewHolder.wordListItemView.setRecord(position, collectionType, items.get(position));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new ViewHolder(WordListItemView_.build(viewGroup.getContext()));
    }

    @UiThread
    public void removeByCategoryWordUid(int categoryWordUid) {
        int position = findPosition(categoryWordUid);
        if (position > -1) {
            notifyItemRemoved(position);
            items.remove(position);
        }
    }

    private int findPosition(int categoryWordUid) {
        for (int i = 0; i < items.size(); i++) {
            if (categoryWordUid == items.get(i).getCategoryWordUid().intValue()) {
                return i;
            }
        }
        return -1;
    }

    private CategoryWord findByCategoryWordUid(int categoryWordUid) {
        for (CategoryWord item : items) {
            if (categoryWordUid == item.getCategoryWordUid().intValue()) {
                return item;
            }
        }
        return null;
    }

    @UiThread
    public void updateCheckByCategoryWordUid(int categoryWordUid, boolean checked) {
        CategoryWord categoryWord = findByCategoryWordUid(categoryWordUid);
        if (categoryWord != null) {
            Word word = categoryWord.getWord();
            word.setChecked(checked);
            notifyDataSetChanged();
        }
    }

    @UiThread
    public void updateCompleteByCategoryWordUid(int categoryWordUid, boolean completec) {
        CategoryWord categoryWord = findByCategoryWordUid(categoryWordUid);
        if (categoryWord != null) {
            Word word = categoryWord.getWord();
            word.setCompleted(completec);

            int position = findPosition(categoryWordUid);
            if (position > -1) {
                notifyItemChanged(position);
            }
        }
    }

    @UiThread
    public void shuffle() {
        Collections.shuffle(items);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        WordListItemView wordListItemView;

        public ViewHolder(View itemView) {
            super(itemView);
            wordListItemView = (WordListItemView) itemView;
        }
    }
}