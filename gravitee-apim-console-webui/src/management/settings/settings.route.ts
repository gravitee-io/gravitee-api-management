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
        component: SettingsAnalyticsDashboardComponent,
        data: {
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
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
          menu: null,
          docs: {
            page: 'management-configuration-categories',
          },
          perms: {
            only: ['environment-category-u', 'environment-category-d'],
          },
        },
      },
    ],
  },
];
@NgModule({
  imports: [RouterModule.forChild(settingsRoutes)],
  exports: [RouterModule],
})
export class SettingsRoutingModule {}
