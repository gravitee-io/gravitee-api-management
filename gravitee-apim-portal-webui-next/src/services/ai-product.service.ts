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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { AiProduct, AiProductsResponse } from '../entities/ai-product';

/** Read-only catalog access to AI Products for the Developer Portal (GET /api-products[, /{id}]). */
@Injectable({
  providedIn: 'root',
})
export class AiProductService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  /** List the AI Products available in the catalog. */
  list(): Observable<AiProductsResponse> {
    return this.http.get<AiProductsResponse>(`${this.configService.baseURL}/api-products`);
  }

  /** Get one AI Product together with its published plans (used to subscribe). */
  get(apiProductId: string): Observable<AiProduct> {
    return this.http.get<AiProduct>(`${this.configService.baseURL}/api-products/${apiProductId}`);
  }
}
