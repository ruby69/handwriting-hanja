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
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.appskimo.app.hanja.BuildConfig;
import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.ui.view.UnconsciousView;
import com.appskimo.app.hanja.ui.view.UnconsciousView_;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.ormlite.annotations.OrmLiteDao;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@EService
public class UnconsciousService extends Service {
    private static final int NOTIFICATION_ID = 999;
    private static final String ACTION_STOP_SERVICE = "com.appskimo.app.hanja.support.UnconsciousService.ACTION_STOP_SERVICE";
    private static final String NOTI_CHANNEL_ID = "hanja";

    @SystemService NotificationManager notificationManager;
    @Pref PrefsService_ prefs;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) VocabService.CategoryDao categoryDao;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) VocabService.CategoryWordDao categoryWordDao;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) VocabService.WordDao wordDao;

    private UnconsciousView unconsciousView;
    private boolean runUnconsciousLearning;
    private Set<String> checkedCategories;
    private Constants.CollectionType collectionType = Constants.CollectionType.ALL;
    private List<Category> categories;

    @Override
    public void onCreate() {
        super.onCreate();
        unconsciousView = UnconsciousView_.build(this);
    }

    @AfterInject
    void afterInject() {
        checkedCategories = prefs.checkedCategories().get();

        int collectionTypeValue = prefs.collectionType().getOr(0);
        if (collectionTypeValue == 3) {
            collectionType = Constants.CollectionType.CHECKED;
        } else if (collectionTypeValue == 2) {
            collectionType = Constants.CollectionType.MASTERED;
        } else if (collectionTypeValue == 1) {
            collectionType = Constants.CollectionType.LEARNING;
        } else {
            collectionType = Constants.CollectionType.ALL;
        }
    }

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
                runUnconsciousLearning = true;
                runUnconsciousLearning();
                startForeground(NOTIFICATION_ID, createNotification(createStopIntent()));
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        runUnconsciousLearning = false;
        prefs.useUnconscious().put(false);
        destroyUnconsciousView();
        stopForeground(true);
        super.onDestroy();
    }

    @Background
    void runUnconsciousLearning() {
        try {
            while(runUnconsciousLearning) {
                var categoryWord = getWordByRandom();
                if (categoryWord == null) {
                    toast(R.string.message_no_words);
                    runUnconsciousLearning = false;
                    break;
                }

                if (unconsciousView != null) {
                    unconsciousView.populate(categoryWord);
                }
                try { Thread.sleep(prefs.interval().get() * 1000L); } catch (Exception e) {}
            }
        } catch (Exception e) {

        } finally {
            stopSelf();
        }
    }

    private void destroyUnconsciousView() {
        if (unconsciousView != null) {
            unconsciousView.destroy();
            unconsciousView = null;
        }
    }

    private PendingIntent createStopIntent() {
        var intent = UnconsciousService_.intent(getApplicationContext()).flags(PendingIntent.FLAG_IMMUTABLE).get();
        intent.setAction(ACTION_STOP_SERVICE);
        return PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification createNotification(PendingIntent stopIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(getApplicationContext(), getNotificationChannel())
                    .setContentTitle(getString(R.string.label_unconscious_noti_title))
                    .setContentText(getText(R.string.label_unconscious_noti_content))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(stopIntent)
                    .setSound(null)
                    .build();
        } else {
            return new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.label_unconscious_noti_title))
                    .setContentText(getText(R.string.label_unconscious_noti_content))
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
            notificationChannel = new NotificationChannel(NOTI_CHANNEL_ID, getText(R.string.label_unconscious), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription(getString(R.string.label_unconscious));
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return NOTI_CHANNEL_ID;
    }

    @UiThread
    void toast(int resId) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_LONG).show();
    }

    private CategoryWord getWordByRandom() {
        try {
            var qb = categoryWordDao.queryBuilder().orderByRaw("RANDOM()");
            qb.setWhere(getCategoryCondition(checkedCategories, qb.where()));
            return collectionType.isAll() ? qb.queryForFirst() : qb.join(getWordQueryBuilder(collectionType)).queryForFirst();
        } catch (SQLException e) {
            if (BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return null;
        }
    }

    private Where<CategoryWord, Integer> getCategoryCondition(Set<String> checkedCategories, Where<CategoryWord, Integer> where) throws SQLException {
        Where<CategoryWord, Integer> condition = null;
        if (checkedCategories == null || checkedCategories.size() < 1) {
            var categories = getCategories();
            var category = categories.get(categories.size() - 1);
            condition = where.eq(Category.FIELD_categoryUid, category.getCategoryUid());

        } else {
            var categoryIds = new String[checkedCategories.size()];
            checkedCategories.toArray(categoryIds);

            if (categoryIds.length < 2) {
                condition = where.eq(Category.FIELD_categoryUid, categoryIds[0]);

            } else {
                Where<CategoryWord, Integer> left = null;
                Where<CategoryWord, Integer> right = null;
                Where<CategoryWord, Integer>[] others = new Where[categoryIds.length - 2];
                for (int i = 0; i < categoryIds.length; i++) {
                    if (i == 0) {
                        left = where.eq(Category.FIELD_categoryUid, categoryIds[i]);
                    } else if (i == 1) {
                        right = where.eq(Category.FIELD_categoryUid, categoryIds[i]);
                    } else {
                        others[i - 2] = where.eq(Category.FIELD_categoryUid, categoryIds[i]);
                    }
                }
                condition = where.or(left, right, others);
            }
        }

        return condition;
    }

    private List<Category> getCategories() {
        if(categories == null || categories.isEmpty()) {
            categories = categoryDao.queryForAll();
        }
        return categories;
    }

    private QueryBuilder<Word, Integer> getWordQueryBuilder(Constants.CollectionType collectionType) throws SQLException {
        var qb = wordDao.queryBuilder();
        if (collectionType.isChecked()) {
            qb.where().eq(Word.FIELD_checked, true);
        } else if (collectionType.isMastered()) {
            qb.where().eq(Word.FIELD_completed, true);
        } else if (collectionType.isLearning()) {
            qb.where().eq(Word.FIELD_completed, false);
        }
        return qb;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(UnconsciousService_.intent(context).flags(PendingIntent.FLAG_IMMUTABLE).get());
        } else {
            UnconsciousService_.intent(context).flags(PendingIntent.FLAG_IMMUTABLE).start();
        }
    }
}
