package com.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fordi (Shopping List) Model for MAWA Phase 2.
 * Represents a complete shopping sheet with planned vs actual summary,
 * potential profit estimation, and accounting posting tracking.
 */
public class FordiModel implements Serializable {
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SHOPPING = "SHOPPING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_POSTED = "POSTED";

    private String id;
    private String title;
    private String date; // "dd/MM/yyyy"
    private List<FordiItemModel> items;
    private String colorHex;
    private String status; // DRAFT, SHOPPING, COMPLETED, POSTED
    private boolean postedToAccounting;
    private String postedExpenseId;
    private double postedAmount;
    private String postedDate;
    private long updatedAt;
    private long deletedAt;

    public FordiModel() {
        this.id = UUID.randomUUID().toString();
        this.items = new ArrayList<>();
        this.colorHex = "#F0FDFA";
        this.status = STATUS_DRAFT;
        this.updatedAt = System.currentTimeMillis();
    }

    public FordiModel(String id, String title, String date, List<FordiItemModel> items, String colorHex) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        this.title = title;
        this.date = date;
        this.items = items != null ? items : new ArrayList<>();
        this.colorHex = colorHex != null ? colorHex : "#F0FDFA";
        this.status = STATUS_DRAFT;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getPlannedTotal() {
        double total = 0.0;
        if (items != null) {
            for (FordiItemModel item : items) {
                total += item.getPlannedTotal();
            }
        }
        return total;
    }

    public double getActualTotal() {
        double total = 0.0;
        if (items != null) {
            for (FordiItemModel item : items) {
                total += item.getActualTotal();
            }
        }
        return total;
    }

    public double getPotentialProfit() {
        double total = 0.0;
        if (items != null) {
            for (FordiItemModel item : items) {
                total += item.getPotentialProfit();
            }
        }
        return total;
    }

    public int getBoughtItemCount() {
        int count = 0;
        if (items != null) {
            for (FordiItemModel item : items) {
                if (!FordiItemModel.STATUS_NOT_BOUGHT.equals(item.getStatus()) && item.getActualQuantity() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getTotalItemCount() {
        return items != null ? items.size() : 0;
    }

    // Getters and Setters

    public String getId() {
        return id != null ? id : UUID.randomUUID().toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "বাজার ফর্দ";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<FordiItemModel> getItems() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        return this.items;
    }

    public void setItems(List<FordiItemModel> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getColorHex() {
        return colorHex != null ? colorHex : "#F0FDFA";
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getStatus() {
        if (status == null) {
            return postedToAccounting ? STATUS_POSTED : STATUS_DRAFT;
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPostedToAccounting() {
        return postedToAccounting;
    }

    public void setPostedToAccounting(boolean postedToAccounting) {
        this.postedToAccounting = postedToAccounting;
        if (postedToAccounting) {
            this.status = STATUS_POSTED;
        }
    }

    public String getPostedExpenseId() {
        return postedExpenseId;
    }

    public void setPostedExpenseId(String postedExpenseId) {
        this.postedExpenseId = postedExpenseId;
    }

    public double getPostedAmount() {
        return postedAmount;
    }

    public void setPostedAmount(double postedAmount) {
        this.postedAmount = postedAmount;
    }

    public String getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
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
