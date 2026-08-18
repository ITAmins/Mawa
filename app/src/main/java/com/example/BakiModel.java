package com.example;

/* loaded from: classes5.dex */
public class BakiModel {
    private double amount;
    private String customerName;
    private String date;
    private String details;
    private String id;

    public BakiModel() {
    }

    public BakiModel(String id, String customerName, double amount, String date, String details) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.date = date;
        this.details = details;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public String getDetails() {
        return this.details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
