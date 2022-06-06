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
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Api, UpdateApi } from '../entities/api';
import { Constants } from '../entities/Constants';
import { FlowSchema } from '../entities/flow/flowSchema';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  get(name: string): Observable<Api> {
    return this.http.get<Api>(`${this.constants.env.baseURL}/apis/${name}`);
  }

  getFlowSchemaForm(): Observable<FlowSchema> {
    return this.http.get<FlowSchema>(`${this.constants.env.baseURL}/apis/schema`);
  }

  update(api: UpdateApi & { id: string }): Observable<Api> {
    return this.http.put<Api>(
      `${this.constants.env.baseURL}/apis/${api.id}`,
      {
        version: api.version,
        description: api.description,
        proxy: api.proxy,
        paths: api.paths,
        flows: api.flows,
        plans: api.plans,
        // TODO To remove ? not present inside backend model
        // private: api.private,
        visibility: api.visibility,
        name: api.name,
        services: api.services,
        properties: api.properties,
        tags: api.tags,
        picture: api.picture,
        picture_url: api.picture_url,
        background: api.background,
        background_url: api.background_url,
        resources: api.resources,
        categories: api.categories,
        groups: api.groups,
        labels: api.labels,
        path_mappings: api.path_mappings,
        response_templates: api.response_templates,
        lifecycle_state: api.lifecycle_state,
        disable_membership_notifications: api.disable_membership_notifications,
        flow_mode: api.flow_mode,
        gravitee: api.gravitee,
        execution_mode: api.execution_mode,
      },
      { headers: new HttpHeaders({ ...(api.etag ? { 'If-Match': api.etag } : {}) }) },
    );
  }

  getAll(
    params: {
      order?: string;
      environmentId?: string;
    } = {},
  ): Observable<Api[]> {
    let baseURL = this.constants.env.baseURL;

    if (params.environmentId) {
      baseURL = `${this.constants.org.baseURL}/environments/${params.environmentId}`;
    }

    return this.http.get<Api[]>(`${baseURL}/apis`, {
      params: {
        ...(params.order ? { order: params.order } : {}),
      },
    });
  }
}
