import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { MoodListComponent } from './mood/mood-list/mood-list.component';
import { MoodFormComponent } from './mood/mood-form/mood-form.component';

const routes: Routes = [
  { path: '', component: MoodListComponent },
  { path: 'new', component: MoodFormComponent },
  { path: 'edit/:id', component: MoodFormComponent }
];

@NgModule({
  declarations: [
    MoodListComponent,
    MoodFormComponent
  ],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class MonitoringModule {}
