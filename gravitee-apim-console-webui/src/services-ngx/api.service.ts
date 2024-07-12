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
import { Inject, Injectable, Injector } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, mapTo } from 'rxjs/operators';
import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';

import { Api, ApiMetrics, ApiQualityMetrics, ApiStateEntity, UpdateApi } from '../entities/api';
import { Constants } from '../entities/Constants';
import { FlowSchema } from '../entities/flow/flowSchema';
import { PagedResult } from '../entities/pagedResult';
import { GroupMember } from '../entities/group/groupMember';
import { ApiHealthAverage } from '../entities/api/ApiHealthAverage';
import { Metadata, NewMetadata, UpdateMetadata } from '../entities/metadata/metadata';
import { Event, EventType } from '../entities/event/event';
import { EventEntityPage } from '../entities/event/EventEntityPage';

export interface ContextPathValidatorParams {
  currentContextPath?: string;
  apiId?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  get(apiId: string): Observable<Api> {
    return this.http.get<Api>(`${this.constants.env.baseURL}/apis/${apiId}`);
  }

  getFlowSchemaForm(): Observable<FlowSchema> {
    return this.http.get<FlowSchema>(`${this.constants.env.baseURL}/apis/schema`);
  }

  update(api: UpdateApi & { id: string }): Observable<Api> {
    return this.http.put<Api>(`${this.constants.env.baseURL}/apis/${api.id}`, {
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
    });
  }

  getAll(
    params: {
      order?: string;
      environmentId?: string;
      category?: string;
    } = {},
  ): Observable<Api[]> {
    let baseURL = this.constants.env.baseURL;

    if (params.environmentId) {
      baseURL = `${this.constants.org.baseURL}/environments/${params.environmentId}`;
    }

    return this.http.get<Api[]>(`${baseURL}/apis`, {
      params: {
        ...(params.order ? { order: params.order } : {}),
        ...(params.category ? { category: params.category } : {}),
      },
    });
  }

  list(query?: string, order?: string, page = 1, size = 10): Observable<PagedResult<Api>> {
    return this.http.post<PagedResult<Api>>(`${this.constants.env.baseURL}/apis/_search/_paged`, null, {
      params: {
        page,
        size,
        q: query ? query : '*',
        ...(order ? { order } : {}),
      },
    });
  }

  isAPISynchronized(apiId: string): Observable<ApiStateEntity> {
    return this.http.get<ApiStateEntity>(`${this.constants.env.baseURL}/apis/${apiId}/state`);
  }

  getQualityMetrics(apiId: string): Observable<ApiQualityMetrics> {
    return this.http.get<ApiQualityMetrics>(`${this.constants.env.baseURL}/apis/${apiId}/quality`);
  }

