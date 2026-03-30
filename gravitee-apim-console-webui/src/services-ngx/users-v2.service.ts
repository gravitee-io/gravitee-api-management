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

import { Constants } from '../entities/Constants';

export interface UserApi {
  id: string;
  name: string;
  version: string;
  visibility: string;
  environmentId: string;
  environmentName: string;
}

export interface UserApiProduct {
  id: string;
  name: string;
  version: string;
  visibility?: string;
  environmentId: string;
  environmentName: string;
}

export interface UserApplication {
  id: string;
  name: string;
  environmentId: string;
  environmentName: string;
}

export interface UserGroupResponse {
  id: string;
  name: string;
  environmentId: string;
  environmentName: string;
  roles: Record<string, string>;
  isApiPrimaryOwner?: boolean;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    perPage: number;
    pageCount: number;
    pageItemsCount: number;
    totalCount: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class UsersV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getUserApis(userId: string, environmentId: string, page = 1, perPage = 10): Observable<PaginatedResponse<UserApi>> {
    return this.http.get<PaginatedResponse<UserApi>>(`${this.constants.org.v2BaseURL}/users/${userId}/apis`, {
      params: { environmentId, page: page.toString(), perPage: perPage.toString() },
    });
  }

  getUserApiProducts(userId: string, environmentId: string, page = 1, perPage = 10): Observable<PaginatedResponse<UserApiProduct>> {
    return this.http.get<PaginatedResponse<UserApiProduct>>(`${this.constants.org.v2BaseURL}/users/${userId}/api-products`, {
      params: { environmentId, page: page.toString(), perPage: perPage.toString() },
    });
  }

  getUserApplications(userId: string, environmentId: string, page = 1, perPage = 10): Observable<PaginatedResponse<UserApplication>> {
    return this.http.get<PaginatedResponse<UserApplication>>(`${this.constants.org.v2BaseURL}/users/${userId}/applications`, {
      params: { environmentId, page: page.toString(), perPage: perPage.toString() },
    });
  }

  getUserGroups(userId: string, environmentId: string, page = 1, perPage = 10): Observable<PaginatedResponse<UserGroupResponse>> {
    return this.http.get<PaginatedResponse<UserGroupResponse>>(`${this.constants.org.v2BaseURL}/users/${userId}/groups`, {
      params: { environmentId, page: page.toString(), perPage: perPage.toString() },
    });
  }
}
