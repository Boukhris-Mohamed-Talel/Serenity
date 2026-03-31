package com.example.marketplace.controller;

import com.example.marketplace.dto.CreateReviewRequestDTO;
import com.example.marketplace.dto.ProductReviewDTO;
import com.example.marketplace.security.JwtTokenProvider;
import com.example.marketplace.service.ReviewService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping({"/api/articles/reviews", "/api/marketplace/reviews"})
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReviewDTO> createOrUpdateReview(
            @RequestHeader(value = "userId", required = false) Long userId,
            Authentication authentication,
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody CreateReviewRequestDTO request) {
        Long resolvedUserId = resolveUserId(userId, httpServletRequest);

        ProductReviewDTO review = reviewService.createOrUpdateReview(
                resolvedUserId,
                authentication.getName(),
                request.getProductId(),
                request.getRating(),
                request.getReviewText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReviewDTO>> getProductReviews(@PathVariable Long productId) {
        List<ProductReviewDTO> reviews = reviewService.getProductReviews(productId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product/{productId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long productId) {
        Double averageRating = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductReviewDTO>> getUserReviews(
            @RequestHeader(value = "userId", required = false) Long userId,
            HttpServletRequest httpServletRequest) {
        Long resolvedUserId = resolveUserId(userId, httpServletRequest);
        List<ProductReviewDTO> reviews = reviewService.getUserReviews(resolvedUserId);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader(value = "userId", required = false) Long userId,
            HttpServletRequest httpServletRequest) {
        Long resolvedUserId = resolveUserId(userId, httpServletRequest);
        reviewService.deleteReview(reviewId, resolvedUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ProductReviewDTO> getReview(@PathVariable Long reviewId) {
        ProductReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    private Long resolveUserId(Long headerUserId, HttpServletRequest request) {
        if (headerUserId != null) {
            return headerUserId;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing user context in request");
        }

        String token = authorizationHeader.substring(7);
        Claims claims = jwtTokenProvider.parseClaims(token);

        Object claimValue = claims.get("userId");
        if (claimValue instanceof Number number) {
            return number.longValue();
        }

        if (claimValue instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // Fall through to throw a user-facing validation message.
            }
        }

        throw new IllegalArgumentException("Unable to resolve userId from token");
    }
}