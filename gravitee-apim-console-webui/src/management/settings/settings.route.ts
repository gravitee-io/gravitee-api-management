import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { SettingsAnalyticsComponent } from '../configuration/analytics/settings-analytics.component';
import { SettingsMigratingComponent } from './settings-migrating-component';

export const settingsRoutes: Routes = [
  {
    path: '',
    component: SettingsNavigationComponent,
    children: [
      {
        path: 'analytics',
        component: SettingsAnalyticsComponent,
        data: {
          menu: null,
          docs: {
            page: 'management-configuration-analytics',
          },
          perms: {
            only: ['environment-dashboard-r'],
            unauthorizedFallbackTo: 'management.settings.apiPortalHeader',
          },
        },
      },
      {
        path: 'analytics/dashboard/:type/new',
        component: SettingsMigratingComponent,
      },
      {
        path: 'analytics/dashboard/:type/:dashboardId',
        component: SettingsMigratingComponent,
      },
    ],
  },
];
@NgModule({
  imports: [RouterModule.forChild(settingsRoutes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
