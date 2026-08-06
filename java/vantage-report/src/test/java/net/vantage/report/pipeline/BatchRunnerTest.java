package net.vantage.report.pipeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.vantage.report.io.CsvUsageReader;
import net.vantage.report.model.Invoice;
import net.vantage.report.model.UsageRecord;
import net.vantage.report.rating.RatingEngine;
import net.vantage.report.report.ReportRenderer;

public class BatchRunnerTest {

    private final ReportRenderer renderer = new ReportRenderer();

    @Test
    public void rendersEveryInvoiceInSubmissionOrder() {
        List<UsageRecord> usage = new CsvUsageReader().readResource("usage-sample.csv");
        List<Invoice> invoices = new RatingEngine(SeedAccounts.load()).buildInvoices("2026-07", usage);

        BatchRunner runner = new BatchRunner(renderer, 4);
        try {
            List<String> rendered = runner.renderAll(invoices);
            assertEquals(invoices.size(), rendered.size());
            for (int i = 0; i < invoices.size(); i++) {
                assertTrue(rendered.get(i).contains(invoices.get(i).getAccount().getAccountId()));
            }
        } finally {
            runner.close();
        }
    }

    @Test
    public void singleThreadedPoolProducesIdenticalOutput() {
        List<UsageRecord> usage = new CsvUsageReader().readResource("usage-sample.csv");
        List<Invoice> invoices = new RatingEngine(SeedAccounts.load()).buildInvoices("2026-07", usage);

        BatchRunner wide = new BatchRunner(renderer, 8);
        BatchRunner narrow = new BatchRunner(renderer, 1);
        try {
            assertEquals(narrow.renderAll(invoices), wide.renderAll(invoices));
        } finally {
            wide.close();
            narrow.close();
        }
    }
}
