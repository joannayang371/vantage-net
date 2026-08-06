package net.vantage.report.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Currency helpers. Every amount that leaves the pipeline is HALF_UP to cents. */
public final class Money {

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int SCALE = 2;

    private Money() {
        throw new AssertionError("no instances");
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentOf(BigDecimal base, BigDecimal pct) {
        return round(base.multiply(pct).divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    public static String format(BigDecimal amount) {
        BigDecimal rounded = round(amount);
        if (rounded.signum() < 0) {
            return "-$" + rounded.negate().toPlainString();
        }
        return "$" + rounded.toPlainString();
    }
}
