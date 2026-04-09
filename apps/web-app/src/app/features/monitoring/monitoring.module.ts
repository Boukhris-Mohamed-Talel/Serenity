import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MoodListComponent } from './mood/mood-list/mood-list.component';
import { MoodFormComponent } from './mood/mood-form/mood-form.component';
import { OutcomeDashboardComponent } from './outcomes/outcome-dashboard.component';

const routes: Routes = [
  { path: '', component: MoodListComponent },
  { path: 'outcomes', component: OutcomeDashboardComponent },
  { path: 'new', component: MoodFormComponent },
  { path: 'edit/:id', component: MoodFormComponent }
];

@NgModule({
  declarations: [
    MoodListComponent,
    MoodFormComponent,
    OutcomeDashboardComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class MonitoringModule {}
