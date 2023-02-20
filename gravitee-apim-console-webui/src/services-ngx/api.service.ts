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
import { Observable, of } from 'rxjs';
import * as _ from 'lodash';
import { catchError, map, mapTo } from 'rxjs/operators';
import { IScope } from 'angular';
import { AbstractControl, AsyncValidatorFn, ValidationErrors, ValidatorFn } from '@angular/forms';

import { Api, ApiQualityMetrics, ApiStateEntity, UpdateApi } from '../entities/api';
import { Constants } from '../entities/Constants';
import { FlowSchema } from '../entities/flow/flowSchema';
import { PagedResult } from '../entities/pagedResult';
import { AjsRootScope } from '../ajs-upgraded-providers';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject('Constants') private readonly constants: Constants,
    @Inject(AjsRootScope) private readonly ajsRootScope: IScope,
  ) {}

  get(apiId: string): Observable<Api> {
    return this.http.get<Api>(`${this.constants.env.baseURL}/apis/${apiId}`);
  }

  getFlowSchemaForm(): Observable<FlowSchema> {
    return this.http.get<FlowSchema>(`${this.constants.env.baseURL}/apis/schema`);
  }

  update(api: UpdateApi & { id: string }): Observable<Api> {
    return this.http
      .put<Api>(`${this.constants.env.baseURL}/apis/${api.id}`, {
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
      })
      .pipe(
        map((api) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api: _.cloneDeep(api) });
          return api;
        }),
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

  start(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/` + apiId + '?action=START', {});
  }

  stop(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/apis/` + apiId + '?action=STOP', {});
  }

  delete(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/apis/` + apiId);
  }

  importApiDefinition(type: 'graviteeJson' | 'graviteeUrl', payload: string, definitionVersion: string, apiId?: string): Observable<Api> {
    const isGraviteeUrl = type === 'graviteeUrl';

    const headers = { 'Content-Type': isGraviteeUrl ? 'text/plain' : 'application/json' };
    const endpoint = isGraviteeUrl ? 'import-url' : 'import';

    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';

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
    definitionVersion: string,
    apiId?: string,
  ): Observable<Api> {
    const params = definitionVersion ? `?definitionVersion=${definitionVersion}` : '';
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

  contextPathValidator(currentContextPath?: string): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value) {
        // If the control is empty, return no error
        return of(null);
      }
      const contextPath = control.value;

      if (contextPath.length < 3) {
        return of({ contextPath: 'Context path has to be more than 3 characters long.' });
      }

      if (currentContextPath === contextPath) {
        return of(null);
      }

      return this.verify(contextPath);
    };
  }

  verify(contextPath): Observable<ValidationErrors | null> {
    return this.http
      .post(
        `${this.constants.env.baseURL}/apis/verify`,
        { context_path: contextPath },
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

  versionValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        // If the control is empty, return no error
        return null;
      }
      const version = control.value;

      return version.length > 32 ? { version: 'Maximum length is 32 characters.' } : null;
    };
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
}
