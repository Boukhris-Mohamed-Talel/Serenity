package com.example.insurance.integration;

import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.NotificationType;
import com.example.insurance.entity.Remboursement;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InsuranceStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(InsuranceStatusPoller.class);

    private final InsuranceClaimRepository claimRepository;
    private final RemboursementRepository remboursementRepository;
    private final InsurancePortalClient insurancePortalClient;
    private final NotificationService notificationService;

    @Transactional
    @Scheduled(fixedDelayString = "${app.status-poller.fixed-delay-ms}")
    public void pollPendingClaims() {
        List<InsuranceClaim> pendingClaims = claimRepository.findByStatusOrderByClaimDateDesc(ClaimStatus.PENDING);
        if (pendingClaims.isEmpty()) {
            return;
        }

        for (InsuranceClaim claim : pendingClaims) {
            String externalRef = claim.getExternalRef();
            if (externalRef == null || externalRef.isBlank()) {
                continue;
            }

            PortalClaimStatusResponse status = insurancePortalClient.fetchClaimStatus(externalRef);
            if (status == null || status.getStatus() == null) {
                continue;
            }

            String portalStatus = status.getStatus().trim().toUpperCase();
            if ("APPROVED".equals(portalStatus)) {
                Double amount = status.getReimbursementAmount() != null ? status.getReimbursementAmount() : 0.0;
                claim.setStatus(ClaimStatus.APPROVED);
                claim.setReimbursementAmount(amount);
                claim.setReason(status.getReason());

                Remboursement remboursement = Remboursement.builder()
                        .montant(amount)
                        .statut(ClaimStatus.APPROVED)
                        .insuranceClaim(claim)
                        .build();
                remboursementRepository.save(remboursement);
                claimRepository.save(claim);
                notificationService.createNotification(
                        claim.getUserId(),
                        claim.getId(),
                        NotificationType.CLAIM_APPROVED,
                        "External insurer approved your claim",
                        "Your claim was approved by the insurance portal with reimbursement amount: " + amount
                                + (status.getReason() != null && !status.getReason().isBlank() ? ". Reason: " + status.getReason() : "")
                );
                log.info("Updated claim {} from portal status APPROVED", externalRef);
            } else if ("REJECTED".equals(portalStatus)) {
                claim.setStatus(ClaimStatus.REJECTED);
                // DB schema requires a non-null reimbursement_amount
                claim.setReimbursementAmount(0.0);
                claim.setReason(status.getReason());
                // If remboursements exist, clear them (orphanRemoval should handle)
                if (claim.getRemboursements() != null) {
                    claim.getRemboursements().clear();
                }
                claimRepository.save(claim);
                notificationService.createNotification(
                        claim.getUserId(),
                        claim.getId(),
                        NotificationType.CLAIM_REJECTED,
                        "External insurer rejected your claim",
                        "Your claim was rejected by the insurance portal."
                                + (status.getReason() != null && !status.getReason().isBlank() ? " Reason: " + status.getReason() : "")
                );
                log.info("Updated claim {} from portal status REJECTED", externalRef);
            }
        }
    }
}

