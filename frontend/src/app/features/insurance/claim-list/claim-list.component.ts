import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-claim-list',
  templateUrl: './claim-list.component.html',
  styleUrls: ['./claim-list.component.scss']
})
export class ClaimListComponent implements OnInit {
  claims: InsuranceClaimResponse[] = [];
  loading = true;
  errorMessage = '';
  isAdmin = false;

  constructor(
    private readonly insuranceService: InsuranceService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading = true;
    const source$ = this.isAdmin
      ? this.insuranceService.getAllClaims()
      : this.insuranceService.getMyClaims();

    source$.subscribe({
      next: (claims) => {
        this.claims = claims;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load claims';
        this.loading = false;
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

  getTotalReimbursed(claim: InsuranceClaimResponse): number {
    return claim.remboursements?.reduce((sum, r) => sum + r.montant, 0) || 0;
  }
}
