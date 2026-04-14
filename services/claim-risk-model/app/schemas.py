from __future__ import annotations

from pydantic import BaseModel, Field


class ClaimScoreRequest(BaseModel):
    """Matches the fields available from InsuranceClaim + InsuranceClaimOcrAudit.

    Required fields come directly from the claim entity.
    Optional OCR fields are populated when OCR analysis has run.
    Optional history fields can be enriched by insurance-service before calling.
    """

    # --- from InsuranceClaim entity (always available) ---
    claim_id: int = Field(..., examples=[42])
    user_id: int = Field(..., examples=[101])
    amount: float = Field(..., gt=0, description="Claimed amount", examples=[450.0])
    reimbursement_amount: float = Field(..., ge=0, examples=[380.0])
    insurance_company: str = Field(..., examples=["CNAM"])
    insurance_grade: float = Field(..., ge=1, le=5, examples=[3.0])
    file_count: int = Field(0, ge=0, description="Number of attached documents (filePaths.size())")

    # --- from InsuranceClaimOcrAudit (optional, populated after OCR) ---
    ocr_decision: str | None = Field(None, description="PASS / MINOR_MISMATCH / MAJOR_BLOCKED")
    ocr_mismatch_count: int = Field(0, ge=0)
    ocr_major_count: int = Field(0, ge=0)
    ocr_minor_count: int = Field(0, ge=0)
    ocr_submitted_amount: float | None = Field(None, ge=0)
    ocr_extracted_amount: float | None = Field(None, ge=0)

    # --- enrichment fields (insurance-service can compute from DB) ---
    user_total_claims: int = Field(0, ge=0, description="Total past claims by this user")
    user_claims_30d: int = Field(0, ge=0, description="User claims in last 30 days")
    days_since_last_claim: float | None = Field(None, ge=0, description="Days since user's last claim")
    user_rejected_claims: int = Field(0, ge=0, description="Past rejected claims for this user")


class ClaimRiskResponse(BaseModel):
    risk_score: float = Field(..., description="0-100 score")
    risk_band: str = Field(..., description="LOW / MEDIUM / HIGH")
    top_reasons: list[str]


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
