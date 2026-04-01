import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleGuard } from '../../core/guards/role.guard';
import { AppointmentListComponent } from './appointment-list/appointment-list.component';
import { AppointmentBookComponent } from './appointment-book/appointment-book.component';
import { AppointmentScheduleComponent } from './appointment-schedule/appointment-schedule.component';
import { AppointmentDetailComponent } from './appointment-detail/appointment-detail.component';

const routes: Routes = [
  { path: '', component: AppointmentListComponent },
  {
    path: 'book',
    component: AppointmentBookComponent,
    canActivate: [RoleGuard],
    data: { roles: ['PATIENT'] }
  },
  {
    path: 'schedule',
    component: AppointmentScheduleComponent,
    canActivate: [RoleGuard],
    data: { roles: ['DOCTOR'] }
  },
  { path: ':id', component: AppointmentDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AppointmentsRoutingModule {}
