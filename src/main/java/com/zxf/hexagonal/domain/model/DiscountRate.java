package com.zxf.hexagonal.domain.model;

import java.math.BigDecimal;

/**
 * 折扣率值对象：[0, 1] 区间校验内聚于构造期。
 *
 * <p>语义约定：值为「减免比例」（0.10 = 减 10%），而非「支付乘数」（0.90）。
 * 领域策略（如 {@code VolumeDiscountPolicy}）产出本类型，定价方案
 * （{@code PriceScheme}）消费本类型——错误的语义在类型层面即被拦截。</p>
 */
public record DiscountRate(BigDecimal value) {

    /** 零折扣。 */
    public static final DiscountRate ZERO = new DiscountRate(BigDecimal.ZERO);

    public DiscountRate {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "Discount rate must be between 0 and 1, was: " + value);
        }
    }

    /**
     * 对金额应用折扣，返回减免后的金额（amount × (1 − 折扣率)）。
     *
     * <p>精度与舍入策略（scale / RoundingMode）尚未约定，随 REQ-002 的 Money 决策一并确定。</p>
     *
     * @param amount 金额，非负
     * @return 减免后的金额
     */
    public BigDecimal applyTo(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must not be negative, was: " + amount);
        }
        return amount.multiply(BigDecimal.ONE.subtract(value));
    }
}
