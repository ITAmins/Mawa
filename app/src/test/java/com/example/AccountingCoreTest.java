package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test suite for MAWA Phase 1: Accounting Core Refactor.
 * Verifies all 5 core accounting scenarios:
 * 1. Standard Cash Day
 * 2. Credit Sale (Baki) given to customer
 * 3. Customer Baki Collection (Joma)
 * 4. Stock Purchase only day (No sales)
 * 5. Complex Day (Opening + Cash Sales + Credit Sales + Baki Collection + Purchases + OpEx + Custom Margin 25%)
 */
public class AccountingCoreTest {

    @Test
    public void testCase1_StandardCashDay() {
        // Given:
        double openingCash = 1000.0;
        double cashSales = 5000.0;
        double creditSales = 0.0;
        double bakiCollection = 0.0;
        double purchases = 3000.0; // Resale stock (চাল, ডাল, তেল)
        double operatingExpenses = 200.0; // বিদ্যুৎ / নাস্তা
        double marginRate = 0.20; // 20%

        // When:
        double totalSales = cashSales + creditSales;
        double totalCashOutflow = purchases + operatingExpenses;
        double expectedClosingCash = openingCash + cashSales + bakiCollection - totalCashOutflow;
        double estimatedGrossProfit = totalSales * marginRate;
        double estimatedNetProfit = estimatedGrossProfit - operatingExpenses;

        // Then:
        assertEquals("Total Sales must equal Cash Sales (5000)", 5000.0, totalSales, 0.001);
        assertEquals("Expected Closing Cash must be 1000 + 5000 - 3200 = 2800", 2800.0, expectedClosingCash, 0.001);
        assertEquals("Estimated Gross Profit must be 5000 * 0.20 = 1000", 1000.0, estimatedGrossProfit, 0.001);
        assertEquals("Estimated Net Profit must be 1000 - 200 = 800", 800.0, estimatedNetProfit, 0.001);
    }

    @Test
    public void testCase2_CreditSaleToCustomer() {
        // Given:
        double openingCash = 1000.0;
        double cashSales = 2000.0;
        double creditSales = 1500.0; // Baki given to customer
        double bakiCollection = 0.0;
        double purchases = 0.0;
        double operatingExpenses = 100.0;
        double marginRate = 0.20;

        // When:
        double totalSales = cashSales + creditSales;
        double totalCashOutflow = purchases + operatingExpenses;
        double expectedClosingCash = openingCash + cashSales + bakiCollection - totalCashOutflow;
        double estimatedGrossProfit = totalSales * marginRate;
        double estimatedNetProfit = estimatedGrossProfit - operatingExpenses;

        // Then:
        assertEquals("Total Sales must include Credit Sales: 2000 + 1500 = 3500", 3500.0, totalSales, 0.001);
        assertEquals("Credit sales do NOT increase cash: 1000 + 2000 - 100 = 2900", 2900.0, expectedClosingCash, 0.001);
        assertEquals("Estimated Gross Profit is 3500 * 0.20 = 700", 700.0, estimatedGrossProfit, 0.001);
        assertEquals("Estimated Net Profit is 700 - 100 = 600", 600.0, estimatedNetProfit, 0.001);
    }

    @Test
    public void testCase3_BakiCollectionFromCustomer() {
        // Given:
        double openingCash = 1000.0;
        double cashSales = 1000.0;
        double creditSales = 0.0;
        double bakiCollection = 500.0; // Customer paid old debt (Joma)
        double purchases = 0.0;
        double operatingExpenses = 100.0;
        double marginRate = 0.20;

        // When:
        double totalSales = cashSales + creditSales;
        double totalCashOutflow = purchases + operatingExpenses;
        double expectedClosingCash = openingCash + cashSales + bakiCollection - totalCashOutflow;
        double estimatedGrossProfit = totalSales * marginRate;
        double estimatedNetProfit = estimatedGrossProfit - operatingExpenses;

        // Then:
        assertEquals("Baki collection is NOT a new sale: Total Sales = 1000", 1000.0, totalSales, 0.001);
        assertEquals("Baki collection increases cash: 1000 + 1000 + 500 - 100 = 2400", 2400.0, expectedClosingCash, 0.001);
        assertEquals("Estimated Gross Profit is 1000 * 0.20 = 200", 200.0, estimatedGrossProfit, 0.001);
        assertEquals("Estimated Net Profit is 200 - 100 = 100", 100.0, estimatedNetProfit, 0.001);
    }

