import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { PharmacistDashboardComponent } from './pharmacist-dashboard/pharmacist-dashboard.component';
import { MyPharmacyComponent } from './my-pharmacy/my-pharmacy.component';
import { PrescriptionInboxComponent } from './prescription-inbox';
import { StockManagementComponent } from './stock-management/stock-management.component';
import { AddMedicineComponent } from './add-medicine/add-medicine.component';

const routes: Routes = [
  {
    path: '',
    component: PharmacistDashboardComponent
  },
  {
    path: 'my-pharmacy',
    component: MyPharmacyComponent
  },
  {
    path: 'inbox',
    component: PrescriptionInboxComponent
  },
  {
    path: 'stock',
    component: StockManagementComponent
  },
  {
    path: 'stock/new',
    component: AddMedicineComponent
  }
];

@NgModule({
  declarations: [
    PharmacistDashboardComponent,
    MyPharmacyComponent,
    PrescriptionInboxComponent,
    StockManagementComponent,
    AddMedicineComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PharmacyModule {}
