package com.example.insurance.dto;

import com.example.insurance.entity.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Result of {@link com.example.insurance.repository.InsuranceClaimRepository#findRemittanceOcrSummaryByJpql()} —
 * one row per claim: aggregates from joined remboursements plus OCR audit count via subquery.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRemittanceOcrSummaryDTO {

    private Long claimId;
    /** Insurer-facing reference on the claim ({@link InsuranceClaim#getExternalRef()}). */
    private String externalRef;
    private Long userId;
    private ClaimStatus status;
    private Double amount;
    private Double reimbursementAmount;
    private String insuranceCompany;
    private Date claimDate;
    private Double totalRemboursementPaid;
    private Long remboursementCount;
    private Long ocrAuditCount;
}
