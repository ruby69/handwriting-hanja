package com.appskimo.app.hanja.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Data
@ToString(of = {"wordUid", "word", "checked"})
@DatabaseTable(tableName = "Word")
public class Word implements Serializable {
    private static final long serialVersionUID = -335796383193905632L;

    public static final String FIELD_wordUid = "wordUid";
    public static final String FIELD_word = "word";
    public static final String FIELD_bushou = "bushou";
    public static final String FIELD_strokeCount = "strokeCount";
    public static final String FIELD_means = "means";
    public static final String FIELD_path = "path";
    public static final String FIELD_completed = "completed";
    public static final String FIELD_checked = "checked";

    @DatabaseField(columnName = FIELD_wordUid, id = true, generatedId = false) private Integer wordUid;
    @DatabaseField private String word;
    @DatabaseField private String bushou;
    @DatabaseField private int strokeCount;
    @DatabaseField private String means;
    @DatabaseField private String path;
    @DatabaseField(defaultValue = "false") private boolean completed;
    @DatabaseField(defaultValue = "false") private boolean checked;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Category.FIELD_categoryUid) private CategoryWord categoryWord;

    public static final String QUERY_RESET = "";

    private List<String> pathList;
    private List<OrderPoint> orderList;

    public String getMeansForCard() {
        return getMeans().replaceAll(",", "\n");
    }

    public String getMeansForNote() {
        return getMeans().replaceAll(",", ", ");
    }

    public String getMeansForList() {
        String value = getMeans().split(",")[0];
        int i = value.indexOf("(");
        return (i > 0) ? value.substring(0, i) : value;
    }

    public String getMeansForGame() {
        return getMeansForNote();
    }

    public String getMeaningForTts() {
        String str = getMeansForList();
        String result = str + ". ";

        int i = str.lastIndexOf(" ");
        if (i > 0) {
            result = str.substring(0, i) + ", " + str.substring(i) + ". ";
        }

        return result;
    }

    private static final String INFO = "%s / %s획";
    public String getInfo() {
        return String.format(INFO, bushou, strokeCount);
    }

    public List<String> getPathList() {
        if (pathList == null) {
            pathList = new ArrayList<>();

            String[] temp = path.split(";");
            for (String pa : temp[0].split("[|]")) {
                pathList.add(pa);
            }
        }
        return pathList;
    }

    public List<OrderPoint> getOrderList() {
        if (orderList == null) {
            orderList = new ArrayList<>();

            String[] temp = path.split(";");
            String[] split = temp[1].split("[|]");
            for (int i = 0; i<split.length; i++) {
                orderList.add(new OrderPoint(i + 1, split[i]));
            }
        }
        return orderList;
    }

    @Getter
    public static class OrderPoint {
        private int order;
        private float x;
        private float y;

        public OrderPoint(int order, String str) {
            this.order = order;
            String[] temp = str.split(",");
            x = Float.valueOf(temp[0]);
            y = Float.valueOf(temp[1]);
        }
    }
}
