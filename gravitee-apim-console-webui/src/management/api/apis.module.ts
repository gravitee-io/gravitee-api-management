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

import { ApiPortalDetailsModule } from './portal/details/api-portal-details.module';
import { ApiListModule } from './list/api-list.module';
import { ApiProxyModule } from './proxy/api-proxy.module';
import { ApiNavigationModule } from './api-navigation/api-navigation.module';

@NgModule({
  imports: [ApiListModule, ApiProxyModule, ApiPortalDetailsModule, ApiNavigationModule],
})
export class ApisModule {}
