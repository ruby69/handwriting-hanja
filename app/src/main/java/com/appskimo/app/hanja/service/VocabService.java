package com.appskimo.app.hanja.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.appskimo.app.hanja.BuildConfig;
import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Callback;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.More;
import com.appskimo.app.hanja.domain.Word;
import com.appskimo.app.hanja.event.CategorySelect;
import com.appskimo.app.hanja.support.SQLiteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.res.IntArrayRes;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.ormlite.annotations.OrmLiteDao;
import org.greenrobot.eventbus.EventBus;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@EBean(scope = EBean.Scope.Singleton)
public class VocabService {
    @RootContext Context context;

    @OrmLiteDao(helper = SQLiteOpenHelper.class) CategoryDao categoryDao;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) WordDao wordDao;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) CategoryWordDao categoryWordDao;

    @Pref PrefsService_ prefs;
    @SystemService ConnectivityManager connectivityManager;

    @IntArrayRes(R.array.category_ids) int [] categoryIds;

    private List<Category> categories;
    private Category selectedCategory;

    public boolean isConnected() {
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE);
        } else {
            return false;
        }
    }

    @Background
    public void checkHttpConnect(Callback<Void> callback) {
        callback.before();

        if (!isConnected()) {
            callback.onError();
            return;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(context.getString(R.string.url_check_http)).openConnection();
            conn.setRequestProperty("User-Agent","Android");
            conn.setConnectTimeout(3000);
            conn.connect();
            if (conn.getResponseCode() == 204) {
                callback.onSuccess(null);
            } else {
                callback.onError();
            }
        } catch (Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            if(conn != null){
                conn.disconnect();
            }
            callback.onFinish();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    @Background
    public void loadCategories(Callback<List<Category>> callback) {
        callback.before();
        try {
            if(categories == null || categories.isEmpty()) {
                categories = categoryDao.queryForAll();
            }
            callback.onSuccess(categories);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    private void selectCategory(Category selectedCategory) {
        this.selectedCategory = selectedCategory;
        EventBus.getDefault().post(new CategorySelect());
    }

    public void selectCategory(int position) {
        selectCategory(categories.get(position));
    }

    public List<Category> getCategories() {
        if(categories == null || categories.isEmpty()) {
            categories = categoryDao.queryForAll();
        }
        return categories;
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    @Background
    public void update(Word word, Callback<Void> callback) {
        callback.before();
        try {
            wordDao.update(word);
            callback.onSuccess(null);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void reset(Callback<Void> callback) {
        callback.before();
        try {
            if (selectedCategory != null) {
                wordDao.updateRaw(String.format(context.getString(R.string.sql_word_reset), selectedCategory.getCategoryUid()));
                categoryWordDao.updateRaw(context.getString(R.string.sql_categoryword_init));
                callback.onSuccess(null);
            }
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void search(String text, Callback<Collection<CategoryWord>> callback) {
        String likeText = "%".concat(text).concat("%");
        callback.before();
        try {
            QueryBuilder<CategoryWord, Integer> qb1 = categoryWordDao.queryBuilder();
            Where<CategoryWord, Integer> where1 = categoryWordDao.queryBuilder().where();
            Where<CategoryWord, Integer> left = null;
            Where<CategoryWord, Integer> right = null;
            Where<CategoryWord, Integer>[] others = new Where[categoryIds.length - 2];
            for(int i = 0; i<categoryIds.length; i++) {
                if (i == 0) {
                    left = where1.eq(Category.FIELD_categoryUid, categoryIds[i]);
                } else if (i == 1) {
                    right = where1.eq(Category.FIELD_categoryUid, categoryIds[i]);
                } else {
                    others[i - 2] = where1.eq(Category.FIELD_categoryUid, categoryIds[i]);
                }
            }
            qb1.setWhere(where1.or(left, right, others));

            QueryBuilder<Word, Integer> qb2 = wordDao.queryBuilder().orderBy(Word.FIELD_word, true);
            Where<Word, Integer> where2 = wordDao.queryBuilder().where();
            where2.or(where2.like(Word.FIELD_word, likeText), where2.like(Word.FIELD_means, likeText));
            qb2.setWhere(where2);

            callback.onSuccess(qb1.join(qb2).query());

        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void findCategoryWord(int categoryWordUid, Callback<CategoryWord> callback) {
        callback.before();
        try {
            callback.onSuccess(categoryWordDao.queryForId(categoryWordUid));
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    public List<CategoryWord> getRandomWords(Category category, long size) {
        try {
            QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderByRaw("RANDOM()").limit(size);
            qb.where().eq(Category.FIELD_categoryUid, category.getCategoryUid());
            qb.join(getWordQueryBuilder(Constants.CollectionType.ALL));
            return qb.query();
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
        }

        return null;
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    @Background
    public void findCategoryWord(Constants.CollectionType collectionType, int categoryWordUid, Callback<CategoryWord> callback) {
        callback.before();
        try {
            if (categoryWordUid > 0) {
                callback.onSuccess(categoryWordDao.queryForId(categoryWordUid));
            } else {
                callback.onSuccess(getCategoryWordForFirst(collectionType, true));
            }
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void findPrevOrNext(Constants.CollectionType collectionType, CategoryWord current, boolean ascending, Callback<CategoryWord> callback) {
        callback.before();

        try {
            CategoryWord categoryWord = null;
            long count = countOf(collectionType);
            if (count == 0) {
                categoryWord = getCategoryWordForFirst(collectionType, true);
            } else if (count == 1) {
                categoryWord = current;
            } else {
                categoryWord = findPrevOrNext(collectionType, current.getCategoryWordUid(), ascending);
                if (categoryWord == null) {
                    categoryWord = getCategoryWordForFirst(collectionType, ascending);
                }
            }
            callback.onSuccess(categoryWord);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void findRandomize(Constants.CollectionType collectionType, CategoryWord current, Callback<CategoryWord> callback) {
        callback.before();

        try {
            CategoryWord categoryWord = null;
            long count = countOf(collectionType);
            if (count == 0) {
                categoryWord = getCategoryWordForFirst(collectionType, true);
            } else if (count == 1) {
                categoryWord = current;
            } else {
                QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderByRaw("RANDOM()");
                qb.where().eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
                qb.join(getWordQueryBuilder(collectionType));

                do {
                    categoryWord = qb.queryForFirst();
                } while(current.getCategoryWordUid().intValue() == categoryWord.getCategoryWordUid());
            }
            callback.onSuccess(categoryWord);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    private CategoryWord getCategoryWordForFirst(Constants.CollectionType collectionType, boolean ascending) {
        try {
            QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, ascending);
            qb.where().eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
            return qb.join(getWordQueryBuilder(collectionType)).queryForFirst();
        } catch (SQLException e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return null;
        }
    }

    public long countOf(Constants.CollectionType collectionType) {
        try {
            QueryBuilder<CategoryWord, Integer> qb1 = categoryWordDao.queryBuilder();
            qb1.where().eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
            return qb1.join(getWordQueryBuilder(collectionType)).countOf();
        } catch (Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return 0;
        }
    }

    private CategoryWord findPrevOrNext(Constants.CollectionType collectionType, int categoryWordUid, boolean ascending) {
        try {
            QueryBuilder<CategoryWord, Integer> qb1 = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, ascending);
            Where<CategoryWord, Integer> where = qb1.where();
            Where<CategoryWord, Integer> condition1 = where.eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
            Where<CategoryWord, Integer> condition2 = ascending ? where.gt(CategoryWord.FIELD_categoryWordUid, categoryWordUid) : where.lt(CategoryWord.FIELD_categoryWordUid, categoryWordUid);
            qb1.setWhere(where.and(condition1, condition2));
            return qb1.join(getWordQueryBuilder(collectionType)).queryForFirst();
        } catch (Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return null;
        }
    }

    private QueryBuilder<Word, Integer> getWordQueryBuilder(Constants.CollectionType collectionType) throws SQLException {
        QueryBuilder<Word, Integer> qb = wordDao.queryBuilder();
        if (collectionType.isChecked()) {
            qb.where().eq(Word.FIELD_checked, true);
        } else if (collectionType.isMastered()) {
            qb.where().eq(Word.FIELD_completed, true);
        } else if (collectionType.isLearning()) {
            qb.where().eq(Word.FIELD_completed, false);
        }
        return qb;
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    @Background
    public void findCategoryWord(Set<String> checkedCategories, int categoryWordUid, Constants.CollectionType collectionType, Callback<CategoryWord> callback) {
        callback.before();
        try {
            if (categoryWordUid > 0) {
                callback.onSuccess(categoryWordDao.queryForId(categoryWordUid));
            } else {
                callback.onSuccess(getCategoryWordForFirst(checkedCategories, true, collectionType));
            }
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void findPrevOrNext(Set<String> checkedCategories, CategoryWord current, boolean ascending, Constants.CollectionType collectionType, Callback<CategoryWord> callback) {
        callback.before();

        try {
            CategoryWord categoryWord = null;
            long count = countOf(checkedCategories, collectionType);
            if (count == 0) {
                categoryWord = getCategoryWordForFirst(checkedCategories, true, collectionType);
            } else if (count == 1) {
                categoryWord = current;
            } else {
                categoryWord = findPrevOrNext(checkedCategories, current.getCategoryWordUid(), ascending, collectionType);
                if (categoryWord == null) {
                    categoryWord = getCategoryWordForFirst(checkedCategories, ascending, collectionType);
                }
            }
            callback.onSuccess(categoryWord);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void findRandomize(Set<String> checkedCategories, CategoryWord current, Constants.CollectionType collectionType, Callback<CategoryWord> callback) {
        callback.before();

        try {
            CategoryWord categoryWord = null;
            long count = countOf(checkedCategories, collectionType);
            if (count == 0) {
                categoryWord = getCategoryWordForFirst(checkedCategories, true, collectionType);
            } else if (count == 1) {
                categoryWord = current;
            } else {
                QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderByRaw("RANDOM()");
                qb.setWhere(getCategoryCondition(checkedCategories, qb.where()));
                if (!collectionType.isAll()) {
                    qb.join(getWordQueryBuilder(collectionType));
                }

                do {
                    categoryWord = qb.queryForFirst();
                } while(current.getCategoryWordUid().intValue() == categoryWord.getCategoryWordUid());
            }
            callback.onSuccess(categoryWord);
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    public long countOf(Set<String> checkedCategories, Constants.CollectionType collectionType) {
        try {
            QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder();
            qb.setWhere(getCategoryCondition(checkedCategories, qb.where()));
            return collectionType.isAll() ? qb.countOf() : qb.join(getWordQueryBuilder(collectionType)).countOf();
        } catch (Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return 0;
        }
    }

    private CategoryWord getCategoryWordForFirst(Set<String> checkedCategories, boolean ascending, Constants.CollectionType collectionType) {
        try {
            QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, ascending);
            qb.setWhere(getCategoryCondition(checkedCategories, qb.where()));
            return collectionType.isAll() ? qb.queryForFirst() : qb.join(getWordQueryBuilder(collectionType)).queryForFirst();
        } catch (SQLException e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return null;
        }
    }

    private CategoryWord findPrevOrNext(Set<String> checkedCategories, int categoryWordUid, boolean ascending, Constants.CollectionType collectionType) {
        try {
            QueryBuilder<CategoryWord, Integer> qb = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, ascending);
            Where<CategoryWord, Integer> where = qb.where();
            Where<CategoryWord, Integer> condition1 = getCategoryCondition(checkedCategories, where);
            Where<CategoryWord, Integer> condition2 = ascending ? where.gt(CategoryWord.FIELD_categoryWordUid, categoryWordUid) : where.lt(CategoryWord.FIELD_categoryWordUid, categoryWordUid);
            qb.setWhere(where.and(condition1, condition2));
            return collectionType.isAll() ? qb.queryForFirst() : qb.join(getWordQueryBuilder(collectionType)).queryForFirst();
        } catch (Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            return null;
        }
    }

    private Where<CategoryWord, Integer> getCategoryCondition(Set<String> checkedCategories, Where<CategoryWord, Integer> where) throws SQLException {
        Where<CategoryWord, Integer> condition = null;
        if (checkedCategories == null || checkedCategories.size() < 1) {
            List<Category> categories = getCategories();
            Category category = categories.get(categories.size() - 1);
            condition = where.eq(Category.FIELD_categoryUid, category.getCategoryUid());

        } else {
            String[] categoryIds = new String[checkedCategories.size()];
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

    // ------------------------------------------------------------------------------------------------------------------------------

    @Background
    public void retrieve(More more, Constants.CollectionType collectionType, Callback<More> callback) {
        callback.before();
        try {
            if (selectedCategory != null) {
                QueryBuilder<CategoryWord, Integer> qb1 = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, true).limit(more.getScale());
                if (more.getLastId() != null) {
                    Where<CategoryWord, Integer> where = categoryWordDao.queryBuilder().where();
                    Where<CategoryWord, Integer> left = where.eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
                    Where<CategoryWord, Integer> right = where.gt(CategoryWord.FIELD_categoryWordUid, more.getLastId());
                    qb1.setWhere(where.and(left, right));
                } else {
                    qb1.where().eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
                }

                QueryBuilder<Word, Integer> qb2 = wordDao.queryBuilder();
                if(collectionType.isChecked()) {
                    qb2.where().eq(Word.FIELD_checked, true);
                } else {
                    qb2.where().eq(Word.FIELD_completed, collectionType.isMastered());
                }

                List<CategoryWord> list = qb1.join(qb2).query();
                more.setContent(list);

                if (list != null && !list.isEmpty()) {
                    QueryBuilder<CategoryWord, Integer> qb3 = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_categoryWordUid, false);
                    CategoryWord last = qb3.queryForFirst();
                    CategoryWord lastContent = list.get(list.size() - 1);
                    more.setHasMore(last.getCategoryWordUid().intValue() > lastContent.getCategoryWordUid().intValue());
                } else {
                    more.setHasMore(false);
                }

                callback.onSuccess(more);
            }

        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void retrieveRand(More more, Constants.CollectionType collectionType, Callback<More> callback) {
        callback.before();
        try {
            if (selectedCategory != null) {
                QueryBuilder<CategoryWord, Integer> qb1 = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_rand, true).limit(more.getScale());
                if (more.getLastRand() != null) {
                    Where<CategoryWord, Integer> where = categoryWordDao.queryBuilder().where();
                    Where<CategoryWord, Integer> left = where.eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
                    Where<CategoryWord, Integer> right = where.gt(CategoryWord.FIELD_rand, more.getLastRand());
                    qb1.setWhere(where.and(left, right));
                } else {
                    qb1.where().eq(Category.FIELD_categoryUid, selectedCategory.getCategoryUid());
                }

                QueryBuilder<Word, Integer> qb2 = wordDao.queryBuilder();
                if(collectionType.isChecked()) {
                    qb2.where().eq(Word.FIELD_checked, true);
                } else {
                    qb2.where().eq(Word.FIELD_completed, collectionType.isMastered());
                }

                List<CategoryWord> list = qb1.join(qb2).query();
                more.setContent(list);

                if (list != null && !list.isEmpty()) {
                    QueryBuilder<CategoryWord, Integer> qb3 = categoryWordDao.queryBuilder().orderBy(CategoryWord.FIELD_rand, false);
                    CategoryWord last = qb3.queryForFirst();
                    CategoryWord lastContent = list.get(list.size() - 1);
                    more.setHasMore(last.getRand().longValue() > lastContent.getRand().longValue());
                } else {
                    more.setHasMore(false);
                }

                callback.onSuccess(more);
            }

        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    @Background
    public void updateRand(Callback<Void> callback) {
        callback.before();
        try {
            if (selectedCategory != null) {
                categoryWordDao.updateRaw(context.getString(R.string.sql_categoryword_rand));
                callback.onSuccess(null);
            }
        } catch(Exception e) {
            if(BuildConfig.DEBUG) {
                Log.e(getClass().getName(), e.getMessage(), e);
            }
            callback.onError();
        } finally {
            callback.onFinish();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    public float getDensity() {
        try {
            return context.getResources().getDisplayMetrics().density;
        } catch (Exception e) {
            return 1.3F;
        }
    }

    public float getGuideScaleUnit() {
        int scaleFactor = context.getResources().getInteger(R.integer.stroke_scale_factor);
        return (float) scaleFactor * 0.1F * getDensity();
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    public static class CategoryDao extends RuntimeExceptionDao<Category, Integer> {
        public CategoryDao(Dao<Category, Integer> dao) {
            super(dao);
        }
    }

    public static class WordDao extends RuntimeExceptionDao<Word, Integer> {
        public WordDao(Dao<Word, Integer> dao) {
            super(dao);
        }
    }

    public static class CategoryWordDao extends RuntimeExceptionDao<CategoryWord, Integer> {
        public CategoryWordDao(Dao<CategoryWord, Integer> dao) {
            super(dao);
        }
    }
}
