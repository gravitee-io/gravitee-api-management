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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';

export type McpProxyApiEntry = {
  id: string;
  name: string;
};

@Injectable()
export class McpProxyApiTypeService {
  private cachedApis: McpProxyApiEntry[] = [];
  private apis$: Observable<McpProxyApiEntry[]> | null = null;

  constructor(private readonly apiV2Service: ApiV2Service) {}

  loadApis$(): Observable<McpProxyApiEntry[]> {
    if (!this.apis$) {
      this.apis$ = this.apiV2Service.search({ apiTypes: ['V4_MCP_PROXY'] }, undefined, 1, 100, false).pipe(
        map((response) =>
          (response.data ?? []).map((api) => ({
            id: api.id,
            name: api.name,
          })),
        ),
        tap((apis) => {
          this.cachedApis = apis;
        }),
        shareReplay(1),
      );
    }
    return this.apis$;
  }

  filterApis$(term: string | undefined): Observable<McpProxyApiEntry[]> {
    return this.loadApis$().pipe(
      map((apis) => {
        if (isEmpty(term)) {
          return apis;
        }
        const lower = term.toLowerCase();
        return apis.filter((api) => api.name.toLowerCase().indexOf(lower) === 0);
      }),
    );
  }

  getApiName(id: string): string {
    const entry = this.cachedApis.find((api) => api.id === id);
    return entry ? entry.name : id;
  }

  apiExists(id: string): boolean {
    return this.cachedApis.some((api) => api.id === id);
  }
}
