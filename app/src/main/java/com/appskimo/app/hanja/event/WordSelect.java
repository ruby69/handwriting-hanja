package com.appskimo.app.hanja.event;

import com.appskimo.app.hanja.Constants;

import lombok.Data;

@Data
public class WordSelect {
    private int categoryWordUid;
    private Constants.CollectionType collectionType;

    public WordSelect(int categoryWordUid, Constants.CollectionType collectionType) {
        this.categoryWordUid = categoryWordUid;
        this.collectionType = collectionType;
    }
}
