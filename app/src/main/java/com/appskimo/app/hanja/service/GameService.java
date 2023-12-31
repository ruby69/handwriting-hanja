package com.appskimo.app.hanja.service;

import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.domain.Category;
import com.appskimo.app.hanja.domain.CategoryWord;
import com.appskimo.app.hanja.domain.GameRecord;
import com.appskimo.app.hanja.support.SQLiteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.ormlite.annotations.OrmLiteDao;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@EBean(scope = EBean.Scope.Singleton)
public class GameService {
    @RootContext Context context;
    @OrmLiteDao(helper = SQLiteOpenHelper.class) GameRecordDao gameRecordDao;
    @Bean VocabService vocabService;
    @ColorRes(R.color.white) int white;

    private Session currentSession;

    public List<Category> getCategories() {
        return vocabService.getCategories();
    }

    public void createGameRecord() {
        gameRecordDao.create(getGameRecord());
    }

    private GameRecord getGameRecord() {
        var gameRecord = new GameRecord();

        gameRecord.setGameId(1);
        gameRecord.setCategory(currentSession.getCategory());
        gameRecord.setWordCount(currentSession.getSelectedWordCount());
        gameRecord.setRecordTime(currentSession.getElapsedTime());
        gameRecord.setScore(currentSession.getScore());
        gameRecord.setMaxComboCount(currentSession.getMaxComboCount());
        gameRecord.setUserUid(1L);
        gameRecord.setUserId("user_local");
        gameRecord.setUserNick("user_nick");
        gameRecord.setComment("hello jvocab");
        gameRecord.setRegistTime(new Date());

        return gameRecord;
    }

