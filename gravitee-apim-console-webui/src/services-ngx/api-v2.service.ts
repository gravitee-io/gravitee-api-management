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
import { BehaviorSubject, Observable, of } from 'rxjs';
import { distinctUntilChanged, filter, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { Constants } from '../entities/Constants';
import {
  Api,
  ApiSearchQuery,
  ApiSortByParam,
  ApisResponse,
  ApiSubscribersResponse,
  ApiV4,
  CreateApi,
  DuplicateApiOptions,
  UpdateApi,
} from '../entities/management-api-v2';
import { ApiTransferOwnership } from '../entities/management-api-v2/api/apiTransferOwnership';
import { PathToVerify, VerifyApiPathResponse } from '../entities/management-api-v2/api/verifyApiPath';
import { VerifyApiHostsResponse } from '../entities/management-api-v2/api/verifyApiHosts';
import { VerifyApiDeployResponse } from '../entities/management-api-v2/api/verifyApiDeploy';
import { ImportSwaggerDescriptor } from '../entities/management-api-v2/api/v4/importSwaggerDescriptor';

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
      tap((api) => {
        this.lastApiFetch$.next(api);
      }),
    );
  }

  update(apiId: string, api: UpdateApi): Observable<Api> {
    return this.http.put<Api>(`${this.constants.env.v2BaseURL}/apis/${apiId}`, api).pipe(
      tap((api) => {
        this.lastApiFetch$.next(api);
      }),
    );
  }

  delete(apiId: string, closePlan = false): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}${closePlan ? '?closePlan=true' : ''}`);
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
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/_export/definition`, {
      responseType: 'blob',
      params: {
        ...(options?.excludeAdditionalData ? { excludeAdditionalData: options.excludeAdditionalData.join(',') } : {}),
      },
    });
  }

  import(importApi: any): Observable<ApiV4> {
    return this.http.post<ApiV4>(`${this.constants.env.v2BaseURL}/apis/_import/definition`, importApi, {
      headers: {
        'Content-Type': 'application/json',
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

  exportCRD(apiId: string): Observable<Blob> {
    return this.http.get(`${this.constants.env.v2BaseURL}/apis/${apiId}/_export/crd`, {
      responseType: 'blob',
    });
  }

  search(searchQuery?: ApiSearchQuery, sortBy?: ApiSortByParam, page = 1, perPage = 10, manageOnly = true): Observable<ApisResponse> {
    return this.http.post<ApisResponse>(`${this.constants.env.v2BaseURL}/apis/_search`, searchQuery, {
      params: {
        page,
        perPage,
        ...(sortBy ? { sortBy } : {}),
        ...(manageOnly ? {} : { manageOnly: false }),
      },
    });
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
      filter((api) => !!api),
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

  verifyPath(apiId: string, paths: PathToVerify[]): Observable<VerifyApiPathResponse> {
    return this.http.post<VerifyApiPathResponse>(`${this.constants.env.v2BaseURL}/apis/_verify/paths`, { apiId, paths });
  }

  verifyHosts(apiId: string, hosts: string[]): Observable<VerifyApiHostsResponse> {
    return this.http.post<VerifyApiHostsResponse>(`${this.constants.env.v2BaseURL}/apis/_verify/hosts`, { apiId, hosts });
  }

  rollback(apiId: string, eventId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/_rollback`, { eventId }).pipe(
      switchMap(() => this.get(apiId)),
      map(() => {}),
    );
  }
}
