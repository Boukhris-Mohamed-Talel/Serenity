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
import { DoctorPrescriptionComponent } from './doctor-prescription/doctor-prescription.component';

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
    path: 'doctor',
    component: DoctorPrescriptionComponent,
    canActivate: [RoleGuard],
    data: { roles: ['DOCTOR'] }
  },
  {
    path: 'patient',
    component: PatientPharmacyComponent,
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
    DoctorPrescriptionComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class PharmacyModule {}
