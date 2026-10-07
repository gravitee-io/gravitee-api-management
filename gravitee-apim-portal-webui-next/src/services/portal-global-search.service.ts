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
import { catchError, forkJoin, from, map, mergeMap, Observable, of, switchMap, tap, toArray } from 'rxjs';

import { PortalCatalogSearchItem } from '../entities/portal-navigation/portal-catalog-search';
import { PortalNavigationItem, PortalNavigationPage } from '../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from './portal-navigation-items.service';

export type GlobalSearchResultKind = 'API' | 'API_PRODUCT' | 'FOLDER' | 'PAGE';

export interface GlobalSearchResult {
  kind: GlobalSearchResultKind;
  title: string;
  subtitle?: string;
  rootId: string;
  navItemId: string;
  matchIn?: 'title' | 'content';
}

const CONTENT_SEARCH_CONCURRENCY = 8;
const MAX_CONTENT_MATCHES = 30;

@Injectable({
  providedIn: 'root',
})
export class PortalGlobalSearchService {
  private readonly treeCache = new Map<string, PortalNavigationItem[]>();
  private catalogRootIdsCache: string[] | null = null;
  private readonly pageContentCache = new Map<string, string>();

  constructor(private readonly portalNavigationItemsService: PortalNavigationItemsService) {}

  search(query: string): Observable<GlobalSearchResult[]> {
    const normalizedQuery = query.trim();
    if (!normalizedQuery) {
      return of([]);
    }

    return forkJoin({
      catalog: this.portalNavigationItemsService.searchCatalogItems(1, normalizedQuery, 50).pipe(catchError(() => of({ data: [] }))),
      apis: this.portalNavigationItemsService
        .searchNavigationItemsWithApis(1, normalizedQuery, 50)
        .pipe(catchError(() => of({ data: [] }))),
    }).pipe(
      switchMap(({ catalog, apis }) => {
        const catalogResults = this.mapCatalogResults(catalog.data);
        const apiResults = apis.data.map(item => ({
          kind: 'API' as const,
          title: item.name,
          subtitle: item.version,
          rootId: item.rootId,
          navItemId: item.navItemId,
          matchIn: 'title' as const,
        }));
        const mergedApis = this.dedupeApiResults([...catalogResults, ...apiResults]);

        return this.ensureDocumentationTrees().pipe(
          switchMap(() => this.searchDocumentation(normalizedQuery)),
          map(docResults => this.dedupeApiResults([...mergedApis, ...docResults])),
        );
      }),
    );
  }

  private mapCatalogResults(items: PortalCatalogSearchItem[]): GlobalSearchResult[] {
    return items.map(item => {
      if (item.type === 'API') {
        return {
          kind: 'API' as const,
          title: item.name,
          subtitle: item.version,
          rootId: item.rootId,
          navItemId: item.navItemId,
          matchIn: 'title' as const,
        };
      }
      return {
        kind: 'API_PRODUCT' as const,
        title: item.name,
        subtitle: item.version,
        rootId: item.rootId,
        navItemId: item.navItemId,
        matchIn: 'title' as const,
      };
    });
  }

