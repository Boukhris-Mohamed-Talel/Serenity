package com.example.marketplace.service.impl;

import com.example.marketplace.dto.CouponDTO;
import com.example.marketplace.entity.Coupon;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final Map<String, Coupon> coupons = new ConcurrentHashMap<>();

    public CouponServiceImpl() {
    LocalDateTime now = LocalDateTime.now();
    registerCoupon(1L, "WELCOME10", "Welcome discount for first orders", 10,
        new BigDecimal("20.00"), 10000, now.minusMonths(1), now.plusYears(2));
    registerCoupon(2L, "CALM15", "Stress relief campaign coupon", 15,
        new BigDecimal("50.00"), 5000, now.minusMonths(1), now.plusYears(1));
    registerCoupon(3L, "SLEEP5", "Sleep support essentials discount", 5,
        new BigDecimal("15.00"), 20000, now.minusMonths(1), now.plusYears(3));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDTO validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = findCouponOrThrow(code);

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is no longer valid");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Order amount must be at least " + coupon.getMinOrderAmount());
        }

        BigDecimal discount = coupon.calculateDiscount(orderAmount);
        return mapToDTO(coupon, discount);
    }

    @Override
    public void applyCoupon(String code) {
        Coupon coupon = findCouponOrThrow(code);

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is no longer valid");
        }

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        coupons.put(coupon.getCode().toUpperCase(), coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponByCode(String code) {
        Coupon coupon = findCouponOrThrow(code);
        return mapToDTO(coupon, BigDecimal.ZERO);
    }

    private Coupon findCouponOrThrow(String code) {
        Coupon coupon = coupons.get(code.toUpperCase());
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon not found: " + code);
        }
        return coupon;
    }

    private void registerCoupon(Long id,
                                String code,
                                String description,
                                Integer discountPercentage,
                                BigDecimal minOrderAmount,
                                Integer maxUsageCount,
                                LocalDateTime validFrom,
                                LocalDateTime validUntil) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCode(code.toUpperCase());
        coupon.setDescription(description);
        coupon.setDiscountPercentage(discountPercentage);
        coupon.setMinOrderAmount(minOrderAmount);
        coupon.setMaxUsageCount(maxUsageCount);
        coupon.setUsageCount(0);
        coupon.setValidFrom(validFrom);
        coupon.setValidUntil(validUntil);
        coupon.setActive(true);
        coupon.setCreatedAt(LocalDateTime.now());
        coupons.put(coupon.getCode(), coupon);
    }

    private CouponDTO mapToDTO(Coupon coupon, BigDecimal discountAmount) {
        CouponDTO dto = new CouponDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountPercentage(coupon.getDiscountPercentage());
        dto.setMinOrderAmount(coupon.getMinOrderAmount());
        dto.setDiscountAmount(discountAmount);
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidUntil(coupon.getValidUntil());
        dto.setIsValid(coupon.isValid());
        return dto;
    }
}
