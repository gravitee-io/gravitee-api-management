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
import { Ng2StateDeclaration, UIRouterModule } from '@uirouter/angular';
import { Transition } from '@uirouter/angularjs';

import { ApiPortalDetailsModule } from './portal/details/api-portal-details.module';
import { ApiListModule } from './list/api-list.module';
import { ApiProxyModule } from './proxy/api-proxy.module';
import { ApiNavigationModule } from './api-navigation/api-navigation.module';
import { ApiPortalPlansModule } from './portal/plans/api-portal-plans.module';
import { ApiAnalyticsModule } from './analytics/api-analytics.module';
import { ApiPortalUserGroupModule } from './portal/user-group-access/api-portal-user-group.module';
import { ApiPortalDocumentationModule } from './portal/documentation/api-portal-documentation.module';
import { ApiPortalDetailsComponent } from './portal/details/api-portal-details.component';
import { ApiNgNavigationComponent } from './api-ng-navigation/api-ng-navigation.component';
import { ApiNgNavigationModule } from './api-ng-navigation/api-ng-navigation.module';
import { ApiV4PolicyStudioModule } from './policy-studio-v4/api-v4-policy-studio.module';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';

// New Angular routing
const states: Ng2StateDeclaration[] = [
  {
    name: 'management.apis.ng',
    url: '/ng/:apiId',
    abstract: true,
    component: ApiNgNavigationComponent,

    resolve: [
      {
        token: 'currentApi',
        deps: [ApiV2Service, Transition],
        resolveFn: (apiV2Service: ApiV2Service, transition: Transition) => apiV2Service.get(transition.params().apiId).toPromise(),
      },
      {
        token: 'currentApiIsSync',
        // TODO: Implement api sync check
        resolveFn: () => false,
      },
      // Load current API permissions for current user into permissionService
      {
        token: 'guard',
        deps: [GioPermissionService, Transition],
        resolveFn: (permissionService: GioPermissionService, transition: Transition) =>
          permissionService.loadApiPermissions(transition.params().apiId).toPromise(),
      },
    ],
  },
  {
    name: 'management.apis.ng.design',
    url: '/design',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: ApiV4PolicyStudioDesignComponent,
  },
  {
    name: 'management.apis.ng.general',
    url: '/general',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: ApiPortalDetailsComponent,
  },
];

@NgModule({
  imports: [
    ApiAnalyticsModule,
    ApiListModule,
    ApiNavigationModule,
    ApiNgNavigationModule,
    ApiV4PolicyStudioModule,
    ApiPortalDetailsModule,
    ApiPortalDocumentationModule,
    ApiPortalPlansModule,
    ApiProxyModule,
    ApiPortalUserGroupModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
