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

import {
  PortalNavigationItemsComponent,
  PortalThemeComponent,
  HomepageComponent,
  SubscriptionFormComponent,
  HasUnsavedChangesGuard,
  PortalConstants,
  GioPermissionService as SharedGioPermissionService,
  PORTAL_TECH_PREVIEW_MESSAGE,
  API_SEARCH_SERVICE,
} from '@gravitee/gravitee-portal';

import { PortalNavigationComponent } from './navigation/portal-navigation.component';
import { PortalBannerComponent } from './banner/portal-banner.component';
import { PortalCatalogComponent } from './catalog/portal-catalog.component';
import { CategoryCatalogComponent } from './catalog/category/category.component';
import { CategoryListComponent } from './catalog/category-list/category-list.component';
import { PortalApiComponent } from './api/portal-api.component';
import { PortalApiListComponent } from './api/api-list/portal-api-list.component';

import { PermissionGuard } from '../shared/components/gio-permission/gio-permission.guard';
import { GioPermissionService } from '../shared/components/gio-permission/gio-permission.service';
import { HasLicenseGuard } from '../shared/components/gio-license/has-license.guard';
import { EnvironmentGuard } from '../management/environment.guard';
import { Constants } from '../entities/Constants';
import { IntegrationsService } from '../services-ngx/integrations.service';
import { ApiV2Service } from '../services-ngx/api-v2.service';

const portalRoutes: Routes = [
  {
    path: '',
    component: PortalNavigationComponent,
    canActivate: [EnvironmentGuard.initEnvConfigAndLoadPermissions],
    canActivateChild: [HasLicenseGuard, PermissionGuard.checkRouteDataPermissions],
    children: [
      {
        path: 'navigation',
        component: PortalNavigationItemsComponent,
        canDeactivate: [HasUnsavedChangesGuard],
        data: {
          permissions: {
            anyOf: ['environment-settings-r', 'environment-settings-u'],
          },
        },
      },
      {
        path: 'catalog',
        component: PortalCatalogComponent,
        children: [
          {
            path: '',
            component: CategoryListComponent,
            data: {
              permissions: {
                anyOf: ['environment-category-r', 'environment-category-u'],
              },
            },
          },
          {
            path: 'category/new',
            component: CategoryCatalogComponent,
            data: {
              permissions: {
                anyOf: ['environment-category-r', 'environment-category-u'],
              },
            },
          },
          {
            path: 'category/:categoryId',
            component: CategoryCatalogComponent,
            data: {
              permissions: {
                anyOf: ['environment-category-r', 'environment-category-u'],
              },
            },
          },
        ],
      },
      {
        path: 'banner',
        component: PortalBannerComponent,
        data: {
          permissions: {
            anyOf: ['environment-settings-r', 'environment-settings-u'],
          },
        },
      },
      {
        path: 'api',
        component: PortalApiComponent,
        children: [
          {
            path: '',
            component: PortalApiListComponent,
            data: {
              permissions: {
                anyOf: ['environment-settings-r', 'environment-settings-u'],
              },
            },
          },
        ],
      },
      {
        path: 'theme',
        component: PortalThemeComponent,
        data: {
          permissions: {
            anyOf: ['environment-theme-r', 'environment-theme-u'],
          },
        },
      },
      {
        path: 'homepage',
        component: HomepageComponent,
        canDeactivate: [HasUnsavedChangesGuard],
        data: {
          permissions: {
            anyOf: ['environment-documentation-r', 'environment-documentation-u'],
          },
        },
      },
      {
        path: 'subscription-form',
        component: SubscriptionFormComponent,
        canDeactivate: [HasUnsavedChangesGuard],
        data: {
          permissions: {
            anyOf: ['environment-metadata-r', 'environment-metadata-u'],
          },
        },
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'navigation',
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(portalRoutes)],
  exports: [RouterModule],
  providers: [
    { provide: PortalConstants, useExisting: Constants },
    { provide: SharedGioPermissionService, useExisting: GioPermissionService },
    {
      provide: PORTAL_TECH_PREVIEW_MESSAGE,
      useFactory: (integrationsService: IntegrationsService) => integrationsService.bannerMessages.techPreview,
      deps: [IntegrationsService],
    },
    { provide: API_SEARCH_SERVICE, useExisting: ApiV2Service },
  ],
})
export class PortalSettingsRoutingModule {}
