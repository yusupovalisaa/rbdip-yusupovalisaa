package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Модуль расчёта цены заказа. Намеренно почти не покрыт тестами и
 * содержит magic numbers / нечитаемые ветвления скидок - цель для
 * характеризационных тестов (ЛР2) и mutation-testing гейта PIT (ЛР5).
 */
public class PricingCalculator {

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > 10) {
                linePrice = linePrice.multiply(BigDecimal.valueOf(0.95));
            }
            total = total.add(linePrice);
        }

        if ("vip".equals(customerType)) {
            total = total.multiply(BigDecimal.valueOf(0.9));
        } else if ("wholesale".equals(customerType)) {
            total = total.multiply(BigDecimal.valueOf(0.85));
        }

        if ("SAVE10".equals(couponCode)) {
            total = total.subtract(BigDecimal.TEN);
        } else if ("SAVE20PERCENT".equals(couponCode)) {
            total = total.multiply(BigDecimal.valueOf(0.8));
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(BigDecimal.valueOf(1000)) > 0) {
            total = total.multiply(BigDecimal.valueOf(0.98));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
