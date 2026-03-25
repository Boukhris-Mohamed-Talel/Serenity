package com.example.insurance.repository;

import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    List<InsuranceClaim> findByUserIdOrderByClaimDateDesc(Long userId);

    List<InsuranceClaim> findAllByOrderByClaimDateDesc();

    List<InsuranceClaim> findByStatusOrderByClaimDateDesc(ClaimStatus status);
}
