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
import { Injectable } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { ApiSearchService, Api, ApiSearchResponse } from '../api-search.service';
import { getMockApiSearchResponse, getMockApiDetails } from './mock-data';

@Injectable({
  providedIn: 'root',
})
export class MockApiSearchService implements Pick<ApiSearchService, 'search' | 'details'> {
  
  search(page = 1, category: string = 'all', q: string = '', size = 9): Observable<ApiSearchResponse> {
    // Simulate network delay
    return of(getMockApiSearchResponse(page, category, q, size)).pipe(
      delay(300) // Simulate 300ms network delay
    );
  }

  details(apiId: string): Observable<Api> {
    const mockApi = getMockApiDetails(apiId);
    
    if (!mockApi) {
      throw new Error(`API with ID '${apiId}' not found`);
    }

    // Simulate network delay
    return of(mockApi).pipe(
      delay(200) // Simulate 200ms network delay
    );
  }
} 