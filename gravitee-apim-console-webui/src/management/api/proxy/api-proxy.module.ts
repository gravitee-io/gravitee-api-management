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

import { ApiProxyEntrypointsModule } from './entrypoints/api-proxy-entrypoints.module';
import { ApiProxyResponseTemplatesModule } from './response-templates/api-proxy-response-templates.module';
import { ApiProxyEndpointModule } from './endpoints/api-proxy-endpoints.module';
import { ApiProxyFailoverModule } from './failover/api-proxy-failover.module';
import { ApiProxyHealthCheckModule } from './health-check/api-proxy-health-check.module';
import { ApiProxyHealthCheckDashboardModule } from './health-check-dashboard/api-proxy-health-check-dashboard.module';
import { ApiV1PropertiesComponent } from './properties-v1/properties.component';
import { ApiPropertiesModule } from './properties/properties/api-properties.module';
import { ApiDynamicPropertiesModule } from './properties/dynamic-properties/api-dynamic-properties.module';

import { ApiV1ResourcesComponent } from '../resources-v1/resources.component';
import { ApiProxyCorsModule } from '../cors/api-proxy-cors.module';

@NgModule({
  declarations: [ApiV1PropertiesComponent, ApiV1ResourcesComponent],
  imports: [
    ApiProxyEntrypointsModule,
    ApiProxyCorsModule,
    ApiProxyResponseTemplatesModule,
    ApiProxyEndpointModule,
    ApiProxyFailoverModule,
    ApiProxyHealthCheckModule,
    ApiProxyHealthCheckDashboardModule,
    ApiPropertiesModule,
    ApiDynamicPropertiesModule,
  ],
})
export class ApiProxyModule {}
