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
import {
  AimAiWorkspace,
  AimBudget,
  AimCatalogModelPage,
  AimLlmProvider,
  AimWorkspaceUsageResponse,
} from '../entities/ai-workspace/aim-ai-workspace';

@Injectable({
  providedIn: 'root',
})
export class AimAiWorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  getById(workspaceId: string): Observable<AimAiWorkspace> {
    return this.http.get<AimAiWorkspace>(`${this.aimBaseUrl()}/ai-workspaces/${encodeURIComponent(workspaceId)}`);
  }

  listBudgets(workspaceId: string): Observable<AimBudget[]> {
    return this.http.get<AimBudget[]>(`${this.aimBaseUrl()}/ai-workspaces/${encodeURIComponent(workspaceId)}/budgets`);
  }

  listModels(workspaceId: string): Observable<AimLlmProvider[]> {
    return this.http.get<AimLlmProvider[]>(`${this.aimBaseUrl()}/ai-workspaces/${encodeURIComponent(workspaceId)}/models`);
  }

  listCatalogModelsBySource(sourceId: string, perPage = 1000): Observable<AimCatalogModelPage> {
    return this.http.get<AimCatalogModelPage>(`${this.aimBaseUrl()}/catalog/models`, {
      params: { sourceId, perPage: String(perPage) },
    });
  }

  getUsersUsage(workspaceId: string, page = 1, perPage = 1000): Observable<AimWorkspaceUsageResponse> {
    return this.http.get<AimWorkspaceUsageResponse>(
      `${this.aimBaseUrl()}/ai-workspaces/${encodeURIComponent(workspaceId)}/users/usage`,
      { params: { page: String(page), perPage: String(perPage) } },
    );
  }

  /**
   * Portal baseURL is `/portal/environments/{envId}`. AIM lives under
   * `/gamma/organizations/{orgId}/environments/{envId}/modules/aim`.
   */
  private aimBaseUrl(): string {
    const portalBase = this.configService.baseURL.replace(/\/$/, '');
    const environmentId = portalBase.split('/').pop() || 'DEFAULT';
    const origin = portalBase.startsWith('http') ? new URL(portalBase).origin : '';
    return `${origin}/gamma/organizations/DEFAULT/environments/${environmentId}/modules/aim`;
  }
}
