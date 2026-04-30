import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, forkJoin, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CartItem,
  CheckoutRequest,
  MarketplaceOrder,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  MarketplaceProductUpsertRequest,
  MarketplaceOrderStatus,
  OrderStatusUpdateRequest,
  ProductReview,
  QuizRecommendationRequest,
  RecommendationResponse
} from '../../shared/models/marketplace.model';

@Injectable({
  providedIn: 'root'
})
export class MarketplaceService {

  private static readonly CART_STORAGE_KEY = 'marketplace_cart_v1';
  private readonly API_URL = `${environment.marketplaceServiceApiUrl}/api/articles`;
  private readonly cartSubject = new BehaviorSubject<CartItem[]>([]);
  readonly cart$ = this.cartSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    this.restoreCartFromStorage();
  }

  getProducts(filters?: {
    query?: string;
    category?: MarketplaceProductCategory | '';
    type?: MarketplaceProductType | '';
  }): Observable<MarketplaceProduct[]> {
    let params = new HttpParams();

    if (filters?.query) {
      params = params.set('query', filters.query);
    }
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.type) {
      params = params.set('type', filters.type);
    }

    return this.http.get<MarketplaceProduct[]>(`${this.API_URL}/products`, { params });
  }

  getProductById(id: number): Observable<MarketplaceProduct> {
    return this.http.get<MarketplaceProduct>(`${this.API_URL}/products/${id}`);
  }

  getAllProductsForAdmin(): Observable<MarketplaceProduct[]> {
    return this.http.get<MarketplaceProduct[]>(`${this.API_URL}/products/admin/all`);
  }

  createProduct(request: MarketplaceProductUpsertRequest): Observable<MarketplaceProduct> {
    return this.http.post<MarketplaceProduct>(`${this.API_URL}/products`, request);
  }

  updateProduct(id: number, request: MarketplaceProductUpsertRequest): Observable<MarketplaceProduct> {
    return this.http.put<MarketplaceProduct>(`${this.API_URL}/products/${id}`, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/products/${id}`);
  }

  getMyOrders(): Observable<MarketplaceOrder[]> {
    return this.http.get<MarketplaceOrder[]>(`${this.API_URL}/orders/me`);
  }

  getAllOrdersForAdmin(): Observable<MarketplaceOrder[]> {
    return this.http.get<MarketplaceOrder[]>(`${this.API_URL}/orders`);
  }

  getOrderByIdForAdmin(orderId: number): Observable<MarketplaceOrder> {
    return this.http.get<MarketplaceOrder>(`${this.API_URL}/orders/${orderId}`);
  }

  /** Submit a request for full digital access (no online payment). Staff confirms in admin. */
  requestDigitalAccess(product: MarketplaceProduct, customerNote?: string): Observable<MarketplaceOrder> {
    const request: CheckoutRequest = {
      items: [{ productId: product.id, quantity: 1 }],
      shippingAddress: 'Digital access request',
      customerNote: customerNote?.trim() || `Full access request: ${product.name}`
    };

    return this.http.post<MarketplaceOrder>(`${this.API_URL}/orders/checkout`, request);
  }

  /** True if the signed-in user has a confirmed (PAID) order that includes this product. */
  hasPaidAccessForProduct(productId: number): Observable<boolean> {
    return this.getMyOrders().pipe(
      map(orders =>
        orders.some(
          o =>
            o.status === 'PAID' &&
            (o.items ?? []).some(i => i.productId === productId)
        )
      )
    );
  }

  updateOrderStatus(orderId: number, status: MarketplaceOrderStatus): Observable<MarketplaceOrder> {
    const request: OrderStatusUpdateRequest = { status };
    return this.http.patch<MarketplaceOrder>(`${this.API_URL}/orders/${orderId}/status`, request);
  }

  cancelOrderForAdmin(orderId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/orders/${orderId}`);
  }

  checkout(shippingAddress: string, customerNote?: string): Observable<MarketplaceOrder> {
    const request: CheckoutRequest = {
      items: this.cartSubject.value.map(item => ({
        productId: item.product.id,
        quantity: item.quantity
      })),
      shippingAddress,
      customerNote
    };

    return this.http.post<MarketplaceOrder>(`${this.API_URL}/orders/checkout`, request).pipe(
      tap(() => {
        this.clearCart();
      })
    );
  }

  /**
   * Max units of this product the user may still add (physical: remaining stock; digital: large cap).
   * @param alreadyInCart excludes the line being edited when used from cart UI.
   */
  maxOrderableQuantity(product: MarketplaceProduct, alreadyInCartForThisProduct: number): number {
    if (product.type !== 'PHYSICAL') {
      return 1_000_000;
    }
    const stock = product.stockQuantity ?? 0;
    return Math.max(0, stock - alreadyInCartForThisProduct);
  }

  /** Returns false if nothing was added (e.g. out of stock). */
  addToCart(product: MarketplaceProduct, quantity = 1): boolean {
    if (!this.isCartEligible(product)) {
      return false;
    }
    if (!Number.isFinite(quantity) || quantity < 1) {
      return false;
    }

    const current = [...this.cartSubject.value];
    const existing = current.find(item => item.product.id === product.id);
    const inCart = existing?.quantity ?? 0;
    let newTotal = inCart + quantity;

    if (product.type === 'PHYSICAL') {
      const stock = product.stockQuantity ?? 0;
      if (stock <= 0) {
        return false;
      }
      newTotal = Math.min(newTotal, stock);
      if (newTotal <= inCart) {
        return false;
      }
    }

    if (existing) {
      existing.quantity = newTotal;
    } else {
      current.push({ product, quantity: newTotal });
    }

    this.cartSubject.next(current);
    this.persistCart();
    return true;
  }

  isCartEligible(product: MarketplaceProduct): boolean {
    return product.type === 'PHYSICAL' || !product.previewable;
  }

  updateCartQuantity(productId: number, quantity: number): void {
    let q = Math.floor(Number(quantity));
    if (!Number.isFinite(q) || q < 1) {
      q = 1;
    }
    const current = this.cartSubject.value.map(item => {
      if (item.product.id !== productId) {
        return item;
      }
      if (item.product.type === 'PHYSICAL') {
        const stock = item.product.stockQuantity ?? 0;
        if (stock <= 0) {
          return { ...item, quantity: 0 };
        }
        q = Math.min(q, stock);
      }
      return { ...item, quantity: q };
    });
    const next = current.filter(item => item.quantity > 0);
    this.cartSubject.next(next);
    this.persistCart();
  }

  /** Re-fetch catalog rows for cart lines so stockQuantity stays accurate after reload. */
  refreshCartProductMeta(): Observable<void> {
    const cart = this.cartSubject.value;
    if (cart.length === 0) {
      return of(undefined);
    }
    const ids = [...new Set(cart.map(i => i.product.id))];
    return forkJoin(ids.map(id => this.getProductById(id))).pipe(
      tap(products => {
        const byId = new Map(products.map(p => [p.id, p]));
        const next = cart.map(item => {
          const fresh = byId.get(item.product.id);
          if (!fresh) {
            return item;
          }
          let qty = item.quantity;
          if (fresh.type === 'PHYSICAL') {
            const stock = fresh.stockQuantity ?? 0;
            qty = Math.min(qty, Math.max(0, stock));
            if (qty < 1) {
              qty = 0;
            }
          }
          return { product: fresh, quantity: qty };
        }).filter(i => i.quantity > 0);
        this.cartSubject.next(next);
        this.persistCart();
      }),
      map(() => undefined)
    );
  }

  removeFromCart(productId: number): void {
    this.cartSubject.next(this.cartSubject.value.filter(item => item.product.id !== productId));
    this.persistCart();
  }

  clearCart(): void {
    this.cartSubject.next([]);
    this.persistCart();
  }

  getCartSnapshot(): CartItem[] {
    return this.cartSubject.value;
  }

  getCartTotal(): number {
    return this.cartSubject.value.reduce(
      (sum, item) => sum + (item.product.price * item.quantity),
      0
    );
  }

  // ===== WISHLIST OPERATIONS =====
  addToWishlist(productId: number): Observable<any> {
    return this.http.post(`${this.API_URL}/wishlist/${productId}`, {});
  }

  removeFromWishlist(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/wishlist/${productId}`);
  }

  getUserWishlist(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/wishlist/me`);
  }

  isProductInWishlist(productId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/wishlist/check/${productId}`);
  }

  // ===== REVIEWS OPERATIONS =====
  createOrUpdateReview(productId: number, rating: number, reviewText: string): Observable<any> {
    const request = { productId, rating, reviewText };
    return this.http.post(`${this.API_URL}/reviews`, request);
  }

  getProductReviews(productId: number): Observable<ProductReview[]> {
    return this.http.get<ProductReview[]>(`${this.API_URL}/reviews/product/${productId}`);
  }

  markReviewHelpful(reviewId: number): Observable<ProductReview> {
    return this.http.post<ProductReview>(`${this.API_URL}/reviews/${reviewId}/helpful`, {});
  }

  getAverageRating(productId: number): Observable<number> {
    return this.http.get<number>(`${this.API_URL}/reviews/product/${productId}/average`);
  }

  getQuizRecommendations(request: QuizRecommendationRequest): Observable<RecommendationResponse> {
    return this.http.post<RecommendationResponse>(`${this.API_URL}/recommendations/quiz`, request);
  }

  getUserReviews(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/reviews/me`);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/reviews/${reviewId}`);
  }

  private restoreCartFromStorage(): void {
    try {
      const raw = localStorage.getItem(MarketplaceService.CART_STORAGE_KEY);
      if (!raw) {
        return;
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return;
      }
      const items: CartItem[] = [];
      for (const row of parsed) {
        if (
          row &&
          typeof row === 'object' &&
          'quantity' in row &&
          'product' in row &&
          row.product &&
          typeof (row as CartItem).product.id === 'number' &&
          typeof (row as CartItem).quantity === 'number'
        ) {
          items.push({ product: (row as CartItem).product, quantity: (row as CartItem).quantity });
        }
      }
      if (items.length > 0) {
        this.cartSubject.next(items);
      }
    } catch {
      /* ignore corrupt cart */
    }
  }

  private persistCart(): void {
    try {
      localStorage.setItem(MarketplaceService.CART_STORAGE_KEY, JSON.stringify(this.cartSubject.value));
    } catch {
      /* ignore quota errors */
    }
  }

}
