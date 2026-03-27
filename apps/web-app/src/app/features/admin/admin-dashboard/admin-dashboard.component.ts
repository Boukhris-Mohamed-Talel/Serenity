import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { InsuranceService } from '../../../core/services/insurance.service';
import { UserResponse } from '../../../shared/models/user.model';
import { MarketplaceOrder } from '../../../shared/models/marketplace.model';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  users: UserResponse[] = [];
  orders: MarketplaceOrder[] = [];
  claims: InsuranceClaimResponse[] = [];
  loading = true;

  constructor(
    private readonly userService: UserService,
    private readonly marketplaceService: MarketplaceService,
    private readonly insuranceService: InsuranceService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    // Load all data in parallel
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
      },
      error: () => {
        console.error('Failed to load users');
      },
      complete: () => this.checkLoadingComplete()
    });

    this.marketplaceService.getAllOrdersForAdmin().subscribe({
      next: (orders) => {
        this.orders = orders;
      },
      error: () => {
        console.error('Failed to load orders');
      },
      complete: () => this.checkLoadingComplete()
    });

    this.insuranceService.getAllClaims().subscribe({
      next: (claims) => {
        this.claims = claims;
      },
      error: () => {
        console.error('Failed to load claims');
      },
      complete: () => this.checkLoadingComplete()
    });
  }

  private checkLoadingComplete(): void {
    // Simple check - if we have data from at least one source or timeout after requests
    if (this.users.length > 0 || this.orders.length > 0 || this.claims.length > 0) {
      this.loading = false;
    }
  }

  // ─── User Stats ──────────────────────────────
  get totalUsers(): number { return this.users.length; }
  get activeUsers(): number { return this.users.filter(u => u.isActive).length; }
  get doctorCount(): number { return this.users.filter(u => u.role === 'DOCTOR').length; }
  get patientCount(): number { return this.users.filter(u => u.role === 'PATIENT').length; }

  // ─── Marketplace Stats ───────────────────────
  get totalOrders(): number { return this.orders.length; }
  get paidOrders(): number { return this.orders.filter(o => o.status === 'PAID').length; }
  get pendingOrders(): number { return this.orders.filter(o => o.status === 'CREATED').length; }
  get totalRevenue(): number { return this.orders.filter(o => o.status === 'PAID').reduce((sum, o) => sum + (o.totalAmount || 0), 0); }

  // ─── Insurance Stats ─────────────────────────
  get totalClaims(): number { return this.claims.length; }
  get approvedClaims(): number { return this.claims.filter(c => c.status === 'APPROVED').length; }
  get pendingClaims(): number { return this.claims.filter(c => c.status === 'PENDING').length; }
  get totalReimbursement(): number { return this.claims.filter(c => c.status === 'APPROVED').reduce((sum, c) => sum + (c.reimbursementAmount || 0), 0); }

  // ─── Recent Data ─────────────────────────────
  get recentOrders(): MarketplaceOrder[] {
    return this.orders.slice(0, 5);
  }

  get recentClaims(): InsuranceClaimResponse[] {
    return this.claims.slice(0, 5);
  }
}
