package com.example;

/* loaded from: classes5.dex */
public class ExpenseModel {
    private double amount;
    private String date;
    private String id;
    private String name;
    private String time;

    public ExpenseModel(String id, String name, double amount, String date, String time) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.time = time;
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

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
