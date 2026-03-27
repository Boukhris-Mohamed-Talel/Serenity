import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit, OnDestroy {
  products: MarketplaceProduct[] = [];
  loading = false;
  query = '';
  selectedCategory: MarketplaceProductCategory | '' = '';
  selectedType: MarketplaceProductType | '' = '';
  resetAnimating = false;

  private searchDebounceId: number | null = null;
  private readonly searchDebounceMs = 220;

  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
      this.searchDebounceId = null;
    }
  }

  loadProducts(): void {
    this.loading = true;
    this.marketplaceService.getProducts({
      query: this.query,
      category: this.selectedCategory,
      type: this.selectedType
    }).subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.query = '';
    this.selectedCategory = '';
    this.selectedType = '';
    this.loadProducts();
  }

  clearFiltersSmooth(): void {
    this.resetAnimating = true;
    this.clearFilters();
    window.setTimeout(() => {
      this.resetAnimating = false;
    }, 320);
  }

  onQueryInputChange(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
    }

    this.searchDebounceId = window.setTimeout(() => {
      this.loadProducts();
    }, this.searchDebounceMs);
  }

  addToCart(product: MarketplaceProduct): void {
    this.marketplaceService.addToCart(product, 1);
  }

  openDetails(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  isManager(): boolean {
    return this.authService.hasRole('MARKETPLACE_MANAGER') || this.authService.isAdmin();
  }
}
