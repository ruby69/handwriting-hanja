package com.appskimo.app.hanja;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.appskimo.app.hanja.event.OnCompleteOrmLite;
import com.appskimo.app.hanja.event.ProgressMessage;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.support.EventBusObserver;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.Subscribe;

@Fullscreen
@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends AppCompatActivity {
    @ViewById(R.id.progressMessage) TextView progressMessage;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;
    @Extra("notiCategoryWordUid") Integer notiCategoryWordUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(new EventBusObserver.AtCreateDestroy(this));
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initialize();
    }

    @UiThread
    void initialize() {
        if (prefs.initializedDb().get() || prefs.initializedDbStatus().get() == 2) { // succeed
            postInitialize();

        } else if (prefs.initializedDbStatus().get() == 3) { // failed
            deleteDatabase(getString(R.string.db_name));
            Toast.makeText(getApplicationContext(), getString(R.string.message_db_fail), Toast.LENGTH_LONG).show();
            finish();
        } else {
            // nothing to do..
            // wait until receive on complete orm-lite event.
        }
    }

    @Subscribe
    public void onEvent(OnCompleteOrmLite event) {
        initialize();
    }


    @UiThread
    void postInitialize() {
        launchMain();
    }

    @UiThread
    void launchMain() {
        int count = prefs.executedCount().get();
        prefs.executedCount().put(++count);
        MainActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
        finish();
    }

    @UiThread
    void updateProgressMessage(String message) {
        progressMessage.setText(message);
    }

    @Subscribe
    public void onEvent(ProgressMessage event) {
        updateProgressMessage(event.getMessage());
    }
}
