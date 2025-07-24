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
import { Injectable, Inject } from '@angular/core';
import { Observable } from 'rxjs';

import { GRAVITEE_MARKDOWN_BASE_URL } from './configuration';

export interface ApiSearchResponse {
  data: Api[];
  metadata: {
    total: number;
    page: number;
    size: number;
  };
}

export interface Api {
  id: string;
  name: string;
  description?: string;
  version: string;
  state: string;
  contextPath: string;
  tags?: string[];
  categories?: string[];
}

@Injectable({
  providedIn: 'root',
})
export class ApiSearchService {
  constructor(
    private readonly http: HttpClient,
    @Inject(GRAVITEE_MARKDOWN_BASE_URL) private baseURL: string,
  ) {}

  search(page = 1, category: string = 'all', q: string = '', size = 9): Observable<ApiSearchResponse> {
    return this.http.post<ApiSearchResponse>(`${this.baseURL}/apis/_search`, null, {
      params: {
        page: page.toString(),
        category: category !== 'all' && category !== undefined ? category : '',
        size: size.toString(),
        q: q,
      },
    });
  }

  details(apiId: string): Observable<Api> {
    return this.http.get<Api>(`${this.baseURL}/apis/${apiId}`);
  }
} 