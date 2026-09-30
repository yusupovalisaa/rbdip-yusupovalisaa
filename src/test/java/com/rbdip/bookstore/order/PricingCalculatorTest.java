package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void appliesBulkDiscountOnlyAboveTenItems() {
        assertThat(calculate("10.00", 10, "regular", null)).isEqualByComparingTo("100.00");
        assertThat(calculate("10.00", 11, "regular", null)).isEqualByComparingTo("104.50");
    }

    @Test
    void appliesCustomerTypeDiscounts() {
        assertThat(calculate("100.00", 1, "vip", null)).isEqualByComparingTo("90.00");
        assertThat(calculate("100.00", 1, "wholesale", null)).isEqualByComparingTo("85.00");
    }

    @Test
    void appliesCouponsAndPreventsNegativeTotals() {
        assertThat(calculate("15.00", 1, "regular", "SAVE10")).isEqualByComparingTo("5.00");
        assertThat(calculate("100.00", 1, "regular", "SAVE20PERCENT")).isEqualByComparingTo("80.00");
        assertThat(calculate("5.00", 1, "regular", "SAVE10")).isEqualByComparingTo("0.00");
        assertThat(calculate("100.00", 1, "vip", "SAVE10")).isEqualByComparingTo("80.00");
    }

    @Test
    void appliesLargeOrderDiscountAndRoundsToCurrency() {
        assertThat(calculate("1000.00", 1, "regular", null)).isEqualByComparingTo("1000.00");
        assertThat(calculate("1000.01", 1, "regular", null)).isEqualByComparingTo("980.01");
        assertThat(calculate("1001.00", 1, "regular", null)).isEqualByComparingTo("980.98");
        assertThat(calculate("10.01", 1, "vip", null)).isEqualByComparingTo("9.01");
    }

    @Test
    void returnsZeroForEmptyOrdersAndIgnoresUnknownCoupon() {
        assertThat(calculator.calculateOrderTotal(List.of(), "regular", null)).isEqualByComparingTo("0.00");
        assertThat(calculate("25.00", 1, "regular", "UNKNOWN")).isEqualByComparingTo("25.00");
    }

    private BigDecimal calculate(String price, int quantity, String customerType, String couponCode) {
        return calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), customerType, couponCode);
    }
}