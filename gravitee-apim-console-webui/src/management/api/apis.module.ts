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
import { ApiRuntimeLogsV4Module } from './runtime-logs-v4/api-runtime-logs-v4.module';
import { ApiNgNavigationComponent } from './api-ng-navigation/api-ng-navigation.component';
import { ApiGeneralInfoComponent } from './general/details/api-general-info.component';
import { ApiGeneralPlanEditComponent } from './general/plans/edit/api-general-plan-edit.component';
import { ApiGeneralPlanListComponent } from './general/plans/list/api-general-plan-list.component';
import { ApiGeneralSubscriptionListComponent } from './general/subscriptions/list/api-general-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApisGeneralModule } from './general/apis-general.module';
import { ApiGeneralSubscriptionEditComponent } from './general/subscriptions/edit/api-general-subscription-edit.component';
import { ApiEndpointsModule } from './endpoints-v4/api-endpoints.module';
import { ApiEntrypointsV4GeneralComponent } from './entrypoints-v4/api-entrypoints-v4-general.component';
import { ApiEntrypointsV4Module } from './entrypoints-v4/api-entrypoints-v4.module';
import { ApiEndpointComponent } from './endpoints-v4/endpoint/api-endpoint.component';
import { ApiEntrypointsV4EditComponent } from './entrypoints-v4/edit/api-entrypoints-v4-edit.component';
import { ApiPropertiesComponent } from './proxy/properties-ng/api-properties.component';
import { ApiResourcesComponent } from './proxy/resources-ng/api-resources.component';
import { GioPolicyStudioRoutingModule } from './policy-studio/gio-policy-studio-routing.module';
import { ApiGeneralMembersComponent } from './general/user-group-access/members/api-general-members.component';
import { ApiGeneralGroupsComponent } from './general/user-group-access/groups/api-general-groups.component';
import { ApiGeneralTransferOwnershipComponent } from './general/user-group-access/transfer-ownership/api-general-transfer-ownership.component';
import { ApiPortalDocumentationMetadataComponent } from './general/documentation/metadata/api-portal-documentation-metadata.component';
import { ApiProxyEntrypointsComponent } from './proxy/entrypoints/api-proxy-entrypoints.component';
import { ApiProxyCorsComponent } from './proxy/cors/api-proxy-cors.component';
import { ApiProxyDeploymentsComponent } from './proxy/deployments/api-proxy-deployments.component';
import { ApiProxyResponseTemplatesListComponent } from './proxy/response-templates/list/api-proxy-response-templates-list.component';
import { ApiProxyResponseTemplatesEditComponent } from './proxy/response-templates/edit/api-proxy-response-templates-edit.component';
import { ApiProxyEndpointListComponent } from './proxy/endpoints/list/api-proxy-endpoint-list.component';
import { ApiEndpointGroupsComponent } from './endpoints-v4/endpoint-groups/api-endpoint-groups.component';
import { ApiProxyGroupEndpointEditComponent } from './proxy/endpoints/groups/endpoint/edit/api-proxy-group-endpoint-edit.component';
import { ApiProxyGroupEditComponent } from './proxy/endpoints/groups/edit/api-proxy-group-edit.component';
import { ApiProxyFailoverComponent } from './proxy/failover/api-proxy-failover.component';
import { ApiProxyHealthCheckComponent } from './proxy/health-check/api-proxy-health-check.component';
import { ApiHealthCheckDashboardComponent } from './proxy/health-check-dashboard/healthcheck-dashboard.component';
import { ApiHealthCheckLogComponent } from './proxy/health-check-dashboard/healthcheck-log.controller';
import { ApiAnalyticsOverviewComponent } from './analytics/overview/analytics-overview.component';
import { ApiAnalyticsLogsComponent } from './analytics/logs/analytics-logs.component';
import { ApiLogsConfigurationComponent } from './analytics/logs/configuration/api-logs-configuration.component';
import { ApiAnalyticsLogComponent } from './analytics/logs/analytics-log.component';
import { ApiPathMappingsComponent } from './analytics/pathMappings/api-path-mappings.component';
import { ApiAlertsDashboardComponent } from './analytics/alerts/api-alerts-dashboard.component';
import { ApiAuditModule } from './audit/api-audit.module';
import { ApiAuditComponent } from './audit/general/audit.component';
import { ApiHistoryComponent } from './audit/history/apiHistory.component';
import { ApiV1PropertiesComponent } from './proxy/properties-v1/properties.component';
import { ApiV1ResourcesComponent } from './proxy/resources-v1/resources.component';
import { ApiV1PoliciesComponent } from './design/policies/policies.component';
import { ApiEventsComponent } from './audit/events/api-events.component';
import { ApiEndpointGroupComponent } from './endpoints-v4/endpoint-group/api-endpoint-group.component';
import { ApiEndpointGroupCreateComponent } from './endpoints-v4/endpoint-group/create/api-endpoint-group-create.component';
import { ApiRuntimeLogsSettingsComponent } from './runtime-logs-v4/runtime-logs-settings/api-runtime-logs-settings.component';
import { ApiRuntimeLogsComponent } from './runtime-logs-v4/runtime-logs/api-runtime-logs.component';
import { NotificationsListModule } from './notifications/notifications-list/notifications-list.module';
import { NotificationsListComponent } from './notifications/notifications-list/notifications-list.component';

import { NotificationsComponent } from '../../components/notifications/notifications.component';
import { Scope } from '../../entities/scope';
import NotificationSettingsService from '../../services/notificationSettings.service';
import { ApiService } from '../../services/api.service';
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
import { Scope as AlertScope } from '../../entities/alert';
import AlertService from '../../services/alert.service';
import NotifierService from '../../services/notifier.service';
import { AlertsComponent } from '../../components/alerts/alerts.component';
import { AlertComponent } from '../../components/alerts/alert/alert.component';
import ResourceService from '../../services/resource.service';
import { MessagesComponent } from '../messages/messages.component';

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

  /**
   * Common Api state
   */
  {
    name: 'management.apis.ng.messages',
    url: '/messages',
    data: {
      useAngularMaterial: true,
      docs: {
        page: 'management-messages',
      },
    },
    component: MessagesComponent,
  },
  {
    name: 'management.apis.ng.general',
    url: '/general',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: ApiGeneralInfoComponent,
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
    component: ApiGeneralPlanListComponent,
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
    component: ApiGeneralPlanEditComponent,
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
    component: ApiGeneralPlanEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-plan-r'],
      },
    },
  },
  {
    name: 'management.apis.ng.subscriptions',
    url: '/subscriptions?page&size&plan&application&status&apiKey',
    component: ApiGeneralSubscriptionListComponent,
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
    component: ApiGeneralSubscriptionEditComponent,
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-subscription-r', 'api-subscription-u'],
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
    name: 'management.apis.ng.metadata',
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
    name: 'management.apis.ng.members',
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
    name: 'management.apis.ng.groups',
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
    name: 'management.apis.ng.transferOwnership',
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
    name: 'management.apis.ng.cors',
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
    name: 'management.apis.ng.deployments',
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
    name: 'management.apis.ng.responseTemplates',
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
    name: 'management.apis.ng.responseTemplateNew',
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
    name: 'management.apis.ng.responseTemplateEdit',
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
    name: 'management.apis.ng.audit',
    component: ApiAuditComponent,
    url: '/audit',
    data: {
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
    name: 'management.apis.ng.history',
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
    name: 'management.apis.ng.events',
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
    name: 'management.apis.ng.notifications',
    component: NotificationsComponent,
    url: '/notifications',
    data: {
      apiPermissions: {
        only: ['api-notification-r'],
      },
      useAngularMaterial: true,
    },
    resolve: [
      {
        token: 'resolvedHookScope',
        resolveFn: () => {
          return Scope.API;
        },
      },
      {
        token: 'resolvedHooks',
        deps: ['NotificationSettingsService'],
        resolveFn: (NotificationSettingsService: NotificationSettingsService) => {
          return NotificationSettingsService.getHooks(Scope.API).then((response) => response.data);
        },
      },
      {
        token: 'resolvedNotifiers',
        deps: ['NotificationSettingsService', '$stateParams'],
        resolveFn: (NotificationSettingsService: NotificationSettingsService, $stateParams) => {
          return NotificationSettingsService.getNotifiers(Scope.API, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'notificationSettings',
        deps: ['NotificationSettingsService', '$stateParams'],
        resolveFn: (NotificationSettingsService: NotificationSettingsService, $stateParams) => {
          return NotificationSettingsService.getNotificationSettings(Scope.API, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'api',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId).then((response) => response.data);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.notifications-ng',
    component: NotificationsListComponent,
    url: '/notifications-ng',
    data: {
      apiPermissions: {
        only: ['api-notification-r'],
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.notifications.notification',
    component: NotificationsComponent,
    url: '/:notificationId',
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
    name: 'management.apis.ng.alerts',
    component: GioEmptyComponent,
    abstract: true,
    url: '/alerts',
    data: {
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
        },
      },
      {
        token: 'notifiers',
        deps: ['NotifierService'],
        resolveFn: (NotifierService: NotifierService) => {
          return NotifierService.list().then((response) => response.data);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.alerts.list',
    component: AlertsComponent,
    url: '/',
    data: {
      apiPermissions: {
        only: ['api-alert-r'],
      },
      docs: {
        page: 'management-alerts',
      },
      useAngularMaterial: true,
    },
    resolve: [
      {
        token: 'alerts',
        deps: ['AlertService', '$stateParams'],
        resolveFn: (AlertService: AlertService, $stateParams) => {
          return AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.alerts.alertnew',
    component: AlertComponent,
    url: '/create',
    data: {
      apiPermissions: {
        only: ['api-alert-c'],
      },
      docs: {
        page: 'management-alerts',
      },
      useAngularMaterial: true,
    },
    resolve: [
      {
        token: 'alerts',
        deps: ['AlertService', '$stateParams'],
        resolveFn: (AlertService: AlertService, $stateParams) => {
          return AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'mode',
        resolveFn: () => {
          return 'create';
        },
      },
      {
        token: 'resolvedApi',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.alerts.editalert',
    component: AlertComponent,
    url: '/:alertId?:tab',
    data: {
      apiPermissions: {
        only: ['api-alert-r'],
      },
      docs: {
        page: 'management-alerts',
      },
      useAngularMaterial: true,
    },
    resolve: [
      {
        token: 'alerts',
        deps: ['AlertService', '$stateParams'],
        resolveFn: (AlertService: AlertService, $stateParams) => {
          return AlertService.listAlerts(AlertScope.API, true, $stateParams.apiId).then((response) => response.data);
        },
      },
      {
        token: 'mode',
        resolveFn: () => {
          return 'detail';
        },
      },
      {
        token: 'resolvedApi',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId);
        },
      },
    ],
  },

  /**
   * V1 Api state only
   */
  {
    name: 'management.apis.ng.policies-v1',
    url: '/v1/policies',
    component: ApiV1PoliciesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-policies',
      },
    },
    resolve: [
      {
        token: 'api',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId).then((response) => response.data);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.properties-v1',
    url: '/v1/properties',
    component: ApiV1PropertiesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-properties',
      },
    },
    resolve: [
      {
        token: 'resolvedApi',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId);
        },
      },
    ],
  },
  {
    name: 'management.apis.ng.resources-v1',
    url: '/v1/resources',
    component: ApiV1ResourcesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-resources',
      },
    },
    resolve: [
      {
        token: 'resolvedApi',
        deps: ['ApiService', '$stateParams'],
        resolveFn: (ApiService: ApiService, $stateParams) => {
          return ApiService.get($stateParams.apiId);
        },
      },
      {
        token: 'resolvedResources',
        deps: ['ResourceService'],
        resolveFn: (ResourceService: ResourceService) => {
          return ResourceService.list();
        },
      },
    ],
  },

  /**
   * V1 & V2 Api state only
   */
  {
    name: 'management.apis.ng.entrypoints-v2',
    component: ApiProxyEntrypointsComponent,
    url: '/v2/entrypoints',
    data: {
      apiPermissions: {
        only: ['api-definition-r', 'api-health-r'],
      },
      docs: {
        page: 'management-api-proxy',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.endpoints-v2',
    component: ApiProxyEndpointListComponent,
    url: '/v2/endpoints',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.endpoint-v2',
    component: ApiProxyGroupEndpointEditComponent,
    url: '/v2/groups/:groupName/endpoints/:endpointName',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-proxy-endpoints',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.endpoint-group-v2',
    component: ApiProxyGroupEditComponent,
    url: '/v2/groups/:groupName',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-proxy-group',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.failover-v2',
    component: ApiProxyFailoverComponent,
    url: '/v2/failover',
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
    name: 'management.apis.ng.healthcheck-v2',
    component: ApiProxyHealthCheckComponent,
    url: '/v2/healthcheck',
    data: {
      apiPermissions: {
        only: ['api-health-c'],
      },
      docs: {
        page: 'management-api-health-check-configure',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.healthcheck-dashboard-v2',
    component: ApiHealthCheckDashboardComponent,
    url: '/v2/healthcheck-dashboard?from&to&page&size',
    data: {
      apiPermissions: {
        only: ['api-health-r'],
      },
      docs: {
        page: 'management-api-health-check',
      },
      useAngularMaterial: true,
    },
    params: {
      from: {
        type: 'int',
        dynamic: true,
      },
      to: {
        type: 'int',
        dynamic: true,
      },
      page: {
        type: 'int',
        dynamic: true,
      },
      size: {
        type: 'int',
        dynamic: true,
      },
    },
  },
  {
    name: 'management.apis.ng.healthcheck-log-v2',
    component: ApiHealthCheckLogComponent,
    url: '/v2/logs/:logId',
    data: {
      apiPermissions: {
        only: ['api-health-r'],
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.analytics-overview-v2',
    component: ApiAnalyticsOverviewComponent,
    url: '/v2/analytics-overview?from&to&q&dashboard',
    data: {
      apiPermissions: {
        only: ['api-analytics-r'],
      },
      docs: {
        page: 'management-api-analytics',
      },
      useAngularMaterial: true,
    },
    params: {
      from: {
        type: 'int',
        dynamic: true,
      },
      to: {
        type: 'int',
        dynamic: true,
      },
      q: {
        type: 'string',
        dynamic: true,
      },
      dashboard: {
        type: 'string',
        dynamic: true,
      },
    },
  },
  {
    name: 'management.apis.ng.analytics-logs-v2',
    component: ApiAnalyticsLogsComponent,
    url: '/v2/analytics-logs?from&to&q&page&size',
    data: {
      apiPermissions: {
        only: ['api-log-r'],
      },
      docs: {
        page: 'management-api-logs',
      },
      useAngularMaterial: true,
      params: {
        from: {
          type: 'int',
          dynamic: true,
        },
        to: {
          type: 'int',
          dynamic: true,
        },
        q: {
          type: 'string',
          dynamic: true,
        },
        page: {
          type: 'int',
          dynamic: true,
        },
        size: {
          type: 'int',
          dynamic: true,
        },
      },
    },
  },
  {
    name: 'management.apis.ng.analytics-logs-configuration-v2',
    component: ApiLogsConfigurationComponent,
    url: '/v2/analytics-logs-configuration',
    data: {
      apiPermissions: {
        only: ['api-log-u'],
      },
      docs: {
        page: 'management-api-logging-configuration',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.analytics-log-v2',
    component: ApiAnalyticsLogComponent,
    url: '/v2/analytics-log/:logId?timestamp&from&to&q&page&size',
    data: {
      apiPermissions: {
        only: ['api-log-r'],
      },
      docs: {
        page: 'management-api-log',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.analytics-path-mappings-v2',
    component: ApiPathMappingsComponent,
    url: '/v2/analytics-path-mappings',
    data: {
      apiPermissions: {
        only: ['api-definition-r'],
      },
      docs: {
        page: 'management-api-pathMappings',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.ng.analytics-alerts-v2',
    component: ApiAlertsDashboardComponent,
    url: '/v2/analytics-alerts',
    data: {
      apiPermissions: {
        only: ['api-alert-r'],
      },
      docs: {
        page: 'management-api-alerts',
      },
      useAngularMaterial: true,
    },
  },

  /**
   * V2 & V4 Api state only
   */
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

  /**
   * V4 Api state only
   */
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
    name: 'management.apis.ng.runtimeLogs',
    url: '/runtime-logs',
    data: {
      apiPermissions: {
        only: ['api-log-r'],
      },
      docs: {
        page: 'management-api-logs',
      },
      useAngularMaterial: true,
    },
    component: ApiRuntimeLogsComponent,
  },
  {
    name: 'management.apis.ng.runtimeLogs-settings',
    url: '/runtime-logs/settings',
    data: {
      apiPermissions: {
        only: ['api-log-u'],
      },
      docs: {
        page: 'management-api-logs',
      },
      useAngularMaterial: true,
    },
    component: ApiRuntimeLogsSettingsComponent,
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
    name: 'management.apis.ng.endpoint-groups',
    url: '/endpoints/groups',
    component: ApiEndpointGroupsComponent,
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
    name: 'management.apis.ng.endpoint-group',
    url: '/endpoints/groups/:groupIndex',
    component: ApiEndpointGroupComponent,
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
    name: 'management.apis.ng.endpoint-group-new',
    url: '/endpoints/groups/new',
    component: ApiEndpointGroupCreateComponent,
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
    name: 'management.apis.ng.endpoint-new',
    url: '/endpoints/groups/:groupIndex/endpoints/new',
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
    url: '/endpoints/groups/:groupIndex/endpoints/:endpointIndex',
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
];

@NgModule({
  declarations: [ApiV1PoliciesComponent],
  imports: [
    ApiAnalyticsModule,
    ApiListModule,
    ApiNavigationModule,
    ApiNgNavigationModule,
    ApiV4PolicyStudioModule,
    ApiRuntimeLogsV4Module,
    ApisGeneralModule,
    ApiProxyModule,
    ApiEntrypointsV4Module,
    ApiEndpointsModule,
    ApiAuditModule,
    NotificationsListModule,
    GioPolicyStudioRoutingModule.withRouting({ stateNamePrefix: 'management.apis.ng.policy-studio-v2' }),
    SpecificJsonSchemaTypeModule,
    DocumentationModule,

    GioEmptyModule,

    UIRouterModule.forChild({ states }),
  ],
})
export class ApisModule {}
