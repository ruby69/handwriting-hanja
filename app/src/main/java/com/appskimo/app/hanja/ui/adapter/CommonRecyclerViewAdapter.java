package com.appskimo.app.hanja.ui.adapter;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public abstract class CommonRecyclerViewAdapter<M, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final List<M> items = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        handler.post(() -> {
            items.clear();
            notifyDataSetChanged();
        });
    }

    public void add(final Collection<M> items) {
        handler.post(() -> {
            if (items != null) {
                int position = this.items.size();
                this.items.addAll(items);
                notifyItemRangeInserted(position, items.size());
            }
        });
    }

    public void reset(final Collection<M> items) {
        handler.post(() -> {
            this.items.clear();
            if (items != null) {
                this.items.addAll(items);
            }
            notifyDataSetChanged();
        });
    }
}