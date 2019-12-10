package com.appskimo.app.hanja.support;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.ReceiverAction;

@EReceiver
public class CommonReceiver extends BroadcastReceiver {
    @ReceiverAction(actions = Intent.ACTION_BOOT_COMPLETED)
    void onActionBootComplete(Context context) {}

    @ReceiverAction(actions = Intent.ACTION_MY_PACKAGE_REPLACED)
    void onActionMyPackageReplaced(Context context) {}

    @ReceiverAction(actions = Intent.ACTION_PACKAGE_REPLACED, dataSchemes = "package")
    void onActionPackageReplaced(Context context) {}

    @Override public void onReceive(Context context, Intent intent) {}
}