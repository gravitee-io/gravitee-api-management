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
import { ApiGeneralInfoComponent } from './general-info/api-general-info.component';
import { ApiPlanEditComponent } from './plans/edit/api-plan-edit.component';
import { ApiPlanListComponent } from './plans/list/api-plan-list.component';
import { ApiSubscriptionListComponent } from './subscriptions/list/api-subscription-list.component';
import { ApiV4PolicyStudioDesignComponent } from './policy-studio-v4/design/api-v4-policy-studio-design.component';
import { ApiSubscriptionEditComponent } from './subscriptions/edit/api-subscription-edit.component';
import { ApiEntrypointsV4GeneralComponent } from './entrypoints-v4/api-entrypoints-v4-general.component';
import { ApiEndpointComponent } from './endpoints-v4/endpoint/api-endpoint.component';
import { ApiEntrypointsV4EditComponent } from './entrypoints-v4/edit/api-entrypoints-v4-edit.component';
import { ApiResourcesComponent } from './resources-ng/api-resources.component';
import { ApiPortalDocumentationMetadataComponent } from './documentation/metadata/api-portal-documentation-metadata.component';
import { ApiEntrypointsComponent } from './entrypoints/api-entrypoints.component';
import { ApiCorsComponent } from './cors/api-cors.component';
import { ApiResponseTemplatesListComponent } from './response-templates/list/api-response-templates-list.component';
import { ApiResponseTemplatesEditComponent } from './response-templates/edit/api-response-templates-edit.component';
import { ApiEndpointGroupsComponent } from './endpoints-v4/endpoint-groups/api-endpoint-groups.component';
import { ApiFailoverComponent } from './failover/api-failover.component';
import { ApiHealthCheckComponent } from './health-check/api-health-check.component';
import { ApiHealthCheckDashboardComponent } from './health-check-dashboard/healthcheck-dashboard.component';
import { ApiHealthCheckLogComponent } from './health-check-dashboard/healthcheck-log.controller';
import { ApiAnalyticsOverviewComponent } from './analytics/overview/analytics-overview.component';
import { ApiAnalyticsLogsComponent } from './analytics/logs/analytics-logs.component';
import { ApiLogsConfigurationComponent } from './analytics/logs/configuration/api-logs-configuration.component';
import { ApiAnalyticsLogComponent } from './analytics/logs/analytics-log.component';
import { ApiPathMappingsComponent } from './analytics/pathMappings/api-path-mappings.component';
import { ApiAlertsDashboardComponent } from './analytics/alerts/api-alerts-dashboard.component';
import { ApiHistoryComponent } from './audit/history/apiHistory.component';
import { ApiV1PropertiesComponent } from './properties-v1/properties.component';
import { ApiV1ResourcesComponent } from './resources-v1/resources.component';
import { ApiV1PoliciesComponent } from './policy-studio-v1/policies/policies.component';
import { ApiEventsComponent } from './audit/events/api-events.component';
import { ApiEndpointGroupComponent } from './endpoints-v4/endpoint-group/api-endpoint-group.component';
import { ApiEndpointGroupCreateComponent } from './endpoints-v4/endpoint-group/create/api-endpoint-group-create.component';
import { ApiRuntimeLogsSettingsComponent } from './api-traffic-v4/runtime-logs-settings/api-runtime-logs-settings.component';
import { ApiRuntimeLogsComponent } from './api-traffic-v4/runtime-logs/api-runtime-logs.component';
import { ApiListComponent } from './list/api-list.component';
import { ApiCreationGetStartedComponent } from './creation-get-started/api-creation-get-started.component';
import { ApiCreationV4Component } from './creation-v4/api-creation-v4.component';
import { ApiCreationV4ConfirmationComponent } from './creation-v4/api-creation-v4-confirmation.component';
import { ApiCreationV2Component } from './creation-v2/steps/api-creation-v2.component';
import { ApiDocumentationV4Component } from './documentation-v4/api-documentation-v4.component';
import { ApiRuntimeLogsDetailsComponent } from './api-traffic-v4/runtime-logs-details/api-runtime-logs-details.component';
import { ApisGuard } from './apis.guard';
import { GioPolicyStudioLayoutComponent } from './policy-studio-v2/gio-policy-studio-layout.component';
import { PolicyStudioDesignComponent } from './policy-studio-v2/design/policy-studio-design.component';
import { PolicyStudioConfigComponent } from './policy-studio-v2/config/policy-studio-config.component';
import { PolicyStudioDebugComponent } from './policy-studio-v2/debug/policy-studio-debug.component';
import { ApiProxyGroupEndpointEditComponent } from './endpoints/groups/endpoint/edit/api-proxy-group-endpoint-edit.component';
import { ApiProxyGroupEditComponent } from './endpoints/groups/edit/api-proxy-group-edit.component';
import { ApiProxyEndpointListComponent } from './endpoints/list/api-proxy-endpoint-list.component';
import { ApiGeneralMembersComponent } from './user-group-access/members/api-general-members.component';
import { ApiAuditListComponent } from './api-audit-list/api-audit-list.component';
import { ApiAuditLogsComponent } from './api-audit-logs/api-audit-logs.component';
import { ApiDynamicPropertiesV4Component } from './properties/components/dynamic-properties-v4/api-dynamic-properties-v4.component';
import { ApiDeploymentConfigurationComponent } from './deployment-configuration-v4/api-deployment-configuration.component';
import { ApiDynamicPropertiesComponent } from './properties/components/dynamic-properties-v2/api-dynamic-properties.component';
import { ApiPropertiesComponent } from './properties/properties/api-properties.component';
import { ApiNotificationComponent } from './api-notification/api-notification.component';
import { ApiRuntimeAlertsComponent } from './runtime-alerts';
import { ApiDocumentationV4MetadataComponent } from './documentation-v4/documentation-metadata/api-documentation-v4-metadata.component';
import { ApiHistoryV4Component } from './history-v4/api-history-v4.component';
import { ApiFailoverV4Component } from './failover-v4/api-failover-v4.component';
import { ApiImportV4Component } from './import-v4/api-import-v4.component';
import { ApiAnalyticsComponent } from './api-traffic-v4/analytics/api-analytics.component';
import { ApiDocumentationV4DefaultPageComponent } from './documentation-v4/documentation-default-page/api-documentation-v4-default-page.component';
import { DocumentationEditCustomPageComponent } from './documentation-v4/documentation-edit-custom-page/documentation-edit-custom-page.component';
import { DocumentationEditHomepageComponent } from './documentation-v4/documentation-edit-homepage/documentation-edit-homepage.component';

