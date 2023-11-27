import { NgModule } from '@angular/core';
import { SettingsLayoutComponent } from './settings-layout.component';
import { SettingsRoutingModule } from './settings.route';
import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { RouterModule } from '@angular/router';
import { GioBreadcrumbModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';

@NgModule({
  imports: [SettingsRoutingModule, RouterModule, GioSubmenuModule, GioBreadcrumbModule, CommonModule],
  declarations: [SettingsLayoutComponent, SettingsNavigationComponent],
})
export class SettingsModule {}
