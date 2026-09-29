package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Calculates order totals using quantity, customer, coupon, and order-size discounts. */
@Component
public class PricingCalculator {

    private static final int BULK_DISCOUNT_MIN_QUANTITY = 10;
    private static final int CURRENCY_SCALE = 2;
    private static final String CUSTOMER_TYPE_VIP = "vip";
    private static final String CUSTOMER_TYPE_WHOLESALE = "wholesale";
    private static final String COUPON_FIXED_DISCOUNT_CODE = "SAVE10";
    private static final String COUPON_PERCENT_DISCOUNT_CODE = "SAVE20PERCENT";
    private static final BigDecimal COUPON_FIXED_DISCOUNT_AMOUNT = BigDecimal.TEN;
    private static final BigDecimal BULK_DISCOUNT_MULTIPLIER = new BigDecimal("0.95");
    private static final BigDecimal VIP_DISCOUNT_MULTIPLIER = new BigDecimal("0.90");
    private static final BigDecimal WHOLESALE_DISCOUNT_MULTIPLIER = new BigDecimal("0.85");
    private static final BigDecimal PERCENT_COUPON_MULTIPLIER = new BigDecimal("0.80");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_DISCOUNT_MULTIPLIER = new BigDecimal("0.98");

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            total = total.add(calculateLineTotal(item));
        }

        total = applyCustomerDiscount(total, customerType);
        total = applyCoupon(total, couponCode);
        total = total.max(BigDecimal.ZERO);

        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = applyMultiplier(total, LARGE_ORDER_DISCOUNT_MULTIPLIER);
        }

        return total.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLineTotal(LineItem item) {
        BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(item.quantity()));
        if (item.quantity() > BULK_DISCOUNT_MIN_QUANTITY) {
            lineTotal = applyMultiplier(lineTotal, BULK_DISCOUNT_MULTIPLIER);
        }
        return lineTotal;
    }

    private BigDecimal applyCustomerDiscount(BigDecimal total, String customerType) {
        if (CUSTOMER_TYPE_VIP.equals(customerType)) {
            return applyMultiplier(total, VIP_DISCOUNT_MULTIPLIER);
        }
        if (CUSTOMER_TYPE_WHOLESALE.equals(customerType)) {
            return applyMultiplier(total, WHOLESALE_DISCOUNT_MULTIPLIER);
        }
        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (COUPON_FIXED_DISCOUNT_CODE.equals(couponCode)) {
            return total.subtract(COUPON_FIXED_DISCOUNT_AMOUNT);
        }
        if (COUPON_PERCENT_DISCOUNT_CODE.equals(couponCode)) {
            return applyMultiplier(total, PERCENT_COUPON_MULTIPLIER);
        }
        return total;
    }

    private BigDecimal applyMultiplier(BigDecimal amount, BigDecimal multiplier) {
        return amount.multiply(multiplier);
    }
}
