package com.example;

import java.util.ArrayList;
import java.util.List;

/* loaded from: classes5.dex */
public class FordiModel {
    private String colorHex;
    private String date;
    private String id;
    private List<FordiItemModel> items;
    private String title;

    public FordiModel() {
        this.items = new ArrayList();
        this.colorHex = "#F1F5F9";
    }

    public FordiModel(String id, String title, String date, List<FordiItemModel> items, String colorHex) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.items = items != null ? items : new ArrayList<>();
        this.colorHex = colorHex != null ? colorHex : "#F1F5F9";
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<FordiItemModel> getItems() {
        return this.items;
    }

    public void setItems(List<FordiItemModel> items) {
        this.items = items;
    }

    public String getColorHex() {
        return this.colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
