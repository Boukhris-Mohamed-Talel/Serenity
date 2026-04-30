package com.example.marketplace.repository;

import com.example.marketplace.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    long countByReview_Id(Long reviewId);

    boolean existsByReview_IdAndUserId(Long reviewId, Long userId);

    @Query("SELECT v.review.id, COUNT(v) FROM ReviewHelpfulVote v WHERE v.review.id IN :ids GROUP BY v.review.id")
    List<Object[]> countByReviewIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT v.review.id FROM ReviewHelpfulVote v WHERE v.userId = :userId AND v.review.id IN :ids")
    List<Long> findReviewIdsMarkedByUser(@Param("ids") Collection<Long> ids, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ReviewHelpfulVote v WHERE v.review.id = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
