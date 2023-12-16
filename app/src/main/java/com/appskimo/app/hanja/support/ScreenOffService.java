package com.appskimo.app.hanja.support;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.LockActivity_;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.event.OnFinishLockscreenActivityAll;
import com.appskimo.app.hanja.service.PrefsService_;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;

import java.util.Date;

@EService
public class ScreenOffService extends Service {
    private static final int NOTIFICATION_ID = 999999999;
    private static final String ACTION_STOP_SERVICE = "com.appskimo.app.hanja.support.ScreenOffService.ACTION_STOP_SERVICE";
    private static final String NOTI_CHANNEL_ID = "hanja_screen_off";

    @Pref PrefsService_ prefs;
    @SystemService NotificationManager notificationManager;

    private boolean busyPhone;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
                notificationManager.cancel(NOTIFICATION_ID);
                stopSelf();

            } else {
                startForeground(NOTIFICATION_ID, createNotification(createStopIntent()));
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().post(new OnFinishLockscreenActivityAll());
        prefs.useLockScreen().put(false);
        stopForeground(true);
        super.onDestroy();
    }

    private PendingIntent createStopIntent() {
        var intent = ScreenOffService_.intent(getApplicationContext()).flags(PendingIntent.FLAG_IMMUTABLE).get();
        intent.setAction(ACTION_STOP_SERVICE);
        return PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification createNotification(PendingIntent stopIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(getApplicationContext(), getNotificationChannel())
                    .setContentTitle(getString(R.string.label_lockscreen_noti_title))
                    .setContentText(getText(R.string.label_lockscreen_noti_content))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(stopIntent)
                    .setSound(null)
                    .build();
        } else {
            return new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.label_lockscreen_noti_title))
                    .setContentText(getText(R.string.label_lockscreen_noti_content))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(stopIntent)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private String getNotificationChannel() {
        var notificationChannel = notificationManager.getNotificationChannel(NOTI_CHANNEL_ID);
        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(NOTI_CHANNEL_ID, getText(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription(getString(R.string.app_name));
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return NOTI_CHANNEL_ID;
    }

    @Receiver(actions = {Intent.ACTION_SCREEN_OFF}, registerAt = Receiver.RegisterAt.OnCreateOnDestroy)
    void onActionScreenOnOff(Context context, Intent intent) {
        if (busyPhone) {
            return;
        }

        if (!prefs.useLockScreen().get()) {
            return;
        }

        final long now = new Date().getTime();
        long gap = now - prefs.lockOffTime().get();
        if (gap < Constants.MIN_5) {
            return;
        }

        LockActivity_.intent(context).flags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | PendingIntent.FLAG_IMMUTABLE).start();
    }

    @Receiver(actions = {TelephonyManager.ACTION_PHONE_STATE_CHANGED}, registerAt = Receiver.RegisterAt.OnCreateOnDestroy)
    void onActionPhoneStateChanged(Intent intent) {
        var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state) || Intent.ACTION_NEW_OUTGOING_CALL.equals(state) || TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            EventBus.getDefault().post(new OnFinishLockscreenActivityAll());
            busyPhone = true;
        } else {
            busyPhone = false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(ScreenOffService_.intent(context).flags(PendingIntent.FLAG_IMMUTABLE).get());
        } else {
            ScreenOffService_.intent(context).flags(PendingIntent.FLAG_IMMUTABLE).start();
        }
    }
}

