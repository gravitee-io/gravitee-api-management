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
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { PortalPageWithDetails } from '../entities/portal/portal-page-with-details';
import { PatchPortalPage } from '../entities/portal/patch-portal-page';
import { PortalPagesResponse } from '../entities/portal/portal-pages-response';

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
  getHomepage(): Observable<PortalPagesResponse> {
    return this.http.get<PortalPagesResponse>(`${this.constants.env.v2BaseURL}/portal-pages?type=homepage&expands=content`);
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
