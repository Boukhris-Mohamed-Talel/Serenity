import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MarketplaceOrder } from '../../../shared/models/marketplace.model';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { DebugSessionService } from '../../../core/debug/debug-session.service';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent {
  loading = false;
  error = '';
  successOrder: MarketplaceOrder | null = null;

  readonly checkoutForm = this.fb.group({
    shippingAddress: ['', [Validators.required, Validators.pattern('.*\\S.*'), Validators.maxLength(500)]],
    customerNote: ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router,
    private readonly debugSessionService: DebugSessionService
  ) {}

  get cartTotal(): number {
    return this.marketplaceService.getCartTotal();
  }

  get isCartEmpty(): boolean {
    return this.marketplaceService.getCartSnapshot().length === 0;
  }

  onPayClicked(): void {
    this.debugSessionService.log('UI_ACTION', 'Checkout pay button clicked', {
      isCartEmpty: this.isCartEmpty,
      loading: this.loading,
      formValid: this.checkoutForm.valid,
      shippingLength: String(this.shippingAddressControl.value || '').length,
      customerNoteLength: String(this.customerNoteControl.value || '').length
    });
    this.submit();
  }

  submit(): void {
    this.error = '';

    if (this.isCartEmpty) {
      this.error = 'Your cart is empty. Add an item before continuing to payment.';
      this.debugSessionService.log('STATE', 'Checkout blocked: empty cart', {}, 'warn');
      return;
    }

    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.error = 'Please provide a valid shipping address before payment.';
      this.debugSessionService.log('STATE', 'Checkout blocked: invalid form', {
        shippingRequired: this.shippingAddressControl.hasError('required'),
        shippingPattern: this.shippingAddressControl.hasError('pattern'),
        shippingMaxLength: this.shippingAddressControl.hasError('maxlength'),
        customerNoteMaxLength: this.customerNoteControl.hasError('maxlength')
      }, 'warn');
      return;
    }

    const shippingAddress = String(this.checkoutForm.value.shippingAddress || '').trim();
    const customerNote = this.checkoutForm.value.customerNote || undefined;

    if (this.marketplaceService.getCartSnapshot().length === 0) {
      this.error = 'Your cart is empty.';
      this.debugSessionService.log('STATE', 'Checkout blocked: empty snapshot', {}, 'warn');
      return;
    }

    this.loading = true;
    this.debugSessionService.log('STATE', 'Checkout submit accepted', {
      cartSize: this.marketplaceService.getCartSnapshot().length
    });

    this.proceedWithCheckout(shippingAddress, customerNote);
  }

  private proceedWithCheckout(shippingAddress: string, customerNote?: string): void {
    this.marketplaceService.checkout(shippingAddress, customerNote).subscribe({
      next: order => {
        this.successOrder = order;
        this.loading = false;
        this.debugSessionService.log('STATE', 'Checkout success response received', {
          orderId: order.id,
          totalAmount: order.totalAmount
        });
      },
      error: err => {
        this.error =
          err?.error?.message ||
          err?.error?.error ||
          'Checkout failed. Please verify your shipping address and try again.';
        this.loading = false;
        this.debugSessionService.log('ERROR', 'Checkout request failed', {
          status: err?.status,
          message: this.error
        }, 'error');
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
