package com.zxf.hexagonal.domain.model;

import java.math.BigDecimal;

/**
 * 定价方案值对象：单价与折扣率内聚构造期校验，纯计算逻辑无外部依赖。
 */
public record PriceScheme(BigDecimal basePrice, DiscountRate discountRate) {

    public PriceScheme {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price must not be negative, was: " + basePrice);
        }
    }

    /**
     * 计算最终价格：小计（单价 × 数量）经折扣率减免。
     *
     * @param quantity 数量，必须为正
     * @return 最终价格
     */
    public BigDecimal calculateFinalPrice(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, was: " + quantity);
        }
        BigDecimal subtotal = basePrice.multiply(BigDecimal.valueOf(quantity));
        return discountRate.applyTo(subtotal);
    }
}
