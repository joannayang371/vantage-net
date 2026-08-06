package net.vantage.report.io;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import net.vantage.report.model.UsageRecord;

public class CsvUsageReaderTest {

    private final CsvUsageReader reader = new CsvUsageReader();

    private static InputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void readsTheBundledSampleExport() {
        List<UsageRecord> records = reader.readResource("usage-sample.csv");
        assertEquals(6, records.size());
        assertEquals("VANTAGE-BILL-88213", records.get(0).getAccountId());
        assertEquals(2914005L, records.get(0).getUsageMb());
    }

    @Test
    public void blankLinesAreSkipped() {
        String csv = "account_id,device_uuid,period,usage_mb\n"
                + "VANTAGE-BILL-70000,dev-a,2026-07,10\n"
                + "\n"
                + "VANTAGE-BILL-70000,dev-b,2026-07,20\n";
        assertEquals(2, reader.read(stream(csv)).size());
    }

    @Test
    public void emptyInputYieldsNoRecords() {
        assertEquals(0, reader.read(stream("")).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unexpectedHeaderIsRejected() {
        reader.read(stream("account,device,period,mb\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shortRowIsRejected() {
        reader.read(stream("account_id,device_uuid,period,usage_mb\nVANTAGE-BILL-70000,dev-a,2026-07\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonNumericUsageIsRejected() {
        reader.read(stream("account_id,device_uuid,period,usage_mb\nVANTAGE-BILL-70000,dev-a,2026-07,many\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeUsageIsRejected() {
        reader.read(stream("account_id,device_uuid,period,usage_mb\nVANTAGE-BILL-70000,dev-a,2026-07,-5\n"));
    }
}
