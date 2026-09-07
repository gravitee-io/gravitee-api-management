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
import { map, Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { AgentCatalogItem, AgentCatalogPage } from '../entities/agent/agent-catalog-info';

@Injectable({
  providedIn: 'root',
})
export class AgentCatalogService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  /**
   * Derives the Gamma catalog API base URL from the portal base URL.
   *
   * Portal baseURL shape: `{host}/portal/environments/{envId}`
   * Gamma catalog shape:  `{host}/gamma/organizations/{orgId}/environments/{envId}/modules/aim/catalog`
   */
  private buildGammaBaseUrl(orgId: string, envId: string): string {
    const portalBase = this.configService.baseURL;
    const hostEnd = portalBase.indexOf('/portal');
    const host = hostEnd >= 0 ? portalBase.substring(0, hostEnd) : portalBase;
    return `${host}/gamma/organizations/${orgId}/environments/${envId}/modules/aim/catalog`;
  }

  searchAgents(orgId: string, envId: string, query: string): Observable<AgentCatalogPage> {
    const baseUrl = this.buildGammaBaseUrl(orgId, envId);
    const params = new HttpParams().set('q', query).set('perPage', '5');
    return this.http.get<AgentCatalogPage>(`${baseUrl}/agents`, { params });
  }

  getAgentById(orgId: string, envId: string, agentId: string): Observable<AgentCatalogItem> {
    const baseUrl = this.buildGammaBaseUrl(orgId, envId);
    return this.http.get<AgentCatalogItem>(`${baseUrl}/agents/${agentId}`);
  }

  /**
   * Finds the catalog agent matching a given agent name.
   * Uses full-text search then picks the first exact name match.
   */
  findAgentByName(orgId: string, envId: string, agentName: string): Observable<AgentCatalogItem | null> {
    return this.searchAgents(orgId, envId, agentName).pipe(
      map(page => page.data?.find(a => a.definition?.name === agentName) ?? page.data?.[0] ?? null),
    );
  }
}
