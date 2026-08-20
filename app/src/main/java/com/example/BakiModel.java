package com.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BakiModel implements Serializable {
    private String id;
    private String customerName;
    private String phone;
    private double amount;
    private String date;
    private String dueDate;
    private String details;
    private List<BakiTransaction> transactions;
    private long updatedAt;
    private long deletedAt;

    public BakiModel() {
        this.transactions = new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }

    public BakiModel(String id, String customerName, double amount, String date, String details) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.date = date;
        this.details = details;
        this.phone = "";
        this.dueDate = "";
        this.transactions = new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
    }

    public BakiModel(String id, String customerName, String phone, double amount, String date, String dueDate, String details) {
        this.id = id;
        this.customerName = customerName;
        this.phone = phone != null ? phone : "";
        this.amount = amount;
        this.date = date;
        this.dueDate = dueDate != null ? dueDate : "";
        this.details = details;
        this.transactions = new ArrayList<>();
        this.updatedAt = System.currentTimeMillis();
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

    public String getPhone() {
        return this.phone != null ? this.phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone : "";
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

    public String getDueDate() {
        return this.dueDate != null ? this.dueDate : "";
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate != null ? dueDate : "";
    }

    public String getDetails() {
        return this.details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<BakiTransaction> getTransactions() {
        if (this.transactions == null) {
            this.transactions = new ArrayList<>();
        }
        return this.transactions;
    }

    public void setTransactions(List<BakiTransaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public void addTransaction(BakiTransaction transaction) {
        if (this.transactions == null) {
            this.transactions = new ArrayList<>();
        }
        this.transactions.add(transaction);
        this.updatedAt = System.currentTimeMillis();
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
