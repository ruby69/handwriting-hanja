package com.appskimo.app.hanja.support;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.appskimo.app.hanja.BuildConfig;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.GameRecord;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.OnCompleteOrmLite;
import com.appskimo.app.hanja.event.ProgressMessage;
import com.appskimo.app.hanja.service.PrefsService_;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SQLiteOpenHelper extends OrmLiteSqliteOpenHelper {
    private Context context;
    private PrefsService_ prefs;

    public SQLiteOpenHelper(Context context) {
        super(context, context.getString(R.string.db_name), null, context.getResources().getInteger(R.integer.db_version));
        this.context = context;
    }

    public SQLiteOpenHelper setPrefs(PrefsService_ prefs) {
        this.prefs = prefs;
        return this;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            prefs.initializedDbStatus().put(1);

            dropAndCreateTables(connectionSource);
            initialize(database);

            prefs.initializedDbStatus().put(2);
            prefs.initializedDb().put(true);
        } catch (Exception e) {
            prefs.initializedDbStatus().put(3);
            prefs.initializedDb().put(false);
            if (BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
        } finally {
            EventBus.getDefault().post(new OnCompleteOrmLite());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            prefs.initializedDbStatus().put(1);

            dropAndCreateTables(connectionSource);
            initialize(database);

            prefs.initializedDbStatus().put(2);
            prefs.initializedDb().put(true);
        } catch (Exception e) {
            prefs.initializedDbStatus().put(3);
            prefs.initializedDb().put(false);
            if (BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
        } finally {
            EventBus.getDefault().post(new OnCompleteOrmLite());
        }
    }

    private void dropAndCreateTables(ConnectionSource connectionSource) throws Exception {
        TableUtils.dropTable(connectionSource, GameRecord.class, true);
        TableUtils.dropTable(connectionSource, CategoryWord.class, true);
        TableUtils.dropTable(connectionSource, Word.class, true);
        TableUtils.dropTable(connectionSource, Category.class, true);

        TableUtils.createTable(connectionSource, Word.class);
        TableUtils.createTable(connectionSource, Category.class);
        TableUtils.createTable(connectionSource, CategoryWord.class);
        TableUtils.createTable(connectionSource, GameRecord.class);
    }

    private void initialize(SQLiteDatabase database) throws Exception {
        executeQuery(database, categoryBinderJson, R.string.sql_category_insert, R.raw.category);
        executeQuery(database, categoryWordBinderJson, R.string.sql_categoryword_insert, R.raw.categoryword);
        executeQuery(database, wordBinderJson, R.string.sql_word_insert, R.raw.word1);
        executeQuery(database, R.string.sql_categoryword_init);
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    private BinderJson categoryBinderJson = (statement, itemArray) -> {
        statement.bindLong(1, itemArray.getLong(0));
        statement.bindString(2, itemArray.getString(1));
    };

    private BinderJson categoryWordBinderJson = (statement, itemArray) -> {
        statement.bindLong(1, itemArray.getLong(0));
        statement.bindLong(2, itemArray.getLong(1));
    };

    private BinderJson wordBinderJson = (statement, itemArray) -> {
        statement.bindLong(1, itemArray.getLong(0));
        statement.bindString(2, itemArray.getString(1));
        statement.bindString(3, itemArray.getString(2));
        statement.bindString(4, itemArray.getString(3));
        statement.bindString(5, itemArray.getString(4));
        statement.bindString(6, itemArray.getString(5));
    };

    // ------------------------------------------------------------------------------------------------------------------------------

    public interface BinderJson {
        void bind(SQLiteStatement statement, JSONArray itemArray) throws Exception;
    }

    private void executeQuery(SQLiteDatabase database, int queryResId) {
        SQLiteStatement statement = null;
        try {
            database.beginTransaction();
            statement = database.compileStatement(context.getString(queryResId));
            statement.execute();
            statement.close();
            database.setTransactionSuccessful();
        } finally {
            if (statement != null) {
                statement.close();
            }

            if (database.inTransaction()) {
                database.endTransaction();
            }
        }
    }

    private void executeQuery(SQLiteDatabase database, BinderJson binderJson, int queryResId, int... arr) throws Exception {
        BufferedReader br = null;
        SQLiteStatement statement = null;
        try {
            database.beginTransaction();
            statement = database.compileStatement(context.getString(queryResId));

            for (int j = 0; j < arr.length; j++) {
                EventBus.getDefault().post(new ProgressMessage(context.getResources().getString(R.string.message_progress_update) + " ::: " + (j + 1) + "/" + arr.length));
                br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(arr[j]), "utf-8"));
                String str = br.readLine();
                if (str != null) {
                    JSONArray jsonArr = new JSONArray(str);
                    for (int i = 0; i < jsonArr.length(); i++) {
                        binderJson.bind(statement, (JSONArray) jsonArr.get(i));
                        statement.execute();
                    }
                }
                br.close();
            }

            statement.close();
            database.setTransactionSuccessful();
        } finally {
            if (statement != null) {
                statement.close();
            }

            if (database.inTransaction()) {
                database.endTransaction();
            }

            if (br != null) {
                br.close();
            }
        }
    }

}