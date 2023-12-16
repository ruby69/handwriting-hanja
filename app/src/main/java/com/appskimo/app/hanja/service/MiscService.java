package com.appskimo.app.hanja.service;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.appskimo.app.hanja.R;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EBean(scope = EBean.Scope.Singleton)
public class MiscService {
    @RootContext Context context;
    @SystemService WindowManager windowManager;

    @Pref PrefsService_ prefs;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @UiThread
    public void showDialog(Activity activity, int positiveLabelResId, DialogInterface.OnClickListener positiveListener) {
        var dialog = new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setMessage(activity.getString(positiveLabelResId))
                .setPositiveButton(R.string.label_confirm, positiveListener)
                .create();

        if (!activity.isFinishing() && !activity.isDestroyed()) {
            dialog.show();
        }
    }

    @UiThread
    public void showDialog(Activity activity, int titleResId, int positiveLabelResId, DialogInterface.OnClickListener positiveListener, int negativeLabelResId, DialogInterface.OnClickListener negativeListener) {
        var dialog = new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(titleResId)
                .setPositiveButton(positiveLabelResId, positiveListener)
                .setNegativeButton(negativeLabelResId, negativeListener)
                .create();

        if (!activity.isFinishing()) {
            dialog.show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void applyFontScale(Context context) {
        var resources = context.getResources();
        var configuration = resources.getConfiguration();
        configuration.fontScale = prefs.fontScale().getOr(0.85F);

        var metrics = resources.getDisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;

        context.createConfigurationContext(configuration);
    }

}
