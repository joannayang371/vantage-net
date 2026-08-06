package net.vantage.report.rating;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.vantage.report.charge.Charge;
import net.vantage.report.charge.LoyaltyCredit;
import net.vantage.report.charge.OverageCharge;
import net.vantage.report.model.Invoice;
import net.vantage.report.model.UsageRecord;
import net.vantage.report.pipeline.SeedAccounts;

public class RatingEngineTest {

    private RatingEngine engine;

    @Before
    public void setUp() {
        engine = new RatingEngine(SeedAccounts.load());
    }

    @Test
    public void overageIsExactMegabytesNotRoundedGigabytes() {
        assertEquals(1L, engine.overageMb(1024L * 1000L + 1L, 1000));
        assertEquals(0L, engine.overageMb(1024L * 1000L, 1000));
        assertEquals(0L, engine.overageMb(5L, 1000));
    }

    @Test
    public void overageIsRatedAtTwelveTenthsOfACentPerMegabyte() {
        assertEquals(new BigDecimal("12.00"), engine.rateOverage(1024L * 1000L + 1000L, 1000));
    }

    @Test
    public void mediationDropsUsageWithoutABillingAccount() {
        List<UsageRecord> usage = Arrays.asList(
                new UsageRecord("VANTAGE-BILL-70000", "dev-a", "2026-07", 100L),
                new UsageRecord("VANTAGE-BILL-70000", "dev-b", "2026-07", 250L),
                new UsageRecord("VANTAGE-BILL-99999", "dev-c", "2026-07", 900L));

        Map<String, Long> totals = engine.mediate(usage);
        assertEquals(1, totals.size());
        assertEquals(Long.valueOf(350L), totals.get("VANTAGE-BILL-70000"));

        List<UsageRecord> unlinked = engine.unlinked(usage);
        assertEquals(1, unlinked.size());
        assertEquals("VANTAGE-BILL-99999", unlinked.get(0).getAccountId());
    }

    @Test
    public void invoiceWithoutOverageHasNoUsageLine() {
        Invoice invoice = engine.buildInvoice("VANTAGE-BILL-70000", "2026-07", 1000L);
        for (Charge charge : invoice.getCharges()) {
            assertFalse(charge instanceof OverageCharge);
        }
        assertEquals(new BigDecimal("690.63"), invoice.total());
    }

    @Test
    public void loyaltyDiscountIsAppliedAfterTax() {
        Invoice invoice = engine.buildInvoice("VANTAGE-BILL-70013", "2026-07", 1024L * 1000L + 1024L);
        LoyaltyCredit credit = null;
        for (Charge charge : invoice.getCharges()) {
            if (charge instanceof LoyaltyCredit) {
                credit = (LoyaltyCredit) charge;
            }
        }
        assertTrue("expected a loyalty credit", credit != null);
        assertEquals(new BigDecimal("28.15"), credit.getAmount());
        assertEquals(new BigDecimal("675.53"), invoice.total());
    }

    @Test
    public void invoicesAreBuiltPerAccountInAccountIdOrder() {
        List<UsageRecord> usage = Arrays.asList(
                new UsageRecord("VANTAGE-BILL-70013", "dev-a", "2026-07", 10L),
                new UsageRecord("VANTAGE-BILL-70000", "dev-b", "2026-07", 20L),
                new UsageRecord("VANTAGE-BILL-99999", "dev-c", "2026-07", 30L));

        List<Invoice> invoices = engine.buildInvoices("2026-07", usage);
        assertEquals(2, invoices.size());
        assertEquals("VANTAGE-BILL-70000", invoices.get(0).getAccount().getAccountId());
        assertEquals("VANTAGE-BILL-70013", invoices.get(1).getAccount().getAccountId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownAccountIsRejected() {
        engine.buildInvoice("VANTAGE-BILL-00000", "2026-07", 10L);
    }
}
