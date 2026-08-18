package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.model.DiscountRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountRateTest {

    @Test
    void constructor_acceptsBounds() {
        assertThat(DiscountRate.ZERO.value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(new DiscountRate(BigDecimal.ONE).value()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(new DiscountRate(new BigDecimal("0.10")).value())
                .isEqualByComparingTo(new BigDecimal("0.10"));
    }

    @Test
    void constructor_rejectsOutOfRange() {
        assertThatThrownBy(() -> new DiscountRate(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscountRate(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DiscountRate(new BigDecimal("1.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyTo_returnsAmountAfterDiscount() {
        DiscountRate rate = new DiscountRate(new BigDecimal("0.10"));

        assertThat(rate.applyTo(new BigDecimal("500.00")))
                .isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(DiscountRate.ZERO.applyTo(new BigDecimal("500.00")))
                .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void applyTo_rejectsNegativeAmount() {
        assertThatThrownBy(() -> DiscountRate.ZERO.applyTo(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
