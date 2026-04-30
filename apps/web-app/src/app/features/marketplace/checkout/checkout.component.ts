import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MarketplaceOrder } from '../../../shared/models/marketplace.model';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceUxService, AddressBookEntry } from '../../../core/services/marketplace-ux.service';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  loading = false;
  error = '';
  successOrder: MarketplaceOrder | null = null;
  stockWarnings: string[] = [];
  newAddressLabel = '';

  readonly checkoutForm = this.fb.group({
    shippingAddress: ['', [Validators.required, Validators.pattern('.*\\S.*'), Validators.maxLength(500)]],
    customerNote: ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router,
    private readonly ux: MarketplaceUxService
  ) {}

  ngOnInit(): void {
    this.marketplaceService.refreshCartProductMeta().subscribe({
      error: () => {
        /* stale snapshot is still better than blocking checkout */
      }
    });
  }

  get cartTotal(): number {
    return this.marketplaceService.getCartTotal();
  }

  get isCartEmpty(): boolean {
    return this.marketplaceService.getCartSnapshot().length === 0;
  }

  get addressBook(): AddressBookEntry[] {
    return this.ux.getAddressBook();
  }

  onPayClicked(): void {
    this.submit();
  }

  applySavedAddress(ev: Event): void {
    const sel = ev.target as HTMLSelectElement;
    const v = sel?.value ?? '';
    if (v) {
      this.checkoutForm.patchValue({ shippingAddress: v });
    }
    sel.selectedIndex = 0;
  }

  saveShippingToBook(): void {
    const text = String(this.checkoutForm.value.shippingAddress || '').trim();
    if (!text) {
      return;
    }
    const label = this.newAddressLabel.trim() || 'Saved address';
    this.ux.addAddressEntry(label, text);
    this.newAddressLabel = '';
  }

  submit(): void {
    this.error = '';
    this.stockWarnings = [];

    if (this.isCartEmpty) {
      this.error = 'Your cart is empty. Add an item before submitting a request.';
      return;
    }

    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.error = 'Please provide valid delivery or contact details before submitting.';
      return;
    }

    const shippingAddress = String(this.checkoutForm.value.shippingAddress || '').trim();
    const customerNote = this.checkoutForm.value.customerNote || undefined;

    if (this.marketplaceService.getCartSnapshot().length === 0) {
      this.error = 'Your cart is empty.';
      return;
    }

    this.loading = true;
    this.marketplaceService.refreshCartProductMeta().subscribe({
      next: () => {
        this.stockWarnings = this.buildStockWarnings();
        if (this.stockWarnings.length > 0) {
          this.error =
            'Some items in your cart no longer match available stock. Review the notes below, adjust quantities in your cart, then try again.';
          this.loading = false;
          return;
        }
        this.proceedWithCheckout(shippingAddress, customerNote);
      },
      error: () => {
        this.proceedWithCheckout(shippingAddress, customerNote);
      }
    });
  }

  private buildStockWarnings(): string[] {
    const lines: string[] = [];
    for (const line of this.marketplaceService.getCartSnapshot()) {
      const p = line.product;
      if (p.type !== 'PHYSICAL') {
        continue;
      }
      const stock = p.stockQuantity ?? 0;
      if (stock <= 0) {
        lines.push(`“${p.name}” is out of stock. Remove it from your cart or wait for a restock.`);
      } else if (line.quantity > stock) {
        lines.push(
          `“${p.name}” — only ${stock} unit(s) available, but your cart requests ${line.quantity}. Open your cart and lower the quantity.`
        );
      }
    }
    return lines;
  }

  private proceedWithCheckout(shippingAddress: string, customerNote?: string): void {
    this.marketplaceService.checkout(shippingAddress, customerNote).subscribe({
      next: order => {
        this.successOrder = order;
        this.loading = false;
      },
      error: err => {
        this.error =
          err?.error?.message ||
          err?.error?.error ||
          'Checkout failed. Please verify your shipping address and try again.';
        this.loading = false;
      }
    });
  }

  get shippingAddressControl() {
    return this.checkoutForm.controls.shippingAddress;
  }

  get customerNoteControl() {
    return this.checkoutForm.controls.customerNote;
  }

  goToOrders(): void {
    this.router.navigate(['/marketplace/orders']);
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }

  goToWishlist(): void {
    this.router.navigate(['/marketplace/wishlist']);
  }
}
