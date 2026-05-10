import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { MiniGameComponent } from './login/mini-game/mini-game.component';
import { SelectRoleComponent } from './select-role/select-role.component';
import { SelectRoleGuard } from '../../core/guards/select-role.guard';
import { AuthGuard } from '../../core/guards/auth.guard';
import { RoleGuard } from '../../core/guards/role.guard';
import { DoctorComponent } from './doctor/doctor.component';
import { DoctorVerificationComponent } from './doctor-verification/doctor-verification.component';
import { DoctorVerificationPendingComponent } from './doctor-verification-pending/doctor-verification-pending.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ForgotPasswordOtpComponent } from './forgot-password-otp/forgot-password-otp.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';
import { RoleLoadingComponent } from './role-loading/role-loading.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'loading', component: RoleLoadingComponent, canActivate: [AuthGuard] },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'forgot-password/otp', component: ForgotPasswordOtpComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'select-role', component: SelectRoleComponent, canActivate: [SelectRoleGuard] },
  { path : 'doctor', component: DoctorComponent},
  {
    path: 'doctor-verification/pending',
    component: DoctorVerificationPendingComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['DOCTOR'] }
  },
  { path : 'doctor-verification', component: DoctorVerificationComponent},
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    LoginComponent,
    RegisterComponent,
    MiniGameComponent,
    SelectRoleComponent,
    ForgotPasswordComponent,
    ForgotPasswordOtpComponent,
    ResetPasswordComponent,
    RoleLoadingComponent
  ],
  imports: [
    SharedModule,
    RouterModule.forChild(routes),
    DoctorVerificationComponent,
    DoctorVerificationPendingComponent
  ]
})
export class AuthModule {}
