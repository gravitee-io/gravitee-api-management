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
import { Transition, TransitionService } from '@uirouter/angularjs';
import * as angular from 'angular';
import { switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { StateParams } from '@uirouter/core';

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
import { ApiPortalSubscriptionListComponent } from './portal/subscriptions/list/api-portal-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApisPortalModule } from './portal/apis-portal.module';
import { ApiPortalSubscriptionEditComponent } from './portal/subscriptions/edit/api-portal-subscription-edit.component';
import { ApiEndpointsModule } from './endpoints-v4/api-endpoints.module';
import { ApiBackendServicesComponent } from './endpoints-v4/backend-services/api-backend-services.component';
import { ApiEntrypointsV4GeneralComponent } from './entrypoints-v4/api-entrypoints-v4-general.component';
import { ApiEntrypointsV4Module } from './entrypoints-v4/api-entrypoints-v4.module';
import { ApiEndpointComponent } from './endpoints-v4/backend-services/endpoint/api-endpoint.component';
import { ApiEntrypointsV4EditComponent } from './entrypoints-v4/edit/api-entrypoints-v4-edit.component';
import { ApiPropertiesComponent } from './proxy/properties-ng/api-properties.component';
import { ApiResourcesComponent } from './proxy/resources-ng/api-resources.component';
import { GioPolicyStudioRoutingModule } from './policy-studio/gio-policy-studio-routing.module';
import { ApiPortalMembersComponent } from './portal/user-group-access/members/api-portal-members.component';
import { ApiPortalGroupsComponent } from './portal/user-group-access/groups/api-portal-groups.component';
import { ApiPortalTransferOwnershipComponent } from './portal/user-group-access/transfer-ownership/api-portal-transfer-ownership.component';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { GioEmptyComponent } from '../../shared/components/gio-empty/gio-empty.component';
import { GioEmptyModule } from '../../shared/components/gio-empty/gio-empty.module';
import { SpecificJsonSchemaTypeModule } from '../../shared/components/specific-json-schema-type/specific-json-schema-type.module';
import { DocumentationModule } from '../../components/documentation/documentation.module';
import { DocumentationQuery, DocumentationService } from '../../services/documentation.service';
import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';
import FetcherService from '../../services/fetcher.service';
import CategoryService from '../../services/category.service';
import GroupService from '../../services/group.service';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';

const graviteeManagementModule = angular.module('gravitee-management');
apiPermissionHook.$inject = ['$transitions', 'ngGioPermissionService'];
function apiPermissionHook($transitions: TransitionService, gioPermissionService: GioPermissionService) {
  $transitions.onBefore(
    {
      to: 'management.apis.ng.**',
    },
    (transition: Transition) => {
      const stateService = transition.router.stateService;

      return gioPermissionService
        .loadApiPermissions(transition.params().apiId)
        .pipe(
          switchMap(() => {
            const permissions = transition.$to().data?.apiPermissions?.only;
            if (!permissions) {
              return of(true);
            }
            if (gioPermissionService.hasAnyMatching(permissions)) {
              return of(true);
            }
            return of(stateService.target('login'));
          }),
        )
        .toPromise();
    },
    { priority: 9 },
  );
}
graviteeManagementModule.run(apiPermissionHook);
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
  },
  {
    name: 'management.apis.ng.policyStudio',
    url: '/policy-studio',
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
      apiPermissions: {
        only: ['api-plan-r'],
      },
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
      apiPermissions: {
        only: ['api-plan-c'],
      },
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
      apiPermissions: {
        only: ['api-plan-u'],
      },
    },
  },
  {
    name: 'management.apis.ng.subscriptions',
    url: '/subscriptions?page&size&plan&application&status&apiKey',
    component: ApiPortalSubscriptionListComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-subscription-r'],
      },
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
      apiKey: {
        type: 'string',
        dynamic: true,
      },
    },
  },
  {
    name: 'management.apis.ng.subscription',
    url: '/subscription',
    component: GioEmptyComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-subscription-r'],
      },
    },
  },
  {
    name: 'management.apis.ng.subscription.edit',
    url: '/:subscriptionId',
    component: ApiPortalSubscriptionEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-subscription-r', 'api-subscription-u'],
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
      apiPermissions: {
        only: ['api-definition-r'],
      },
    },
  },
  {
    name: 'management.apis.ng.entrypoints-edit',
    url: '/entrypoints/:entrypointId',
    component: ApiEntrypointsV4EditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-definition-u'],
      },
    },
  },
  {
    name: 'management.apis.ng.endpoints',
    url: '/endpoints',
    component: ApiBackendServicesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
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
      apiPermissions: {
        only: ['api-definition-u'],
      },
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
      apiPermissions: {
        only: ['api-definition-u'],
      },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
    },
  },
  {
    name: 'management.apis.ng.properties',
    url: '/properties',
    component: ApiPropertiesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-policy-studio-properties',
      },
    },
  },
  {
    name: 'management.apis.ng.resources',
    url: '/resources',
    component: ApiResourcesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-policy-studio-resources',
      },
    },
  },
  {
    name: 'management.apis.ng.documentation',
    component: DocumentationManagementComponent,
    url: '/documentation?parent',
    resolve: [
      {
        token: 'pages',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          if ($stateParams.parent && '' !== $stateParams.parent) {
            q.parent = $stateParams.parent;
          } else {
            q.root = true;
          }

          return DocumentationService.search(q, $stateParams.apiId).then((response) => {
            return response.data;
          });
        },
      },
      {
        token: 'folders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'systemFolders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
    ],
    data: {
      docs: {
        page: 'management-api-documentation',
      },
      apiPermissions: {
        only: ['api-documentation-r'],
      },
    },
    params: {
      parent: {
        type: 'string',
        value: '',
        squash: false,
      },
    },
  },
  {
    name: 'management.apis.ng.documentationNew',
    component: DocumentationNewPageComponent,
    url: '/documentation/new?type&parent',
    resolve: [
      {
        token: 'resolvedFetchers',
        deps: ['FetcherService'],
        resolveFn: (FetcherService: FetcherService) => {
          return FetcherService.list().then((response) => {
            return response.data;
          });
        },
      },
      {
        token: 'folders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'systemFolders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'pageResources',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
          }
        },
      },
      {
        token: 'categoryResources',
        deps: ['CategoryService', '$stateParams'],
        resolveFn: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then((response) => response.data);
          }
        },
      },
      {
        token: 'pagesToLink',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
            const q = new DocumentationQuery();
            q.homepage = false;
            q.published = true;
            return DocumentationService.search(q, $stateParams.apiId).then((response) =>
              response.data.filter(
                (page) =>
                  page.type.toUpperCase() === 'MARKDOWN' ||
                  page.type.toUpperCase() === 'SWAGGER' ||
                  page.type.toUpperCase() === 'ASCIIDOC' ||
                  page.type.toUpperCase() === 'ASYNCAPI',
              ),
            );
          }
        },
      },
    ],
    data: {
      docs: {
        page: 'management-api-documentation',
      },
      apiPermissions: {
        only: ['api-documentation-c'],
      },
    },
    params: {
      type: {
        type: 'string',
        value: '',
        squash: false,
      },
      parent: {
        type: 'string',
        value: '',
        squash: false,
      },
    },
  },
  {
    name: 'management.apis.ng.documentationImport',
    component: DocumentationImportPagesComponent,
    url: '/documentation/import',
    resolve: [
      {
        token: 'resolvedFetchers',
        deps: ['FetcherService'],
        resolveFn: (FetcherService: FetcherService) => {
          return FetcherService.list().then((response) => {
            return response.data;
          });
        },
      },
      {
        token: 'resolvedRootPage',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'ROOT';
          return DocumentationService.search(q, $stateParams.apiId).then((response) =>
            response.data && response.data.length > 0 ? response.data[0] : null,
          );
        },
      },
    ],
    data: {
      docs: {
        page: 'management-api-documentation',
      },
      apiPermissions: {
        only: ['api-documentation-c'],
      },
    },
    params: {
      type: {
        type: 'string',
        value: '',
        squash: false,
      },
      parent: {
        type: 'string',
        value: '',
        squash: false,
      },
    },
  },
  {
    name: 'management.apis.ng.documentationEdit',
    component: DocumentationEditPageComponent,
    url: '/documentation/:pageId?:tab&type',
    resolve: [
      {
        token: 'resolvedFetchers',
        deps: ['FetcherService'],
        resolveFn: (FetcherService: FetcherService) => {
          return FetcherService.list().then((response) => {
            return response.data;
          });
        },
      },
      {
        token: 'resolvedPage',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) =>
          DocumentationService.get($stateParams.apiId, $stateParams.pageId).then((response) => response.data),
      },
      {
        token: 'resolvedGroups',
        deps: ['GroupService'],
        resolveFn: (GroupService: GroupService) =>
          GroupService.list().then((response) => {
            return response.data;
          }),
      },
      {
        token: 'folders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'systemFolders',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          const q = new DocumentationQuery();
          q.type = 'SYSTEM_FOLDER';
          return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'pageResources',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            const q = new DocumentationQuery();
            return DocumentationService.search(q, $stateParams.apiId).then((response) => response.data);
          }
        },
      },
      {
        token: 'categoryResources',
        deps: ['CategoryService', '$stateParams'],
        resolveFn: (CategoryService: CategoryService, $stateParams: StateParams) => {
          if ($stateParams.type === 'LINK') {
            return CategoryService.list().then((response) => response.data);
          }
        },
      },
      {
        token: 'pagesToLink',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'MARKDOWN_TEMPLATE') {
            const q = new DocumentationQuery();
            q.homepage = false;
            q.published = true;
            return DocumentationService.search(q, $stateParams.apiId).then((response) =>
              response.data.filter(
                (page) =>
                  page.type.toUpperCase() === 'MARKDOWN' ||
                  page.type.toUpperCase() === 'SWAGGER' ||
                  page.type.toUpperCase() === 'ASCIIDOC' ||
                  page.type.toUpperCase() === 'ASYNCAPI',
              ),
            );
          }
        },
      },
      {
        token: 'attachedResources',
        deps: ['DocumentationService', '$stateParams'],
        resolveFn: (DocumentationService: DocumentationService, $stateParams: StateParams) => {
          if ($stateParams.type === 'MARKDOWN' || $stateParams.type === 'ASCIIDOC' || $stateParams.type === 'ASYNCAPI') {
            return DocumentationService.getMedia($stateParams.pageId, $stateParams.apiId).then((response) => response.data);
          }
        },
      },
    ],
    data: {
      docs: {
        page: 'management-api-documentation',
      },
      apiPermissions: {
        only: ['api-documentation-c'],
      },
    },
    params: {
      type: {
        type: 'string',
        value: '',
        squash: false,
      },
      parent: {
        type: 'string',
        value: '',
        squash: false,
      },
    },
  },
  {
    name: 'management.apis.ng.members',
    component: ApiPortalMembersComponent,
    url: '/members',
    data: {
      apiPermissions: {
        only: ['api-member-r'],
      },
      docs: {
        page: 'management-api-members',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.groups',
    component: ApiPortalGroupsComponent,
    url: '/groups',
    data: {
      apiPermissions: {
        only: ['api-member-r'],
      },
      docs: {
        page: 'management-api-members',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.transferOwnership',
    component: ApiPortalTransferOwnershipComponent,
    url: '/transfer-ownership',
    data: {
      apiPermissions: {
        only: ['api-member-r'],
      },
      docs: {
        page: 'management-api-members',
      },
      useAngularMaterial: true,
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
    GioPolicyStudioRoutingModule.withRouting({ stateNamePrefix: 'management.apis.ng.policy-studio-v2' }),
    SpecificJsonSchemaTypeModule,
    DocumentationModule,

    GioEmptyModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
