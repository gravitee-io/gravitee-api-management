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
import { BehaviorSubject, EMPTY, from, mergeMap, Observable, of } from 'rxjs';
import { distinctUntilChanged, expand, filter, map, reduce, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { Constants } from '../entities/Constants';
import {
  Api,
  ApiSearchQuery,
  ApiSortByParam,
  ApisResponse,
  ApiSubscribersResponse,
  ApiTransferOwnership,
  ApiV4,
  CreateApi,
  DuplicateApiOptions,
  ListenerType,
  Member,
  UpdateApi,
  VerifyApiDeployResponse,
} from '../entities/management-api-v2';
import { PathToVerify, VerifyApiPathResponse } from '../entities/management-api-v2/api/verifyApiPath';
import { VerifyApiHostsResponse } from '../entities/management-api-v2/api/verifyApiHosts';
import { ImportSwaggerDescriptor } from '../entities/management-api-v2/api/v4/importSwaggerDescriptor';
import { ImportWsdlDescriptor } from '../entities/management-api-v2/api/v4/importWsdlDescriptor';
import { MigrateToV4Response } from '../entities/management-api-v2/api/v2/migrateToV4Response';
import { ApiProductInfo } from '../entities/management-api-v2/api-product/apiProductInfo';

export interface ApiProductsForApiResponse {
  data: ApiProductInfo[];
}

export interface HostValidatorParams {
  currentHost?: string;
  apiId?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApiV2Service {
  private lastApiFetch$: BehaviorSubject<Api | null> = new BehaviorSubject<Api | null>(null);

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  create(newApi: CreateApi): Observable<Api> {
    return this.http.post<Api>(`${this.constants.env.v2BaseURL}/apis`, newApi);
  }

  get(id: string): Observable<Api> {
    return this.http.get<Api>(`${this.constants.env.v2BaseURL}/apis/${id}`).pipe(
      tap(api => {
        this.lastApiFetch$.next(api);
      }),
    );
  }

  update(apiId: string, api: UpdateApi): Observable<Api> {
    return this.http.put<Api>(`${this.constants.env.v2BaseURL}/apis/${apiId}`, api).pipe(
      tap(api => {
        this.lastApiFetch$.next(api);
      }),
    );
  }

  updateGroups(apiId: string, groups: string[]): Observable<string[]> {
    return this.http.put<string[]>(`${this.constants.env.v2BaseURL}/apis/${apiId}/groups`, groups);
  }

  delete(apiId: string, closePlans = false): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}${closePlans ? '?closePlans=true' : ''}`);
  }

  detach(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_detach`, {});
  }

  start(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_start`, {});
  }

  stop(apiId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_stop`, {});
  }

  deploy(apiId: string, deploymentLabel?: string): Observable<void> {
    return this.http
      .post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/deployments`, {
        deploymentLabel,
      })
      .pipe(switchMap(() => this.refreshLastApiFetch()));
  }

  verifyDeploy(apiId: string): Observable<VerifyApiDeployResponse> {
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/deployments/_verify`);
  }

  getCurrentDeployment(apiId: string): Observable<unknown> {
    return this.http.get<unknown>(`${this.constants.env.v2BaseURL}/apis/${apiId}/deployments/current`);
  }

  duplicate(apiId: string, options: DuplicateApiOptions): Observable<Api> {
    return this.http.post<Api>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_duplicate`, options);
  }

  export(
    apiId: string,
    options?: {
      excludeAdditionalData?: string[];
    },
  ): Observable<Blob> {
    return this.http
      .get(`${this.constants.env.v2BaseURL}/apis/${apiId}/_export/definition`, {
        responseType: 'blob',
        params: {
          ...(options?.excludeAdditionalData ? { excludeAdditionalData: options.excludeAdditionalData.join(',') } : {}),
        },
      })
      .pipe(
        mergeMap(blob => from(blob.text())),
        map(content => {
          return new Blob([JSON.stringify(JSON.parse(content), undefined, 2)], {
            type: 'application/json',
          });
        }),
      );
  }

  import(importApi: any): Observable<ApiV4> {
    return this.http.post<ApiV4>(`${this.constants.env.v2BaseURL}/apis/_import/definition`, importApi, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Creates a v4 API by asking the backend to fetch a Gravitee export from `definitionUrl` and import it.
   * Body is the URL string itself (`text/plain`). The backend ignores any `Authorization` header.
   *
   * Create only — there is no update-from-URL equivalent yet (planned in APIM-12142).
   */
  importFromUrl(definitionUrl: string): Observable<ApiV4> {
    return this.http.post<ApiV4>(`${this.constants.env.v2BaseURL}/apis/_import/definition-url`, definitionUrl, {
      headers: {
        'Content-Type': 'text/plain',
      },
    });
  }

  importSwaggerApi(descriptor: ImportSwaggerDescriptor) {
    return this.http.post<ApiV4>(`${this.constants.env.v2BaseURL}/apis/_import/swagger`, descriptor, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  importWsdlApi(descriptor: ImportWsdlDescriptor): Observable<ApiV4> {
    return this.http.post<ApiV4>(`${this.constants.env.v2BaseURL}/apis/_import/wsdl`, descriptor, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  updateApiFromWsdl(apiId: string, descriptor: ImportWsdlDescriptor): Observable<ApiV4> {
    return this.http.put<ApiV4>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_import/wsdl`, descriptor, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Updates an existing v4 API from a Gravitee export document (same shape as {@link export}).
   */
  updateApiFromDefinition(apiId: string, definition: unknown): Observable<ApiV4> {
    return this.http.put<ApiV4>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_import/definition`, definition, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Updates an existing v4 API from an OpenAPI / Swagger descriptor.
   */
  updateApiFromSwagger(apiId: string, descriptor: ImportSwaggerDescriptor): Observable<ApiV4> {
    return this.http.put<ApiV4>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_import/swagger`, descriptor, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  exportCRD(apiId: string): Observable<Blob> {
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/_export/crd`, {
      responseType: 'blob',
    });
  }

  search(searchQuery: ApiSearchQuery = {}, sortBy?: ApiSortByParam, page = 1, perPage = 10, manageOnly = true): Observable<ApisResponse> {
    return this.http.post<ApisResponse>(`${this.constants.env.v2BaseURL}/apis/_search`, searchQuery, {
      params: {
        page,
        perPage,
        ...(sortBy ? { sortBy } : {}),
        ...(manageOnly ? {} : { manageOnly: false }),
      },
    });
  }

  /**
   * Lists APIs in a single request (for dropdowns/filters).
   *
   * Mirrors {@link ApplicationService.getAll}: one HTTP call instead of paginating through every page
   * like {@link listAll}. Includes all definition versions (v1, v2, v4, federated, federated agent).
   */
  getAll(params: { environmentId?: string } = {}): Observable<Api[]> {
    const baseURL = params.environmentId
      ? `${this.constants.v2BaseURL}/environments/${params.environmentId}`
      : this.constants.env.v2BaseURL;

    return this.http
      .get<ApisResponse>(`${baseURL}/apis`, {
        params: { page: 1, perPage: 9999 },
      })
      .pipe(map(response => response.data ?? []));
  }

  /**
   * Lists every API in an environment regardless of definition version (v1, v2, v4, federated, federated agent).
   *
   * Use this instead of `ApiService.getAll()`, which relies on the legacy Management v1 endpoint and
   * silently omits v4+ APIs.
   *
   * The method paginates through the v2 endpoint using the `pageCount` returned in the response and
   * emits a single aggregated `Api[]` once every page has been fetched.
   */
  listAll(params: { environmentId?: string } = {}): Observable<Api[]> {
    const baseURL = params.environmentId
      ? `${this.constants.v2BaseURL}/environments/${params.environmentId}`
      : this.constants.env.v2BaseURL;

    const fetchPage = (page: number): Observable<ApisResponse> =>
      this.http.get<ApisResponse>(`${baseURL}/apis`, {
        params: { page },
      });

    return fetchPage(1).pipe(
      expand(response => {
        const currentPage = response.pagination?.page ?? 1;
        const pageCount = response.pagination?.pageCount ?? 1;
        return currentPage < pageCount ? fetchPage(currentPage + 1) : EMPTY;
      }),
      reduce((acc, response) => acc.concat(response.data ?? []), [] as Api[]),
    );
  }

  updatePicture(apiId: string, newImage: string): Observable<void> {
    return this.http.put<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/picture`, newImage);
  }

  updateBackground(apiId: string, newImage: string): Observable<void> {
    return this.http.put<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/background`, newImage);
  }

  deletePicture(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/picture`);
  }

  deleteBackground(apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/background`);
  }

  getSubscribers(apiId: string, name?: string, page = 1, perPage = 10): Observable<ApiSubscribersResponse> {
    return this.http.get<ApiSubscribersResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/subscribers`, {
      params: {
        page,
        perPage,
        ...(name ? { name } : {}),
      },
    });
  }

  getLastApiFetch(apiId: string): Observable<Api> {
    const start = this.lastApiFetch$.value && this.lastApiFetch$.value.id === apiId ? of(this.lastApiFetch$.value) : this.get(apiId);
    return start.pipe(
      switchMap(() => this.lastApiFetch$.asObservable()),
      filter(api => !!api),
      distinctUntilChanged(isEqual),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  refreshLastApiFetch(): Observable<void> {
    if (this.lastApiFetch$.value && this.lastApiFetch$.value.id) {
      return this.get(this.lastApiFetch$.value.id).pipe(map(() => {}));
    }
    return of(void 0);
  }

  transferOwnership(api: string, ownership: ApiTransferOwnership): Observable<any> {
    return this.http.post(`${this.constants.env.v2BaseURL}/apis/${api}/_transfer-ownership`, ownership);
  }

  getPrimaryOwner(api: string): Observable<Member> {
    return this.http.get<Member>(`${this.constants.env.v2BaseURL}/apis/${api}/primaryowner`);
  }

  verifyPath(apiId: string, paths: PathToVerify[]): Observable<VerifyApiPathResponse> {
    return this.http.post<VerifyApiPathResponse>(`${this.constants.env.v2BaseURL}/apis/_verify/paths`, {
      apiId,
      paths,
    });
  }

  verifyHosts(apiId: string, listenerType: ListenerType, hosts: string[]): Observable<VerifyApiHostsResponse> {
    return this.http.post<VerifyApiHostsResponse>(`${this.constants.env.v2BaseURL}/apis/_verify/hosts`, {
      apiId,
      listenerType,
      hosts,
    });
  }

  rollback(apiId: string, eventId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_rollback`, { eventId }).pipe(
      switchMap(() => this.get(apiId)),
      map(() => {}),
    );
  }

  migrateToV4(apiId: string, mode?: 'DRY_RUN' | 'FORCE'): Observable<MigrateToV4Response> {
    let httpParams = new HttpParams();

    if (mode) {
      httpParams = httpParams.set('mode', mode);
    }

    return this.http.post<MigrateToV4Response>(
      `${this.constants.env.v2BaseURL}/apis/${apiId}/_migrate`,
      {},
      {
        params: httpParams,
      },
    );
  }

  getApiProductsForApi(apiId: string, page = 1, perPage = 500): Observable<ApiProductsForApiResponse> {
    const params = new HttpParams().set('page', page.toString()).set('perPage', perPage.toString());
    return this.http.get<ApiProductsForApiResponse>(`${this.constants.env.v2BaseURL}/apis/${apiId}/api-products`, { params });
  }
}
