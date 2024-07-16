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
import { RouterModule, Routes } from '@angular/router';

import { SettingsNavigationComponent } from './settings-navigation/settings-navigation.component';
import { SettingsAnalyticsComponent } from './analytics/settings-analytics.component';
import { SettingsAnalyticsDashboardComponent } from './analytics/dashboard/settings-analytics-dashboard.component';
import { CategoriesComponent } from './categories/categories.component';
import { CategoryComponent } from './categories/category/category.component';
import { GroupsComponent } from './groups/groups.component';
import { GroupComponent } from './groups/group/group.component';
import { ApiPortalHeaderComponent as ApiPortalHeaderComponentMigrated } from './api-portal-header/api-portal-header.component';
import { ClientRegistrationProvidersComponent } from './client-registration-providers/client-registration-providers.component';
import { ClientRegistrationProviderComponent } from './client-registration-providers/client-registration-provider/client-registration-provider.component';
import { EnvironmentMetadataComponent } from './metadata/environment-metadata.component';
import { PortalThemeComponent } from './portal-theme/portalTheme.component';
import { TopApisComponent as TopApisComponentMigrated } from './top-apis/top-apis.component';
import { ApiLoggingComponent } from './api-logging/api-logging.component';
import { DictionariesComponent } from './dictionaries/dictionaries.component';
import { DictionaryComponent } from './dictionaries/dictionary.component';
import { CustomUserFieldsComponent } from './custom-user-fields/custom-user-fields.component';
import { ApiQualityRulesComponent } from './api-quality-rules/api-quality-rules.component';
import { EnvironmentNotificationComponent } from './notification/environment-notification.component';
import { IdentityProvidersComponent } from './identity-providers/identity-providers.component';
import { PortalSettingsComponent } from './portal-settings/portal-settings.component';
import { EnvironmentFlowsComponent } from './environment-flows/environment-flows.component';

import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';
import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';

