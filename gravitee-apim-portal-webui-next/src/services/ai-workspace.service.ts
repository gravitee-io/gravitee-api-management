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
import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';

import { ConfigService } from './config.service';
import {
  AiWorkspace,
  AiWorkspaceConsumption,
  AiWorkspaceModelsResponse,
  AiWorkspacesResponse,
} from '../entities/ai-workspace/ai-workspace';

@Injectable({
  providedIn: 'root',
})
export class AiWorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  private get baseUrl(): string {
    return `${this.configService.baseURL}/ai-workspaces`;
  }

  list(page = 1, size = 20, name?: string): Observable<AiWorkspacesResponse> {
    let params = new HttpParams().set('page', page).set('size', size);
    const trimmed = name?.trim();
    if (trimmed) {
      params = params.set('name', trimmed);
    }
    return this.http.get<AiWorkspacesResponse>(this.baseUrl, { params });
  }

  get(aiWorkspaceId: string): Observable<AiWorkspace> {
    return this.http.get<AiWorkspace>(`${this.baseUrl}/${aiWorkspaceId}`);
  }

  getConsumption(aiWorkspaceId: string): Observable<AiWorkspaceConsumption> {
    return this.http.get<AiWorkspaceConsumption>(`${this.baseUrl}/${aiWorkspaceId}/consumption`).pipe(
      catchError(() =>
        of({
          tokens: 0,
          requests: 0,
          cost: 0,
          from: '',
          to: '',
        }),
      ),
    );
  }

  listModels(aiWorkspaceId: string, page = 1, size = 5): Observable<AiWorkspaceModelsResponse> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<AiWorkspaceModelsResponse>(`${this.baseUrl}/${aiWorkspaceId}/models`, { params }).pipe(
      catchError(
        (): Observable<AiWorkspaceModelsResponse> =>
          of({ data: [], metadata: { pagination: { current_page: 1, size, total: 0, total_pages: 0 } } }),
      ),
      map(
        (response): AiWorkspaceModelsResponse => ({
          data: response.data ?? [],
          metadata: response.metadata,
          links: response.links,
        }),
      ),
    );
  }
}
