package com.appskimo.app.hanja.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Collection;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(of = {"categoryUid", "name"})
@DatabaseTable(tableName = "Category")
public class Category implements Serializable {
    private static final long serialVersionUID = -617444128430377273L;

    public static final String FIELD_categoryUid = "categoryUid";

    @DatabaseField(columnName = FIELD_categoryUid, id = true, generatedId = false) protected Integer categoryUid;
    @DatabaseField protected String name;
    @ForeignCollectionField protected Collection<CategoryWord> categoryWords;
}
