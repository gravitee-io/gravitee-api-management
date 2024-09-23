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

import { Ng2StateDeclaration } from '@uirouter/angular';
import { StateParams } from '@uirouter/core';

import { ApiNavigationComponent } from './api-navigation/api-navigation.component';
import { ApiGeneralInfoComponent } from './general/details/api-general-info.component';
import { ApiGeneralPlanEditComponent } from './general/plans/edit/api-general-plan-edit.component';
import { ApiGeneralPlanListComponent } from './general/plans/list/api-general-plan-list.component';
import { ApiGeneralSubscriptionListComponent } from './general/subscriptions/list/api-general-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApiGeneralSubscriptionEditComponent } from './general/subscriptions/edit/api-general-subscription-edit.component';
import { ApiEntrypointsV4GeneralComponent } from './entrypoints-v4/api-entrypoints-v4-general.component';
import { ApiEndpointComponent } from './endpoints-v4/endpoint/api-endpoint.component';
import { ApiEntrypointsV4EditComponent } from './entrypoints-v4/edit/api-entrypoints-v4-edit.component';
import { ApiPropertiesOldComponent } from './proxy/properties-ng/api-properties-old.component';
import { ApiResourcesComponent } from './proxy/resources-ng/api-resources.component';
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
import { ApiListComponent } from './list/api-list.component';
import { ApiNotificationSettingsListComponent } from './notification-settings/notification-settings-list/api-notification-settings-list.component';
import { ApiNotificationSettingsDetailsComponent } from './notification-settings/notofication-settings-details/api-notification-settings-details.component';
import { ApiCreationGetStartedComponent } from './creation-get-started/api-creation-get-started.component';
import { ApiCreationV4Component } from './creation-v4/api-creation-v4.component';
import { ApiCreationV4ConfirmationComponent } from './creation-v4/api-creation-v4-confirmation.component';
import { ApiCreationV2Component } from './creation-v2/steps/api-creation-v2.component';
import { ApiPropertiesComponent } from './proxy/properties/properties/api-properties.component';
import { ApiDocumentationV4Component } from './documentation-v4/api-documentation-v4.component';
import { ApiDocumentationV4EditPageComponent } from './documentation-v4/documentation-edit-page/api-documentation-v4-edit-page.component';
import { ApiDynamicPropertiesComponent } from './proxy/properties/dynamic-properties/api-dynamic-properties.component';
import { ApiRuntimeLogsDetailsComponent } from './runtime-logs-v4/runtime-logs-details/api-runtime-logs-details.component';

import { ApiService } from '../../services/api.service';
import { GioEmptyComponent } from '../../shared/components/gio-empty/gio-empty.component';
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
import TenantService from '../../services/tenant.service';
import TagService from '../../services/tag.service';
import { ApimFeature } from '../../shared/components/gio-license/gio-license-data';