    public List<GameRecord> getTopRank() {
        try {
            return gameRecordDao.queryBuilder().orderBy(GameRecord.FIELD_score, false).limit(100L).query();
        } catch (SQLException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    public void initializeSession(int position, int count, long hintDelay) {
        currentSession = new Session();

        var category = getCategories().get(position);
        var words = vocabService.getRandomWords(category, count);

        currentSession.setCategory(category);
        currentSession.setWords(words);
        currentSession.getRemainWords().addAll(words);
        currentSession.setSelectedCategoryPosition(position);
        currentSession.setSelectedWordCount(count);
        currentSession.setSelectedHintDelay(hintDelay);
    }

    public void initBoard(List<TextView> buttons) {
        var words = currentSession.getWords();
        var usedWords = currentSession.getUsedWords();
        var remainWords = currentSession.getRemainWords();

        int size = words.size();
        if (size > buttons.size()) {
            size = buttons.size();
        }

        for (int i = 0; i<size; i++) {
            var word = getWord(i);
            var button = buttons.get(i);
            button.setText(word.getWord().getWord());
            button.setTag(word);

            usedWords.add(word);
        }
        remainWords.removeAll(usedWords);
    }

    public CategoryWord getWord(int position) {
        var words = currentSession.getWords();
        if (words.size() > position) {
            return words.get(position);
        } else {
            return null;
        }
    }

    public synchronized void readySession() {
        currentSession.setPausedTime(0L);
        currentSession.setLastTouchedTime(0L);
        currentSession.setStatus(SessionStatus.READY);
    }

    public synchronized void startSession() {
        currentSession.setLastTouchedTime(SystemClock.elapsedRealtime());
        currentSession.setStatus(SessionStatus.PLAY);
    }

    public synchronized void stopSession() {
        currentSession.setStatus(SessionStatus.STOP);
    }

    public synchronized void cancelSession() {
        currentSession.setStatus(SessionStatus.CANCEL);
    }

    public synchronized void pauseSession() {
        currentSession.setPausedTime(System.currentTimeMillis());
        currentSession.setStatus(SessionStatus.PAUSE);
    }

    public synchronized void resumeSession() {
        currentSession.setPausedTime(0L);
        currentSession.setLastTouchedTime(SystemClock.elapsedRealtime());
        currentSession.setStatus(SessionStatus.PLAY);
    }

    public boolean isReady() {
        return currentSession.getStatus() == SessionStatus.READY;
    }

    public boolean isPlaying() {
        return currentSession.getStatus() == SessionStatus.PLAY;
    }

    public boolean hasCanceled() {
        return currentSession.getStatus() == SessionStatus.CANCEL;
    }

    public boolean hasStopped() {
        return currentSession == null || currentSession.getStatus() == SessionStatus.STOP;
    }

    public boolean availableHint() {
        return currentSession.getStatus() == SessionStatus.PLAY && SystemClock.elapsedRealtime() - currentSession.getLastTouchedTime() > currentSession.getSelectedHintDelay();
    }

    public boolean hasLockedTouch() {
        return currentSession.getStatus() != SessionStatus.PLAY;
    }

    public CategoryWord getCurrentWord() {
        return currentSession.getCurrentWord();
    }

    public Category getCurrentCategory() {
        return currentSession.getCategory();
    }

    public void setLastTouchedTime(long lastTouchedTime) {
        currentSession.setLastTouchedTime(lastTouchedTime);
    }

    public boolean emptyWordsInSession() {
        return isEmptyWords() && isEmptyRemain();
    }

    public boolean isEmptyWords() {
        return currentSession.getWords().isEmpty();
    }

    public boolean isEmptyRemain() {
        return currentSession.getRemainWords().isEmpty();
    }

    public CategoryWord getRemovedRemainWord() {
        return currentSession.getRemainWords().remove(0);
    }

    public CategoryWord getRemovedCurrentWord() {
        var word = currentSession.getWords().remove(0);
        currentSession.setCurrentWord(word);
        return word;
    }

    public int getCategoryPosition() {
        return currentSession.getSelectedCategoryPosition();
    }

    public long getPausedTime() {
        return currentSession.getPausedTime();
    }

    public synchronized void processCombo(long currentTouchedTime){
        boolean isCombo = currentTouchedTime - currentSession.getLastTouchedTime() < 1500L;
        if (isCombo){
            currentSession.increaseComboCount();
        } else {
            currentSession.resetComboCount();
        }
    }

    public synchronized void processScore(long currentTouchedTime) {
        long addedValue = Session.SCORE_BASE;
        long timeGap = currentTouchedTime - currentSession.getLastTouchedTime();
        long hintDelay = currentSession.getSelectedHintDelay();
        if (hintDelay == 3000L) {
            if (timeGap < 501L) {
                addedValue -= 0;
            } else if(timeGap < 1001L) {
                addedValue -= 2;
            } else if(timeGap < 1501L) {
                addedValue -= 3;
            } else if(timeGap < 2001L) {
                addedValue -= 4;
            } else if(timeGap < 2501L) {
                addedValue -= 5;
            } else if(timeGap < 3001L) {
                addedValue -= 6;
            } else {
                addedValue -= 10;
            }
        } else {
            if(timeGap < 501L) {
                addedValue -= 0;
            } else if(timeGap < 1001L) {
                addedValue -= 2;
            } else if(timeGap < 1501L) {
                addedValue -= 3;
            } else if(timeGap < 2001L) {
                addedValue -= 4;
            } else if(timeGap < 2501L) {
                addedValue -= 5;
            } else if(timeGap < 3001L) {
                addedValue -= 6;
            } else if(timeGap < 5001L) {
                addedValue -= 8;
            } else {
                addedValue -= 10;
            }
        }

        long score = Session.SCORE_BASE + addedValue;

        int comboCount = currentSession.getComboCount();
        if(comboCount > 8) { // 9
            score *= 10;
        } else if(comboCount > 7) { // 8
            score *= 9;
        } else if(comboCount > 6) { // 7
            score *= 8;
        } else if(comboCount > 5) { // 6
            score *= 7;
        } else if(comboCount > 4) { // 5
            score *= 6;
        } else if(comboCount > 3) { // 4
            score *= 5;
        } else if(comboCount > 2) { // 3
            score *= 4;
        } else if(comboCount > 1) { // 2
            score *= 3;
        } else if(comboCount > 0) { // 1
            score *= 2;
        }

        currentSession.increaseScore(score);
    }

    public long getScore() {
        return currentSession.getScore();
    }

    public int getMaxComboCount() {
        return currentSession.getMaxComboCount();
    }

    public void setElapsedTime(long elapsedTime) {
        currentSession.setElapsedTime(elapsedTime);
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    @Data
    private class Session implements Serializable {
        private static final long serialVersionUID = -7655741687947673268L;

        public static final long SCORE_BASE = 10L;
        private SessionStatus status = SessionStatus.STOP;

        private int selectedCategoryPosition;
        private int selectedWordCount;
        private long selectedHintDelay;

        private Category category;
        private List<CategoryWord> words;
        private List<CategoryWord> remainWords = new ArrayList<>();
        private List<CategoryWord> usedWords = new ArrayList<>();
        private CategoryWord currentWord;

        private long pausedTime;
        private long lastTouchedTime;

        private int comboCount;
        private int maxComboCount;
        private long score;
        private long elapsedTime;

        public void resetComboCount() {
            comboCount = 0;
        }

        public void increaseComboCount() {
            comboCount++;
            maxComboCount = maxComboCount > comboCount ? maxComboCount : comboCount;
        }

        public void increaseScore(long score) {
            this.score += score;
        }
    }

    private enum SessionStatus {
        READY, PLAY, PAUSE, STOP, CANCEL
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    public static class GameRecordDao extends RuntimeExceptionDao<GameRecord, Integer> {
        public GameRecordDao(Dao<GameRecord, Integer> dao) {
            super(dao);
        }
    }
}
