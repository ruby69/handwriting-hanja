package com.appskimo.app.hanja.event;

import com.appskimo.app.hanja.Constants;

import lombok.Data;

@Data
public class WordsMore {
    private Constants.EventDomain eventDomain;
    private Constants.CollectionType collectionType;

    public WordsMore(Constants.EventDomain eventDomain, Constants.CollectionType collectionType) {
        this.eventDomain = eventDomain;
        this.collectionType = collectionType;
    }
}
