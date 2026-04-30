import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MarketplaceLayoutComponent } from './marketplace-layout/marketplace-layout.component';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { CartComponent } from './cart/cart.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderHistoryComponent } from './order-history/order-history.component';
import { WishlistComponent } from './wishlist/wishlist.component';
import { ProductCompareComponent } from './product-compare/product-compare.component';

const routes: Routes = [
  {
    path: '',
    component: MarketplaceLayoutComponent,
    children: [
      { path: '', component: ProductListComponent },
      { path: 'product/:id', component: ProductDetailComponent },
      { path: 'cart', component: CartComponent },
      { path: 'checkout', component: CheckoutComponent },
      { path: 'orders', component: OrderHistoryComponent },
      { path: 'wishlist', component: WishlistComponent },
      { path: 'compare', component: ProductCompareComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MarketplaceRoutingModule {}
