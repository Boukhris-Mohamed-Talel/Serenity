import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { MiniGameComponent } from './login/mini-game/mini-game.component';
import { SelectRoleComponent } from './select-role/select-role.component';
import { SelectRoleGuard } from '../../core/guards/select-role.guard';
import { DoctorComponent } from './doctor/doctor.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'select-role', component: SelectRoleComponent, canActivate: [SelectRoleGuard] },
  { path : 'doctor', component: DoctorComponent},
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];

@NgModule({
  declarations: [LoginComponent, RegisterComponent, MiniGameComponent, SelectRoleComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule {}
