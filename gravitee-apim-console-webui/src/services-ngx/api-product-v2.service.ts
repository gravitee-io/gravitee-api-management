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

import { Constants } from '../entities/Constants';
import { ApiProduct, ApiProductsResponse, CreateApiProduct, UpdateApiProduct } from '../entities/management-api-v2/api-product';

@Injectable({
  providedIn: 'root',
})
export class ApiProductV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

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
   * Get a single API Product by ID
   * Calls GET /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @returns Observable of the API Product
   */
  get(apiProductId: string): Observable<ApiProduct> {
    return this.http.get<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`);
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
    return this.http.put<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`, updateApiProduct);
  }

  /**
   * Update API Product's APIs list
   * Calls PUT /environments/{envId}/api-products/{apiProductId}
   * @param apiProductId - The API Product ID
   * @param apiIds - The new list of API IDs
   * @returns Observable of the updated API Product
   */
  updateApiProductApis(apiProductId: string, apiIds: string[]): Observable<ApiProduct> {
    return this.http.put<ApiProduct>(`${this.constants.env.v2BaseURL}/api-products/${apiProductId}`, {
      apiIds,
    });
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
}