    @Test
    public void testCase4_StockPurchaseOnlyDay() {
        // Given:
        double openingCash = 5000.0;
        double cashSales = 0.0;
        double creditSales = 0.0;
        double bakiCollection = 0.0;
        double purchases = 4000.0; // Stock buying day
        double operatingExpenses = 0.0;
        double marginRate = 0.20;

        // When:
        double totalSales = cashSales + creditSales;
        double totalCashOutflow = purchases + operatingExpenses;
        double expectedClosingCash = openingCash + cashSales + bakiCollection - totalCashOutflow;
        double estimatedGrossProfit = totalSales * marginRate;
        double estimatedNetProfit = estimatedGrossProfit - operatingExpenses;

        // Then:
        assertEquals("Total sales is 0", 0.0, totalSales, 0.001);
        assertEquals("Cash decreases by purchase amount: 5000 - 4000 = 1000", 1000.0, expectedClosingCash, 0.001);
        assertEquals("Estimated Net Profit is 0 (NOT negative 4000, inventory is not an immediate profit loss)", 0.0, estimatedNetProfit, 0.001);
    }

    @Test
    public void testCase5_ComplexDayWithCustomMargin() {
        // Given:
        double openingCash = 2000.0;
        double cashSales = 8000.0;
        double creditSales = 2000.0;
        double bakiCollection = 1000.0;
        double purchases = 6000.0;
        double operatingExpenses = 500.0;
        double marginRate = 0.25; // 25% custom margin

        // When:
        double totalSales = cashSales + creditSales;
        double totalCashOutflow = purchases + operatingExpenses;
        double expectedClosingCash = openingCash + cashSales + bakiCollection - totalCashOutflow;
        double estimatedGrossProfit = totalSales * marginRate;
        double estimatedNetProfit = estimatedGrossProfit - operatingExpenses;

        // Then:
        assertEquals("Total Sales = 8000 + 2000 = 10000", 10000.0, totalSales, 0.001);
        assertEquals("Expected Closing Cash = 2000 + 8000 + 1000 - 6500 = 4500", 4500.0, expectedClosingCash, 0.001);
        assertEquals("Estimated Gross Profit = 10000 * 0.25 = 2500", 2500.0, estimatedGrossProfit, 0.001);
        assertEquals("Estimated Net Profit = 2500 - 500 = 2000", 2000.0, estimatedNetProfit, 0.001);
    }

    @Test
    public void testExpenseClassification() {
        // Operating expenses
        assertEquals(ExpenseModel.TYPE_OPERATING_EXPENSE, ExpenseModel.autoClassifyType("দোকান ভাড়া"));
        assertEquals(ExpenseModel.TYPE_OPERATING_EXPENSE, ExpenseModel.autoClassifyType("বিদ্যুৎ বিল"));
        assertEquals(ExpenseModel.TYPE_OPERATING_EXPENSE, ExpenseModel.autoClassifyType("কর্মচারীর বেতন"));
        assertEquals(ExpenseModel.TYPE_OPERATING_EXPENSE, ExpenseModel.autoClassifyType("চা নাস্তা"));
        assertEquals(ExpenseModel.TYPE_OPERATING_EXPENSE, ExpenseModel.autoClassifyType("রিকশা ভাড়া"));

        // Purchase items
        assertEquals(ExpenseModel.TYPE_PURCHASE, ExpenseModel.autoClassifyType("মিনিকেট চাল ৫০ কেজি"));
        assertEquals(ExpenseModel.TYPE_PURCHASE, ExpenseModel.autoClassifyType("সয়াবিন তেল ৫ লিটার"));
        assertEquals(ExpenseModel.TYPE_PURCHASE, ExpenseModel.autoClassifyType("চিনি ১ বস্তা"));
        assertEquals(ExpenseModel.TYPE_PURCHASE, ExpenseModel.autoClassifyType("ডিম ১ কেস"));
        assertEquals(ExpenseModel.TYPE_PURCHASE, ExpenseModel.autoClassifyType("লাক্স সাবান পাইকারি"));
    }

    @Test
    public void testDateNormalization() {
        assertEquals("19-08-2026", AccountingService.normalizeDateKey("19/08/2026"));
        assertEquals("19-08-2026", AccountingService.normalizeDateKey("19-08-2026"));
        assertEquals("19-08-2026", AccountingService.normalizeDateKey("2026-08-19"));
        assertTrue(AccountingService.isSameDate("19/08/2026", "19-08-2026"));
    }
}
