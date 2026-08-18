package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.model.DiscountRate;
import com.zxf.hexagonal.domain.model.PriceScheme;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceSchemeTest {

    @Test
    void calculateFinalPrice_appliesDiscountToSubtotal() {
        PriceScheme scheme = new PriceScheme(
                new BigDecimal("100.00"), new DiscountRate(new BigDecimal("0.10")));

        // 100 × 5 = 500, 500 × (1 − 0.10) = 450
        assertThat(scheme.calculateFinalPrice(5))
                .isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    void calculateFinalPrice_zeroDiscountReturnsFullSubtotal() {
        PriceScheme scheme = new PriceScheme(new BigDecimal("33.33"), DiscountRate.ZERO);

        assertThat(scheme.calculateFinalPrice(3))
                .isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void constructor_rejectsNegativeBasePrice() {
        assertThatThrownBy(() -> new PriceScheme(
                new BigDecimal("-1"), DiscountRate.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsInvalidDiscountRate() {
        // [0,1] 区间校验内聚在 DiscountRate 构造期
        assertThatThrownBy(() -> new PriceScheme(
                new BigDecimal("100"), new DiscountRate(new BigDecimal("1.01"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateFinalPrice_rejectsNonPositiveQuantity() {
        PriceScheme scheme = new PriceScheme(new BigDecimal("100.00"), DiscountRate.ZERO);

        assertThatThrownBy(() -> scheme.calculateFinalPrice(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheme.calculateFinalPrice(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
