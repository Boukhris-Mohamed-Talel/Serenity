package com.example.insurance.service.impl;

import com.example.insurance.dto.InsuranceClaimOcrAuditResponseDTO;
import com.example.insurance.dto.InsuranceClaimResponseDTO;
import com.example.insurance.dto.InsuranceClaimTransitionResponseDTO;
import com.example.insurance.entity.ClaimStatus;
import com.example.insurance.entity.InsuranceClaim;
import com.example.insurance.entity.InsuranceClaimOcrAudit;
import com.example.insurance.entity.InsuranceClaimTransition;
import com.example.insurance.entity.OcrAnalysisDecision;
import com.example.insurance.entity.OcrAttemptType;
import com.example.insurance.integration.InsurancePortalClient;
import com.example.insurance.ocr.ClaimConsistencyService;
import com.example.insurance.ocr.OcrExtractionService;
import com.example.insurance.repository.InsuranceClaimOcrAuditRepository;
import com.example.insurance.repository.InsuranceClaimRepository;
import com.example.insurance.repository.InsuranceClaimTransitionRepository;
import com.example.insurance.repository.RemboursementRepository;
import com.example.insurance.service.ClaimRiskScoringService;
import com.example.insurance.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceClaimServiceImplBroadCoverageTest {

    @Mock private InsuranceClaimRepository claimRepository;
    @Mock private InsuranceClaimOcrAuditRepository ocrAuditRepository;
    @Mock private InsuranceClaimTransitionRepository transitionRepository;
    @Mock private RemboursementRepository remboursementRepository;
    @Mock private InsurancePortalClient insurancePortalClient;
    @Mock private NotificationService notificationService;
    @Mock private OcrExtractionService ocrExtractionService;
    @Mock private ClaimConsistencyService claimConsistencyService;
    @Mock private ClaimRiskScoringService claimRiskScoringService;

    @InjectMocks private InsuranceClaimServiceImpl service;

    @BeforeEach
    void cfg() {
        ReflectionTestUtils.setField(service, "uploadDir", "build/test-uploads");
        ReflectionTestUtils.setField(service, "publicBaseUrl", "http://localhost:8090");
        ReflectionTestUtils.setField(service, "ocrEnabled", false);
        ReflectionTestUtils.setField(service, "strictMajorBlock", true);
        ReflectionTestUtils.setField(service, "storeFullExtractedText", false);
        ReflectionTestUtils.setField(service, "ocrAdminAlertUserIds", "");
    }

    private static InsuranceClaim baseClaim(Long id, Long userId, ClaimStatus status) {
        return InsuranceClaim.builder()
                .id(id)
                .userId(userId)
                .description("Some valid description here")
                .amount(50.0)
                .reimbursementAmount(5.0)
                .insuranceCompany("Insurance 1")
                .insuranceGrade(1.0)
                .status(status)
                .externalRef("ref-" + id)
                .claimDate(new Date())
                .filePaths(List.of())
                .build();
    }

    @Test
    void getClaimsByUserId_listsFromRepository() {
        InsuranceClaim c = baseClaim(1L, 7L, ClaimStatus.SUBMITTED);
        when(claimRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(c));

        List<InsuranceClaimResponseDTO> list = service.getClaimsByUserId(7L, null, null, null, null, "claimDate", "desc");
        assertThat(list).hasSize(1);
        assertEquals(1L, list.get(0).getId());
    }

    @Test
    void getAllClaimsPaged_returnsPage() {
        InsuranceClaim c = baseClaim(10L, 5L, ClaimStatus.SUBMITTED);
        when(claimRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1));

        var page = service.getAllClaimsPaged(null, null, null, null, "amount", "asc", null, 0, 10);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getClaimById_whenOwner_returnsDto() {
        InsuranceClaim c = baseClaim(2L, 8L, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(2L)).thenReturn(Optional.of(c));

        InsuranceClaimResponseDTO dto = service.getClaimById(2L, 8L, false);
        assertThat(dto.getUserId()).isEqualTo(8L);
    }

    @Test
    void approveClaim_submitted_transitionsAndNotifies() {
        InsuranceClaim c = baseClaim(3L, 9L, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(3L)).thenReturn(Optional.of(c));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        InsuranceClaimResponseDTO dto = service.approveClaim(3L, 25.0, 1L);
        assertThat(dto.getStatus()).isEqualTo(ClaimStatus.APPROVED.name());
        verify(remboursementRepository).save(any());
        verify(notificationService).createNotification(eq(9L), eq(3L), any(), anyString(), anyString());
    }

    @Test
    void rejectClaim_submitted_transitions() {
        InsuranceClaim c = baseClaim(4L, 9L, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(4L)).thenReturn(Optional.of(c));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        InsuranceClaimResponseDTO dto = service.rejectClaim(4L, 2L);
        assertThat(dto.getStatus()).isEqualTo(ClaimStatus.REJECTED.name());
    }

    @Test
    void deleteClaim_whenRejected_deletes() {
        InsuranceClaim c = baseClaim(5L, 1L, ClaimStatus.REJECTED);
        when(claimRepository.findById(5L)).thenReturn(Optional.of(c));

        service.deleteClaim(5L);
        verify(claimRepository).delete(c);
    }

    @Test
    void getClaimTimeline_returnsMappedTransitions() {
        InsuranceClaim c = baseClaim(6L, 10L, ClaimStatus.APPROVED);
        when(claimRepository.findById(6L)).thenReturn(Optional.of(c));
        InsuranceClaimTransition tr = InsuranceClaimTransition.builder()
                .id(100L)
                .insuranceClaim(c)
                .fromStatus(ClaimStatus.SUBMITTED)
                .toStatus(ClaimStatus.APPROVED)
                .changedByUserId(1L)
                .changedByRole("ADMIN")
                .reason("ok")
                .changedAt(LocalDateTime.now())
                .build();
        when(transitionRepository.findByInsuranceClaimIdOrderByChangedAtAsc(6L)).thenReturn(List.of(tr));

        List<InsuranceClaimTransitionResponseDTO> list = service.getClaimTimeline(6L, 10L, false);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getToStatus()).isEqualTo(ClaimStatus.APPROVED.name());
    }

    @Test
    void getClaimOcrAudit_mapsAudits() {
        InsuranceClaim c = baseClaim(7L, 11L, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(7L)).thenReturn(Optional.of(c));
        InsuranceClaimOcrAudit audit = InsuranceClaimOcrAudit.builder()
                .id(1L)
                .claimId(7L)
                .userId(11L)
                .attemptType(OcrAttemptType.INITIAL_SUBMISSION)
                .decision(OcrAnalysisDecision.PASS)
                .mismatchCount(0)
                .majorCount(0)
                .minorCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        when(ocrAuditRepository.findByClaimIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(audit));

        List<InsuranceClaimOcrAuditResponseDTO> list = service.getClaimOcrAudit(7L, 11L, false);
        assertThat(list).hasSize(1);
        assertEquals("PASS", list.get(0).getDecision());
    }

    @Test
    void requestAdditionalDocuments_whenSubmitted_movesToNeedsInfo() {
        InsuranceClaim c = baseClaim(20L, 3L, ClaimStatus.SUBMITTED);
        when(claimRepository.findById(20L)).thenReturn(Optional.of(c));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        InsuranceClaimResponseDTO dto = service.requestAdditionalDocuments(20L, 99L, " need docs ", new Date());

        assertThat(dto.getStatus()).isEqualTo(ClaimStatus.NEEDS_INFO.name());
        verify(notificationService).createNotification(eq(3L), eq(20L), any(), anyString(), anyString());
    }

    @Test
    void getRemittanceOcrSummaryReport_delegates() {
        when(claimRepository.findRemittanceOcrSummaryByJpql()).thenReturn(List.of());
        assertThat(service.getRemittanceOcrSummaryReport()).isEmpty();
    }

    @Test
    void getClaimsByUserId_invalidSortBy_throws() {
        assertThatThrownBy(() -> service.getClaimsByUserId(1L, null, null, null, null, "not_a_field", "desc"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getClaimsByUserId_fromAfterTo_throws() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate to = LocalDate.of(2026, 5, 1);
        assertThatThrownBy(() -> service.getClaimsByUserId(1L, null, null, from, to, "claimDate", "desc"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
