import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';
import { UserService } from '../../../core/services/user.service';

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
  private readonly userLabelsByUserId = new Map<number, string>();

  statusFilter: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED' = 'ALL';
  fromDate = '';
  toDate = '';
  userFilter = 'ALL';
  sortBy: 'DATE' | 'REIMBURSEMENT' = 'DATE';
  sortDirection: 'ASC' | 'DESC' = 'DESC';
  openDropdown: 'status' | 'user' | 'sortBy' | 'sortDirection' | null = null;

  constructor(
    private readonly insuranceService: InsuranceService,
    private readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    const statusFromQuery = this.route.snapshot.queryParamMap.get('status');
    if (statusFromQuery === 'PENDING' || statusFromQuery === 'APPROVED' || statusFromQuery === 'REJECTED') {
      this.statusFilter = statusFromQuery;
    }
    if (this.isAdmin) {
      this.loadUsernames();
    }
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

  get filteredClaims(): InsuranceClaimResponse[] {
    let list = [...this.claims];

    if (this.statusFilter !== 'ALL') {
      list = list.filter(c => c.status === this.statusFilter);
    }

    if (this.isAdmin && this.userFilter !== 'ALL') {
      list = list.filter(c => this.getUserKey(c) === this.userFilter);
    }

    if (this.fromDate) {
      const from = new Date(this.fromDate);
      from.setHours(0, 0, 0, 0);
      list = list.filter(c => new Date(c.claimDate) >= from);
    }

    if (this.toDate) {
      const to = new Date(this.toDate);
      to.setHours(23, 59, 59, 999);
      list = list.filter(c => new Date(c.claimDate) <= to);
    }

    list.sort((a, b) => {
      const factor = this.sortDirection === 'ASC' ? 1 : -1;
      if (this.sortBy === 'DATE') {
        const av = new Date(a.claimDate).getTime();
        const bv = new Date(b.claimDate).getTime();
        return (av - bv) * factor;
      }
      const av = a.reimbursementAmount ?? 0;
      const bv = b.reimbursementAmount ?? 0;
      return (av - bv) * factor;
    });

    return list;
  }

  get availableUsers(): { key: string; label: string }[] {
    const map = new Map<string, string>();
    for (const claim of this.claims) {
      const key = this.getUserKey(claim);
      const label = this.getUserLabel(claim);
      map.set(key, label);
    }
    return Array.from(map.entries())
      .map(([key, label]) => ({ key, label }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }

  clearFilters(): void {
    this.statusFilter = 'ALL';
    this.fromDate = '';
    this.toDate = '';
    this.userFilter = 'ALL';
    this.sortBy = 'DATE';
    this.sortDirection = 'DESC';
  }

  onFromDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
  }

  onToDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
  }

  toggleDropdown(name: 'status' | 'user' | 'sortBy' | 'sortDirection', event: MouseEvent): void {
    event.stopPropagation();
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectStatus(value: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'): void {
    this.statusFilter = value;
    this.openDropdown = null;
  }

  selectUser(value: string): void {
    this.userFilter = value;
    this.openDropdown = null;
  }

  selectSortBy(value: 'DATE' | 'REIMBURSEMENT'): void {
    this.sortBy = value;
    this.openDropdown = null;
  }

  selectSortDirection(value: 'ASC' | 'DESC'): void {
    this.sortDirection = value;
    this.openDropdown = null;
  }

  get statusFilterLabel(): string {
    switch (this.statusFilter) {
      case 'PENDING': return 'Pending';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      default: return 'All';
    }
  }

  get sortByLabel(): string {
    return this.sortBy === 'DATE' ? 'Date' : 'Reimbursement amount';
  }

  get sortDirectionLabel(): string {
    return this.sortDirection === 'DESC' ? 'Descending' : 'Ascending';
  }

  get selectedUserLabel(): string {
    if (this.userFilter === 'ALL') {
      return 'All users';
    }
    return this.availableUsers.find(u => u.key === this.userFilter)?.label || 'All users';
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openDropdown = null;
  }

  private loadUsernames(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.userLabelsByUserId.clear();
        for (const user of users) {
          const isAnonymous = !!user.profile?.isAnonymous;
          const label = isAnonymous
            ? `AnonymousUser#${user.id}`
            : (user.firstName?.trim() || user.email?.split('@')[0]?.trim() || `User #${user.id}`);
          this.userLabelsByUserId.set(user.id, label);
        }
      },
      error: () => {
        // Keep graceful fallback labels when user list cannot be loaded.
      }
    });
  }

  private getUserLabel(claim: InsuranceClaimResponse): string {
    const label = this.userLabelsByUserId.get(claim.userId);
    if (label) {
      return label;
    }
    return claim.userFullName?.trim() || `User #${claim.userId}`;
  }

  private getUserKey(claim: InsuranceClaimResponse): string {
    return String(claim.userId);
  }
}
