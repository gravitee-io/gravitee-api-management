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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FlowServiceAbstract } from '@gravitee/ui-policy-studio-angular';

import { Constants } from '../entities/Constants';
import { FlowConfigurationSchema } from '../entities/flow/configurationSchema';
import { PlatformFlowSchema } from '../entities/flow/platformFlowSchema';
import { OrganizationFlowConfiguration } from '../entities/flow/organizationFlowConfiguration';

@Injectable({
  providedIn: 'root',
})
export class FlowService implements FlowServiceAbstract {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getConfigurationSchemaForm(): Observable<FlowConfigurationSchema> {
    return this.http.get<FlowConfigurationSchema>(`${this.constants.org.baseURL}/configuration/flows/configuration-schema`);
  }

  getPlatformFlowSchemaForm(): Observable<PlatformFlowSchema> {
    return this.http.get<PlatformFlowSchema>(`${this.constants.org.baseURL}/configuration/flows/flow-schema`);
  }

  getConfiguration(): Observable<OrganizationFlowConfiguration> {
    return this.http.get<OrganizationFlowConfiguration>(`${this.constants.org.baseURL}/configuration/flows`);
  }
}
