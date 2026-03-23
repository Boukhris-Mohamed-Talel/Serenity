import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { RoleGuard } from '../../core/guards/role.guard';
import { PharmacistDashboardComponent } from './pharmacist-dashboard/pharmacist-dashboard.component';
import { MyPharmacyComponent } from './my-pharmacy/my-pharmacy.component';
import { PrescriptionInboxComponent } from './prescription-inbox';
import { StockManagementComponent } from './stock-management/stock-management.component';
import { AddMedicineComponent } from './add-medicine/add-medicine.component';
import { PatientPharmacyComponent } from './patient-pharmacy/patient-pharmacy.component';
import { PatientPrescriptionsComponent } from './patient-prescriptions/patient-prescriptions.component';

const routes: Routes = [
  {
    path: '',
    component: PharmacistDashboardComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'my-pharmacy',
    component: MyPharmacyComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'inbox',
    component: PrescriptionInboxComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'stock',
    component: StockManagementComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'stock/new',
    component: AddMedicineComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PHARMACIST'] }
  },
  {
    path: 'patient',
    component: PatientPharmacyComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  },
  {
    path: 'patient/prescriptions',
    component: PatientPrescriptionsComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  }
];

@NgModule({
  declarations: [
    PharmacistDashboardComponent,
    MyPharmacyComponent,
    PrescriptionInboxComponent,
    StockManagementComponent,
    AddMedicineComponent,
    PatientPharmacyComponent,
    PatientPrescriptionsComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PharmacyModule {}
