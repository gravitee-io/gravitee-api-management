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
import { ApiProxyModule } from './proxy/api-proxy.module';
import { ApiV4PolicyStudioModule } from './policy-studio-v4/api-v4-policy-studio.module';
import { ApiNgNavigationComponent } from './api-ng-navigation/api-ng-navigation.component';
import { ApiPortalDetailsComponent } from './portal/details/api-portal-details.component';
import { ApiPortalPlanEditComponent } from './portal/plans/edit/api-portal-plan-edit.component';
import { ApiPortalPlanListComponent } from './portal/plans/list/api-portal-plan-list.component';
import { ApiPortalSubscriptionListComponent } from './portal/ng-subscriptions/list/api-portal-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApisPortalModule } from './portal/apis-portal.module';
import { ApiPortalSubscriptionEditComponent } from './portal/ng-subscriptions/edit/api-portal-subscription-edit.component';
import { ApiEndpointsModule } from './endpoints-v4/api-endpoints.module';
import { ApiBackendServicesComponent } from './endpoints-v4/backend-services/api-backend-services.component';
import { ApiEntrypointsV4GeneralComponent } from './entrypoints-v4/api-entrypoints-v4-general.component';
import { ApiEntrypointsV4Module } from './entrypoints-v4/api-entrypoints-v4.module';
import { ApiEndpointComponent } from './endpoints-v4/backend-services/endpoint/api-endpoint.component';
import { ApiEntrypointsV4EditComponent } from './entrypoints-v4/edit/api-entrypoints-v4-edit.component';

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
    url: '/new?{selectedPlanMenuItem:string}',
    component: ApiPortalPlanEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    params: {
      selectedPlanMenuItem: {
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
    name: 'management.apis.ng.entrypoints',
    url: '/entrypoints',
    component: ApiEntrypointsV4GeneralComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
  },
  {
    name: 'management.apis.ng.entrypoints-edit',
    url: '/entrypoints/:entrypointId',
    component: ApiEntrypointsV4EditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
  },
  {
    name: 'management.apis.ng.subscription',
    url: '/subscription',
    component: GioEmptyComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
  },
  {
    name: 'management.apis.ng.subscription.edit',
    url: '/:subscriptionId',
    component: ApiPortalSubscriptionEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
    },
  },
  {
    name: 'management.apis.ng.endpoints',
    url: '/endpoints',
    component: ApiBackendServicesComponent,
    data: {
      useAngularMaterial: true,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
    },
  },
  {
    name: 'management.apis.ng.endpoint-new',
    url: '/groups/:groupIndex/endpoint/new',
    component: ApiEndpointComponent,
    data: {
      useAngularMaterial: true,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
    },
  },
  {
    name: 'management.apis.ng.endpoint-edit',
    url: '/groups/:groupIndex/endpoint/:endpointIndex/edit',
    component: ApiEndpointComponent,
    data: {
      useAngularMaterial: true,
      // TODO: Implement permissions
      // perms: {
      //   only: ['api-plan-r'],
      // },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
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
    ApisPortalModule,
    ApiProxyModule,
    ApiEntrypointsV4Module,
    ApiEndpointsModule,

    GioEmptyModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
