package com.appskimo.app.hanja.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"categoryWordUid"})
@DatabaseTable(tableName = "CategoryWord")
public class CategoryWord implements Serializable {
    private static final long serialVersionUID = 6888590899155692765L;

    public static final String FIELD_categoryWordUid = "categoryWordUid";
    public static final String FIELD_rand = "rand";

    @DatabaseField(columnName = FIELD_categoryWordUid, generatedId = true) private Integer categoryWordUid;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Category.FIELD_categoryUid) private Category category;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = Word.FIELD_wordUid) private Word word;
    @DatabaseField private Long rand;

    private boolean open;
}