// New Angular routing
export const states: Ng2StateDeclaration[] = [
  {
    name: 'management.apis-list',
    url: '/apis/?q&page&size&order',
    component: ApiListComponent,
    data: {
      useAngularMaterial: true,
      docs: {
        page: 'management-apis',
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
  },

  /**
   * New API
   */
  {
    name: 'management.apis-new',
    url: '/apis/new',
    component: ApiCreationGetStartedComponent,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-api-c'],
      },
      docs: {
        page: 'management-apis-create',
      },
    },
  },
  {
    name: 'management.apis-new-v4',
    url: '/apis/new/v4',
    component: ApiCreationV4Component,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-api-c'],
      },
    },
  },
  {
    name: 'management.apis-new-v4-confirmation',
    url: '/apis/new/v4/:apiId',
    component: ApiCreationV4ConfirmationComponent,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-api-c'],
      },
    },
  },
  {
    name: 'management.apis-new-v2',
    url: '/apis/new/v2',
    component: ApiCreationV2Component,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-api-c'],
      },
    },
    resolve: [
      {
        token: 'groups',
        deps: ['GroupService'],
        resolveFn: (groupService: GroupService) => groupService.list().then((response) => response.data),
      },
      {
        token: 'tenants',
        deps: ['TenantService'],
        resolveFn: (tenantService: TenantService) => tenantService.list().then((response) => response.data),
      },
      {
        token: 'tags',
        deps: ['TagService'],
        resolveFn: (tagService: TagService) => tagService.list().then((response) => response.data),
      },
    ],
  },

  /**
   * Existing API
   */
  {
    name: 'management.apis',
    url: '/apis/:apiId',
    abstract: true,
    component: ApiNavigationComponent,
  },

  /**
   * Common Api state
   */
  {
    name: 'management.apis.messages',
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
    name: 'management.apis.general',
    url: '/general',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: ApiGeneralInfoComponent,
  },
  {
    name: 'management.apis.plans',
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
    name: 'management.apis.plan',
    url: '/plan',
    component: GioEmptyComponent,
    abstract: true,
  },
  {
    name: 'management.apis.plan.new',
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
    name: 'management.apis.plan.edit',
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
    name: 'management.apis.subscriptions',
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
    name: 'management.apis.subscription',
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
    name: 'management.apis.subscription.edit',
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
    name: 'management.apis.documentation',
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
    name: 'management.apis.alerts.list',
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
    name: 'management.apis.alerts.alertnew',
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
    name: 'management.apis.alerts.editalert',
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
    name: 'management.apis.policies-v1',
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
    name: 'management.apis.properties-v1',
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
    name: 'management.apis.resources-v1',
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
    name: 'management.apis.entrypoints-v2',
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
    name: 'management.apis.endpoints-v2',
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
    name: 'management.apis.endpoint-v2',
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
    name: 'management.apis.endpoint-group-v2',
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
    name: 'management.apis.failover-v2',
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
    name: 'management.apis.healthcheck-v2',
    component: ApiProxyHealthCheckComponent,
    url: '/v2/healthcheck',
    data: {
      apiPermissions: {
        only: ['api-health-r'],
      },
      docs: {
        page: 'management-api-health-check-configure',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: 'management.apis.healthcheck-dashboard-v2',
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
    name: 'management.apis.healthcheck-log-v2',
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
    name: 'management.apis.analytics-overview-v2',
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
    name: 'management.apis.analytics-logs-v2',
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
    name: 'management.apis.analytics-logs-configuration-v2',
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
    name: 'management.apis.analytics-log-v2',
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
    name: 'management.apis.analytics-path-mappings-v2',
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
    name: 'management.apis.analytics-alerts-v2',
    component: ApiAlertsDashboardComponent,
    url: '/v2/analytics-alerts',
    data: {
      requireLicense: {
        license: { feature: ApimFeature.ALERT_ENGINE },
        redirect: 'management.apis-list',
      },
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
    name: 'management.apis.properties-old',
    url: '/properties-old',
    component: ApiPropertiesOldComponent,
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
    name: 'management.apis.properties',
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
    name: 'management.apis.dynamicProperties',
    url: '/dynamic-properties',
    component: ApiDynamicPropertiesComponent,
    data: {
      useAngularMaterial: true,
      apiPermissions: {
        only: ['api-definition-r'],
      },
    },
  },
  {
    name: 'management.apis.resources',
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
    name: 'management.apis.documentationV4',
    url: '/documentation-v4?parentId',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    params: {
      parentId: {
        type: 'string',
        value: 'ROOT',
        dynamic: true,
      },
    },
    component: ApiDocumentationV4Component,
  },
  {
    name: 'management.apis.documentationV4-create',
    url: '/documentation-v4/create?parentId',
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-documentation-c'],
      },
    },
    params: {
      parentId: {
        type: 'string',
        value: 'ROOT',
        dynamic: true,
      },
    },
    component: ApiDocumentationV4EditPageComponent,
  },
  {
    name: 'management.apis.documentationV4-edit',
    url: '/documentation-v4/:pageId/edit',
    data: {
      useAngularMaterial: true,
      docs: null,
      apiPermissions: {
        only: ['api-documentation-u', 'api-documentation-r'],
      },
    },
    params: {
      pageId: {
        type: 'string',
        dynamic: true,
      },
    },
    component: ApiDocumentationV4EditPageComponent,
  },
  {
    name: 'management.apis.policyStudio',
    url: '/policy-studio',
    data: {
      useAngularMaterial: true,
      docs: null,
    },
    component: ApiV4PolicyStudioDesignComponent,
  },
  {
    name: 'management.apis.runtimeLogs',
    url: '/runtime-logs?page&perPage&from&to&applicationIds&planIds&methods&statuses',
    data: {
      apiPermissions: {
        only: ['api-log-r'],
      },
      docs: {
        page: 'management-api-logs',
      },
      useAngularMaterial: true,
    },
    params: {
      page: {
        type: 'int',
        value: 1,
        dynamic: true,
      },
      perPage: {
        type: 'int',
        value: 10,
        dynamic: true,
      },
      from: {
        type: 'int',
        dynamic: true,
      },
      to: {
        type: 'int',
        dynamic: true,
      },
      applicationIds: {
        type: 'string',
        dynamic: true,
      },
      planIds: {
        type: 'string',
        dynamic: true,
      },
      methods: {
        type: 'string',
        dynamic: true,
      },
      statuses: {
        type: 'string',
        dynamic: true,
      },
    },
    component: ApiRuntimeLogsComponent,
  },
  {
    name: 'management.apis.runtimeLogs-details',
    url: '/runtime-logs/:requestId',
    data: {
      apiPermissions: {
        only: ['api-log-r'],
      },
      docs: {
        page: 'management-api-logs',
      },
      useAngularMaterial: true,
    },
    component: ApiRuntimeLogsDetailsComponent,
  },
  {
    name: 'management.apis.runtimeLogs-settings',
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
    name: 'management.apis.entrypoints',
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
    name: 'management.apis.entrypoints-edit',
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
    name: 'management.apis.endpoint-groups',
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
    name: 'management.apis.endpoint-group',
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
    name: 'management.apis.endpoint-group-new',
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
    name: 'management.apis.endpoint-new',
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
    name: 'management.apis.endpoint-edit',
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
