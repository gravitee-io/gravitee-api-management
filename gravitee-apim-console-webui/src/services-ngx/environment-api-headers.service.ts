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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { ApiPortalHeader } from '../entities/apiPortalHeader';
import { ApiPortalHeaderEditDialogResult } from '../management/settings/api-portal-header/migrated/api-portal-header-edit-dialog/api-portal-header-edit-dialog.component';

@Injectable({
  providedIn: 'root',
})
export class EnvironmentApiHeadersService {
  private headersList$: BehaviorSubject<ApiPortalHeader[]> = new BehaviorSubject([]);

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private httpClient: HttpClient,
  ) {}

  public getHeadersList$(): Observable<ApiPortalHeader[]> {
    return this.headersList$.asObservable();
  }

  getApiHeaders(): Observable<ApiPortalHeader[]> {
    return this.httpClient.get<ApiPortalHeader[]>(`${this.constants.env.baseURL}/configuration/apiheaders/`).pipe(
      tap((apiPortalHeaders: ApiPortalHeader[]) => {
        this.headersList$.next(apiPortalHeaders);
      }),
    );
  }

  createApiHeader(headerDialogResult: ApiPortalHeaderEditDialogResult): Observable<ApiPortalHeader[]> {
    return this.httpClient
      .post(`${this.constants.env.baseURL}/configuration/apiheaders/`, headerDialogResult)
      .pipe(switchMap(() => this.getApiHeaders()));
  }

  updateApiHeader(apiPortalHeader: ApiPortalHeader): Observable<ApiPortalHeader[]> {
    return this.httpClient
      .put(`${this.constants.env.baseURL}/configuration/apiheaders/` + apiPortalHeader.id, {
        name: apiPortalHeader.name,
        value: apiPortalHeader.value,
        order: apiPortalHeader.order,
      })
      .pipe(switchMap(() => this.getApiHeaders()));
  }

  deleteApiHeader(apiPortalHeader: ApiPortalHeader): Observable<ApiPortalHeader[]> {
    return this.httpClient
      .delete(`${this.constants.env.baseURL}/configuration/apiheaders/` + apiPortalHeader.id)
      .pipe(switchMap(() => this.getApiHeaders()));
  }
}