import { DocumentationManagementComponent } from '../../components/documentation/documentation-management.component';
import { DocumentationNewPageComponent } from '../../components/documentation/new-page.component';
import { DocumentationEditPageComponent } from '../../components/documentation/edit-page.component';
import { DocumentationImportPagesComponent } from '../../components/documentation/import-pages.component';
import { AlertsComponent } from '../../components/alerts/alerts.component';
import { AlertComponent } from '../../components/alerts/alert/alert.component';
import { MessagesComponent } from '../messages/messages.component';
import { ApimFeature } from '../../shared/components/gio-license/gio-license-data';
import { HasLicenseGuard } from '../../shared/components/gio-license/has-license.guard';
import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';
import { RuntimeAlertCreateComponent } from '../../components/runtime-alerts';

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
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      permissions: {
        anyOf: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new/v4',
    component: ApiCreationV4Component,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      permissions: {
        anyOf: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new/v2',
    component: ApiCreationV2Component,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      permissions: {
        anyOf: ['environment-api-c'],
      },
    },
  },
  {
    path: 'new',
    component: ApiCreationGetStartedComponent,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: {
      permissions: {
        anyOf: ['environment-api-c'],
      },
      docs: {
        page: 'management-apis-create',
      },
    },
  },
  {
    path: 'import/v4',
    component: ApiImportV4Component,
    canActivate: [PermissionGuard.checkRouteDataPermissions],
    data: { permissions: { anyOf: ['environment-api-c'] } },
  },

  /**
   * Existing API
   */
  {
    path: ':apiId',
    component: ApiNavigationComponent,
    canActivate: [ApisGuard.loadPermissions],
    canActivateChild: [PermissionGuard.checkRouteDataPermissions, HasLicenseGuard],
    canDeactivate: [ApisGuard.clearPermissions],
    children: [
      {
        path: '',
        component: ApiGeneralInfoComponent,
      },
      {
        path: 'general',
        redirectTo: '',
        pathMatch: 'full',
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
          permissions: {
            anyOf: ['api-plan-r'],
          },
        },
        component: ApiPlanListComponent,
      },
      {
        path: 'plans/new',
        component: ApiPlanEditComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-plan-c'],
          },
        },
      },
      {
        path: 'plans/:planId',
        component: ApiPlanEditComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-plan-r'],
          },
        },
      },
      {
        path: 'subscriptions',
        component: ApiSubscriptionListComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-subscription-r'],
          },
        },
      },
      {
        path: 'subscriptions/:subscriptionId',
        component: ApiSubscriptionEditComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-subscription-r', 'api-subscription-u'],
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
          permissions: {
            anyOf: ['api-documentation-r'],
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
          permissions: {
            anyOf: ['api-documentation-c'],
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
          permissions: {
            anyOf: ['api-documentation-c'],
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
          permissions: {
            anyOf: ['api-documentation-r'],
          },
        },
      },
      {
        path: 'metadata',
        component: ApiPortalDocumentationMetadataComponent,
        data: {
          permissions: {
            anyOf: ['api-metadata-r'],
          },
        },
      },
      {
        path: 'members',
        component: ApiGeneralMembersComponent,
        data: {
          permissions: {
            anyOf: ['api-member-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'deployments',
        component: ApiDeploymentConfigurationComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'response-templates/new',
        component: ApiResponseTemplatesEditComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            anyOf: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
          },
        },
      },
      {
        path: 'response-templates/:responseTemplateId',
        component: ApiResponseTemplatesEditComponent,
        data: {
          permissions: {
            anyOf: ['api-response_templates-c', 'api-response_templates-r', 'api-response_templates-u'],
          },
          docs: {
            page: 'management-api-proxy-response-templates',
          },
        },
      },
      {
        path: 'response-templates',
        component: ApiResponseTemplatesListComponent,
        data: {
          permissions: {
            anyOf: ['api-response_templates-r'],
          },
          docs: {
            page: 'management-api-proxy-response-templates',
          },
        },
      },
      {
        path: 'audit',
        component: ApiAuditListComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
            redirect: '/',
          },
          permissions: {
            anyOf: ['api-audit-r'],
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
          permissions: {
            anyOf: ['api-event-r'],
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
          permissions: {
            anyOf: ['api-event-r'],
          },
          docs: {
            page: 'management-api-events',
          },
        },
      },
      {
        path: 'notifications',
        component: ApiNotificationComponent,
        data: {
          permissions: {
            anyOf: ['api-notification-r'],
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
          permissions: {
            anyOf: ['api-alert-c'],
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
          permissions: {
            anyOf: ['api-alert-r'],
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
          permissions: {
            anyOf: ['api-alert-r'],
          },
          docs: {
            page: 'management-alerts',
          },
        },
      },
      {
        path: 'ng/alerts',
        component: ApiRuntimeAlertsComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          permissions: {
            anyOf: ['api-alert-r'],
          },
        },
      },
      {
        path: 'ng/alerts/new',
        component: RuntimeAlertCreateComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.ALERT_ENGINE },
            redirect: '/',
          },
          apiPermissions: {
            only: ['api-alert-c'],
          },
          referenceType: 'API',
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
          permissions: {
            anyOf: ['api-alert-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
        component: ApiEntrypointsComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r', 'api-health-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v2/failover',
        component: ApiFailoverComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-proxy',
          },
        },
      },
      {
        path: 'v2/healthcheck',
        component: ApiHealthCheckComponent,
        data: {
          permissions: {
            anyOf: ['api-health-c'],
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
          permissions: {
            anyOf: ['api-health-r'],
          },
        },
      },
      {
        path: 'v2/healthcheck-dashboard',
        component: ApiHealthCheckDashboardComponent,
        data: {
          permissions: {
            anyOf: ['api-health-r'],
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
          permissions: {
            anyOf: ['api-analytics-r'],
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
          permissions: {
            anyOf: ['api-log-u'],
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
          permissions: {
            anyOf: ['api-log-r'],
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
          permissions: {
            anyOf: ['api-log-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-r'],
          },
          docs: {
            page: 'management-api-policy-studio-properties',
          },
        },
      },
      {
        path: 'properties/dynamic-properties',
        component: ApiDynamicPropertiesComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
        },
      },
      {
        path: 'resources',
        component: ApiResourcesComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
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
        children: [
          {
            path: '',
            redirectTo: 'pages',
            pathMatch: 'full',
          },
          {
            path: 'pages',
            component: ApiDocumentationV4Component,
          },
          {
            path: 'pages/new',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-documentation-c'],
              },
            },
            component: DocumentationEditCustomPageComponent,
          },
          {
            path: 'pages/:pageId',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-documentation-u', 'api-documentation-r'],
              },
            },
            component: DocumentationEditCustomPageComponent,
          },
          {
            path: 'metadata',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-metadata-r'],
              },
            },
            component: ApiDocumentationV4MetadataComponent,
          },
          {
            path: 'main-pages',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-documentation-u'],
              },
            },
            component: ApiDocumentationV4DefaultPageComponent,
          },
          {
            path: 'main-pages/homepage/new',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-documentation-c', 'api-documentation-u', 'api-documentation-r'],
              },
            },
            component: DocumentationEditHomepageComponent,
          },
          {
            path: 'main-pages/homepage/:pageId',
            data: {
              docs: null,
              permissions: {
                anyOf: ['api-documentation-c', 'api-documentation-u', 'api-documentation-r'],
              },
            },
            component: DocumentationEditHomepageComponent,
          },
        ],
      },
      {
        path: 'v4/policy-studio',
        data: {
          docs: null,
        },
        component: ApiV4PolicyStudioDesignComponent,
      },
      {
        path: 'v4/analytics',
        data: {
          permissions: {
            anyOf: ['api-analytics-r'],
          },
        },
        component: ApiAnalyticsComponent,
      },
      {
        path: 'v4/runtime-logs',
        data: {
          permissions: {
            anyOf: ['api-log-r'],
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
          permissions: {
            anyOf: ['api-log-r'],
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
          permissions: {
            anyOf: ['api-log-u'],
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
          permissions: {
            anyOf: ['api-definition-r'],
          },
        },
      },
      {
        path: 'v4/cors',
        component: ApiCorsComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-definition-r'],
          },
        },
      },
      {
        path: 'v4/entrypoints/:entrypointId',
        component: ApiEntrypointsV4EditComponent,
        data: {
          docs: null,
          permissions: {
            anyOf: ['api-definition-u'],
          },
        },
      },
      {
        path: 'v4/endpoints',
        component: ApiEndpointGroupsComponent,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-u'],
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
          permissions: {
            anyOf: ['api-definition-r'],
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
          permissions: {
            anyOf: ['api-definition-u'],
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
          permissions: {
            anyOf: ['api-definition-u'],
          },
          docs: {
            page: 'management-api-proxy-endpoints',
          },
        },
      },
      {
        path: 'v4/failover',
        component: ApiFailoverV4Component,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
        },
      },
      {
        path: 'properties/v4/dynamic-properties',
        component: ApiDynamicPropertiesV4Component,
        data: {
          permissions: {
            anyOf: ['api-definition-r'],
          },
        },
      },
      {
        path: 'v4/audit',
        component: ApiAuditLogsComponent,
        data: {
          requireLicense: {
            license: { feature: ApimFeature.APIM_AUDIT_TRAIL },
            redirect: '/',
          },
          permissions: {
            anyOf: ['api-audit-r'],
          },
        },
      },
      {
        path: 'v4/history',
        component: ApiHistoryV4Component,
        data: {
          permissions: {
            anyOf: ['api-event-r'],
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
