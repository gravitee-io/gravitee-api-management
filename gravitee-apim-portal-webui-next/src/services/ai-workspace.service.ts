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
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';

export interface AiWorkspaceBudget {
  amount?: number;
  period?: string;
}

export interface AiWorkspaceSummary {
  id: string;
  name?: string;
  description?: string;
  budget?: AiWorkspaceBudget;
}

export interface AiWorkspacesResponse {
  data?: AiWorkspaceSummary[];
  page?: number;
  size?: number;
  total?: number;
}

export interface AiWorkspaceKey {
  value?: string;
  status?: string;
  createdAt?: string;
}

export interface AiWorkspaceModel {
  name?: string;
  inputPrice?: number;
  outputPrice?: number;
}

export interface AiWorkspaceDetails {
  id: string;
  name?: string;
  description?: string;
  budget?: AiWorkspaceBudget;
  endpointUrl?: string;
  key?: AiWorkspaceKey;
  models?: AiWorkspaceModel[];
}

export interface AiWorkspaceConsumption {
  tokens?: number;
  requests?: number;
  cost?: number;
  from?: string;
  to?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AiWorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  list(page: number, size: number, name: string): Observable<AiWorkspacesResponse> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (name.trim()) {
      params = params.set('name', name.trim());
    }
    return this.http.get<AiWorkspacesResponse>(`${this.configService.baseURL}/ai-workspaces`, { params });
  }

  get(aiWorkspaceId: string): Observable<AiWorkspaceDetails> {
    return this.http.get<AiWorkspaceDetails>(`${this.configService.baseURL}/ai-workspaces/${aiWorkspaceId}`);
  }

  consumption(aiWorkspaceId: string): Observable<AiWorkspaceConsumption> {
    return this.http.get<AiWorkspaceConsumption>(`${this.configService.baseURL}/ai-workspaces/${aiWorkspaceId}/consumption`);
  }
}
