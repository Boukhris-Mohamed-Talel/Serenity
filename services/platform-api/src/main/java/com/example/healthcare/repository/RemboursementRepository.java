package com.example.healthcare.repository;

import com.example.healthcare.entity.Remboursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemboursementRepository extends JpaRepository<Remboursement, Long> {

    List<Remboursement> findByInsuranceClaimId(Long claimId);
}
