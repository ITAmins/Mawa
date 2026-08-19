package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SmartFordiProductMemoryTest {

    private Context context;
    private StorageManager storageManager;
    private AccountingService accountingService;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storageManager = StorageManager.getInstance(context);
        storageManager.clearAll();
        accountingService = AccountingService.getInstance(context);
    }

    @Test
    public void testProductMemory_SeedAndRates() {
        List<ProductModel> products = storageManager.loadProductMemory();
        assertNotNull(products);
        assertFalse(products.isEmpty());

        // Find Sugar (চিনি)
        ProductModel sugar = storageManager.findProductByName("চিনি");
        assertNotNull(sugar);
        assertTrue(sugar.getLastPurchasePrice() > 0);
        assertTrue(sugar.getSellingPrice() > sugar.getLastPurchasePrice());

        // Update with new purchase rate
        double initialAvg = sugar.getAveragePurchasePrice();
        sugar.recordNewPurchase(140.0, 10.0, "25-05-2026");
        assertEquals(140.0, sugar.getLastPurchasePrice(), 0.001);
        assertTrue(sugar.getAveragePurchasePrice() >= initialAvg);

        storageManager.saveOrUpdateProduct(sugar);
        ProductModel retrieved = storageManager.findProductByName("চিনি");
        assertEquals(140.0, retrieved.getLastPurchasePrice(), 0.001);
    }

    @Test
    public void testFordiItem_PlannedVsActualCalculations() {
        // Create an item: Sugar, planned 5 kg @ 130 purchase, 140 sell
        FordiItemModel item = new FordiItemModel("prod_1", "চিনি", ProductModel.UNIT_KG, 5.0, 130.0, 140.0);
        assertEquals(5.0, item.getPlannedQuantity(), 0.001);
        assertEquals(650.0, item.getPlannedTotal(), 0.001); // 5 * 130
        assertEquals(50.0, item.getPotentialProfit(), 0.001); // 5 * (140 - 130) = 50

        // Default actual is 5 kg @ 130
        assertEquals(650.0, item.getActualTotal(), 0.001);
        assertEquals(FordiItemModel.STATUS_FULLY_BOUGHT, item.getStatus());

        // Scenario: Market rate was 135 and bought only 4 kg
        item.setActualQuantity(4.0);
        item.setActualPurchaseRate(135.0);
        item.recalculate();

        assertEquals(540.0, item.getActualTotal(), 0.001); // 4 * 135 = 540
        assertEquals(FordiItemModel.STATUS_PARTIALLY_BOUGHT, item.getStatus());

        // Scenario: Item was not available (0 bought)
        item.setActualQuantity(0.0);
        item.recalculate();
        assertEquals(0.0, item.getActualTotal(), 0.001);
        assertEquals(FordiItemModel.STATUS_NOT_BOUGHT, item.getStatus());
    }

    @Test
    public void testFordi_PostToAccounting_Integration() {
        String testDate = "25-05-2026";
        storageManager.saveSabekCash(testDate, 1000.0);
        storageManager.saveAvailableCash(testDate, 2000.0);

        // Create a Smart Fordi with 2 items
        FordiModel fordi = new FordiModel("fordi_101", "সকালের বাজার", testDate, new ArrayList<>(), "#F0FDFA");
        
        FordiItemModel item1 = new FordiItemModel("p1", "সয়াবিন তেল", ProductModel.UNIT_LITER, 2.0, 185.0, 195.0);
        item1.setActualQuantity(2.0);
        item1.setActualPurchaseRate(185.0);
        item1.recalculate(); // Actual = 370

        FordiItemModel item2 = new FordiItemModel("p2", "মসুর ডাল", ProductModel.UNIT_KG, 3.0, 125.0, 140.0);
        item2.setActualQuantity(3.0);
        item2.setActualPurchaseRate(125.0);
        item2.recalculate(); // Actual = 375

        fordi.getItems().add(item1);
        fordi.getItems().add(item2);

        assertEquals(745.0, fordi.getActualTotal(), 0.001);
        assertFalse(fordi.isPostedToAccounting());

        // Post to Accounting
        boolean posted = accountingService.postFordiPurchaseToDailyAccounting(fordi, testDate);
        assertTrue(posted);
        assertTrue(fordi.isPostedToAccounting());
        assertEquals(FordiModel.STATUS_POSTED, fordi.getStatus());
        assertEquals(745.0, fordi.getPostedAmount(), 0.001);

        // Double posting must be rejected!
        boolean doublePost = accountingService.postFordiPurchaseToDailyAccounting(fordi, testDate);
        assertFalse(doublePost);

        // Verify Daily Accounting contains the PURCHASE expense
        AccountingService.DailyAccountingSummary summary = accountingService.calculateDailySummary(testDate);
        assertEquals(745.0, summary.totalPurchases, 0.001);
        assertEquals(745.0, summary.totalCashOutflow, 0.001);
        // Purchases are goods for resale (stock addition), NOT operating expense:
        assertEquals(0.0, summary.totalOperatingExpenses, 0.001);

        // Verify Product Memory was updated with the purchase rates
        ProductModel oil = storageManager.findProductByName("সয়াবিন তেল");
        assertNotNull(oil);
        assertEquals(185.0, oil.getLastPurchasePrice(), 0.001);
    }
}