  askForReview(apiId: string, message?: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/${apiId}/reviews?action=ASK`, { message });
  }

  acceptReview(apiId: string, message?: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/${apiId}/reviews?action=ACCEPT`, { message });
  }

  rejectReview(apiId: string, message?: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/${apiId}/reviews?action=REJECT`, { message });
  }

  start(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/` + apiId + '?action=START', {});
  }

  stop(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/` + apiId + '?action=STOP', {});
  }

  delete(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/apis/` + apiId);
  }

  importApiDefinition(type: 'graviteeJson' | 'graviteeUrl', payload: string, apiId?: string): Observable<Api> {
    const isGraviteeUrl = type === 'graviteeUrl';

    const headers = { 'Content-Type': isGraviteeUrl ? 'text/plain' : 'application/json' };
    const endpoint = isGraviteeUrl ? 'import-url' : 'import';

    const params = `?definitionVersion=2.0.0`;

    return apiId
      ? this.http.put<Api>(`${this.constants.env.baseURL}/apis/${apiId}/${endpoint}${params}`, payload, { headers })
      : this.http.post<Api>(`${this.constants.env.baseURL}/apis/${endpoint}${params}`, payload, { headers });
  }

  importSwaggerApi(
    payload: {
      payload: string;
      format?: 'WSDL' | 'API';
      type?: 'INLINE' | 'URL';
      with_documentation?: boolean;
      with_path_mapping?: boolean;
      with_policies?: Array<string>;
      with_policy_paths?: boolean;
    },
    apiId?: string,
  ): Observable<Api> {
    const params = `?definitionVersion=2.0.0`;
    return apiId
      ? this.http.put<Api>(`${this.constants.env.baseURL}/apis/${apiId}/import/swagger${params}`, payload)
      : this.http.post<Api>(`${this.constants.env.baseURL}/apis/import/swagger${params}`, payload);
  }

  export(apiId: string, exclude: string[], exportVersion: string): Observable<Blob> {
    const params: string[] = [];
    if (exclude && exclude.length > 0) {
      params.push(`exclude=${exclude.join(',')}`);
    }
    if (exportVersion) {
      params.push(`version=${exportVersion}`);
    }

    return this.http.get(`${this.constants.env.baseURL}/apis/${apiId}/export${params ? `?${params.join('&')}` : ''}`, {
      responseType: 'blob',
    });
  }

  exportCrd(apiId: string): Observable<Blob> {
    return this.http.get(`${this.constants.env.baseURL}/apis/${apiId}/crd`, {
      responseType: 'blob',
    });
  }

  duplicate(apiId: string, config: { context_path: string; version: string; filtered_fields: string[] }): Observable<Api> {
    return this.http.post<Api>(`${this.constants.env.baseURL}/apis/${apiId}/duplicate`, config);
  }

  contextPathValidator(params: ContextPathValidatorParams): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value) {
        // If the control is empty, return no error
        return of(null);
      }
      const contextPath = control.value;

      if (contextPath.length < 3) {
        return of({ contextPath: 'Context path has to be more than 3 characters long.' });
      }

      if (params.currentContextPath === contextPath) {
        return of(null);
      }
      const { apiId } = params;
      return this.verify(contextPath, apiId);
    };
  }

  verify(contextPath, apiId?): Observable<ValidationErrors | null> {
    return this.http
      .post(
        `${this.constants.env.baseURL}/apis/verify`,
        { context_path: contextPath, apiId },
        {
          responseType: 'text',
        },
      )
      .pipe(
        mapTo(null),
        catchError((error) => {
          let message = 'Context path is not valid.';
          try {
            const errorResponse = JSON.parse(error.error);
            message = errorResponse.message;
          } catch (error) {}

          return of({ contextPath: message });
        }),
      );
  }

  migrateApiToPolicyStudio(apiId: string): Observable<Api> {
    return this.http.post<Api>(`${this.constants.env.baseURL}/apis/${apiId}/_migrate`, {});
  }

  importPathMappings(apiId: string, pageId: string, definitionVersion?: string): Observable<Api> {
    let params = `?page=${pageId}`;

    if (definitionVersion) {
      params += `&definitionVersion=${definitionVersion}`;
    }

    return this.http.post<Api>(`${this.constants.env.baseURL}/apis/${apiId}/import-path-mappings${params}`, {});
  }

  transferOwnership(
    api: string,
    ownership: {
      id?: string;
      reference?: string;
      role: string;
    },
  ): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/${api}/members/transfer_ownership`, ownership);
  }

  getGroupIdsWithMembers(apiId: string): Observable<Record<string, GroupMember[]>> {
    return this.http.get<Record<string, GroupMember[]>>(`${this.constants.env.baseURL}/apis/${apiId}/groups`);
  }

  apiHealth(api: string, type?: 'availability' | 'response_time', field?: 'endpoint' | 'gateway'): Observable<ApiMetrics> {
    const params = [];
    if (type !== undefined) {
      params.push(`type=${type}`);
    }
    if (field !== undefined) {
      params.push(`field=${field}`);
    }

    const paramsStr = params.length > 0 ? `?${params.join('&')}` : '';

    return this.http.get<ApiMetrics>(`${this.constants.env.baseURL}/apis/${api}/health${paramsStr}`);
  }

  apiHealthAverage(
    apiId: string,
    params: {
      from: number;
      to: number;
      interval: number;
      type: 'RESPONSE_TIME' | 'AVAILABILITY';
    },
  ): Observable<ApiHealthAverage> {
    const queryParams = [];

    if (params.type !== undefined) {
      queryParams.push(`type=${params.type}`);
    }
    if (params.from !== undefined) {
      queryParams.push(`from=${params.from}`);
    }
    if (params.to !== undefined) {
      queryParams.push(`to=${params.to}`);
    }
    if (params.interval !== undefined) {
      queryParams.push(`interval=${params.interval}`);
    }

    const paramsStr = queryParams.length > 0 ? `?${queryParams.join('&')}` : '';

    return this.http.get<ApiHealthAverage>(`${this.constants.env.baseURL}/apis/${apiId}/health/average${paramsStr}`);
  }

  listMetadata(apiId: string): Observable<Metadata[]> {
    return this.http.get<Metadata[]>(`${this.constants.env.baseURL}/apis/${apiId}/metadata/`);
  }

  createMetadata(apiId: string, metadata: NewMetadata): Observable<Metadata> {
    return this.http.post<Metadata>(`${this.constants.env.baseURL}/apis/${apiId}/metadata/`, metadata);
  }

  updateMetadata(apiId: string, metadata: UpdateMetadata): Observable<Metadata> {
    return this.http.put<Metadata>(`${this.constants.env.baseURL}/apis/${apiId}/metadata/${metadata.key}`, metadata);
  }

  deleteMetadata(apiId: string, metadataKey: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/apis/${apiId}/metadata/${metadataKey}`);
  }

  getPermissions(apiId: string): Observable<Record<string, ('C' | 'R' | 'U' | 'D')[]>> {
    return this.http.get<Record<string, ('C' | 'R' | 'U' | 'D')[]>>(`${this.constants.env.baseURL}/apis/${apiId}/members/permissions`);
  }

  /*
   * API events
   */
  getApiEvents(apiId: string, eventTypes: EventType[]): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.constants.env.baseURL}/apis/${apiId}/events?type=${eventTypes ? `${eventTypes.join(',')}` : ''}`);
  }

  searchApiEvents(
    type: string[],
    api: string,
    from: number,
    to: number,
    page: number,
    size: number,
    withPayload = false,
  ): Observable<EventEntityPage> {
    const params = {
      page,
      size,
      type: type ? type.join(',') : '',
    };
    if (from) {
      params['from'] = from;
    }
    if (to) {
      params['to'] = to;
    }
    if (withPayload) {
      params['withPayload'] = withPayload;
    }

    return this.http.get<EventEntityPage>(
      `${this.constants.env.baseURL}/apis/${api}/events/search?${Object.entries(params)
        .map(([key, value]) => `${key}=${value}`)
        .join('&')}`,
    );
  }
}

export const ajsApiServiceProvider = {
  deps: ['$injector'],
  provide: 'ajsApiService',
  useFactory: (injector: Injector) => injector.get('ApiService'),
};
