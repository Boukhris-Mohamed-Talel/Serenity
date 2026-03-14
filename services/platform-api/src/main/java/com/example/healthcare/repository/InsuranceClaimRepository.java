package com.example.healthcare.repository;

import com.example.healthcare.entity.ClaimStatus;
import com.example.healthcare.entity.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    List<InsuranceClaim> findByUserIdOrderByClaimDateDesc(Long userId);

    List<InsuranceClaim> findAllByOrderByClaimDateDesc();

    List<InsuranceClaim> findByStatusAndExternalRefIsNotNull(ClaimStatus status);
}
