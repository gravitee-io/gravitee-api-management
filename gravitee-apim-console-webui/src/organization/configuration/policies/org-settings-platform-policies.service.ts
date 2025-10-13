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

import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../../../entities/Constants';
import { FlowSchema } from '../../../entities/flow/flowSchema';
import { ApiService } from '../../../services-ngx/api.service';
import { PlatformFlowSchema } from '../../../entities/flow/platformFlowSchema';
import { FlowService } from '../../../services-ngx/flow.service';
import { ListParams } from '../../../management/api/policy-studio-v2/models/ListParams';
import { PolicyDocumentation, PolicyListItem, PolicySchema } from '../../../entities/policy';
import { ResourceListItem } from '../../../entities/resource/resourceListItem';

@Injectable({
  providedIn: 'root',
})
export class OrgSettingsPlatformPoliciesService {
  constructor(
    readonly http: HttpClient,
    readonly apiService: ApiService,
    readonly flowService: FlowService,
    @Inject(Constants) readonly constants: Constants,
  ) {}

  listPolicies(params: ListParams): Observable<PolicyListItem[]> {
    let httpParams = new HttpParams();

    if (params.expandSchema) {
      httpParams = httpParams.append('expand', 'schema');
    }
    if (params.expandIcon) {
      httpParams = httpParams.append('expand', 'icon');
    }
    if (params.withoutResource) {
      httpParams = httpParams.set('withResource', false);
    }

    return this.http.get<PolicyListItem[]>(`${this.constants.env.baseURL}/policies`, {
      params: httpParams,
    });
  }

  listSwaggerPolicies(): Observable<PolicyListItem[]> {
    return this.http.get<PolicyListItem[]>(`${this.constants.env.baseURL}/policies/swagger`);
  }

  getSchema(policyId: string): Observable<PolicySchema> {
    return this.http.get<PolicySchema>(`${this.constants.env.baseURL}/policies/${policyId}/schema`);
  }

  getDocumentation(policyId: string): Observable<PolicyDocumentation> {
    return this.http.get<PolicyDocumentation>(`${this.constants.org.v2BaseURL}/plugins/policies/${policyId}/documentation-ext`);
  }

  getFlowSchemaForm(): Observable<FlowSchema> {
    return this.apiService.getFlowSchemaForm();
  }

  getPlatformFlowSchemaForm(): Observable<PlatformFlowSchema> {
    return this.flowService.getPlatformFlowSchemaForm();
  }

  getSpelGrammar(): Observable<any> {
    return this.http.get(`${this.constants.env.baseURL}/configuration/spel/grammar`);
  }

  listResources(params: ListParams): Observable<ResourceListItem[]> {
    let httpParams = new HttpParams();

    if (params.expandSchema) {
      httpParams = httpParams.append('expand', 'schema');
    }
    if (params.expandIcon) {
      httpParams = httpParams.append('expand', 'icon');
    }

    return this.http.get<ResourceListItem[]>(`${this.constants.env.baseURL}/resources`, {
      params: httpParams,
    });
  }

  getConfigurationSchemaForm() {
    return this.flowService.getConfigurationSchemaForm();
  }
}
