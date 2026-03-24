import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-claim-detail',
  templateUrl: './claim-detail.component.html',
  styleUrls: ['./claim-detail.component.scss']
})
export class ClaimDetailComponent implements OnInit {
  claim: InsuranceClaimResponse | null = null;
  loading = true;
  errorMessage = '';
  reimbursementAmount: number | null = null;
  isAdmin = false;
  processing = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly insuranceService: InsuranceService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadClaim(id);
    }
  }

  loadClaim(id: number): void {
    this.loading = true;
    this.insuranceService.getClaimById(id).subscribe({
      next: (claim) => {
        this.claim = claim;
        this.reimbursementAmount = claim.amount;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load claim';
        this.loading = false;
      }
    });
  }

  approveClaim(): void {
    if (!this.claim || !this.reimbursementAmount) return;
    this.processing = true;
    this.insuranceService.approveClaim(this.claim.id, this.reimbursementAmount).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.processing = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to approve claim';
        this.processing = false;
      }
    });
  }

  rejectClaim(): void {
    if (!this.claim) return;
    this.processing = true;
    this.insuranceService.rejectClaim(this.claim.id).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.processing = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to reject claim';
        this.processing = false;
      }
    });
  }

  deleteClaim(): void {
    if (!this.claim) return;
    this.processing = true;
    this.insuranceService.deleteClaim(this.claim.id).subscribe({
      next: () => {
        this.goBack();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to delete claim';
        this.processing = false;
      }
    });
  }

  getFileUrl(path: string): string {
    return `${environment.insuranceApiUrl}/files/open?path=${encodeURIComponent(path)}`;
  }

  getFileName(path: string): string {
    const parts = path.split('/');
    const full = parts[parts.length - 1];
    const underscoreIdx = full.indexOf('_');
    return underscoreIdx > -1 ? full.substring(underscoreIdx + 1) : full;
  }

  openFile(path: string, event: Event): void {
    event.preventDefault();
    const url = this.getFileUrl(path);
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        window.open(objectUrl, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
      },
      error: () => {
        this.errorMessage = 'Failed to open attachment';
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'badge badge-success';
      case 'REJECTED': return 'badge badge-danger';
      default: return 'badge badge-primary';
    }
  }

  goBack(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin/insurance']);
    } else {
      this.router.navigate(['/insurance']);
    }
  }
}
