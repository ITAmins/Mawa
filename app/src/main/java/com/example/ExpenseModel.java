package com.example;

import java.io.Serializable;

/* loaded from: classes5.dex */
public class ExpenseModel implements Serializable {
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_OPERATING_EXPENSE = "OPERATING_EXPENSE";
    public static final String TYPE_LEGACY_EXPENSE = "LEGACY_EXPENSE";

    private double amount;
    private String date;
    private String id;
    private String name;
    private String time;
    private String type;
    private String note;

    public ExpenseModel() {
        this.type = TYPE_LEGACY_EXPENSE;
    }

    public ExpenseModel(String id, String name, double amount, String date, String time) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.type = autoClassifyType(name);
    }

    public ExpenseModel(String id, String name, double amount, String date, String time, String type) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.type = (type != null && !type.trim().isEmpty()) ? type : autoClassifyType(name);
    }

    public static String autoClassifyType(String name) {
        if (name == null || name.trim().isEmpty()) {
            return TYPE_LEGACY_EXPENSE;
        }
        String lower = name.trim().toLowerCase();

        // 1. Resale Products / Stock Purchase keywords (Inventory/Goods for sale)
        String[] purchaseKeywords = {
            "চাল", "আটা", "ময়দা", "ময়দা", "সুজি", "তেল", "সয়াবিন", "সয়াবিন", "সরিষা", "ঘি",
            "চিনি", "গুড়", "গুড়", "লবণ", "লবন", "ডাল", "মসুর", "মুগ", "ছোলা", "বুট",
            "ডিম", "দুধ", "মিল্ক", "গুঁড়ো দুধ", "মাখন",
            "আলু", "পেঁয়াজ", "পেয়াজ", "পিঁয়াজ", "পিয়াজ", "রসুন", "আদা", "মরিচ", "হলুদ", "মসলা", "জিরা", "ধনিয়া", "ধনিয়া", "এলাচ", "দারুচিনি",
            "বিস্কুট", "টোস্ট", "চানাচুর", "চিপস", "চকলেট", "কেক", "পাউরুটি", "বনরুটি", "বেকারি",
            "চা পাতা", "কফি", "সিগারেট", "তামাক", "পান", "সুপারি", "ম্যাচ", "দিয়াশলাই", "দিয়াশলাই",
            "সাবান", "ডিটারজেন্ট", "হুইল", "সার্ফ", "শ্যাম্পু", "টুথপেস্ট", "ব্রাশ",
            "কোল্ড ড্রিংক", "ড্রিংকস", "জুস", "আইসক্রিম", "পানি", "মিনারেল",
            "কয়েল", "কয়েল", "গুডনাইট", "ওষুধ", "স্যালাইন",
            "কাঁচামাল", "সবজি", "শাক", "মাছ", "মাংস", "মুরগি", "গরু", "খাসি",
            "বাজার", "মাল", "পণ্য", "স্টক", "পাইকারি", "মহাজন", "কোম্পানি", "ডিলার"
        };
        for (String pk : purchaseKeywords) {
            if (lower.contains(pk)) {
                return TYPE_PURCHASE;
            }
        }

        // 2. Operating Expenses keywords (Rent, Utility, Salary, Transport, Tea/Food, Maintenance, Taxes/Fees)
        String[] opKeywords = {
            "দোকান ভাড়া", "মেস ভাড়া", "বাড়ি ভাড়া", "ভাড়া", "ভাড়া",
            "বিদ্যুৎ বিল", "কারেন্ট বিল", "বিদ্যুৎ", "কারেন্ট", "ইলেকট্রিক", "বিল",
            "কর্মচারী বেতন", "বেতন", "মজুরি", "কর্মচারী", "হাজিরা",
            "চা-নাস্তা", "চা নাস্তা", "চায়ের বিল", "আপ্যায়ন", "আপ্যায়ন", "টিফিন", "মিষ্টি", "নাস্তা", "খাবার",
            "যাতায়াত", "যাতায়াত", "গাড়ি ভাড়া", "ভ্যান ভাড়া", "রিকশা", "রিক্সা", "পরিবহন",
            "মেরামত", "সার্ভিস", "রিপেয়ার", "রং",
            "বিকাশ খরচ", "নগদ খরচ", "ব্যাংক", "সার্ভিস চার্জ", "চার্জ", "ট্যাক্স", "ভ্যাট", "চাঁদা",
            "পরিষ্কার", "ঝাড়ু", "ঝাড়ু", "পলিথিন", "ক্যালকুলেটর", "কাগজ", "কলম", "টিস্যু", "মেমো", "দোকান খরচ"
        };
        for (String op : opKeywords) {
            if (lower.contains(op)) {
                return TYPE_OPERATING_EXPENSE;
            }
        }

        return TYPE_LEGACY_EXPENSE;
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

    public String getType() {
        if (this.type == null || this.type.trim().isEmpty()) {
            return autoClassifyType(this.name);
        }
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isPurchase() {
        return TYPE_PURCHASE.equalsIgnoreCase(getType());
    }

    public boolean isOperatingExpense() {
        return TYPE_OPERATING_EXPENSE.equalsIgnoreCase(getType());
    }

    public boolean isLegacyExpense() {
        String t = getType();
        return TYPE_LEGACY_EXPENSE.equalsIgnoreCase(t) || (!isPurchase() && !isOperatingExpense());
    }
}
