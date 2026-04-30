import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MarketplaceShellService {
  private readonly miniCartOpen = new BehaviorSubject(false);
  readonly miniCartOpen$ = this.miniCartOpen.asObservable();

  private readonly accessExplainerOpen = new BehaviorSubject(false);
  readonly accessExplainerOpen$ = this.accessExplainerOpen.asObservable();

  openMiniCart(): void {
    this.miniCartOpen.next(true);
  }

  closeMiniCart(): void {
    this.miniCartOpen.next(false);
  }

  toggleMiniCart(): void {
    this.miniCartOpen.next(!this.miniCartOpen.value);
  }

  openAccessExplainer(): void {
    this.accessExplainerOpen.next(true);
  }

  closeAccessExplainer(): void {
    this.accessExplainerOpen.next(false);
  }
}
