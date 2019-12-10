package com.appskimo.app.hanja.event;

import lombok.Data;

@Data
public class CategoryGameSelect {
    private int position;

    public CategoryGameSelect(int position) {
        this.position = position;
    }
}
