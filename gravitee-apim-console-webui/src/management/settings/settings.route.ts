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
import { ApiPortalHeaderComponent } from './api-portal-header/api-portal-header.component';
import { ApiQualityRulesComponent } from './api-quality-rules/api-quality-rules.component';
import { ApiQualityRuleComponent } from './api-quality-rules/api-quality-rule/api-quality-rule.component';
import { IdentityProvidersComponent } from './identityProviders/identity-providers.component';
import { CategoriesComponent } from './categories/categories.component';
import { CategoryComponent } from './categories/category/category.component';
import { GroupsComponent } from './groups/groups.component';
import { GroupComponent } from './groups/group/group.component';
import { ClientRegistrationProvidersComponent } from './client-registration-providers/client-registration-providers.component';
import { ClientRegistrationProviderComponent } from './client-registration-providers/client-registration-provider/client-registration-provider.component';
import { EnvironmentMetadataComponent } from './metadata/environment-metadata.component';
import { PortalComponent } from './portal/portal.component';
import { PortalThemeComponent } from './portal-theme/portalTheme.component';
import { TopApisComponent } from './top-apis/top-apis.component';
import { ApiLoggingComponent } from './api-logging/api-logging.component';
import { DictionariesComponent } from './dictionaries/dictionaries.component';
import { DictionaryComponent } from './dictionaries/dictionary.component';
import { CustomUserFieldsComponent } from './custom-user-fields/custom-user-fields.component';
import { EnvironmentNotificationSettingsListComponent } from './notifications/notification-settings/notification-settings-list/environment-notification-settings-list.component';
import { EnvironmentNotificationSettingsDetailsComponent } from './notifications/notification-settings/notification-settings-details/environment-notification-settings-details.component';

import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';

export const settingsRoutes: Routes = [
  {
    path: '',
    component: SettingsNavigationComponent,
    children: [
      {
        path: 'analytics',
        component: SettingsAnalyticsComponent,
        data: {
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
        component: SettingsAnalyticsDashboardComponent,
        data: {
          docs: {
            page: 'management-configuration-dashboard',
          },
          perms: {
            only: ['environment-dashboard-c'],
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
          perms: {
            only: ['environment-dashboard-u'],
          },
        },
      },
      {
        path: 'api-portal-header',
        component: ApiPortalHeaderComponent,
        data: {
          docs: {
            page: 'management-configuration-apiportalheader',
          },
          perms: {
            only: ['environment-api_header-r'],
            unauthorizedFallbackTo: 'management.settings.apiQuality.list',
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
          perms: {
            only: ['environment-quality_rule-r'],
            unauthorizedFallbackTo: 'management.settings.environment.identityproviders',
          },
        },
      },
      {
        path: 'api-quality-rules/new',
        component: ApiQualityRuleComponent,
        data: {
          docs: {
            page: 'management-configuration-apiquality',
          },
          perms: {
            only: ['environment-quality_rule-c'],
          },
        },
      },
      {
        path: 'api-quality-rules/:qualityRuleId',
        component: ApiQualityRuleComponent,
        data: {
          docs: {
            page: 'management-configuration-apiquality',
          },
          perms: {
            only: ['environment-quality_rule-u'],
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
          perms: {
            only: ['environment-identity_provider_activation-r'],
            unauthorizedFallbackTo: 'management.settings.categories.list',
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
          perms: {
            only: ['environment-category-r'],
            unauthorizedFallbackTo: 'management.settings.clientregistrationproviders.list',
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
          perms: {
            only: ['environment-category-c'],
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
          perms: {
            only: ['environment-category-u', 'environment-category-d'],
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
          perms: {
            only: ['environment-client_registration_provider-r'],
            unauthorizedFallbackTo: 'management.settings.documentation.list',
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
          perms: {
            only: ['environment-client_registration_provider-c'],
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
          perms: {
            only: [
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
          perms: {
            only: ['environment-documentation-c'],
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
          perms: {
            only: ['environment-documentation-u'],
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
          perms: {
            only: ['environment-documentation-u'],
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
          perms: {
            only: ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d'],
            unauthorizedFallbackTo: 'management.settings.metadata',
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
          perms: {
            only: ['environment-metadata-r'],
            unauthorizedFallbackTo: 'management.settings.portal',
          },
        },
      },
      {
        path: 'portal',
        component: PortalComponent,
        data: {
          docs: {
            page: 'management-configuration-portal',
          },
          perms: {
            only: ['environment-settings-r'],
            unauthorizedFallbackTo: 'management.settings.theme',
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
          perms: {
            only: ['environment-theme-r'],
            unauthorizedFallbackTo: 'management.settings.top-apis',
          },
        },
      },
      {
        path: 'top-apis',
        component: TopApisComponent,
        data: {
          docs: {
            page: 'management-configuration-top_apis',
          },
          perms: {
            only: ['environment-top_apis-r'],
            unauthorizedFallbackTo: 'management.settings.api_logging',
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
          perms: {
            only: ['organization-settings-r'],
            unauthorizedFallbackTo: 'management.settings.dictionaries.list',
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
          perms: {
            only: ['environment-dictionary-r'],
            unauthorizedFallbackTo: 'management.settings.customUserFields',
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
          perms: {
            only: ['environment-dictionary-c'],
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
          perms: {
            only: ['environment-dictionary-c', 'environment-dictionary-r', 'environment-dictionary-u', 'environment-dictionary-d'],
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
          perms: {
            only: ['organization-custom_user_fields-r'],
            unauthorizedFallbackTo: 'management.settings.groups.list',
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
          perms: {
            only: ['environment-group-r'],
            unauthorizedFallbackTo: 'management.settings.notification-settings',
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
          perms: {
            only: ['environment-group-r'],
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
          perms: {
            only: ['environment-group-r'],
          },
        },
      },
      {
        path: 'notifications',
        component: EnvironmentNotificationSettingsListComponent,
        data: {
          docs: {
            page: 'management-configuration-notifications',
          },
          perms: {
            unauthorizedFallbackTo: 'management.home',
          },
        },
      },
      {
        path: 'notifications/:notificationId',
        component: EnvironmentNotificationSettingsDetailsComponent,
        data: {
          docs: {
            page: 'management-configuration-notifications',
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