  private dedupeApiResults(results: GlobalSearchResult[]): GlobalSearchResult[] {
    const seen = new Set<string>();
    return results.filter(result => {
      const key = `${result.kind}:${result.navItemId}`;
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  }

  private ensureDocumentationTrees(): Observable<void> {
    return this.loadDocumentationRootIds().pipe(
      switchMap(rootIds => {
        const missingRootIds = rootIds.filter(rootId => !this.treeCache.has(rootId));
        if (missingRootIds.length === 0) {
          return of(undefined);
        }
        return forkJoin(
          missingRootIds.map(rootId =>
            this.portalNavigationItemsService.getNavigationItems('TOP_NAVBAR', true, rootId).pipe(
              tap(items => this.treeCache.set(rootId, items)),
              catchError(() => {
                this.treeCache.set(rootId, []);
                return of([]);
              }),
            ),
          ),
        ).pipe(map(() => undefined));
      }),
    );
  }

  private loadDocumentationRootIds(): Observable<string[]> {
    const navRoots = this.portalNavigationItemsService
      .topNavbarItems()
      .filter(item => item.type !== 'LINK')
      .map(item => item.id);

    if (this.catalogRootIdsCache) {
      return of([...new Set([...navRoots, ...this.catalogRootIdsCache])]);
    }

    return this.portalNavigationItemsService.searchCatalogItems(1, '', -1).pipe(
      map(response => {
        this.catalogRootIdsCache = [...new Set(response.data.map(item => item.rootId))];
        return [...new Set([...navRoots, ...this.catalogRootIdsCache])];
      }),
      catchError(() => of(navRoots)),
    );
  }

  private searchDocumentation(query: string): Observable<GlobalSearchResult[]> {
    const normalizedQuery = query.toLowerCase();
    const titleMatches: GlobalSearchResult[] = [];
    const pagesForContentSearch: { rootId: string; page: PortalNavigationPage }[] = [];

    for (const [rootId, items] of this.treeCache) {
      for (const item of items) {
        if (item.published === false) {
          continue;
        }
        if (item.type === 'FOLDER' || item.type === 'API' || item.type === 'API_PRODUCT') {
          if (item.title.toLowerCase().includes(normalizedQuery)) {
            titleMatches.push({
              kind: item.type,
              title: item.title,
              rootId,
              navItemId: item.id,
              matchIn: 'title',
            });
          }
        }
        if (item.type === 'PAGE') {
          if (item.title.toLowerCase().includes(normalizedQuery)) {
            titleMatches.push({
              kind: 'PAGE',
              title: item.title,
              rootId,
              navItemId: item.id,
              matchIn: 'title',
            });
          } else {
            pagesForContentSearch.push({ rootId, page: item });
          }
        }
      }
    }

    if (pagesForContentSearch.length === 0) {
      return of(titleMatches);
    }

    return from(pagesForContentSearch).pipe(
      mergeMap(
        ({ rootId, page }) => this.loadPagePlainText(page.id).pipe(map(content => ({ rootId, page, content }))),
        CONTENT_SEARCH_CONCURRENCY,
      ),
      mergeMap(({ rootId, page, content }) => {
        if (!content.includes(normalizedQuery)) {
          return of([] as GlobalSearchResult[]);
        }
        const match: GlobalSearchResult = {
          kind: 'PAGE',
          title: page.title,
          subtitle: this.buildContentExcerpt(content, normalizedQuery),
          rootId,
          navItemId: page.id,
          matchIn: 'content',
        };
        return of([match]);
      }, CONTENT_SEARCH_CONCURRENCY),
      toArray(),
      map(contentMatchBatches => {
        const fromContent = contentMatchBatches.flat().slice(0, MAX_CONTENT_MATCHES);
        return [...titleMatches, ...fromContent];
      }),
    );
  }

  private loadPagePlainText(pageId: string): Observable<string> {
    const cached = this.pageContentCache.get(pageId);
    if (cached !== undefined) {
      return of(cached);
    }

    return this.portalNavigationItemsService.getNavigationItemContent(pageId).pipe(
      map(response => this.normalizeContentForSearch(response.content ?? '')),
      tap(text => this.pageContentCache.set(pageId, text)),
      catchError(() => {
        this.pageContentCache.set(pageId, '');
        return of('');
      }),
    );
  }

  private normalizeContentForSearch(content: string): string {
    return content
      .replace(/```[\s\S]*?```/g, ' ')
      .replace(/`[^`]*`/g, ' ')
      .replace(/<[^>]+>/g, ' ')
      .replace(/[#>*_\[\]()!|-]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase();
  }

  private buildContentExcerpt(content: string, query: string): string {
    const index = content.indexOf(query);
    if (index < 0) {
      return '';
    }
    const start = Math.max(0, index - 40);
    const end = Math.min(content.length, index + query.length + 60);
    const prefix = start > 0 ? '…' : '';
    const suffix = end < content.length ? '…' : '';
    return `${prefix}${content.slice(start, end).trim()}${suffix}`;
  }
}
