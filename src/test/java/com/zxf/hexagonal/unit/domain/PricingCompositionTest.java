package com.zxf.hexagonal.unit.domain;

import com.zxf.hexagonal.domain.model.DiscountRate;
import com.zxf.hexagonal.domain.model.PriceScheme;
import com.zxf.hexagonal.domain.service.VolumeDiscountPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 策略与值对象的组合验证：「领域策略产出 DiscountRate → 定价方案消费」的
 * 衔接由类型保证——错误语义（乘数 vs 折扣率）无法通过裸 BigDecimal 混入。
 */
class PricingCompositionTest {

    @Test
    void composition_strategyOutputFeedsPriceScheme() {
        VolumeDiscountPolicy volumeDiscountPolicy = new VolumeDiscountPolicy();

        // 应用层查好事实（历史订单数 10）→ 策略给出折扣率 → 定价方案算出最终价
        DiscountRate rate = volumeDiscountPolicy.discountRateFor(10);
        PriceScheme scheme = new PriceScheme(new BigDecimal("100.00"), rate);

        // 100 × 5 = 500, 阶梯折扣 10% → 450
        assertThat(scheme.calculateFinalPrice(5))
                .isEqualByComparingTo(new BigDecimal("450.00"));
    }
}
