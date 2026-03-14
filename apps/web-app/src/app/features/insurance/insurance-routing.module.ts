import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClaimListComponent } from './claim-list/claim-list.component';
import { ClaimFormComponent } from './claim-form/claim-form.component';
import { ClaimDetailComponent } from './claim-detail/claim-detail.component';

const routes: Routes = [
  { path: '', component: ClaimListComponent },
  { path: 'new', component: ClaimFormComponent },
  { path: ':id', component: ClaimDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class InsuranceRoutingModule {}
