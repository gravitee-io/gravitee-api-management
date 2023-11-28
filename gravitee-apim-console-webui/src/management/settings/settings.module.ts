import { NgModule } from '@angular/core';
import { SettingsRoutingModule } from './settings.route';
import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { RouterModule } from '@angular/router';
import { GioBreadcrumbModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { SettingsAnalyticsComponent } from '../configuration/analytics/settings-analytics.component';
import { SettingsMigratingComponent } from './settings-migrating-component';

@NgModule({
  imports: [SettingsRoutingModule, RouterModule, GioSubmenuModule, GioBreadcrumbModule, CommonModule],
  declarations: [SettingsNavigationComponent, SettingsAnalyticsComponent, SettingsMigratingComponent],
})
export class SettingsModule {}
