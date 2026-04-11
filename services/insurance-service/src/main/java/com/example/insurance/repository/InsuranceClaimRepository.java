package com.example.insurance.repository;

import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long>, JpaSpecificationExecutor<InsuranceClaim> {

    List<InsuranceClaim> findByUserIdOrderByClaimDateDesc(Long userId);

    List<InsuranceClaim> findAllByOrderByClaimDateDesc();

    List<InsuranceClaim> findByStatusOrderByClaimDateDesc(ClaimStatus status);

    List<InsuranceClaim> findByStatusInOrderByClaimDateDesc(List<ClaimStatus> statuses);
}
