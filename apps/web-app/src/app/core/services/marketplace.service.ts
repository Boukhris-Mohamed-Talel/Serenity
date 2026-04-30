import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
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

  addToCart(product: MarketplaceProduct, quantity = 1): void {
    if (!this.isCartEligible(product)) {
      return;
    }

    const current = [...this.cartSubject.value];
    const existing = current.find(item => item.product.id === product.id);

    if (existing) {
      existing.quantity += quantity;
    } else {
      current.push({ product, quantity });
    }

    this.cartSubject.next(current);
    this.persistCart();
  }

  isCartEligible(product: MarketplaceProduct): boolean {
    return product.type === 'PHYSICAL' || !product.previewable;
  }

  updateCartQuantity(productId: number, quantity: number): void {
    const current = this.cartSubject.value
      .map(item => item.product.id === productId ? { ...item, quantity } : item)
      .filter(item => item.quantity > 0);
    this.cartSubject.next(current);
    this.persistCart();
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

  getProductReviews(productId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/reviews/product/${productId}`);
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
