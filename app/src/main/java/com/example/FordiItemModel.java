package com.example;

import java.io.Serializable;
import java.util.UUID;

/**
 * Smart Fordi Item Model for MAWA Phase 2.
 * Supports planned vs actual purchases, rate memory, and potential profit calculation.
 * Preserves legacy fields (name, price, isChecked) for 100% backward compatibility.
 */
public class FordiItemModel implements Serializable {
    public static final String STATUS_NOT_BOUGHT = "NOT_BOUGHT";
    public static final String STATUS_PARTIALLY_BOUGHT = "PARTIALLY_BOUGHT";
    public static final String STATUS_FULLY_BOUGHT = "FULLY_BOUGHT";

    private String id;
    private String productId;
    private String productName;
    private String unit; // kg, liter, piece, etc.
    private double plannedQuantity;
    private double actualQuantity;
    private double purchaseRate; // Planned purchase rate
    private double actualPurchaseRate; // Actual purchase rate at market
    private double sellingRate;
    private double plannedTotal;
    private double actualTotal;
    private double potentialProfit;
    private String status; // NOT_BOUGHT, PARTIALLY_BOUGHT, FULLY_BOUGHT
    private boolean postedToAccounting;

    // Legacy fields for backward compatibility
    private String name;
    private double price;
    private boolean isChecked;

    public FordiItemModel() {
        this.id = UUID.randomUUID().toString();
        this.unit = ProductModel.UNIT_KG;
        this.status = STATUS_NOT_BOUGHT;
    }

    public FordiItemModel(String id, String name, boolean isChecked, double price) {
        this.id = (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString();
        this.name = name;
        this.productName = name;
        this.isChecked = isChecked;
        this.price = price;
        this.plannedQuantity = 1.0;
        this.actualQuantity = isChecked ? 1.0 : 0.0;
        this.purchaseRate = price;
        this.actualPurchaseRate = price;
        this.unit = ProductModel.UNIT_KG;
        this.status = isChecked ? STATUS_FULLY_BOUGHT : STATUS_NOT_BOUGHT;
        recalculate();
    }

    public FordiItemModel(String productId, String productName, String unit, double plannedQuantity, double purchaseRate, double sellingRate) {
        this.id = UUID.randomUUID().toString();
        this.productId = productId;
        this.productName = ProductModel.normalizeName(productName);
        this.name = this.productName;
        this.unit = (unit != null && !unit.isEmpty()) ? unit : ProductModel.UNIT_KG;
        this.plannedQuantity = plannedQuantity > 0 ? plannedQuantity : 1.0;
        this.actualQuantity = this.plannedQuantity; // default actual to planned for convenience
        this.purchaseRate = purchaseRate;
        this.actualPurchaseRate = purchaseRate;
        this.sellingRate = sellingRate;
        this.status = STATUS_FULLY_BOUGHT;
        this.isChecked = true;
        recalculate();
    }

    /**
     * Recalculates plannedTotal, actualTotal, potentialProfit, and status.
     */
    public void recalculate() {
        if (this.productName == null || this.productName.isEmpty()) {
            if (this.name != null) {
                this.productName = this.name;
            }
        }
        this.name = this.productName;

        if (this.actualPurchaseRate <= 0 && this.purchaseRate > 0) {
            this.actualPurchaseRate = this.purchaseRate;
        }

        this.plannedTotal = this.plannedQuantity * this.purchaseRate;
        this.potentialProfit = this.plannedQuantity * Math.max(0.0, this.sellingRate - this.purchaseRate);

        if (this.actualQuantity <= 0.0) {
            this.status = STATUS_NOT_BOUGHT;
            this.actualTotal = 0.0;
            this.isChecked = false;
        } else if (this.actualQuantity < this.plannedQuantity) {
            this.status = STATUS_PARTIALLY_BOUGHT;
            this.actualTotal = this.actualQuantity * this.actualPurchaseRate;
            this.isChecked = true;
        } else {
            this.status = STATUS_FULLY_BOUGHT;
            this.actualTotal = this.actualQuantity * this.actualPurchaseRate;
            this.isChecked = true;
        }

        // Backward compatibility price field
        this.price = this.actualTotal > 0 ? this.actualTotal : this.plannedTotal;
    }

    // Getters and Setters

    public String getId() {
        return id != null ? id : UUID.randomUUID().toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return (productName != null && !productName.isEmpty()) ? productName : (name != null ? name : "");
    }

    public void setProductName(String productName) {
        this.productName = ProductModel.normalizeName(productName);
        this.name = this.productName;
    }

    public String getUnit() {
        return unit != null ? unit : ProductModel.UNIT_KG;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPlannedQuantity() {
        return plannedQuantity > 0 ? plannedQuantity : 1.0;
    }

    public void setPlannedQuantity(double plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
        recalculate();
    }

    public double getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(double actualQuantity) {
        this.actualQuantity = actualQuantity;
        recalculate();
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
        if (this.actualPurchaseRate <= 0) {
            this.actualPurchaseRate = purchaseRate;
        }
        recalculate();
    }

    public double getActualPurchaseRate() {
        return actualPurchaseRate > 0 ? actualPurchaseRate : purchaseRate;
    }

    public void setActualPurchaseRate(double actualPurchaseRate) {
        this.actualPurchaseRate = actualPurchaseRate;
        recalculate();
    }

    public double getSellingRate() {
        return sellingRate;
    }

    public void setSellingRate(double sellingRate) {
        this.sellingRate = sellingRate;
        recalculate();
    }

    public double getPlannedTotal() {
        if (plannedTotal <= 0 && plannedQuantity > 0 && purchaseRate > 0) {
            return plannedQuantity * purchaseRate;
        }
        return plannedTotal > 0 ? plannedTotal : getPrice();
    }

    public void setPlannedTotal(double plannedTotal) {
        this.plannedTotal = plannedTotal;
    }

    public double getActualTotal() {
        if (status != null && status.equals(STATUS_NOT_BOUGHT)) {
            return 0.0;
        }
        if (actualTotal <= 0 && actualQuantity > 0) {
            return actualQuantity * getActualPurchaseRate();
        }
        return actualTotal;
    }

    public void setActualTotal(double actualTotal) {
        this.actualTotal = actualTotal;
    }

    public double getPotentialProfit() {
        if (potentialProfit <= 0 && sellingRate > purchaseRate) {
            return plannedQuantity * (sellingRate - purchaseRate);
        }
        return potentialProfit;
    }

    public void setPotentialProfit(double potentialProfit) {
        this.potentialProfit = potentialProfit;
    }

    public String getStatus() {
        if (status == null) {
            return isChecked ? STATUS_FULLY_BOUGHT : STATUS_NOT_BOUGHT;
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
    }

    // Legacy getters and setters for backward compatibility

    public String getName() {
        return getProductName();
    }

    public void setName(String name) {
        setProductName(name);
    }

    public boolean isChecked() {
        return !STATUS_NOT_BOUGHT.equals(getStatus());
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
        if (!checked) {
            this.actualQuantity = 0.0;
            this.status = STATUS_NOT_BOUGHT;
        } else {
            if (this.actualQuantity <= 0.0) {
                this.actualQuantity = getPlannedQuantity();
            }
            this.status = STATUS_FULLY_BOUGHT;
        }
        recalculate();
    }

    public double getPrice() {
        if (price <= 0) {
            return plannedTotal > 0 ? plannedTotal : (actualTotal > 0 ? actualTotal : 0.0);
        }
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        if (this.purchaseRate <= 0) {
            this.purchaseRate = price;
            this.actualPurchaseRate = price;
        }
    }
}
