package com.appskimo.app.hanja.ui.view;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.event.CategoryGameSelect;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;

@EViewGroup(R.layout.view_category_item)
public class CategoryItemView extends RelativeLayout {
    @ViewById(R.id.titleView) TextView titleView;

    private int position = -1;

    public CategoryItemView(Context context) {
        super(context);
    }

    public void setTitle(int position, String title) {
        this.position = position;
        titleView.setText(title);
    }

    @Click(R.id.titleView)
    void onClick() {
        if(position != -1) {
            EventBus.getDefault().post(new CategoryGameSelect(position));
        }
    }
}