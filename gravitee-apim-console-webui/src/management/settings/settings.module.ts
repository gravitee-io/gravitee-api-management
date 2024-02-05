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
import { SettingsAnalyticsComponent } from './analytics/settings-analytics.component';
import { SettingsNavigationComponent } from './settings-navigation/settings-navigation.component';
import { SettingsAnalyticsDashboardComponent } from './analytics/dashboard/settings-analytics-dashboard.component';
import { ApiPortalHeaderComponent } from './api-portal-header/api-portal-header.component';
import { ApiQualityRulesComponent } from './api-quality-rules/api-quality-rules.component';
import { ApiQualityRuleComponent } from './api-quality-rules/api-quality-rule/api-quality-rule.component';
import { IdentityProvidersComponent } from './identityProviders/identity-providers.component';
import { CategoriesComponent } from './categories/categories.component';
import { CategoryComponent } from './categories/category/category.component';
import { GroupsComponent } from './groups/groups.component';
import { GroupComponent } from './groups/group/group.component';
import { EnvironmentMetadataModule } from './metadata/environment-metadata.module';
import { PortalComponent } from './portal/portal.component';
import { ClientRegistrationProvidersModule } from './client-registration-providers/client-registration-providers.module';
import { PortalThemeComponent } from './portal-theme/portalTheme.component';
import { TopApisComponent } from './top-apis/top-apis.component';
import { ApiLoggingModule } from './api-logging/api-logging.module';
import { DictionariesComponent } from './dictionaries/dictionaries.component';
import { DictionaryComponent } from './dictionaries/dictionary.component';
import { CustomUserFieldsComponent } from './custom-user-fields/custom-user-fields.component';
import { EnvironmentNotificationSettingsModule } from './notifications/notification-settings/environment-notification-settings.module';
import { ApiQualityRulesNgModule } from './api-quality-rules-ng/api-quality-rules-ng.module';

import { DocumentationModule } from '../../components/documentation/documentation.module';

@NgModule({
  imports: [
    ApiQualityRulesNgModule,
    SettingsRoutingModule,
    RouterModule,
    GioSubmenuModule,
    GioBreadcrumbModule,
    CommonModule,
    DocumentationModule,
    EnvironmentMetadataModule,
    ClientRegistrationProvidersModule,
    ApiLoggingModule,
    EnvironmentNotificationSettingsModule,
  ],
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
    PortalComponent,
    PortalThemeComponent,
    TopApisComponent,
    DictionariesComponent,
    DictionaryComponent,
    CustomUserFieldsComponent,
  ],
})
export class SettingsModule {}
