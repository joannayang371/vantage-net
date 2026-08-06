package net.vantage.report.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.vantage.report.charge.LoyaltyCredit;
import net.vantage.report.charge.OverageCharge;
import net.vantage.report.charge.PlanFeeCharge;
import net.vantage.report.charge.ProrationAdjustment;
import net.vantage.report.charge.TaxCharge;
import net.vantage.report.model.Invoice;
import net.vantage.report.model.UsageRecord;
import net.vantage.report.pipeline.SeedAccounts;
import net.vantage.report.rating.RatingEngine;

public class ReportRendererTest {

    private final ReportRenderer renderer = new ReportRenderer();
    private final ChargeDescriber describer = new ChargeDescriber();
    private final RatingEngine engine = new RatingEngine(SeedAccounts.load());

    @Test
    public void invoiceRendersHeaderChargesAndTotal() {
        Invoice invoice = engine.buildInvoice("VANTAGE-BILL-70013", "2026-07", 1024L * 1001L);
        String rendered = renderer.renderInvoice(invoice);

        assertTrue(rendered.startsWith("VANTAGE NETWORK SERVICES\n"));
        assertTrue(rendered.contains("Account:  VANTAGE-BILL-70013"));
        assertTrue(rendered.contains("Customer: Cedar Point Health Systems"));
        assertTrue(rendered.contains("Usage overage: 1024 MB at 0.012/MB"));
        assertTrue(rendered.contains("Loyalty credit of 4.0% (post-tax)"));
        assertTrue(rendered.trim().endsWith(invoice.total().toPlainString()));
    }

    @Test
    public void creditsRenderWithALeadingMinusSign() {
        Invoice invoice = engine.buildInvoice("VANTAGE-BILL-88213", "2026-07", 1024L * 3001L);
        String rendered = renderer.renderInvoice(invoice);
        assertTrue(rendered.contains("-$"));
    }

    @Test
    public void cycleSummaryListsUnlinkedUsage() {
        List<UsageRecord> usage = Arrays.asList(
                new UsageRecord("VANTAGE-BILL-70000", "dev-a", "2026-07", 100L),
                new UsageRecord("VANTAGE-BILL-99999", "dev-c", "2026-07", 900L));

        List<Invoice> invoices = engine.buildInvoices("2026-07", usage);
        String summary = renderer.renderCycleSummary("2026-07", invoices, engine.unlinked(usage));

        assertTrue(summary.contains("Invoices rendered: 1"));
        assertTrue(summary.contains("Unlinked records:  1"));
        assertTrue(summary.contains("VANTAGE-BILL-99999  device=dev-c  900 MB"));
    }

    @Test
    public void cycleSummaryOmitsUnlinkedSectionWhenClean() {
        List<UsageRecord> usage = Collections.singletonList(
                new UsageRecord("VANTAGE-BILL-70000", "dev-a", "2026-07", 100L));
        String summary = renderer.renderCycleSummary(
                "2026-07", engine.buildInvoices("2026-07", usage), engine.unlinked(usage));
        assertTrue(!summary.contains("UNLINKED USAGE"));
    }

    @Test
    public void csvHasOneRowPerCharge() {
        Invoice invoice = engine.buildInvoice("VANTAGE-BILL-70013", "2026-07", 1024L * 1001L);
        String csv = renderer.renderCsv(Collections.singletonList(invoice));

        String[] lines = csv.split("\n");
        assertEquals(invoice.getCharges().size() + 1, lines.length);
        assertEquals("account_id,period,category,code,amount", lines[0]);
        assertTrue(lines[1].startsWith("VANTAGE-BILL-70013,2026-07,RECURRING,PLAN-FEE,"));
    }

    @Test
    public void describerCoversEveryChargeType() {
        assertEquals("Monthly plan fee (METRO-BURST)",
                describer.describe(new PlanFeeCharge("METRO-BURST", new BigDecimal("310.00"))));
        assertEquals("Usage overage: 5 MB at 0.012/MB",
                describer.describe(new OverageCharge(5L, new BigDecimal("0.012"), new BigDecimal("0.06"))));
        assertEquals("Sales tax at 6.25% on 100.00",
                describer.describe(new TaxCharge(
                        new BigDecimal("6.25"), new BigDecimal("100.00"), new BigDecimal("6.25"))));
        assertEquals("Loyalty credit of 8.0% (post-tax)",
                describer.describe(new LoyaltyCredit(new BigDecimal("8.0"), new BigDecimal("8.50"))));
        assertEquals("Proration refund for 10 of 30 days",
                describer.describe(new ProrationAdjustment(10, 30, true, new BigDecimal("25.00"))));
    }

    @Test
    public void categoriesMatchTheVisitorDispatch() {
        assertEquals("ADJUSTMENT",
                describer.category(new ProrationAdjustment(10, 30, false, new BigDecimal("25.00"))));
        assertEquals("CREDIT",
                describer.category(new ProrationAdjustment(10, 30, true, new BigDecimal("25.00"))));
        assertEquals("TAX", describer.category(new TaxCharge(
                new BigDecimal("6.25"), new BigDecimal("100.00"), new BigDecimal("6.25"))));
    }
}
