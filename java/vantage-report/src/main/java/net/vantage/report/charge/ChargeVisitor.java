package net.vantage.report.charge;

/** Double-dispatch visitor over the {@link Charge} hierarchy. */
public interface ChargeVisitor<T> {

    T visitOverage(OverageCharge charge);

    T visitPlanFee(PlanFeeCharge charge);

    T visitTax(TaxCharge charge);

    T visitLoyaltyCredit(LoyaltyCredit credit);

    T visitProration(ProrationAdjustment adjustment);
}
