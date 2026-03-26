import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
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
  OrderStatusUpdateRequest
} from '../../shared/models/marketplace.model';

@Injectable({
  providedIn: 'root'
})
export class MarketplaceService {

  private readonly API_URL = `${environment.marketplaceServiceApiUrl}/marketplace`;
  private readonly cartSubject = new BehaviorSubject<CartItem[]>([]);
  readonly cart$ = this.cartSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

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
      tap(() => this.clearCart())
    );
  }

  addToCart(product: MarketplaceProduct, quantity = 1): void {
    const current = [...this.cartSubject.value];
    const existing = current.find(item => item.product.id === product.id);

    if (existing) {
      existing.quantity += quantity;
    } else {
      current.push({ product, quantity });
    }

    this.cartSubject.next(current);
  }

  updateCartQuantity(productId: number, quantity: number): void {
    const current = this.cartSubject.value
      .map(item => item.product.id === productId ? { ...item, quantity } : item)
      .filter(item => item.quantity > 0);
    this.cartSubject.next(current);
  }

  removeFromCart(productId: number): void {
    this.cartSubject.next(this.cartSubject.value.filter(item => item.product.id !== productId));
  }

  clearCart(): void {
    this.cartSubject.next([]);
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

  getUserReviews(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/reviews/me`);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/reviews/${reviewId}`);
  }

  // ===== COUPON/DISCOUNT OPERATIONS =====
  validateCoupon(code: string, orderAmount: number): Observable<any> {
    const params = new HttpParams()
      .set('code', code)
      .set('orderAmount', orderAmount.toString());
    return this.http.get(`${this.API_URL}/coupons/validate`, { params });
  }

  applyCoupon(code: string): Observable<void> {
    const params = new HttpParams().set('code', code);
    return this.http.post<void>(`${this.API_URL}/coupons/apply`, {}, { params });
  }

  getCoupon(code: string): Observable<any> {
    return this.http.get(`${this.API_URL}/coupons/${code}`);
  }
}