export const settingsRoutes: Routes = [
  {
    path: '',
    component: SettingsNavigationComponent,
    canActivateChild: [PermissionGuard.checkRouteDataPermissions],
    children: [
      {
        path: 'analytics',
        component: SettingsAnalyticsComponent,
        data: {
          docs: {
            page: 'management-configuration-analytics',
          },
          permissions: {
            anyOf: ['environment-dashboard-r'],
            unauthorizedFallbackTo: '../api-portal-header',
          },
        },
      },
      {
        path: 'analytics/dashboard/:type/new',
        component: SettingsAnalyticsDashboardComponent,
        data: {
          docs: {
            page: 'management-configuration-dashboard',
          },
          permissions: {
            anyOf: ['environment-dashboard-c'],
          },
        },
      },
      {
        path: 'analytics/dashboard/:type/:dashboardId',
        component: SettingsAnalyticsDashboardComponent,
        data: {
          docs: {
            page: 'management-configuration-dashboard',
          },
          permissions: {
            anyOf: ['environment-dashboard-u'],
          },
        },
      },
      {
        path: 'api-portal-header',
        component: ApiPortalHeaderComponentMigrated,
        data: {
          permissions: {
            anyOf: ['environment-api_header-r'],
            unauthorizedFallbackTo: '../api-quality-rules',
          },
        },
      },
      {
        path: 'api-quality-rules',
        component: ApiQualityRulesComponent,
        data: {
          docs: {
            page: 'management-configuration-apiquality',
          },
          permissions: {
            anyOf: ['environment-quality_rule-r'],
            unauthorizedFallbackTo: '../identity-providers',
          },
        },
      },
      {
        path: 'identity-providers',
        component: IdentityProvidersComponent,
        data: {
          docs: {
            page: 'management-configuration-identityproviders',
          },
          permissions: {
            anyOf: ['environment-identity_provider_activation-r'],
            unauthorizedFallbackTo: '../categories',
          },
        },
      },

      {
        path: 'categories',
        component: CategoriesComponent,
        data: {
          docs: {
            page: 'management-configuration-categories',
          },
          permissions: {
            anyOf: ['environment-category-r'],
            unauthorizedFallbackTo: '../client-registration-providers',
          },
        },
      },
      {
        path: 'categories/new',
        component: CategoryComponent,
        data: {
          docs: {
            page: 'management-configuration-categories',
          },
          permissions: {
            anyOf: ['environment-category-c'],
          },
        },
      },
      {
        path: 'categories/:categoryId',
        component: CategoryComponent,
        data: {
          docs: {
            page: 'management-configuration-categories',
          },
          permissions: {
            anyOf: ['environment-category-u', 'environment-category-d'],
          },
        },
      },
      {
        path: 'client-registration-providers',
        component: ClientRegistrationProvidersComponent,
        data: {
          docs: {
            page: 'management-configuration-client-registration-providers',
          },
          permissions: {
            anyOf: ['environment-client_registration_provider-r'],
            unauthorizedFallbackTo: '../documentation',
          },
        },
      },
      {
        path: 'client-registration-providers/new',
        component: ClientRegistrationProviderComponent,
        data: {
          docs: {
            page: 'management-configuration-client-registration-provider',
          },
          permissions: {
            anyOf: ['environment-client_registration_provider-c'],
          },
        },
      },
      {
        path: 'client-registration-providers/:providerId',
        component: ClientRegistrationProviderComponent,
        data: {
          docs: {
            page: 'management-configuration-client-registration-provider',
          },
          permissions: {
            anyOf: [
              'environment-client_registration_provider-r',
              'environment-client_registration_provider-u',
              'environment-client_registration_provider-d',
            ],
          },
        },
      },
      {
        path: 'documentation/new',
        component: DocumentationNewPageComponent,
        data: {
          docs: {
            page: 'management-configuration-portal-pages',
          },
          permissions: {
            anyOf: ['environment-documentation-c'],
          },
        },
      },
      {
        path: 'documentation/import',
        component: DocumentationImportPagesComponent,
        data: {
          docs: {
            page: 'management-configuration-portal-pages',
          },
          permissions: {
            anyOf: ['environment-documentation-u'],
          },
        },
      },
      {
        path: 'documentation/:pageId',
        component: DocumentationEditPageComponent,
        data: {
          docs: {
            page: 'management-configuration-portal-pages',
          },
          permissions: {
            anyOf: ['environment-documentation-u'],
          },
        },
      },
      {
        path: 'documentation',
        component: DocumentationManagementComponent,
        data: {
          docs: {
            page: 'management-configuration-portal-pages',
          },
          permissions: {
            anyOf: ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d'],
            unauthorizedFallbackTo: '../environment-flows',
          },
        },
      },
      {
        path: 'environment-flows',
        component: EnvironmentFlowsComponent,
        data: {
          permissions: {
            anyOf: ['environment-environment_flows-r'],
            unauthorizedFallbackTo: '../metadata',
          },
        },
      },
      {
        path: 'metadata',
        component: EnvironmentMetadataComponent,
        data: {
          docs: {
            page: 'management-configuration-metadata',
          },
          permissions: {
            anyOf: ['environment-metadata-r'],
            unauthorizedFallbackTo: '../portal',
          },
        },
      },
      {
        path: 'portal',
        component: PortalSettingsComponent,
        data: {
          docs: {
            page: 'management-configuration-portal',
          },
          permissions: {
            anyOf: ['environment-settings-r'],
            unauthorizedFallbackTo: '../theme',
          },
        },
      },
      {
        path: 'theme',
        component: PortalThemeComponent,
        data: {
          docs: {
            page: 'management-configuration-portal-theme',
          },
          permissions: {
            anyOf: ['environment-theme-r'],
            unauthorizedFallbackTo: '../top-apis',
          },
        },
      },
      {
        path: 'top-apis',
        component: TopApisComponentMigrated,
        data: {
          docs: {
            page: 'management-configuration-top_apis',
          },
          permissions: {
            anyOf: ['environment-top_apis-r'],
            unauthorizedFallbackTo: '../api-logging',
          },
        },
      },
      {
        path: 'api-logging',
        component: ApiLoggingComponent,
        data: {
          docs: {
            page: 'management-configuration-apilogging',
          },
          permissions: {
            anyOf: ['organization-settings-r'],
            unauthorizedFallbackTo: '../dictionaries',
          },
        },
      },
      {
        path: 'dictionaries',
        component: DictionariesComponent,
        data: {
          docs: {
            page: 'management-configuration-dictionaries',
          },
          permissions: {
            anyOf: ['environment-dictionary-r'],
            unauthorizedFallbackTo: '../custom-user-fields',
          },
        },
      },
      {
        path: 'dictionaries/new',
        component: DictionaryComponent,
        data: {
          docs: {
            page: 'management-configuration-dictionary',
          },
          permissions: {
            anyOf: ['environment-dictionary-c'],
          },
        },
      },
      {
        path: 'dictionaries/:dictionaryId',
        component: DictionaryComponent,
        data: {
          menu: null,
          docs: {
            page: 'management-configuration-dictionary',
          },
          permissions: {
            anyOf: ['environment-dictionary-c', 'environment-dictionary-r', 'environment-dictionary-u', 'environment-dictionary-d'],
          },
        },
      },
      {
        path: 'custom-user-fields',
        component: CustomUserFieldsComponent,
        data: {
          docs: {
            page: 'management-configuration-custom-user-fields',
          },
          permissions: {
            anyOf: ['organization-custom_user_fields-r'],
            unauthorizedFallbackTo: '../groups',
          },
        },
      },
      {
        path: 'groups',
        component: GroupsComponent,
        data: {
          docs: {
            page: 'management-configuration-groups',
          },
          permissions: {
            anyOf: ['environment-group-r'],
            unauthorizedFallbackTo: '../notifications',
          },
        },
      },
      {
        path: 'groups/new',
        component: GroupComponent,
        data: {
          docs: {
            page: 'management-configuration-group',
          },
          permissions: {
            anyOf: ['environment-group-r'],
          },
        },
      },
      {
        path: 'groups/:groupId',
        component: GroupComponent,
        data: {
          docs: {
            page: 'management-configuration-group',
          },
          permissions: {
            anyOf: ['environment-group-r'],
          },
        },
      },
      {
        path: 'notifications',
        component: EnvironmentNotificationComponent,
        data: {
          docs: {
            page: 'management-configuration-notifications',
          },
          permissions: {
            anyOf: ['environment-notification-r'],
            unauthorizedFallbackTo: '../../',
          },
        },
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'analytics',
      },
    ],
  },
];
@NgModule({
  imports: [RouterModule.forChild(settingsRoutes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
