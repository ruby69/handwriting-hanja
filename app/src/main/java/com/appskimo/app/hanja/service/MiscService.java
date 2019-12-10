package com.appskimo.app.hanja.service;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.appskimo.app.hanja.BuildConfig;
import com.appskimo.app.hanja.On;
import com.appskimo.app.hanja.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

@EBean(scope = EBean.Scope.Singleton)
public class MiscService {
    @RootContext Context context;
    @SystemService WindowManager windowManager;

    @Pref PrefsService_ prefs;
    @StringRes(R.string.admob_app_id) String admobAppId;
    @StringRes(R.string.admob_banner_unit_id) String bannerAdUnitId;

    private AdRequest adRequest;
    private AdView rectangleAdView;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean initializedMobileAds;
    public void initializeMobileAds() {
        if (!initializedMobileAds) {
            initializedMobileAds = true;
            MobileAds.initialize(context, admobAppId);
            generateRectangleAdView(context);
        }
    }

    @UiThread
    public void loadBannerAdView(AdView adView) {
        if (adView != null && !adView.isLoading()) {
            adView.loadAd(getAdRequest());
        }
    }

    private AdRequest getAdRequest() {
        if(adRequest == null) {
            AdRequest.Builder builder = new AdRequest.Builder();
            if(BuildConfig.DEBUG) {
                for(String device : context.getResources().getStringArray(R.array.t_devices)) {
                    builder.addTestDevice(device);
                }
            }
            adRequest = builder.build();
        }
        return adRequest;
    }

    private AdView generateRectangleAdView(Context context) {
        if (rectangleAdView == null) {
            rectangleAdView = new AdView(context);
            rectangleAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
            rectangleAdView.setAdUnitId(bannerAdUnitId);
            loadBannerAdView(rectangleAdView);
        }
        return rectangleAdView;
    }

    @UiThread
    public void showAdDialog(Activity activity, int positiveLabelResId, DialogInterface.OnClickListener positiveListener) {
        showAdDialog(activity, true, positiveLabelResId, positiveListener);
    }

    @UiThread
    public void showAdDialog(Activity activity, boolean cancelable, int positiveLabelResId, DialogInterface.OnClickListener positiveListener) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setCancelable(cancelable)
                .setTitle(" ")
                .setPositiveButton(positiveLabelResId, positiveListener)
                .create();

        AdView adView = generateRectangleAdView(activity);
        if (adView != null) {
            ViewParent parent = adView.getParent();
            if(parent != null) {
                ((ViewGroup) parent).removeView(adView);
            }
            dialog.setView(adView);
        }

        if (!activity.isFinishing() && !activity.isDestroyed()) {
            dialog.show();
        }
    }

    @UiThread
    public void showAdDialog(Activity activity, int titleResId, int positiveLabelResId, DialogInterface.OnClickListener positiveListener, int negativeLabelResId, DialogInterface.OnClickListener negativeListener) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(titleResId)
                .setPositiveButton(positiveLabelResId, positiveListener)
                .setNegativeButton(negativeLabelResId, negativeListener)
                .create();

        AdView adView = generateRectangleAdView(activity);
        if (adView != null) {
            ViewParent parent = adView.getParent();
            if(parent != null) {
                ((ViewGroup) parent).removeView(adView);
            }
            dialog.setView(adView);
        }

        if (!activity.isFinishing()) {
            dialog.show();
        }
    }


    @UiThread
    public void showAdDialog(Activity activity, On<Void> on) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(" ")
                .setPositiveButton(R.string.label_continue, (d,i) -> on.success(null))
                .setOnDismissListener(d -> on.success(null))
                .create();

        AdView adView = generateRectangleAdView(activity);
        if (adView != null) {
            ViewParent parent = adView.getParent();
            if(parent != null) {
                ((ViewGroup) parent).removeView(adView);
            }
            dialog.setView(adView);
        }

        if (!activity.isFinishing()) {
            dialog.show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void applyFontScale(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.fontScale = prefs.fontScale().getOr(0.85F);

        DisplayMetrics metrics = resources.getDisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;

        context.createConfigurationContext(configuration);
    }

}
