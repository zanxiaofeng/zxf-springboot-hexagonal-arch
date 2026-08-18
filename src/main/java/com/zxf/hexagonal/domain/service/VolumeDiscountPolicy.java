package com.zxf.hexagonal.domain.service;

import com.zxf.hexagonal.domain.model.DiscountRate;

import java.math.BigDecimal;

/**
 * 阶梯折扣领域策略：按用户历史订单数确定折扣率。
 *
 * <p>规则的输入是跨聚合事实（历史订单数），不自然归属任何实体/值对象，
 * 故以领域策略承载于 {@code domain/service}。纯规则计算、零框架依赖、
 * 不访问任何端口——需要的事实由应用层查好后作为参数传入。</p>
 */
public class VolumeDiscountPolicy {

    private static final long LOYALTY_THRESHOLD = 10;
    private static final long VIP_THRESHOLD = 100;
    private static final DiscountRate LOYALTY_RATE = new DiscountRate(new BigDecimal("0.10"));
    private static final DiscountRate VIP_RATE = new DiscountRate(new BigDecimal("0.15"));

    /**
     * 按历史订单数确定折扣率：满 100 单 15%，满 10 单 10%，其余不打折。
     *
     * @param previousOrderCount 用户历史订单数，不可为负
     * @return 折扣率
     */
    public DiscountRate discountRateFor(long previousOrderCount) {
        if (previousOrderCount < 0) {
            throw new IllegalArgumentException(
                    "Previous order count must not be negative, was: " + previousOrderCount);
        }
        if (previousOrderCount >= VIP_THRESHOLD) {
            return VIP_RATE;
        }
        if (previousOrderCount >= LOYALTY_THRESHOLD) {
            return LOYALTY_RATE;
        }
        return DiscountRate.ZERO;
    }
}
