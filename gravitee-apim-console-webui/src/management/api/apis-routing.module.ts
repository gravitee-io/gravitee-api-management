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

import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';

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
import { ApiResourcesComponent } from './proxy/resources-ng/api-resources.component';
import { ApiGeneralMembersComponent } from './general/user-group-access/members/api-general-members.component';
import { ApiGeneralGroupsComponent } from './general/user-group-access/groups/api-general-groups.component';
import { ApiGeneralTransferOwnershipComponent } from './general/user-group-access/transfer-ownership/api-general-transfer-ownership.component';
import { ApiPortalDocumentationMetadataComponent } from './general/documentation/metadata/api-portal-documentation-metadata.component';
import { ApiProxyEntrypointsComponent } from './proxy/entrypoints/api-proxy-entrypoints.component';
import { ApiCorsComponent } from './cors/api-cors.component';
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
import { ApiV1PoliciesComponent } from './policy-studio-v1/policies/policies.component';
import { ApiEventsComponent } from './audit/events/api-events.component';
import { ApiEndpointGroupComponent } from './endpoints-v4/endpoint-group/api-endpoint-group.component';
import { ApiEndpointGroupCreateComponent } from './endpoints-v4/endpoint-group/create/api-endpoint-group-create.component';
import { ApiRuntimeLogsSettingsComponent } from './api-traffic-v4/runtime-logs-settings/api-runtime-logs-settings.component';
import { ApiRuntimeLogsComponent } from './api-traffic-v4/runtime-logs/api-runtime-logs.component';
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
import { ApiRuntimeLogsDetailsComponent } from './api-traffic-v4/runtime-logs-details/api-runtime-logs-details.component';
import { HasApiPermissionGuard } from './has-api-permission.guard';
import { GioPolicyStudioLayoutComponent } from './policy-studio-v2/gio-policy-studio-layout.component';
import { PolicyStudioDesignComponent } from './policy-studio-v2/design/policy-studio-design.component';
import { PolicyStudioConfigComponent } from './policy-studio-v2/config/policy-studio-config.component';
import { PolicyStudioDebugComponent } from './policy-studio-v2/debug/policy-studio-debug.component';

import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';
import { AlertsComponent } from '../../components/alerts/alerts.component';
import { AlertComponent } from '../../components/alerts/alert/alert.component';
import { MessagesComponent } from '../messages/messages.component';
import { ApimFeature } from '../../shared/components/gio-license/gio-license-data';
import { HasLicenseGuard } from '../../shared/components/gio-license/has-license.guard';

const apisRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: ApiListComponent,
    data: {
      docs: {
        page: 'management-apis',
      },
    },
  },

  /**
   * New API
   */
  {
    path: 'new/v4/:apiId',
    component: ApiCreationV4ConfirmationComponent,
    data: {
      perms: {
        only: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new/v4',
    component: ApiCreationV4Component,
    data: {
      perms: {
        only: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new/v2',
    component: ApiCreationV2Component,
    data: {
      perms: {
        only: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new',
    component: ApiCreationGetStartedComponent,
    data: {
      perms: {
        only: ['environment-api-c'],
      },
      docs: {
        page: 'management-apis-create',
      },
    },
  },

  /**
   * Existing API
   */
  {
    path: ':apiId',
    component: ApiNavigationComponent,
    canActivate: [HasApiPermissionGuard],
    canActivateChild: [HasApiPermissionGuard, HasLicenseGuard],
    canDeactivate: [HasApiPermissionGuard],
    children: [
      {
        path: '',
        component: ApiGeneralInfoComponent,
      },

      /**
       * Common Api state
       */
      {
        path: 'messages',
        data: {
          docs: {
            page: 'management-messages',
          },
        },
        component: MessagesComponent,
      },
      {
        path: 'plans',
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-plan-r'],
          },
        },
        component: ApiGeneralPlanListComponent,
      },
      {
        path: 'plans/new',
        component: ApiGeneralPlanEditComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-plan-c'],
          },
        },
      },
      {
        path: 'plans/:planId',
        component: ApiGeneralPlanEditComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-plan-r'],
          },
        },
      },
      {
        path: 'subscriptions',
        component: ApiGeneralSubscriptionListComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-subscription-r'],
          },
        },
      },
      {
        path: 'subscriptions/:subscriptionId',
        component: ApiGeneralSubscriptionEditComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-subscription-r', 'api-subscription-u'],
          },
        },
      },
      {
        path: 'documentation/new',
        component: DocumentationNewPageComponent,
        data: {
          docs: {
            page: 'management-api-documentation',
          },
          apiPermissions: {
            only: ['api-documentation-r'],
          },
        },
      },
      {
        path: 'documentation/import',
        component: DocumentationImportPagesComponent,
        data: {
          docs: {
            page: 'management-api-documentation',
          },
          apiPermissions: {
            only: ['api-documentation-c'],
          },
        },
      },
      {
        path: 'documentation/:pageId',
        component: DocumentationEditPageComponent,
        data: {
          docs: {
            page: 'management-api-documentation',
          },
          apiPermissions: {
            only: ['api-documentation-c'],
          },
        },
      },
      {
        path: 'documentation',
        component: DocumentationManagementComponent,
        data: {
          docs: {
            page: 'management-api-documentation',
          },
          apiPermissions: {
            only: ['api-documentation-r'],
          },
        },
      },
      {
        path: 'metadata',
        component: ApiPortalDocumentationMetadataComponent,
        data: {
          apiPermissions: {
            only: ['api-metadata-r'],
          },
        },
      },
      {
        path: 'members',
        component: ApiGeneralMembersComponent,
        data: {
          apiPermissions: {
            only: ['api-member-r'],
          },
          docs: {
            page: 'management-api-members',
          },
        },
      },
      {
        path: 'groups',
        component: ApiGeneralGroupsComponent,
        data: {
          apiPermissions: {
            only: ['api-member-r'],
          },
          docs: {
            page: 'management-api-members',
          },
        },
      },
      {
        path: 'transfer-ownership',
        component: ApiGeneralTransferOwnershipComponent,
        data: {
          apiPermissions: {
            only: ['api-member-r'],
          },
          docs: {
            page: 'management-api-members',
          },
        },
      },
      {
        path: 'cors',
        component: ApiCorsComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'response-templates/new',
        component: ApiProxyResponseTemplatesEditComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
          },
        },
      },
      {
        path: 'response-templates/:responseTemplateId',
        component: ApiProxyResponseTemplatesEditComponent,
        data: {
          apiPermissions: {
            only: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
          },
          docs: {
            page: 'management-api-proxy-response-templates',
          },
        },
      },
      {
        path: 'response-templates',
        component: ApiProxyResponseTemplatesListComponent,
        data: {
          apiPermissions: {
            only: ['api-response_templates-r'],
          },
          docs: {
            page: 'management-api-proxy-response-templates',
          },
        },
      },
      {
        path: 'audit',
        component: ApiAuditComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-audit-r'],
          },
          docs: {
            page: 'management-api-audit',
          },
        },
      },
      {
        path: 'history',
        component: ApiHistoryComponent,
        data: {
          apiPermissions: {
            only: ['api-event-r'],
          },
          docs: {
            page: 'management-api-history',
          },
        },
      },
      {
        path: 'events',
        component: ApiEventsComponent,
        data: {
          apiPermissions: {
            only: ['api-event-r'],
          },
          docs: {
            page: 'management-api-events',
          },
        },
      },
      {
        path: 'notification-settings',
        component: ApiNotificationSettingsListComponent,
        data: {
          apiPermissions: {
            only: ['api-notification-r'],
          },
          docs: {
            page: 'management-api-notifications',
          },
        },
      },
      {
        path: 'notification-settings/:notificationId',
        component: ApiNotificationSettingsDetailsComponent,
        data: {
          apiPermissions: {
            only: ['api-notification-r', 'api-notification-c', 'api-notification-u'],
          },
          docs: {
            page: 'management-api-notifications',
          },
        },
      },
      {
        path: 'alerts/new',
        component: AlertComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-alert-c'],
          },
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'alerts/:alertId',
        component: AlertComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-alert-r'],
          },
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'alerts',
        component: AlertsComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-alert-r'],
          },
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'analytics-alerts',
        component: ApiAlertsDashboardComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-alert-r'],
          },
          docs: {
            page: 'management-api-alerts',
          },
        },
      },

      /**
       * V1 Api state only
       */
      {
        path: 'v1/policies',
        component: ApiV1PoliciesComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-policies',
          },
        },
      },
      {
        path: 'v1/properties',
        component: ApiV1PropertiesComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-properties',
          },
        },
      },
      {
        path: 'v1/resources',
        component: ApiV1ResourcesComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-resources',
          },
        },
      },

      /**
       * V1 & V2 Api state only
       */
      {
        path: 'v2/entrypoints',
        component: ApiProxyEntrypointsComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r', 'api-health-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'v2/endpoints/:groupName/:endpointName',
        component: ApiProxyGroupEndpointEditComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v2/endpoints/:groupName',
        component: ApiProxyGroupEditComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-group',
          },
        },
      },
      {
        path: 'v2/endpoints',
        component: ApiProxyEndpointListComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v2/failover',
        component: ApiProxyFailoverComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'v2/healthcheck',
        component: ApiProxyHealthCheckComponent,
        data: {
          apiPermissions: {
            only: ['api-health-c'],
          },
          docs: {
            page: 'management-api-health-check-configure',
          },
        },
      },
      {
        path: 'v2/healthcheck-dashboard/:logId',
        component: ApiHealthCheckLogComponent,
        data: {
          apiPermissions: {
            only: ['api-health-r'],
          },
        },
      },
      {
        path: 'v2/healthcheck-dashboard',
        component: ApiHealthCheckDashboardComponent,
        data: {
          apiPermissions: {
            only: ['api-health-r'],
          },
          docs: {
            page: 'management-api-health-check',
          },
        },
      },
      {
        path: 'v2/analytics-overview',
        component: ApiAnalyticsOverviewComponent,
        data: {
          apiPermissions: {
            only: ['api-analytics-r'],
          },
          docs: {
            page: 'management-api-analytics',
          },
        },
      },
      {
        path: 'v2/analytics-logs/configuration',
        component: ApiLogsConfigurationComponent,
        data: {
          apiPermissions: {
            only: ['api-log-u'],
          },
          docs: {
            page: 'management-api-logging-configuration',
          },
        },
      },
      {
        path: 'v2/analytics-logs/:logId',
        component: ApiAnalyticsLogComponent,
        data: {
          apiPermissions: {
            only: ['api-log-r'],
          },
          docs: {
            page: 'management-api-log',
          },
        },
      },
      {
        path: 'v2/analytics-logs',
        component: ApiAnalyticsLogsComponent,
        data: {
          apiPermissions: {
            only: ['api-log-r'],
          },
          docs: {
            page: 'management-api-logs',
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
        path: 'v2/path-mappings',
        component: ApiPathMappingsComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-pathMappings',
          },
        },
      },
      {
        path: 'v2/policy-studio',
        component: GioPolicyStudioLayoutComponent,
        data: {
          menu: null,
          docs: null,
        },
        children: [
          {
            path: 'design',
            component: PolicyStudioDesignComponent,
            data: {
              menu: null,
              docs: {
                page: 'management-api-policy-studio-design',
              },
            },
          },
          {
            path: 'config',
            component: PolicyStudioConfigComponent,
            data: {
              menu: null,
              docs: {
                page: 'management-api-policy-studio-config',
              },
            },
          },
          {
            path: 'debug',
            component: PolicyStudioDebugComponent,
            data: {
              menu: null,
              docs: {
                page: 'management-api-policy-studio-try-it',
              },
            },
          },
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'design',
          },
        ],
      },

      /**
       * V2 & V4 Api state only
       */
      {
        path: 'properties',
        component: ApiPropertiesComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-policy-studio-properties',
          },
        },
      },
      {
        path: 'dynamic-properties',
        component: ApiDynamicPropertiesComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
        },
      },
      {
        path: 'resources',
        component: ApiResourcesComponent,
        data: {
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
        path: 'v4/documentation',
        data: {
          docs: null,
        },
        component: ApiDocumentationV4Component,
      },
      {
        path: 'v4/documentation/new',
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-documentation-c'],
          },
        },
        component: ApiDocumentationV4EditPageComponent,
      },
      {
        path: 'v4/documentation/:pageId',
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-documentation-u', 'api-documentation-r'],
          },
        },
        component: ApiDocumentationV4EditPageComponent,
      },
      {
        path: 'v4/policy-studio',
        data: {
          docs: null,
        },
        component: ApiV4PolicyStudioDesignComponent,
      },
      {
        path: 'v4/runtime-logs',
        data: {
          apiPermissions: {
            only: ['api-log-r'],
          },
          docs: {
            page: 'management-api-logs',
          },
        },
        component: ApiRuntimeLogsComponent,
      },
      {
        path: 'v4/runtime-logs/:requestId',
        data: {
          apiPermissions: {
            only: ['api-log-r'],
          },
          docs: {
            page: 'management-api-logs',
          },
        },
        component: ApiRuntimeLogsDetailsComponent,
      },
      {
        path: 'v4/runtime-logs-settings',
        data: {
          apiPermissions: {
            only: ['api-log-u'],
          },
          docs: {
            page: 'management-api-logs',
          },
        },
        component: ApiRuntimeLogsSettingsComponent,
      },
      {
        path: 'v4/entrypoints',
        component: ApiEntrypointsV4GeneralComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-definition-r'],
          },
        },
      },
      {
        path: 'v4/cors',
        component: ApiCorsComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-definition-r'],
          },
        },
      },
      {
        path: 'v4/entrypoints/:entrypointId',
        component: ApiEntrypointsV4EditComponent,
        data: {
          docs: null,
          apiPermissions: {
            only: ['api-definition-u'],
          },
        },
      },
      {
        path: 'v4/endpoints',
        component: ApiEndpointGroupsComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v4/endpoints/new',
        component: ApiEndpointGroupCreateComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-u'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v4/endpoints/:groupIndex',
        component: ApiEndpointGroupComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v4/endpoints/:groupIndex/new',
        component: ApiEndpointComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-u'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v4/endpoints/:groupIndex/:endpointIndex',
        component: ApiEndpointComponent,
        data: {
          apiPermissions: {
            only: ['api-definition-u'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(apisRoutes)],
  exports: [RouterModule],
})
export class ApisRoutingModule {}
