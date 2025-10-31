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

import { ApiEndpointGroupsModule } from './endpoint-groups/api-endpoint-groups.module';
import { ApiEndpointGroupModule } from './endpoint-group/api-endpoint-group.module';
import { ApiEndpointModule } from './endpoint/api-endpoint.module';
import { ApiProviderModule } from './provider/api-provider.module';

@NgModule({
  imports: [CommonModule, ApiEndpointGroupsModule, ApiEndpointGroupModule, ApiEndpointModule, ApiProviderModule],
})
export class ApiEndpointsV4Module {}
