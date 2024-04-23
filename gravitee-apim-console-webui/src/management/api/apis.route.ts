/*
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
import { StateParams, StateService } from '@uirouter/core';
import { StateProvider } from '@uirouter/angularjs';
import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import EnvironmentService from '../../services/environment.service';
import GroupService from '../../services/group.service';
import PolicyService from '../../services/policy.service';
import TagService from '../../services/tag.service';
import TenantService from '../../services/tenant.service';
import { ApiV2Service } from '../../services-ngx/api-v2.service';
import { isApiV4 } from '../../util';

export default apisRouterConfig;

/* @ngInject */
function apisRouterConfig($stateProvider: StateProvider) {
  $stateProvider
    .state('management.apis', {
      abstract: true,
      template: '<div flex layout="column" ui-view></div>',
      url: '/apis',
    })
    .state('management.apis.detail', {
      abstract: true,
      url: '/:apiId',
      template: require('./apiAdmin.html'),
      controller: 'ApiAdminController',
      controllerAs: 'apiCtrl',
      resolve: {
        resolvedApiState: function ($stateParams, ApiService) {
          return ApiService.isAPISynchronized($stateParams.apiId);
        },
        resolvedApiV1V2: function (
          $stateParams: StateParams,
          ApiService: ApiService,
          ngApiV2Service: ApiV2Service,
          $state: StateService,
          Constants: any,
          EnvironmentService: EnvironmentService,
          $timeout: angular.ITimeoutService,
        ) {
          return ngApiV2Service
            .get($stateParams.apiId)
            .toPromise()
            .then((api) => {
              if (isApiV4(api)) {
                // 📝 Best effort to redirect to the right new route state (no mapping for $stateParams as not necessary for current routes)
                // Used for the task page links
                const ngStateRedirectionMap = {
                  'management.apis.detail.portal.general': 'management.apis.ng.general',
                  'management.apis.detail.portal.subscription.edit': 'management.apis.ng.subscription.edit',
                };

                const ngRedirectToState = ngStateRedirectionMap[$state.transition.to().name];
                if (ngRedirectToState) {
                  $state.transition.abort();

                  $timeout(() => {
                    $state.go(ngRedirectToState, $stateParams, { location: 'replace' });
                  });
                  return;
                }
                // If thrown, Check origin state link and update it to new Angular one. Or add redirect mapping upper
                throw new Error(`Illegal state: V4 API should never trigger navigation to this route ${$state.current.name}.`);
              }
              return ApiService.get($stateParams.apiId).catch((err) => {
                if (err && err.interceptorFuture) {
                  $state.go('management.apis.list', {
                    environmentId: EnvironmentService.getFirstHridOrElseId(Constants.org.currentEnv.id),
                  });
                }
              });
            });
        },
        resolvedGroups: (GroupService: GroupService) => {
          return GroupService.list().then((response) => {
            return response.data;
          });
        },
<<<<<<< HEAD
        resolvedApiPermissions: (ApiService: ApiService, $stateParams: StateParams) =>
          ApiService.getPermissions($stateParams.apiId).catch((err) => {
            if (err && err.interceptorFuture) {
              err.interceptorFuture.cancel(); // avoid a duplicated notification with the same error
            }
=======
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
        token: 'readOnly',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId).then((res) => res.data?.definition_context?.origin === 'kubernetes');
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
      },
    },
  },
  {
    name: 'management.apis.documentationNew',
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
    name: 'management.apis.documentationImport',
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
    name: 'management.apis.documentationEdit',
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
>>>>>>> 2a74f294c0 (feat: set v2 doc pages to read only for kube origin)
          }),
        onEnter: function (UserService, resolvedApiPermissions) {
          UserService.currentUser.userApiPermissions = [];
          if (resolvedApiPermissions && resolvedApiPermissions.data) {
            _.forEach(_.keys(resolvedApiPermissions.data), (permission) => {
              _.forEach(resolvedApiPermissions.data[permission], (right) => {
                const permissionName = 'API-' + permission + '-' + right;
                UserService.currentUser.userApiPermissions.push(_.toLower(permissionName));
              });
            });
          }
          UserService.reloadPermissions();
        },
      },
    })
    .state('management.apis.new', {
      url: '/new',
      component: 'ngApiCreationGetStartedComponent',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create',
        },
      },
    })
    .state('management.apis.new-import', {
      url: '/new/import/:definitionVersion',
      template: require('./creation/newApiImport.html'),
      controller: 'NewApiImportController',
      controllerAs: '$ctrl',
      resolve: {
        policies: (PolicyService: PolicyService) => PolicyService.listSwaggerPolicies().then((response) => response.data),
      },
      data: {
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create',
        },
      },
    })
    .state('management.apis.create-v4-confirmation', {
      url: '/new/create/v4/confirmation/:apiId',
      component: 'ngApiCreationV4ConfirmationComponent',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
        },
      },
