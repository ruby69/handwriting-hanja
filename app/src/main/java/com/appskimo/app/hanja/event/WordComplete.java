package com.appskimo.app.hanja.event;

import com.appskimo.app.hanja.Constants;

import lombok.Data;

@Data
public class WordComplete {
    private int categoryWordUid;
    private Constants.EventDomain eventDomain;
    private boolean completed;

    public WordComplete(int categoryWordUid, Constants.EventDomain eventDomain, boolean completed) {
        this.categoryWordUid = categoryWordUid;
        this.eventDomain = eventDomain;
        this.completed = completed;
    }
}
