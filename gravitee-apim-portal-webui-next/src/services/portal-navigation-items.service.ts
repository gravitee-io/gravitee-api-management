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
import { Injectable, signal, WritableSignal } from '@angular/core';
import { catchError, map, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import {
  PortalNavigationApisSearchResponse,
  PortalNavigationApiSearchItem,
  PortalNavigationItemsSearchResponse,
} from '../entities/portal-navigation/portal-navigation-apis-search';
import { PortalNavigationApi, PortalNavigationItem, PortalArea } from '../entities/portal-navigation/portal-navigation-item';
import { PortalPageContent } from '../entities/portal-navigation/portal-page-content';

export interface ApiDocumentationNavigationTarget {
  rootId: string;
  navItemId: string;
}

@Injectable({
  providedIn: 'root',
})
export class PortalNavigationItemsService {
  public topNavbarItems: WritableSignal<PortalNavigationItem[]> = signal([]);

  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  getNavigationItems(area: PortalArea, loadChildren: boolean = true, parentId?: string): Observable<PortalNavigationItem[]> {
    const params = {
      ...(parentId ? { parentId } : {}),
      area,
      loadChildren,
    };

    return this.http
      .get<PortalNavigationItem[]>(`${this.configService.baseURL}/portal-navigation-items`, { params })
      .pipe(catchError(_ => of([])));
  }

  getNavigationItem(id: string): Observable<PortalNavigationItem> {
    return this.http.get<PortalNavigationItem>(`${this.configService.baseURL}/portal-navigation-items/${id}`).pipe(catchError(_ => of()));
  }

  getNavigationItemContent(id: string): Observable<PortalPageContent> {
    return this.http.get<PortalPageContent>(`${this.configService.baseURL}/portal-navigation-items/${id}/content`);
  }

  loadTopNavBarItems(): Observable<void> {
    return this.getNavigationItems('TOP_NAVBAR', false).pipe(
      switchMap(value => {
        this.topNavbarItems.set(value);
        return of(undefined);
      }),
    );
  }

  searchNavigationItemsWithApis(page = 1, query = '', size = 8): Observable<PortalNavigationApisSearchResponse> {
    const params: Record<string, string | number> = {
      type: 'api',
      include: 'api',
      page,
      size,
    };
    if (query) params['query'] = query;

    return this.http
      .get<PortalNavigationItemsSearchResponse>(`${this.configService.baseURL}/portal-navigation-items/_search`, { params })
      .pipe(map(res => this.mapToSearchResponse(res)));
  }

  private mapToSearchResponse(res: PortalNavigationItemsSearchResponse): PortalNavigationApisSearchResponse {
    const navItems = res.data ?? [];
    const apis = res.apis ?? [];
    const apiById = new Map(apis.map(api => [api.id, api]));

    const data: PortalNavigationApiSearchItem[] = navItems
      .filter((item): item is PortalNavigationApi => item.type === 'API')
      .flatMap(item => {
        const api = apiById.get(item.apiId);
        return api
          ? [
              {
                id: api.id,
                name: api.name,
                version: api.version,
                description: api.description,
                _links: api._links,
                mcp: api.mcp,
                labels: api.labels,
                rootId: item.rootId,
                navItemId: item.id,
              },
            ]
          : [];
      });

    return {
      data,
      metadata: res.metadata,
      links: res.links,
    };
  }
}
