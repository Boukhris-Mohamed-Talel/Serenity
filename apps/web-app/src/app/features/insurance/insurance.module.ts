import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { InsuranceRoutingModule } from './insurance-routing.module';
import { ClaimListComponent } from './claim-list/claim-list.component';
import { ClaimFormComponent } from './claim-form/claim-form.component';
import { ClaimDetailComponent } from './claim-detail/claim-detail.component';

@NgModule({
  declarations: [
    ClaimListComponent,
    ClaimFormComponent,
    ClaimDetailComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    InsuranceRoutingModule
  ]
})
export class InsuranceModule {}
