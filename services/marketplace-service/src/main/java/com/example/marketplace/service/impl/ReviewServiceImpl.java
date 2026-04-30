package com.example.marketplace.service.impl;

import com.example.marketplace.dto.ProductReviewDTO;
import com.example.marketplace.entity.OrderStatus;
import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductReview;
import com.example.marketplace.entity.ReviewHelpfulVote;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.MarketplaceOrderRepository;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.repository.ProductReviewRepository;
import com.example.marketplace.repository.ReviewHelpfulVoteRepository;
import com.example.marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final MarketplaceOrderRepository orderRepository;

    @Override
    public ProductReviewDTO createOrUpdateReview(Long userId, String userEmail, Long productId, Integer rating, String reviewText) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> ProductReview.builder()
                        .product(product)
                        .userId(userId)
                        .userEmail(userEmail)
                        .build());

        review.setRating(rating);
        review.setReviewText(reviewText);
        review = reviewRepository.save(review);

        return mapSingleReview(review, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewDTO getReviewById(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return mapSingleReview(review, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewDTO> getProductReviews(Long productId, Long viewerUserId) {
        List<ProductReview> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = reviews.stream().map(ProductReview::getId).toList();
        Map<Long, Long> helpfulByReviewId = loadHelpfulCounts(reviewIds);
        Set<Long> verifiedAuthors =
                orderRepository.findCustomerUserIdsWithPaidPurchaseForProduct(productId, OrderStatus.PAID);
        Set<Long> markedByViewer = viewerUserId == null
                ? Collections.emptySet()
                : new HashSet<>(helpfulVoteRepository.findReviewIdsMarkedByUser(reviewIds, viewerUserId));

        return reviews.stream()
                .map(r -> mapToDTO(
                        r,
                        helpfulByReviewId.getOrDefault(r.getId(), 0L).intValue(),
                        verifiedAuthors.contains(r.getUserId()),
                        markedByViewer.contains(r.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewDTO> getUserReviews(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(r -> mapSingleReview(r, userId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        helpfulVoteRepository.deleteByReviewId(reviewId);
        reviewRepository.deleteById(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        Double average = reviewRepository.getAverageRatingByProductId(productId);
        return average != null ? average : 0.0;
    }

    @Override
    public ProductReviewDTO markReviewHelpful(Long reviewId, Long userId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (helpfulVoteRepository.existsByReview_IdAndUserId(reviewId, userId)) {
            return mapSingleReview(review, userId);
        }

        ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                .review(review)
                .userId(userId)
                .build();
        helpfulVoteRepository.save(vote);

        return mapSingleReview(review, userId);
    }

    private ProductReviewDTO mapSingleReview(ProductReview review, Long viewerUserId) {
        Long rid = review.getId();
        int helpful = (int) helpfulVoteRepository.countByReview_Id(rid);

        Long productId = review.getProduct().getId();
        boolean verified = orderRepository
                .findCustomerUserIdsWithPaidPurchaseForProduct(productId, OrderStatus.PAID)
                .contains(review.getUserId());

        boolean marked = viewerUserId != null && helpfulVoteRepository.existsByReview_IdAndUserId(rid, viewerUserId);

        return mapToDTO(review, helpful, verified, marked);
    }

    private Map<Long, Long> loadHelpfulCounts(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = helpfulVoteRepository.countByReviewIds(reviewIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    private ProductReviewDTO mapToDTO(ProductReview review, int helpfulCount, boolean verifiedPurchase, boolean viewerMarkedHelpful) {
        return ProductReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUserId())
                .userEmail(review.getUserEmail())
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .helpfulCount(helpfulCount)
                .verifiedPurchase(verifiedPurchase)
                .viewerMarkedHelpful(viewerMarkedHelpful)
                .build();
    }
}
