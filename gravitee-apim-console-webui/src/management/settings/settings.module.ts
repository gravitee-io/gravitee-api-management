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

import { SettingsRoutingModule } from './settings-routing.module';
import { SettingsAnalyticsComponent } from './analytics/settings-analytics.component';
import { SettingsNavigationComponent } from './settings-navigation/settings-navigation.component';
import { SettingsAnalyticsDashboardComponent } from './analytics/dashboard/settings-analytics-dashboard.component';
import { GroupsComponent } from './groups/groups.component';
import { GroupComponent } from './groups/group/group.component';
import { EnvironmentMetadataModule } from './metadata/environment-metadata.module';
import { ClientRegistrationProvidersModule } from './client-registration-providers/client-registration-providers.module';
import { PortalThemeComponent } from './portal-theme/portalTheme.component';
import { ApiLoggingModule } from './api-logging/api-logging.module';
import { DictionariesComponent } from './dictionaries/dictionaries.component';
import { DictionaryComponent } from './dictionaries/dictionary.component';
import { CustomUserFieldsComponent } from './custom-user-fields/custom-user-fields.component';
import { EnvironmentNotificationModule } from './notification/environment-notification.module';
import { ApiQualityRulesModule } from './api-quality-rules/api-quality-rules.module';
import { ApiPortalHeaderModule } from './api-portal-header/api-portal-header.module';
import { IdentityProvidersModule } from './identity-providers/identity-providers.module';
import { PortalSettingsModule } from './portal-settings/portal-settings.module';
import { TopApisModule } from './top-apis/top-apis.module';
import { CategoriesModule } from './categories/categories.module';
import { CustomUserFieldsModule } from './custom-user-fields/migrated/custom-user-fields-migrated.module';

import { DocumentationModule } from '../../components/documentation/documentation.module';

@NgModule({
  imports: [
    TopApisModule,
    ApiQualityRulesModule,
    SettingsRoutingModule,
    RouterModule,
    GioSubmenuModule,
    GioBreadcrumbModule,
    CommonModule,
    DocumentationModule,
    EnvironmentMetadataModule,
    ClientRegistrationProvidersModule,
    ApiLoggingModule,
    IdentityProvidersModule,
    EnvironmentNotificationModule,
    PortalSettingsModule,
    ApiPortalHeaderModule,
    CustomUserFieldsModule,
    CategoriesModule,
  ],
  declarations: [
    SettingsNavigationComponent,
    SettingsAnalyticsComponent,
    SettingsAnalyticsDashboardComponent,
    GroupsComponent,
    GroupComponent,
    PortalThemeComponent,
    DictionariesComponent,
    DictionaryComponent,
    CustomUserFieldsComponent,
  ],
})
export class SettingsModule {}
