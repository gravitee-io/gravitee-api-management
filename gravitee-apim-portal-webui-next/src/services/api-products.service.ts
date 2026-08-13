/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { ApiProduct } from '../entities/api-product/api-product';
import { PlansResponse } from '../entities/plan/plans-response';

@Injectable({
  providedIn: 'root',
})
export class ApiProductsService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  getById(apiProductId: string): Observable<ApiProduct> {
    return this.http.get<ApiProduct>(`${this.configService.baseURL}/api-products/${apiProductId}`);
  }

  listPlans(apiProductId: string): Observable<PlansResponse> {
    return this.http.get<PlansResponse>(`${this.configService.baseURL}/api-products/${apiProductId}/plans?size=-1`);
  }
}
