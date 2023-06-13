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

import { ApiAnalyticsModule } from './analytics/api-analytics.module';
import { ApiListModule } from './list/api-list.module';
import { ApiNavigationModule } from './api-navigation/api-navigation.module';
import { ApiNgNavigationModule } from './api-ng-navigation/api-ng-navigation.module';
import { ApiPortalDetailsModule } from './portal/details/api-portal-details.module';
import { ApiPortalDocumentationModule } from './portal/documentation/api-portal-documentation.module';
import { ApiPortalPlansModule } from './portal/plans/api-portal-plans.module';
import { ApiPortalSubscriptionsModule } from './portal/ng-subscriptions/api-portal-subscriptions.module';
import { ApiPortalUserGroupModule } from './portal/user-group-access/api-portal-user-group.module';
import { ApiProxyModule } from './proxy/api-proxy.module';
import { ApiV4PolicyStudioModule } from './policy-studio-v4/api-v4-policy-studio.module';
import { ApiNgNavigationComponent } from './api-ng-navigation/api-ng-navigation.component';
import { ApiPortalDetailsComponent } from './portal/details/api-portal-details.component';
import { ApiPortalPlanEditComponent } from './portal/plans/edit/api-portal-plan-edit.component';
import { ApiPortalPlanListComponent } from './portal/plans/list/api-portal-plan-list.component';
import { ApiPortalSubscriptionListComponent } from './portal/ng-subscriptions/list/api-portal-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApiProxyV4EntrypointsComponent } from './proxy-v4/api-proxy-v4-entrypoints.component';
import { ApiProxyV4Module } from './proxy-v4/api-proxy-v4.module';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';
import { GioEmptyComponent } from '../../shared/components/gio-empty/gio-empty.component';
import { GioEmptyModule } from '../../shared/components/gio-empty/gio-empty.module';

// New Angular routing
const states: Ng2StateDeclaration[] = [
  {
    name: 'management.apis.ng',
    url: '/ng/:apiId',
    abstract: true,
    component: ApiNgNavigationComponent,
    data: {
      baseRouteState: 'management.apis.ng',
    },
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
  {
    name: 'management.apis.ng.plans',
    url: '/plans?status',
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
    params: {
      status: {
        type: 'string',
        dynamic: true,
      },
    },
    component: ApiPortalPlanListComponent,
  },
  {
    name: 'management.apis.ng.plan',
    url: '/plan',
    component: GioEmptyComponent,
    abstract: true,
  },
  {
    name: 'management.apis.ng.plan.new',
    url: '/new?{securityType:string}',
    component: ApiPortalPlanEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    params: {
      securityType: {
        dynamic: true,
      },
    },
  },
  {
    name: 'management.apis.ng.plan.edit',
    url: '/:planId/edit',
    component: ApiPortalPlanEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-u'],
      // },
    },
  },
  {
    name: 'management.apis.ng.subscriptions',
    url: '/subscriptions?page&size&plan&application&status&apikey',
    component: ApiPortalSubscriptionListComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
    params: {
      status: {
        type: 'string',
        dynamic: true,
      },
      application: {
        type: 'string',
        dynamic: true,
      },
      plan: {
        type: 'string',
        dynamic: true,
      },
      page: {
        type: 'int',
        value: 1,
        dynamic: true,
      },
      size: {
        type: 'int',
        value: 10,
        dynamic: true,
      },
      apikey: {
        type: 'string',
        dynamic: true,
      },
    },
  },
  {
    name: 'management.apis.ng.proxy',
    url: '/proxy',
    component: ApiProxyV4EntrypointsComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
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
    ApiPortalSubscriptionsModule,
    ApiProxyModule,
    ApiProxyV4Module,
    ApiPortalUserGroupModule,

    GioEmptyModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
