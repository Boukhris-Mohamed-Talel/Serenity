import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { skip } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceShellService } from '../../../core/services/marketplace-shell.service';
import { MarketplaceUxService, SavedSearchEntry } from '../../../core/services/marketplace-ux.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  MarketplaceSort,
  ProductRecommendationItem
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit, OnDestroy {
  allProducts: MarketplaceProduct[] = [];
  loading = false;
  query = '';
  selectedCategory: MarketplaceProductCategory | '' = '';
  selectedType: MarketplaceProductType | '' = '';
  resetAnimating = false;
  showQuiz = false;
  quizSubmitting = false;
  quizError = '';
  quizReasoning = '';
  quizRecommendations: ProductRecommendationItem[] = [];

  recentProducts: MarketplaceProduct[] = [];
  searchAlertMessage = '';
  savedSearchNameInput = '';
  goalsSleep = 3;
  goalsStress = 3;
  goalsFocus = 3;
  compareHint = '';

  sortBy: MarketplaceSort = 'newest';
  readonly pageSize = 9;
  currentPage = 1;
  catalogTurning = false;

  readonly sortOptions: { value: MarketplaceSort; label: string }[] = [
    { value: 'newest', label: 'Featured' },
    { value: 'price_asc', label: 'Price: low to high' },
    { value: 'price_desc', label: 'Price: high to low' },
    { value: 'name', label: 'Name A–Z' }
  ];

  anxietyLevel = 3;
  stressLevel = 3;
  sleepNeed = 3;

  private searchDebounceId: number | null = null;
  private readonly searchDebounceMs = 220;
  private routeSub?: Subscription;

  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly ux: MarketplaceUxService,
    private readonly shell: MarketplaceShellService
  ) {}

  ngOnInit(): void {
    const g = this.ux.getGoals();
    this.goalsSleep = g.sleep;
    this.goalsStress = g.stress;
    this.goalsFocus = g.focus;
    this.applyRouteParams(this.route.snapshot.queryParamMap);
    this.loadProducts();
    this.loadRecentSnapshots();
    this.routeSub = this.route.queryParamMap.pipe(skip(1)).subscribe(map => {
      const filtersChanged = this.applyRouteParams(map);
      if (filtersChanged) {
        this.loadProducts();
      } else {
        this.clampCurrentPage();
        this.syncFiltersToUrl();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
      this.searchDebounceId = null;
    }
    this.routeSub?.unsubscribe();
  }

  get sortedProducts(): MarketplaceProduct[] {
    const list = [...this.allProducts];
    switch (this.sortBy) {
      case 'price_asc':
        return list.sort((a, b) => a.price - b.price);
      case 'price_desc':
        return list.sort((a, b) => b.price - a.price);
      case 'name':
        return list.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
      default:
        return list.sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
    }
  }

  get visibleSortedProducts(): MarketplaceProduct[] {
    return this.sortedProducts.filter(p => !this.ux.isHidden(p.id));
  }

  get picksForYou(): MarketplaceProduct[] {
    const base = this.allProducts.filter(p => !this.ux.isHidden(p.id));
    const scored = base
      .map(p => ({ p, s: this.ux.scoreForGoals(p) }))
      .filter(x => x.s > 0)
      .sort((a, b) => b.s - a.s);
    return scored.map(x => x.p).slice(0, 8);
  }

  get paginatedProducts(): MarketplaceProduct[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.visibleSortedProducts.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.visibleSortedProducts.length / this.pageSize));
  }

  get showingFrom(): number {
    if (!this.visibleSortedProducts.length) {
      return 0;
    }
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get showingTo(): number {
    if (!this.visibleSortedProducts.length) {
      return 0;
    }
    return Math.min(this.currentPage * this.pageSize, this.visibleSortedProducts.length);
  }

  get pageNumbers(): number[] {
    const total = this.totalPages;
    if (total <= 9) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }
    const cur = this.currentPage;
    const set = new Set<number>([1, total]);
    for (let i = cur - 2; i <= cur + 2; i++) {
      if (i >= 1 && i <= total) {
        set.add(i);
      }
    }
    return [...set].sort((a, b) => a - b);
  }

  loadProducts(): void {
    this.loading = true;
    this.marketplaceService
      .getProducts({
        query: this.query,
        category: this.selectedCategory,
        type: this.selectedType
      })
      .subscribe({
        next: products => {
          this.allProducts = products;
          this.clampCurrentPage();
          this.loading = false;
          this.syncFiltersToUrl();
          this.announceSearchAlerts();
        },
        error: () => {
          this.loading = false;
        }
      });
  }

  private applyRouteParams(map: ParamMap): boolean {
    const prevQ = this.query;
    const prevCat = this.selectedCategory;
    const prevTyp = this.selectedType;

    const q = map.get('q') ?? '';
    const catRaw = map.get('category') ?? '';
    const typRaw = map.get('type') ?? '';

    this.query = q;
    this.selectedCategory = MARKETPLACE_CATEGORIES.some(c => c.value === catRaw)
      ? (catRaw as MarketplaceProductCategory)
      : '';
    this.selectedType = MARKETPLACE_TYPES.some(t => t.value === typRaw) ? (typRaw as MarketplaceProductType) : '';

    const rawPage = map.get('page');
    let p = rawPage ? parseInt(rawPage, 10) : 1;
    if (!Number.isFinite(p) || p < 1) {
      p = 1;
    }
    this.currentPage = p;

    const sortRaw = map.get('sort');
    const allowed: MarketplaceSort[] = ['newest', 'price_asc', 'price_desc', 'name'];
    this.sortBy = allowed.includes(sortRaw as MarketplaceSort) ? (sortRaw as MarketplaceSort) : 'newest';

    return prevQ !== this.query || prevCat !== this.selectedCategory || prevTyp !== this.selectedType;
  }

  private clampCurrentPage(): void {
    const tp = Math.max(1, Math.ceil(this.visibleSortedProducts.length / this.pageSize));
    if (this.currentPage > tp) {
      this.currentPage = tp;
    }
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
  }

  private syncFiltersToUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.query?.trim() ? this.query.trim() : undefined,
        category: this.selectedCategory || undefined,
        type: this.selectedType || undefined,
        page: this.currentPage > 1 ? this.currentPage : undefined,
        sort: this.sortBy !== 'newest' ? this.sortBy : undefined
      },
      replaceUrl: true
    });
  }

  clearFilters(): void {
    this.query = '';
    this.selectedCategory = '';
    this.selectedType = '';
    this.currentPage = 1;
    this.sortBy = 'newest';
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
      this.currentPage = 1;
      this.loadProducts();
    }, this.searchDebounceMs);
  }

  submitSearchNow(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
      this.searchDebounceId = null;
    }
    this.currentPage = 1;
    this.loadProducts();
  }

  onCategoryChange(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  onTypeChange(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  onSortChange(): void {
    this.currentPage = 1;
    this.syncFiltersToUrl();
  }

  private startCatalogTurn(): void {
    this.catalogTurning = true;
    window.setTimeout(() => {
      this.catalogTurning = false;
    }, 420);
  }

  setPage(n: number): void {
    const tp = this.totalPages;
    const next = Math.min(Math.max(1, n), tp);
    if (next === this.currentPage) {
      return;
    }
    this.startCatalogTurn();
    this.currentPage = next;
    this.syncFiltersToUrl();
  }

  prevPage(): void {
    this.setPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.setPage(this.currentPage + 1);
  }

  goToPage(n: number): void {
    this.setPage(n);
  }

  trackByProductId(_index: number, product: MarketplaceProduct): number {
    return product.id;
  }

  toggleQuiz(): void {
    this.showQuiz = !this.showQuiz;
    this.quizError = '';
  }

  submitQuiz(): void {
    this.quizSubmitting = true;
    this.quizError = '';
    this.quizReasoning = '';

    this.marketplaceService
      .getQuizRecommendations({
        anxietyLevel: this.anxietyLevel,
        stressLevel: this.stressLevel,
        sleepNeed: this.sleepNeed
      })
      .subscribe({
        next: response => {
          this.quizRecommendations = response.recommendations || [];
          this.quizReasoning = response.reasoning || '';
          this.quizSubmitting = false;
        },
        error: err => {
          this.quizError = err?.error?.message || 'Unable to generate recommendations right now.';
          this.quizSubmitting = false;
        }
      });
  }

  resetQuiz(): void {
    this.anxietyLevel = 3;
    this.stressLevel = 3;
    this.sleepNeed = 3;
    this.quizReasoning = '';
    this.quizError = '';
    this.quizRecommendations = [];
  }

  openDetails(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  addToCart(product: MarketplaceProduct, ev?: Event): void {
    ev?.stopPropagation();
    if (!this.marketplaceService.addToCart(product, 1)) {
      return;
    }
    this.shell.openMiniCart();
  }

  toggleCompare(product: MarketplaceProduct, ev: Event): void {
    ev.stopPropagation();
    const ok = this.ux.toggleCompare(product.id);
    this.compareHint = ok ? '' : 'You can compare up to three products.';
    if (this.compareHint) {
      window.setTimeout(() => (this.compareHint = ''), 3200);
    }
  }

  hideFromFeed(product: MarketplaceProduct, ev: Event): void {
    ev.stopPropagation();
    this.ux.hideProduct(product.id);
    this.clampCurrentPage();
  }

  restoreHidden(): void {
    this.ux.clearHiddenProducts();
    this.clampCurrentPage();
  }

  isComparing(id: number): boolean {
    return this.ux.isInCompare(id);
  }

  saveNamedSearch(): void {
    const name = this.savedSearchNameInput.trim() || 'My search';
    this.ux.saveCurrentSearch(name, {
      query: this.query.trim(),
      category: this.selectedCategory,
      type: this.selectedType,
      sort: this.sortBy
    });
    this.savedSearchNameInput = '';
  }

  savedSearches(): SavedSearchEntry[] {
    return this.ux.getSavedSearches();
  }

  applySavedSearch(entry: SavedSearchEntry): void {
    this.query = entry.query;
    this.selectedCategory = entry.category;
    this.selectedType = entry.type;
    this.sortBy = entry.sort;
    this.currentPage = 1;
    this.loadProducts();
  }

  removeSaved(entry: SavedSearchEntry, ev: Event): void {
    ev.stopPropagation();
    this.ux.removeSavedSearch(entry.id);
  }

  toggleAlert(entry: SavedSearchEntry, ev: Event): void {
    const input = ev.target as HTMLInputElement | null;
    if (!input) {
      return;
    }
    this.ux.setAlertForSearch(entry.id, input.checked);
  }

  alertOn(entry: SavedSearchEntry): boolean {
    return this.ux.isAlertEnabled(entry.id);
  }

  applyGoals(): void {
    this.ux.setGoals({ sleep: this.goalsSleep, stress: this.goalsStress, focus: this.goalsFocus });
  }

  openAccessExplainer(): void {
    this.shell.openAccessExplainer();
  }

  openQuickCart(ev: Event): void {
    ev.preventDefault();
    this.shell.openMiniCart();
  }

  private loadRecentSnapshots(): void {
    const ids = this.ux.getRecentlyViewedIds().slice(0, 10);
    if (ids.length === 0) {
      this.recentProducts = [];
      return;
    }
    forkJoin(ids.map(id => this.marketplaceService.getProductById(id))).subscribe({
      next: rows => {
        const order = new Map(ids.map((id, i) => [id, i]));
        this.recentProducts = [...rows].sort(
          (a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0)
        );
      },
      error: () => {
        this.recentProducts = [];
      }
    });
  }

  private announceSearchAlerts(): void {
    this.searchAlertMessage = '';
    for (const ss of this.ux.getSavedSearches()) {
      if (!this.ux.isAlertEnabled(ss.id)) {
        continue;
      }
      if (!this.filtersMatchSaved(ss)) {
        continue;
      }
      const msg = this.ux.consumeSearchAlertNotifications(ss.id, this.allProducts);
      if (msg) {
        this.searchAlertMessage = msg;
        break;
      }
    }
  }

  private filtersMatchSaved(ss: SavedSearchEntry): boolean {
    return (
      (ss.query ?? '').trim() === this.query.trim() &&
      (ss.category ?? '') === this.selectedCategory &&
      (ss.type ?? '') === this.selectedType &&
      (ss.sort ?? 'newest') === this.sortBy
    );
  }

  isManager(): boolean {
    return this.authService.hasRole('MARKETPLACE_MANAGER') || this.authService.isAdmin();
  }

  isDigital(product: MarketplaceProduct): boolean {
    return product.type === 'DIGITAL';
  }

  canUseCart(product: MarketplaceProduct): boolean {
    return this.marketplaceService.isCartEligible(product);
  }

  get hasResults(): boolean {
    return this.allProducts.length > 0;
  }

  get hasVisibleResults(): boolean {
    return this.visibleSortedProducts.length > 0;
  }
}
