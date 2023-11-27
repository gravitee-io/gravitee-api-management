import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SettingsLayoutComponent } from './settings-layout.component';

export const settingsRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: SettingsLayoutComponent,
  },
];
@NgModule({
  imports: [RouterModule.forChild(settingsRoutes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
