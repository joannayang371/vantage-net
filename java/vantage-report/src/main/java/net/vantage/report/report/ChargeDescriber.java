package net.vantage.report.report;

import net.vantage.report.charge.Charge;
import net.vantage.report.charge.LoyaltyCredit;
import net.vantage.report.charge.OverageCharge;
import net.vantage.report.charge.PlanFeeCharge;
import net.vantage.report.charge.ProrationAdjustment;
import net.vantage.report.charge.TaxCharge;

/**
 * Human-readable labels for charges.
 *
 * <p>Two dispatch styles are kept side by side: {@link #describe(Charge)} walks
 * an {@code instanceof} chain with explicit casts, while {@link #category(Charge)}
 * goes through the visitor. Both must stay in sync.
 */
public final class ChargeDescriber {

    public String describe(Charge charge) {
        if (charge instanceof PlanFeeCharge) {
            PlanFeeCharge fee = (PlanFeeCharge) charge;
            return "Monthly plan fee (" + fee.getPlanCode() + ")";
        } else if (charge instanceof OverageCharge) {
            OverageCharge overage = (OverageCharge) charge;
            return "Usage overage: " + overage.getOverageMb() + " MB at "
                    + overage.getRatePerMb().toPlainString() + "/MB";
        } else if (charge instanceof TaxCharge) {
            TaxCharge tax = (TaxCharge) charge;
            return "Sales tax at " + tax.getRatePct().toPlainString() + "% on "
                    + tax.getTaxableBase().toPlainString();
        } else if (charge instanceof LoyaltyCredit) {
            LoyaltyCredit credit = (LoyaltyCredit) charge;
            return "Loyalty credit of " + credit.getDiscountPct().toPlainString() + "% (post-tax)";
        } else if (charge instanceof ProrationAdjustment) {
            ProrationAdjustment adjustment = (ProrationAdjustment) charge;
            String verb = adjustment.isRefund() ? "refund" : "charge";
            return "Proration " + verb + " for " + adjustment.getDaysInService()
                    + " of " + adjustment.getDaysInPeriod() + " days";
        } else {
            throw new IllegalStateException("unhandled charge type: " + charge.getClass());
        }
    }

    public String category(Charge charge) {
        return charge.accept(new net.vantage.report.charge.ChargeVisitor<String>() {
            @Override
            public String visitOverage(OverageCharge value) {
                return "USAGE";
            }

            @Override
            public String visitPlanFee(PlanFeeCharge value) {
                return "RECURRING";
            }

            @Override
            public String visitTax(TaxCharge value) {
                return "TAX";
            }

            @Override
            public String visitLoyaltyCredit(LoyaltyCredit value) {
                return "CREDIT";
            }

            @Override
            public String visitProration(ProrationAdjustment value) {
                return value.isRefund() ? "CREDIT" : "ADJUSTMENT";
            }
        });
    }
}
