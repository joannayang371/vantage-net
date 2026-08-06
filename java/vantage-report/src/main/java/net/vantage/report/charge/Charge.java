package net.vantage.report.charge;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Base type for every line that can appear on an invoice.
 *
 * <p>The hierarchy is closed by convention only — subclasses live in this
 * package and callers are expected to dispatch through {@link ChargeVisitor}
 * or an {@code instanceof} chain.
 */
public abstract class Charge {

    private final String code;
    private final BigDecimal amount;

    protected Charge(String code, BigDecimal amount) {
        this.code = Objects.requireNonNull(code, "code");
        this.amount = Objects.requireNonNull(amount, "amount");
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /** Signed contribution of this charge to the invoice total. */
    public abstract BigDecimal signedAmount();

    public abstract <T> T accept(ChargeVisitor<T> visitor);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{code=" + code + ", amount=" + amount + '}';
    }
}
