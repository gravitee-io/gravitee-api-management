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

import { ApiProxyCorsModule } from './cors/api-proxy-cors.module';
import { ApiProxyDeploymentsModule } from './deployments/api-proxy-deployments.module';
import { ApiProxyEntrypointsModule } from './entrypoints/api-proxy-entrypoints.module';
import { ApiProxyResponseTemplatesModule } from './response-templates/api-proxy-response-templates.module';
import { ApiProxyEndpointModule } from './endpoints/api-proxy-endpoints.module';
import { ApiProxyFailoverModule } from './failover/api-proxy-failover.module';
import { ApiProxyHealthCheckModule } from './health-check/api-proxy-health-check.module';
import { ApiPropertiesModule } from './properties-ng/api-properties.module';
import { ApiResourcesModule } from './resources-ng/api-resources.module';
import { ApiProxyHealthCheckDashboardModule } from './health-check-dashboard/api-proxy-health-check-dashboard.module';

@NgModule({
  imports: [
    ApiProxyEntrypointsModule,
    ApiProxyCorsModule,
    ApiProxyDeploymentsModule,
    ApiProxyResponseTemplatesModule,
    ApiProxyEndpointModule,
    ApiProxyFailoverModule,
    ApiProxyHealthCheckModule,
    ApiProxyHealthCheckDashboardModule,
    ApiPropertiesModule,
    ApiResourcesModule,
  ],
})
export class ApiProxyModule {}
