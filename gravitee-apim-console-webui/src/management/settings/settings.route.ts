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

import { SettingsNavigationComponent } from '../configuration/settings-navigation/settings-navigation.component';
import { SettingsAnalyticsComponent } from '../configuration/analytics/settings-analytics.component';
import { SettingsAnalyticsDashboardComponent } from '../configuration/analytics/dashboard/settings-analytics-dashboard.component';
import { ApiPortalHeaderComponent } from '../configuration/api-portal-header/api-portal-header.component';
import { ApiQualityRulesComponent } from '../configuration/api-quality-rules/api-quality-rules.component';
import { ApiQualityRuleComponent } from '../configuration/api-quality-rules/api-quality-rule/api-quality-rule.component';
import { IdentityProvidersComponent } from '../configuration/identityProviders/identity-providers.component';
import { CategoriesComponent } from '../configuration/categories/categories.component';
import { CategoryComponent } from '../configuration/categories/category/category.component';
import { GroupsComponent } from '../configuration/groups/groups.component';
import { GroupComponent } from '../configuration/groups/group/group.component';
import { ClientRegistrationProvidersComponent } from '../configuration/client-registration-providers/client-registration-providers.component';
import { ClientRegistrationProviderComponent } from '../configuration/client-registration-providers/client-registration-provider/client-registration-provider.component';
import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';
import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';

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
