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
import { Inject, Injectable, signal } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { distinctUntilChanged, filter, shareReplay, switchMap, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { Constants } from '../entities/Constants';
import { ApisResponse } from '../entities/management-api-v2/apisResponse';
import {
  ApiProduct,
  ApiProductSearchQuery,
  ApiProductsResponse,
  CreateApiProduct,
  toApiProductSortByParam,
  UpdateApiProduct,
  VerifyApiProductDeployResponse,
} from '../entities/management-api-v2/api-product';

@Injectable({
  providedIn: 'root',
})
export class ApiProductV2Service {
  private readonly _planStateVersion = signal(0);
  readonly planStateVersion = this._planStateVersion.asReadonly();

  private readonly lastApiProductFetch$ = new BehaviorSubject<ApiProduct | null>(null);

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  /**
   * Notifies subscribers to refetch the API Product (deployment state, verify deploy).
   * Call after plan changes (create, update, publish, deprecate, close, reorder, delete) or after
   * definition changes that affect gateway sync (e.g. API list on the product). PUT responses may
   * omit computed deploymentState; this forces a GET so the "Deploy API Product" banner stays in sync.
   */
  notifyApiProductChanged(): void {
    this._planStateVersion.update(v => v + 1);
  }

  /**
   * Create a new API Product
   * Calls POST /environments/{envId}/api-products
   * @param newApiProduct - The API Product data to create (name, version, description, apiIds)
   * @returns Observable of the created API Product
   */
  create(newApiProduct: CreateApiProduct): Observable<ApiProduct> {
    return this.http.post<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products`, newApiProduct);
  }

  /**
   * Get list of API Products
   * Calls GET /environments/{envId}/api-products
   * @param page - Page number (starting from 1)
   * @param perPage - Number of items per page (1-100)
   * @returns Observable of ApiProductsResponse with paginated data
   */
  list(page = 1, perPage = 10): Observable<ApiProductsResponse> {
    const params = new HttpParams().set('page', page.toString()).set('perPage', perPage.toString());
    return this.http.get<ApiProductsResponse>(`${this.constants.env.v2BaseURL}/api-products`, { params });
  }

  /**
   * Search API Products by name or IDs (same as APIs list)
   * Calls POST /environments/{envId}/api-products/_search
   * @param searchQuery - Optional query (text search on name/description/owner) and/or ids
   * @param sortBy - Sort by name, version, apis, or owner; prefix "-" for desc. Invalid values are ignored.
   * @param page - Page number (starting from 1)
   * @param perPage - Number of items per page (1-100)
   * @returns Observable of ApiProductsResponse with paginated data
   */
  search(searchQuery: ApiProductSearchQuery = {}, sortBy?: string, page = 1, perPage = 10): Observable<ApiProductsResponse> {
    let params = new HttpParams().set('page', page.toString()).set('perPage', perPage.toString());
    const validSortBy = toApiProductSortByParam(sortBy);
    if (validSortBy) {
      params = params.set('sortBy', validSortBy);
    }
    return this.http.post<ApiProductsResponse>(`${this.constants.env.v2BaseURL}/api-products/_search`, searchQuery, {
      params,
    });
  }

  /**
   * Get paginated list of APIs in an API Product with optional search and sort.
   * Calls GET /environments/{envId}/api-products/{apiProductId}/apis
   * @param apiProductId - The API Product ID
   * @param page - Page number (1-based)
   * @param perPage - Page size
   * @param query - Optional search query (searches name)
   * @param sortBy - Optional sort (e.g. "name", "-name", "paths", "-paths")
   * @returns Observable of ApisResponse (data, pagination, links)
   */
  getApis(apiProductId: string, page = 1, perPage = 10, query = '', sortBy?: string): Observable<ApisResponse> {
    let params = new HttpParams().set('page', page.toString()).set('perPage', perPage.toString());
    if (query?.trim()) {
      params = params.set('query', query.trim());
    }
    if (sortBy?.trim()) {
      params = params.set('sortBy', sortBy.trim());
    }
    return this.http.get<ApisResponse>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/apis`, {
      params,
    });
  }

  /**
   * Get a single API Product by ID
   * Calls GET /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @returns Observable of the API Product
   */
  get(apiProductId: string): Observable<ApiProduct> {
    return this.http
      .get<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`)
      .pipe(tap(api => this.lastApiProductFetch$.next(api)));
  }

  /**
   * Returns the last fetched API Product, refetching if the cache is empty or for a different ID.
   * Subscribers receive updates when the product is refreshed (e.g. after deploy or plan changes).
   */
  getLastApiProductFetch(apiProductId: string): Observable<ApiProduct> {
    const start = this.lastApiProductFetch$.value?.id === apiProductId ? of(this.lastApiProductFetch$.value) : this.get(apiProductId);
    return start.pipe(
      switchMap(() => this.lastApiProductFetch$.asObservable()),
      filter((api): api is ApiProduct => api !== null && api.id === apiProductId),
      distinctUntilChanged(isEqual),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  /**
   * Refreshes the cached API Product by re-fetching from the server.
   * Use when plan state or deployment state may have changed (e.g. after plan publish, deploy).
   */
  refreshLastApiProductFetch(apiProductId: string): Observable<ApiProduct> {
    return this.get(apiProductId);
  }

  /**
   * Delete an API from an API Product
   * Calls DELETE /environments/{envId}/api-products/{apiProductId}/apis/{apiId}
   * @param apiProductId - The API Product ID
   * @param apiId - The API ID to remove
   * @returns Observable of void
   */
  deleteApiFromApiProduct(apiProductId: string, apiId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/apis/${apiId}`);
  }

  /**
   * Delete all APIs from an API Product
   * Calls DELETE /environments/{envId}/api-products/{apiProductId}/apis
   * @param apiProductId - The API Product ID
   * @returns Observable of void
   */
  deleteAllApisFromApiProduct(apiProductId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/apis`);
  }

  /**
   * Update an API Product
   * Calls PUT /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @param updateApiProduct - The API Product data to update (name, version, description, apiIds)
   * @returns Observable of the updated API Product
   */
  update(apiProductId: string, updateApiProduct: UpdateApiProduct): Observable<ApiProduct> {
    return this.http
      .put<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`, updateApiProduct)
      .pipe(tap(api => this.lastApiProductFetch$.next(api)));
  }

  /**
   * Update API Product's APIs list
   * Calls PUT /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @param apiIds - The new list of API IDs
   * @returns Observable of the updated API Product
   */
  updateApiProductApis(apiProductId: string, apiIds: string[]): Observable<ApiProduct> {
    return this.http
      .put<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`, {
        apiIds,
      })
      .pipe(tap(api => this.lastApiProductFetch$.next(api)));
  }

  /**
   * Delete an API Product
   * Calls DELETE /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @returns Observable of void
   */
  delete(apiProductId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`);
  }

  /**
   * Deploy an API Product to gateway instances
   * Calls POST /environments/{envId}/api-products/{apiProductId}/deployments
   * @param apiProductId - The API Product ID
   * @returns Observable of the deployed API Product
   */
  deploy(apiProductId: string): Observable<ApiProduct> {
    return this.http
      .post<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/deployments`, {})
      .pipe(tap(api => this.lastApiProductFetch$.next(api)));
  }

  /**
   * Check whether an API Product can be deployed (license check)
   * Calls GET /environments/{envId}/api-products/{apiProductId}/deployments/_verify
   * @param apiProductId - The API Product ID
   * @returns Observable with ok boolean and optional reason
   */
  verifyDeploy(apiProductId: string): Observable<VerifyApiProductDeployResponse> {
    return this.http.get<VerifyApiProductDeployResponse>(
      `${this.constants.env.v2BaseURL}/api-products/${apiProductId}/deployments/_verify`,
    );
  }

  /**
   * Verify if an API Product name is unique
   * Calls POST /environments/{envId}/api-products/_verify
   * @param name - The API Product name to verify
   * @returns Observable with ok boolean and optional reason
   */
  verify(name: string): Observable<{ ok: boolean; reason?: string }> {
    return this.http.post<{ ok: boolean; reason?: string }>(`${this.constants.env.v2BaseURL}/api-products/_verify`, {
      name,
    });
  }

  /**
   * Get the current user's permissions for a given API Product
   * Calls GET /environments/{envId}/api-products/{apiProductId}/members/permissions
   * @param apiProductId - The API Product ID
   * @returns Observable of permission map, where each value is a string of permission characters (e.g. { PLAN: 'CRUD' })
   */
  getPermissions(apiProductId: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}/members/permissions`);
  }
}
