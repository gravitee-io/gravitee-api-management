/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { GioBreadcrumbModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';

import { SettingsRoutingModule } from './settings.route';

import { SettingsAnalyticsComponent } from '../configuration/analytics/settings-analytics.component';
import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { SettingsAnalyticsDashboardComponent } from '../configuration/analytics/dashboard/settings-analytics-dashboard.component';
import { ApiPortalHeaderComponent } from '../configuration/api-portal-header/api-portal-header.component';
import { ApiQualityRulesComponent } from '../configuration/api-quality-rules/api-quality-rules.component';
import { ApiQualityRuleComponent } from '../configuration/api-quality-rules/api-quality-rule/api-quality-rule.component';
import { IdentityProvidersComponent } from '../configuration/identityProviders/identity-providers.component';
import { CategoriesComponent } from '../configuration/categories/categories.component';
import { CategoryComponent } from '../configuration/categories/category/category.component';
import { GroupsComponent } from '../configuration/groups/groups.component';
import { GroupComponent } from '../configuration/groups/group/group.component';
import { DocumentationModule } from '../../components/documentation/documentation.module';

@NgModule({
  imports: [SettingsRoutingModule, RouterModule, GioSubmenuModule, GioBreadcrumbModule, CommonModule, DocumentationModule],
  declarations: [
    SettingsNavigationComponent,
    SettingsAnalyticsComponent,
    SettingsAnalyticsDashboardComponent,
    ApiPortalHeaderComponent,
    ApiQualityRulesComponent,
    ApiQualityRuleComponent,
    IdentityProvidersComponent,
    CategoriesComponent,
    CategoryComponent,
    GroupsComponent,
    GroupComponent,
  ],
})
export class SettingsModule {}
