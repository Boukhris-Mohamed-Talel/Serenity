package com.example.healthcare.service;

import com.example.healthcare.entity.ClaimStatus;
import com.example.healthcare.entity.InsuranceClaim;
import com.example.healthcare.entity.Remboursement;
import com.example.healthcare.repository.InsuranceClaimRepository;
import com.example.healthcare.repository.RemboursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceStatusPoller {

    private final InsuranceClaimRepository claimRepository;
    private final RemboursementRepository remboursementRepository;
    private final InsurancePortalClient portalClient;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void pollPendingClaims() {
        List<InsuranceClaim> pendingClaims = claimRepository
                .findByStatusAndExternalRefIsNotNull(ClaimStatus.PENDING);

        if (pendingClaims.isEmpty()) return;

        log.info("Polling {} pending claims from insurance portal", pendingClaims.size());

        for (InsuranceClaim claim : pendingClaims) {
            InsurancePortalClient.ClaimStatusResponse response =
                    portalClient.pollClaimStatus(claim.getExternalRef());

            if (response == null) continue;

            if ("APPROVED".equals(response.getStatus())) {
                claim.setStatus(ClaimStatus.APPROVED);
                claimRepository.save(claim);

                Double montant = response.getReimbursementAmount() != null
                        ? response.getReimbursementAmount()
                        : claim.getAmount();

                Remboursement remboursement = Remboursement.builder()
                        .montant(montant)
                        .statut(ClaimStatus.APPROVED)
                        .insuranceClaim(claim)
                        .build();
                remboursementRepository.save(remboursement);

                log.info("Claim {} approved by insurance — reimbursement: {} TND",
                        claim.getExternalRef(), montant);

            } else if ("REJECTED".equals(response.getStatus())) {
                claim.setStatus(ClaimStatus.REJECTED);
                claimRepository.save(claim);
                log.info("Claim {} rejected by insurance", claim.getExternalRef());
            }
        }
    }
}
