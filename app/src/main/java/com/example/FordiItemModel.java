package com.example;

/* loaded from: classes5.dex */
public class FordiItemModel {
    private String id;
    private boolean isChecked;
    private String name;
    private double price;

    public FordiItemModel() {
    }

    public FordiItemModel(String id, String name, boolean isChecked, double price) {
        this.id = id;
        this.name = name;
        this.isChecked = isChecked;
        this.price = price;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
