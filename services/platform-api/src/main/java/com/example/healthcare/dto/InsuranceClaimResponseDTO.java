package com.example.healthcare.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceClaimResponseDTO {

    private Long id;
    private String description;
    private Date claimDate;
    private Double amount;
    private String insuranceCompany;
    private Integer insuranceGrade;
    private Double reimbursementAmount;
    private String status;
    private String externalRef;
    private List<String> filePaths;
    private Long userId;
    private String userFullName;
    private List<RemboursementResponseDTO> remboursements;
}
