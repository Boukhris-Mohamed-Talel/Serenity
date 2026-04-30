import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceShellService } from '../../../core/services/marketplace-shell.service';
import { MarketplaceUxService } from '../../../core/services/marketplace-ux.service';
import { CartItem } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-layout',
  templateUrl: './marketplace-layout.component.html',
  styleUrls: ['./marketplace-layout.component.scss']
})
export class MarketplaceLayoutComponent implements OnInit, OnDestroy {
  cartItems: CartItem[] = [];
  compareCount = 0;
  miniCartOpen = false;
  accessModalOpen = false;
  disclaimerOpen = false;
  disclaimerChecked = false;

  private cartSub?: Subscription;
  private miniSub?: Subscription;
  private accessSub?: Subscription;
  private compareSub?: Subscription;
  private routerSub?: Subscription;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly shell: MarketplaceShellService,
    private readonly ux: MarketplaceUxService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.cartSub = this.marketplaceService.cart$.subscribe(items => {
      this.cartItems = items;
    });
    this.miniSub = this.shell.miniCartOpen$.subscribe(open => {
      this.miniCartOpen = open;
    });
    this.accessSub = this.shell.accessExplainerOpen$.subscribe(open => {
      this.accessModalOpen = open;
    });
    this.compareSub = this.ux.compareIds$.subscribe(ids => {
      this.compareCount = ids.length;
    });

    this.disclaimerOpen = !this.ux.hasDigitalDisclaimerAck();

    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        this.shell.closeMiniCart();
      });
  }

  ngOnDestroy(): void {
    this.cartSub?.unsubscribe();
    this.miniSub?.unsubscribe();
    this.accessSub?.unsubscribe();
    this.compareSub?.unsubscribe();
    this.routerSub?.unsubscribe();
  }

  toggleMiniCart(): void {
    this.shell.toggleMiniCart();
  }

  closeMiniCart(): void {
    this.shell.closeMiniCart();
  }

  openAccess(): void {
    this.shell.openAccessExplainer();
  }

  closeAccess(): void {
    this.shell.closeAccessExplainer();
  }

  acknowledgeDisclaimer(): void {
    if (!this.disclaimerChecked) {
      return;
    }
    this.ux.setDigitalDisclaimerAck();
    this.disclaimerOpen = false;
  }

  go(path: string): void {
    this.router.navigateByUrl(path);
    this.shell.closeMiniCart();
  }

  removeLine(productId: number): void {
    this.marketplaceService.removeFromCart(productId);
  }

  cartLineTotal(item: CartItem): number {
    return item.product.price * item.quantity;
  }

  cartGrandTotal(): number {
    return this.marketplaceService.getCartTotal();
  }
}
