package com.appskimo.app.hanja.event;

import lombok.Data;

@Data
public class UnconsciousTheme {
    private int resId;

    public UnconsciousTheme(int resId) {
        this.resId = resId;
    }
}