<<<<<<< HEAD
    })
    .state('management.apis.create-v4', {
      url: '/new/create/v4',
      component: 'ngApiCreationV4Component',
      data: {
        useAngularMaterial: true,
        perms: {
          only: ['environment-api-c'],
=======
      {
        token: 'readOnly',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId).then((res) => res.data?.definition_context?.origin === 'kubernetes');
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
    name: 'management.apis.metadata',
    component: ApiPortalDocumentationMetadataComponent,
    url: '/metadata',
    data: {
      apiPermissions: {
        only: ['api-metadata-r'],
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.members',
    component: ApiGeneralMembersComponent,
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
    name: 'management.apis.groups',
    component: ApiGeneralGroupsComponent,
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
    name: 'management.apis.transferOwnership',
    component: ApiGeneralTransferOwnershipComponent,
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
  {
    name: 'management.apis.cors',
    component: ApiProxyCorsComponent,
    url: '/cors',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-proxy',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.deployments',
    component: ApiProxyDeploymentsComponent,
    url: '/deployments',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-proxy',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.responseTemplates',
    component: ApiProxyResponseTemplatesListComponent,
    url: '/response-templates',
    data: {
      apiPermissions: {
        only: ['api-response_templates-r'],
      },
      docs: {
        page: 'management-api-proxy-response-templates',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.responseTemplateNew',
    component: ApiProxyResponseTemplatesEditComponent,
    url: '/response-template',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.responseTemplateEdit',
    component: ApiProxyResponseTemplatesEditComponent,
    url: '/response-template/:responseTemplateId',
    data: {
      apiPermissions: {
        only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
      },
      docs: {
        page: 'management-api-proxy-response-templates',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.audit',
    component: ApiAuditComponent,
    url: '/audit',
    data: {
      requireLicense: {
        license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
        redirect: 'management.apis-list',
      },
      apiPermissions: {
        only: ['api-audit-r'],
      },
      docs: {
        page: 'management-api-audit',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.history',
    component: ApiHistoryComponent,
    url: '/history',
    data: {
      apiPermissions: {
        only: ['api-event-r'],
      },
      docs: {
        page: 'management-api-history',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.events',
    component: ApiEventsComponent,
    url: '/events',
    data: {
      apiPermissions: {
        only: ['api-event-r'],
      },
      docs: {
        page: 'management-api-events',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.notification-settings',
    component: ApiNotificationSettingsListComponent,
    url: '/notification-settings',
    data: {
      apiPermissions: {
        only: ['api-notification-r'],
      },
      docs: {
        page: 'management-api-notifications',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.notification-settings-details',
    component: ApiNotificationSettingsDetailsComponent,
    url: '/notification-settings/:notificationId',
    data: {
      apiPermissions: {
        only: ['api-notification-r', 'api-notification-c', 'api-notification-u'],
      },
      docs: {
        page: 'management-api-notifications',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.alerts',
    component: GioEmptyComponent,
    abstract: true,
    url: '/alerts',
    data: {
      requireLicense: {
        license: { feature: ApimFeature.ALERT_ENGINE },
        redirect: 'management.apis-list',
      },
      apiPermissions: {
        only: ['api-alert-r'],
      },
      useAngularMaterial: true,
    },
    resolve: [
      {
        token: 'status',
        deps: ['AlertService', '$stateParams'],
        resolveFn: (AlertService: AlertService, $stateParams) => {
          return AlertService.getStatus(AlertScope.API, $stateParams.apiId).then((response) => response.data);
>>>>>>> 2a74f294c0 (feat: set v2 doc pages to read only for kube origin)
        },
      },
    })
    .state('management.apis.create-v2', {
      url: '/new/create/v2',
      component: 'apiCreation',
      resolve: {
        groups: (GroupService: GroupService) => GroupService.list().then((response) => response.data),
        tenants: (TenantService: TenantService) => TenantService.list().then((response) => response.data),
        tags: (TagService: TagService) => TagService.list().then((response) => response.data),
      },
      data: {
        perms: {
          only: ['environment-api-c'],
        },
        docs: {
          page: 'management-apis-create-steps',
        },
      },
    })
    .state('management.apis.ng-list', {
      url: '/?q&page&size&order',
      component: 'ngApiList',
      data: {
        useAngularMaterial: true,
        docs: {
          page: 'management-apis',
        },
        ncyBreadcrumb: {
          label: 'APIs',
        },
      },
      params: {
        page: {
          value: '1',
          dynamic: true,
        },
        q: {
          dynamic: true,
        },
        size: {
          value: '10',
          dynamic: true,
        },
        order: {
          dynamic: true,
        },
      },
    });
}
