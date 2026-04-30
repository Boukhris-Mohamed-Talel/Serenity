export type MarketplaceProductType = 'PHYSICAL' | 'DIGITAL';
export type PreviewContentType = 'VIDEO' | 'BOOK' | 'AUDIO';
export type MarketplaceOrderStatus = 'CREATED' | 'PAID' | 'CANCELLED';

export type MarketplaceSort = 'newest' | 'price_asc' | 'price_desc' | 'name';

export interface ProductReview {
  id: number;
  productId: number;
  userId: number;
  userEmail: string;
  rating: number;
  reviewText: string;
  createdAt: string;
  updatedAt?: string;
  helpfulCount: number;
  verifiedPurchase: boolean;
  viewerMarkedHelpful: boolean;
}

export type MarketplaceProductCategory =
  | 'SELF_CARE'
  | 'SLEEP_SUPPORT'
  | 'STRESS_RELIEF'
  | 'MINDFULNESS'
  | 'THERAPY_TOOLS'
  | 'EDUCATION';

export interface MarketplaceProduct {
  id: number;
  name: string;
  description: string;
  category: MarketplaceProductCategory;
  type: MarketplaceProductType;
  price: number;
  active: boolean;
  imageUrl?: string;
  previewable: boolean;
  previewType?: PreviewContentType;
  previewUrl?: string;
  contentUrl?: string;
  /** Physical stock. Omitted or null for digital (unlimited). */
  stockQuantity?: number | null;
}

export interface MarketplaceProductUpsertRequest {
  name: string;
  description: string;
  category: MarketplaceProductCategory;
  type: MarketplaceProductType;
  price: number;
  imageUrl?: string;
  previewable: boolean;
  previewType?: PreviewContentType;
  previewUrl?: string;
  contentUrl?: string;
  active: boolean;
  /** Required for physical (>= 0). Omit for digital. */
  stockQuantity?: number | null;
}

export interface CartItem {
  product: MarketplaceProduct;
  quantity: number;
}

export interface CheckoutItem {
  productId: number;
  quantity: number;
}

export interface CheckoutRequest {
  items: CheckoutItem[];
  shippingAddress: string;
  customerNote?: string;
}

export interface PaymentAttempt {
  reference: string;
  status: string;
  message: string;
}

export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface MarketplaceOrder {
  id: number;
  customerEmail: string;
  status: MarketplaceOrderStatus;
  totalAmount: number;
  currency: string;
  shippingAddress: string;
  customerNote?: string;
  paymentAttempt?: PaymentAttempt;
  items: OrderItem[];
  createdAt: string;
}

export interface OrderStatusUpdateRequest {
  status: MarketplaceOrderStatus;
}

export interface QuizRecommendationRequest {
  anxietyLevel: number;
  stressLevel: number;
  sleepNeed: number;
}

export interface ProductRecommendationItem {
  productId: number;
  productName: string;
  category: string;
  reason: string;
  confidence: number;
}

export interface RecommendationResponse {
  recommendations: ProductRecommendationItem[];
  reasoning: string;
  totalRecommendations: number;
  generatedAt: string;
}

export const MARKETPLACE_CATEGORIES: { value: MarketplaceProductCategory; label: string }[] = [
  { value: 'SELF_CARE', label: 'Self Care' },
  { value: 'SLEEP_SUPPORT', label: 'Sleep Support' },
  { value: 'STRESS_RELIEF', label: 'Stress Relief' },
  { value: 'MINDFULNESS', label: 'Mindfulness' },
  { value: 'THERAPY_TOOLS', label: 'Therapy Tools' },
  { value: 'EDUCATION', label: 'Education' }
];

export const MARKETPLACE_TYPES: { value: MarketplaceProductType; label: string }[] = [
  { value: 'PHYSICAL', label: 'Physical' },
  { value: 'DIGITAL', label: 'Digital' }
];
