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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { PortalPageWithDetails } from '../entities/portal/portal-page-with-details';
import { PatchPortalPage } from '../entities/portal/patch-portal-page';
import { PortalNavigationItem, PortalNavigationItemsResponse, PortalNavigationPage } from '../entities/portal/portal-navigation-item';
import { PortalPageContent } from '../entities/portal/portal-page-content';

@Injectable({
  providedIn: 'root',
})
export class PortalPagesService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  /**
   * Get the homepage portal page
   */
  getHomepage(): Observable<{ navigationItem: PortalNavigationItem; content: PortalPageContent } | null> {
    const navUrl = `${this.constants.env.v2BaseURL}/portal-navigation-items?area=HOMEPAGE`;
    return this.http.get<PortalNavigationItemsResponse>(navUrl).pipe(
      map((response) => {
        const items = response?.items ?? [];
        if (items.length === 0) {
          return null as unknown as PortalNavigationPage | null;
        }
        const firstPageItem = items.find((i) => i.type === 'PAGE');
        if (!firstPageItem) {
          return null as unknown as PortalNavigationPage | null;
        }
        return firstPageItem as PortalNavigationPage;
      }),
      switchMap((portalNavigationPage) => {
        if (!portalNavigationPage) {
          return of(null);
        }
        return this.http
          .get<PortalPageContent>(
            `${this.constants.env.v2BaseURL}/portal-page-content/${portalNavigationPage.configuration.portalPageContentId}`,
          )
          .pipe(map((content) => ({ navigationItem: portalNavigationPage, content: content })));
      }),
    );
  }

  publishPage(pageId: string): Observable<PortalPageWithDetails> {
    return this.http.post<PortalPageWithDetails>(`${this.constants.env.v2BaseURL}/portal-pages/${pageId}/_publish`, {});
  }

  unpublishPage(pageId: string): Observable<PortalPageWithDetails> {
    return this.http.post<PortalPageWithDetails>(`${this.constants.env.v2BaseURL}/portal-pages/${pageId}/_unpublish`, {});
  }

  patchPortalPage(portalPageId: string, patchedPage: PatchPortalPage): Observable<PortalPageWithDetails> {
    return this.http.patch<PortalPageWithDetails>(`${this.constants.env.v2BaseURL}/portal-pages/${portalPageId}`, patchedPage);
  }
}
