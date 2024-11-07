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
import { CommonModule } from '@angular/common';

import { ApiAnalyticsModule } from './analytics/api-analytics.module';
import { ApiListModule } from './list/api-list.module';
import { ApiNavigationModule } from './api-navigation/api-navigation.module';
import { ApiV4PolicyStudioModule } from './policy-studio-v4/api-v4-policy-studio.module';
import { ApiTrafficV4Module } from './api-traffic-v4/api-traffic-v4.module';
import { ApiEndpointsV4Module } from './endpoints-v4/api-endpoints-v4.module';
import { ApiEntrypointsV4Module } from './entrypoints-v4/api-entrypoints-v4.module';
import { GioPolicyStudioRoutingModule } from './policy-studio-v2/gio-policy-studio-routing.module';
import { ApiAuditModule } from './audit/api-audit.module';
import { ApiV1PoliciesComponent } from './policy-studio-v1/policies/policies.component';
import { ApiCreationV2Module } from './creation-v2/api-creation-v2.module';
import { ApiCreationGetStartedModule } from './creation-get-started/api-creation-get-started.module';
import { ApiCreationV4Module } from './creation-v4/api-creation-v4.module';
import { ApiDocumentationV4Module } from './documentation-v4/api-documentation-v4.module';
import { ApisRoutingModule } from './apis-routing.module';
import { ApiGeneralInfoModule } from './general-info/api-general-info.module';
import { ApiResourcesModule } from './resources-ng/api-resources.module';
import { ApiEntrypointsModule } from './entrypoints/api-entrypoints.module';
import { ApiResponseTemplatesModule } from './response-templates/api-response-templates.module';
import { ApiEndpointsModule } from './endpoints/api-endpoints.module';
import { ApiFailoverModule } from './failover/api-failover.module';
import { ApiHealthCheckModule } from './health-check/api-health-check.module';
import { ApiPlansModule } from './plans/api-plans.module';
import { ApiSubscriptionsModule } from './subscriptions/api-subscriptions.module';
import { ApiDocumentationModule } from './documentation/api-documentation.module';
import { ApiUserGroupModule } from './user-group-access/api-user-group.module';
import { ApiAuditListModule } from './api-audit-list/api-audit-list.module';
import { ApiDeploymentConfigurationModule } from './deployment-configuration-v4/api-deployment-configuration.module';
import { ApiAuditLogsModule } from './api-audit-logs/api-audit-logs.module';
import { ApiNotificationModule } from './api-notification/api-notification.module';
import { ApiCorsModule } from './cors/api-cors.module';
import { ApiPropertiesModule } from './properties/properties/api-properties.module';
import { ApiProxyHealthCheckDashboardModule } from './health-check-dashboard/api-proxy-health-check-dashboard.module';
import { ApiRuntimeAlertsModule } from './runtime-alerts';
import { ApiHistoryV4Module } from './history-v4/api-history-v4.module';
import { ApiFailoverV4Module } from './failover-v4/api-failover-v4.module';
import { ApiResourcesComponent } from './resources/api-resources.component';
import { ApiScoringModule } from './scoring/api-scoring.module';
import { ApiHealthCheckDashboardV4Module } from './health-check-dashboard-v4/api-health-check-dashboard-v4.module';

import { DocumentationModule } from '../../components/documentation/documentation.module';
import { AlertsModule } from '../../components/alerts/alerts.module';

@NgModule({
  declarations: [ApiV1PoliciesComponent],
  imports: [
    CommonModule,

    ApisRoutingModule,

    ApiHealthCheckDashboardV4Module,
    AlertsModule,
    ApiAnalyticsModule,
    ApiAuditModule,
    ApiAuditLogsModule,
    ApiAuditListModule,
    ApiCorsModule,
    ApiCreationGetStartedModule,
    ApiCreationV2Module,
    ApiCreationV4Module,
    ApiDeploymentConfigurationModule,
    ApiDocumentationModule,
    ApiDocumentationV4Module,
    ApiEndpointsModule,
    ApiEndpointsV4Module,
    ApiEntrypointsModule,
    ApiEntrypointsV4Module,
    ApiFailoverModule,
    ApiFailoverV4Module,
    ApiGeneralInfoModule,
    ApiProxyHealthCheckDashboardModule,
    ApiHealthCheckModule,
    ApiHistoryV4Module,
    ApiListModule,
    ApiNavigationModule,
    ApiNotificationModule,
    ApiPlansModule,
    ApiPropertiesModule,
    ApiResourcesModule,
    ApiResourcesComponent,
    ApiResponseTemplatesModule,
    ApiTrafficV4Module,
    ApiRuntimeAlertsModule,
    ApiSubscriptionsModule,
    ApiV4PolicyStudioModule,
    ApiUserGroupModule,
    DocumentationModule,
    GioPolicyStudioRoutingModule,
    ApiHistoryV4Module,
    ApiScoringModule,
  ],
})
export class ApisModule {}
