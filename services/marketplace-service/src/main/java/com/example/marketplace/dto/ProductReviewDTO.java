package com.example.marketplace.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewDTO {
    private Long id;
    private Long productId;
    private Long userId;
    private String userEmail;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Count of signed-in users who marked this review helpful. */
    private int helpfulCount;
    /** True when this reviewer has a PAID order that includes this product. */
    private boolean verifiedPurchase;
    /** Present for the signed-in viewer: whether they already marked helpful. */
    private boolean viewerMarkedHelpful;
}
