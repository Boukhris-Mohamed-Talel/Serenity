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
  MarketplaceProductType
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

  getMyOrders(): Observable<MarketplaceOrder[]> {
    return this.http.get<MarketplaceOrder[]>(`${this.API_URL}/orders/me`);
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
}
