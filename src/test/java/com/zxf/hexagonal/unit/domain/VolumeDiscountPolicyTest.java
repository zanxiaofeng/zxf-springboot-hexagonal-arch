package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.service.VolumeDiscountPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolumeDiscountPolicyTest {

    private final VolumeDiscountPolicy policy = new VolumeDiscountPolicy();

    @Test
    void discountRateFor_belowLoyaltyThresholdReturnsZero() {
        assertThat(policy.discountRateFor(0).value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(policy.discountRateFor(9).value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void discountRateFor_loyaltyTierReturnsTenPercent() {
        assertThat(policy.discountRateFor(10).value()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(policy.discountRateFor(99).value()).isEqualByComparingTo(new BigDecimal("0.10"));
    }

    @Test
    void discountRateFor_vipTierReturnsFifteenPercent() {
        assertThat(policy.discountRateFor(100).value()).isEqualByComparingTo(new BigDecimal("0.15"));
        assertThat(policy.discountRateFor(1000).value()).isEqualByComparingTo(new BigDecimal("0.15"));
    }

    @Test
    void discountRateFor_rejectsNegativeOrderCount() {
        assertThatThrownBy(() -> policy.discountRateFor(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
