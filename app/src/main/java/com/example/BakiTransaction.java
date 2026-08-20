package com.example;

import java.io.Serializable;

public class BakiTransaction implements Serializable {
    private String id;
    private String date;
    private String time;
    private String type; // "BAKI" (Debt added) or "JOMA" (Payment received)
    private double amount;
    private String note;
    private double balanceAfter;
    private long updatedAt;
    private long deletedAt;

    public BakiTransaction() {
        this.updatedAt = System.currentTimeMillis();
    }

    public BakiTransaction(String id, String date, String time, String type, double amount, String note, double balanceAfter) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.balanceAfter = balanceAfter;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public double getBalanceAfter() {
        return this.balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public long getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getDeletedAt() {
        return this.deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }
}
