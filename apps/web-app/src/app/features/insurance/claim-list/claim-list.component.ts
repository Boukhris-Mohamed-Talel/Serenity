import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';
import { UserService } from '../../../core/services/user.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-claim-list',
  templateUrl: './claim-list.component.html',
  styleUrls: ['./claim-list.component.scss']
})
export class ClaimListComponent implements OnInit, OnDestroy {
  claims: InsuranceClaimResponse[] = [];
  loading = true;
  errorMessage = '';
  isAdmin = false;
  private readonly userLabelsByUserId = new Map<number, string>();
  private queryParamsSub?: Subscription;

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
    if (this.isAdmin) {
      this.loadUsernames();
    }
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      this.applyQueryParams(params);
      this.loadClaims();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
  }

  loadClaims(): void {
    this.loading = true;
    const backendFilters = {
      status: this.statusFilter === 'ALL' ? undefined : this.statusFilter,
      fromDate: this.fromDate || undefined,
      toDate: this.toDate || undefined,
      sortBy: this.sortBy === 'DATE' ? 'claimDate' : 'reimbursementAmount',
      sortDir: this.sortDirection.toLowerCase()
    };

    const source$ = this.isAdmin
      ? this.insuranceService.getAllClaims(backendFilters)
      : this.insuranceService.getMyClaims(backendFilters);

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

    if (this.isAdmin && this.userFilter !== 'ALL') {
      list = list.filter(c => this.getUserKey(c) === this.userFilter);
    }

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
    this.userFilter = 'ALL';
    this.updateQueryParams({
      user: null,
      status: null,
      fromDate: null,
      toDate: null,
      sortBy: null,
      sortDir: null
    });
  }

  onFromDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
    this.updateQueryParams({
      fromDate: this.fromDate || null,
      toDate: this.toDate || null
    });
  }

  onToDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
    this.updateQueryParams({
      fromDate: this.fromDate || null,
      toDate: this.toDate || null
    });
  }

  toggleDropdown(name: 'status' | 'user' | 'sortBy' | 'sortDirection', event: MouseEvent): void {
    event.stopPropagation();
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectStatus(value: 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'): void {
    this.statusFilter = value;
    this.openDropdown = null;
    this.updateQueryParams({
      status: value === 'ALL' ? null : value
    });
  }

  selectUser(value: string): void {
    this.userFilter = value;
    this.openDropdown = null;
    this.updateQueryParams({
      user: value === 'ALL' ? null : value
    });
  }

  selectSortBy(value: 'DATE' | 'REIMBURSEMENT'): void {
    this.sortBy = value;
    this.openDropdown = null;
    this.updateQueryParams({
      sortBy: value === 'DATE' ? 'claimDate' : 'reimbursementAmount'
    });
  }

  selectSortDirection(value: 'ASC' | 'DESC'): void {
    this.sortDirection = value;
    this.openDropdown = null;
    this.updateQueryParams({
      sortDir: value.toLowerCase()
    });
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

  private applyQueryParams(params: import('@angular/router').ParamMap): void {
    const statusParam = params.get('status');
    this.statusFilter =
      statusParam === 'PENDING' || statusParam === 'APPROVED' || statusParam === 'REJECTED'
        ? statusParam
        : 'ALL';

    const fromDate = params.get('fromDate') || '';
    const toDate = params.get('toDate') || '';
    this.fromDate = fromDate;
    this.toDate = toDate;

    const sortByParam = params.get('sortBy');
    this.sortBy = sortByParam === 'reimbursementAmount' ? 'REIMBURSEMENT' : 'DATE';

    const sortDirParam = params.get('sortDir');
    this.sortDirection = sortDirParam === 'asc' ? 'ASC' : 'DESC';

    const userParam = params.get('user');
    this.userFilter = this.isAdmin && userParam ? userParam : 'ALL';
  }

  private updateQueryParams(queryParams: {
    user?: string | null;
    status?: string | null;
    fromDate?: string | null;
    toDate?: string | null;
    sortBy?: string | null;
    sortDir?: string | null;
  }): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });
  }
}
