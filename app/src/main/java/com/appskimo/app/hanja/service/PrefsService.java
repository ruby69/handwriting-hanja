package com.appskimo.app.hanja.service;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultLong;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

import java.util.Set;

@SharedPref(value = SharedPref.Scope.UNIQUE)
public interface PrefsService {
    @DefaultInt(0)
    int executedCount();

    @DefaultFloat(10.0F)
    float guideLineStrokeWidth();

    @DefaultInt(2)
    int strokeRepeatCount();

    @DefaultInt(3)
    int strokeSpeed();

    @DefaultBoolean(false)
    boolean checkedKanjiNotice();

    @DefaultBoolean(false)
    boolean initializedDb();

    @DefaultInt(0) // 0 - before, 1 - prgoress, 2 - succeed, 3 - failed
    int initializedDbStatus();

    @DefaultFloat(0.85F)
    float fontScale();

/////////////////////////////////////////////////////////

    @DefaultBoolean(false)
    boolean useLockScreen();

    Set<String> checkedCategories();

    @DefaultInt(0)
    int position();

    @DefaultBoolean(false)
    boolean showMeans();

    @DefaultBoolean(true)
    boolean showWord();

    @DefaultBoolean(true)
    boolean showRomaji();

    @DefaultInt(0)
    int categoryWordUid();

    @DefaultInt(0)
    int collectionType();

    @DefaultBoolean(true)
    boolean useClock();
    int clockPosX();
    int clockPosY();

    @DefaultBoolean(false)
    boolean autoSpeech();

    @DefaultLong(0)
    long lockOffTime();

/////////////////////////////////////////////////////////

    @DefaultBoolean(false)
    boolean useUnconscious();

    int unconsciousPosX();
    int unconsciousPosY();

    int bgResId();

    @DefaultInt(10)
    int interval();
}
