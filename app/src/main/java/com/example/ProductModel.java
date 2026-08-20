package com.example;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Product Memory model for MAWA Phase 2.
 * Stores product metadata, unit, historical purchase rates, and selling price.
 * (NOT full inventory/POS, just pricing memory for grocery shops).
 */
public class ProductModel implements Serializable {
    private String id;
    private String name;
    private String unit; // kg, gram, liter, ml, piece, packet, box, dozen, bag
    private double lastPurchasePrice;
    private double averagePurchasePrice;
    private double sellingPrice;
    private int purchaseCount;
    private String lastPurchaseDate;
    private String category; // e.g. চাল/ডাল, তেল, চিনি, মসলা, বিস্কুট, পানীয়, পরিষ্কারক, অন্যান্য
    private long createdAt;
    private long updatedAt;
    private long deletedAt;

    public static final String UNIT_KG = "kg";
    public static final String UNIT_GRAM = "gram";
    public static final String UNIT_LITER = "liter";
    public static final String UNIT_ML = "ml";
    public static final String UNIT_PIECE = "piece";
    public static final String UNIT_PACKET = "packet";
    public static final String UNIT_BOX = "box";
    public static final String UNIT_DOZEN = "dozen";
    public static final String UNIT_BAG = "bag";

    public ProductModel() {
        this.id = UUID.randomUUID().toString();
        this.unit = UNIT_KG;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public ProductModel(String id, String name, String unit, double purchasePrice, double sellingPrice, String category) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        this.name = normalizeName(name);
        this.unit = (unit != null && !unit.isEmpty()) ? unit : UNIT_KG;
        this.lastPurchasePrice = purchasePrice;
        this.averagePurchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.purchaseCount = (purchasePrice > 0) ? 1 : 0;
        this.category = (category != null && !category.isEmpty()) ? category : "অন্যান্য";
        this.lastPurchaseDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public static String normalizeName(String rawName) {
        if (rawName == null) return "";
        return rawName.trim().replaceAll("\\s+", " ");
    }

    public static String getBengaliUnitLabel(String unit) {
        if (unit == null) return "কেজি";
        switch (unit.toLowerCase().trim()) {
            case UNIT_KG:
            case "কেজি":
                return "কেজি";
            case UNIT_GRAM:
            case "গ্রাম":
            case "gm":
                return "গ্রাম";
            case UNIT_LITER:
            case "লিটার":
            case "l":
            case "ltr":
                return "লিটার";
            case UNIT_ML:
            case "মি.লি.":
                return "মি.লি.";
            case UNIT_PIECE:
            case "পিস":
            case "টি":
            case "টা":
                return "পিস";
            case UNIT_PACKET:
            case "প্যাকেট":
            case "pkt":
                return "প্যাকেট";
            case UNIT_BOX:
            case "বক্স":
            case "কার্টন":
                return "বক্স";
            case UNIT_DOZEN:
            case "ডজন":
                return "ডজন";
            case UNIT_BAG:
            case "বস্তা":
                return "বস্তা";
            default:
                return unit;
        }
    }

    /**
     * Records a new actual purchase.
     * Updates lastPurchasePrice and calculates cumulative averagePurchasePrice.
     */
    public void recordNewPurchase(double newRate, double quantity, String dateKey) {
        if (newRate <= 0) return;
        this.lastPurchasePrice = newRate;
        if (this.purchaseCount <= 0 || this.averagePurchasePrice <= 0) {
            this.averagePurchasePrice = newRate;
            this.purchaseCount = 1;
        } else {
            // Weighted / incremental average
            double totalOld = this.averagePurchasePrice * this.purchaseCount;
            this.purchaseCount++;
            this.averagePurchasePrice = (totalOld + newRate) / this.purchaseCount;
        }
        if (dateKey != null && !dateKey.isEmpty()) {
            this.lastPurchaseDate = dateKey;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getLastPurchasePrice() {
        return lastPurchasePrice;
    }

    public void setLastPurchasePrice(double lastPurchasePrice) {
        this.lastPurchasePrice = lastPurchasePrice;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getAveragePurchasePrice() {
        return averagePurchasePrice > 0 ? averagePurchasePrice : lastPurchasePrice;
    }

    public void setAveragePurchasePrice(double averagePurchasePrice) {
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public String getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(String lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getSuggestionDisplay() {
        String u = getBengaliUnitLabel(this.unit);
        String purchaseStr = PdfExporter.formatBengaliNumber(this.lastPurchasePrice);
        String sellStr = PdfExporter.formatBengaliNumber(this.sellingPrice);
        return this.name + " (" + u + ") • কেনা ৳" + purchaseStr + " • বিক্রি ৳" + sellStr;
    }
}
