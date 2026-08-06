package net.vantage.report.rating;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.vantage.report.charge.Charge;
import net.vantage.report.charge.LoyaltyCredit;
import net.vantage.report.charge.OverageCharge;
import net.vantage.report.charge.PlanFeeCharge;
import net.vantage.report.charge.TaxCharge;
import net.vantage.report.model.Account;
import net.vantage.report.model.Invoice;
import net.vantage.report.model.UsageRecord;
import net.vantage.report.util.Money;
import net.vantage.report.util.Pair;

/**
 * Rates mediated usage into invoices.
 *
 * <p>Mirrors {@code app/billing}: usage is rated to the exact megabyte, tax is
 * charged on the pre-discount subtotal, and the loyalty discount is applied
 * after tax as a credit.
 */
public final class RatingEngine {

    public static final BigDecimal OVERAGE_RATE_PER_MB = new BigDecimal("0.012");
    public static final long MB_PER_GB = 1024L;

    private static final Map<String, BigDecimal> PLAN_FEES;

    static {
        Map<String, BigDecimal> fees = new HashMap<String, BigDecimal>();
        fees.put("ENTERPRISE-FLEX", new BigDecimal("2400.00"));
        fees.put("BUSINESS-FLEX", new BigDecimal("650.00"));
        fees.put("METRO-BURST", new BigDecimal("310.00"));
        PLAN_FEES = Collections.unmodifiableMap(fees);
    }

    private final Map<String, Account> accountsById;

    public RatingEngine(List<Account> accounts) {
        Map<String, Account> byId = new LinkedHashMap<String, Account>();
        for (Account account : accounts) {
            byId.put(account.getAccountId(), account);
        }
        this.accountsById = Collections.unmodifiableMap(byId);
    }

    public long overageMb(long usageMb, int includedGb) {
        long includedMb = includedGb * MB_PER_GB;
        long overage = usageMb - includedMb;
        return overage > 0L ? overage : 0L;
    }

    public BigDecimal rateOverage(long usageMb, int includedGb) {
        long overage = overageMb(usageMb, includedGb);
        return Money.round(BigDecimal.valueOf(overage).multiply(OVERAGE_RATE_PER_MB));
    }

    public BigDecimal planFee(String planCode) {
        BigDecimal fee = PLAN_FEES.get(planCode);
        if (fee == null) {
            throw new IllegalArgumentException("unknown plan code: " + planCode);
        }
        return fee;
    }

    /** Sums usage per account, dropping records that have no billing account. */
    public Map<String, Long> mediate(List<UsageRecord> records) {
        Map<String, Long> totals = new LinkedHashMap<String, Long>();
        for (UsageRecord record : records) {
            if (!accountsById.containsKey(record.getAccountId())) {
                continue;
            }
            Long current = totals.get(record.getAccountId());
            long updated = (current == null ? 0L : current.longValue()) + record.getUsageMb();
            totals.put(record.getAccountId(), Long.valueOf(updated));
        }
        return totals;
    }

    /** Usage that could not be attached to any account, newest first. */
    public List<UsageRecord> unlinked(List<UsageRecord> records) {
        List<UsageRecord> orphans = records.stream()
                .filter(record -> !accountsById.containsKey(record.getAccountId()))
                .collect(Collectors.toList());
        Collections.sort(orphans, new Comparator<UsageRecord>() {
            @Override
            public int compare(UsageRecord left, UsageRecord right) {
                int byPeriod = right.getPeriod().compareTo(left.getPeriod());
                if (byPeriod != 0) {
                    return byPeriod;
                }
                return left.getAccountId().compareTo(right.getAccountId());
            }
        });
        return orphans;
    }

    public Optional<Account> findAccount(String accountId) {
        return Optional.ofNullable(accountsById.get(accountId));
    }

    public Invoice buildInvoice(String accountId, String period, long usageMb) {
        Account account = findAccount(accountId)
                .orElseThrow(() -> new IllegalArgumentException("no such account: " + accountId));

        List<Charge> charges = new ArrayList<Charge>();
        charges.add(new PlanFeeCharge(account.getPlanCode(), planFee(account.getPlanCode())));

        long overage = overageMb(usageMb, account.getIncludedGb());
        if (overage > 0L) {
            charges.add(new OverageCharge(
                    overage, OVERAGE_RATE_PER_MB, rateOverage(usageMb, account.getIncludedGb())));
        }

        BigDecimal taxable = BigDecimal.ZERO;
        for (Charge charge : charges) {
            taxable = taxable.add(charge.signedAmount());
        }
        taxable = Money.round(taxable);

        BigDecimal tax = Money.percentOf(taxable, account.getTaxPct());
        charges.add(new TaxCharge(account.getTaxPct(), taxable, tax));

        if (account.hasLoyaltyDiscount()) {
            BigDecimal postTax = Money.round(taxable.add(tax));
            charges.add(new LoyaltyCredit(
                    account.getLoyaltyDiscountPct(),
                    Money.percentOf(postTax, account.getLoyaltyDiscountPct())));
        }

        return new Invoice(account, period, charges);
    }

    /** Invoices for every account with mediated usage, ordered by account id. */
    public List<Invoice> buildInvoices(String period, List<UsageRecord> records) {
        Map<String, Long> totals = mediate(records);
        List<Pair<String, Long>> ordered = totals.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .sorted(new Comparator<Pair<String, Long>>() {
                    @Override
                    public int compare(Pair<String, Long> left, Pair<String, Long> right) {
                        return left.getFirst().compareTo(right.getFirst());
                    }
                })
                .collect(Collectors.toList());

        List<Invoice> invoices = new ArrayList<Invoice>();
        for (Pair<String, Long> entry : ordered) {
            invoices.add(buildInvoice(entry.getFirst(), period, entry.getSecond().longValue()));
        }
        return Collections.unmodifiableList(invoices);
    }
}
