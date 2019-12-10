package com.appskimo.app.hanja.event;

import com.appskimo.app.hanja.Constants;

import lombok.Data;

@Data
public class WordCheck {
    private int categoryWordUid;
    private Constants.EventDomain eventDomain;
    private boolean checked;

    public WordCheck(int categoryWordUid, Constants.EventDomain eventDomain, boolean checked) {
        this.categoryWordUid = categoryWordUid;
        this.eventDomain = eventDomain;
        this.checked = checked;
    }
}
