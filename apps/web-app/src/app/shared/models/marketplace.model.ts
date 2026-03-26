export type MarketplaceProductType = 'PHYSICAL' | 'DIGITAL';

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
  status: string;
  totalAmount: number;
  currency: string;
  shippingAddress: string;
  customerNote?: string;
  paymentAttempt: PaymentAttempt;
  items: OrderItem[];
  createdAt: string;
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
