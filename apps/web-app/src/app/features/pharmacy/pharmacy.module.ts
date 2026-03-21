import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { PharmacistDashboardComponent } from './pharmacist-dashboard/pharmacist-dashboard.component';
import { MyPharmacyComponent } from './my-pharmacy/my-pharmacy.component';
import { PrescriptionInboxComponent } from './prescription-inbox';

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
  }
];

@NgModule({
  declarations: [
    PharmacistDashboardComponent,
    MyPharmacyComponent,
    PrescriptionInboxComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PharmacyModule {}
