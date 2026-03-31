package com.example.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "article_coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    private Long id;

    private String code;

    private String description;

    private Integer discountPercentage; // 0-100

    private BigDecimal minOrderAmount; // Minimum order amount to apply coupon

    private Integer maxUsageCount;

    private Integer usageCount;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private Boolean active;

    private LocalDateTime createdAt;

    public Coupon() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Integer discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Integer getMaxUsageCount() {
        return maxUsageCount;
    }

    public void setMaxUsageCount(Integer maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    private Boolean active;

    private LocalDateTime createdAt;

    public Boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return this.active &&
                now.isAfter(this.validFrom) &&
                now.isBefore(this.validUntil) &&
                this.usageCount < this.maxUsageCount;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || orderAmount.compareTo(this.minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        return orderAmount.multiply(BigDecimal.valueOf(this.discountPercentage))
                .divide(BigDecimal.valueOf(100));
    }
}
