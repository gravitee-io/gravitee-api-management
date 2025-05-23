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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import {
  CreateDocumentation,
  Breadcrumb,
  Page,
  EditDocumentation,
  ApiLifecycleState,
  SUPPORTED_FOR_EDIT,
} from '../entities/management-api-v2';

export interface ApiDocumentationPageResult {
  pages: Page[];
  breadcrumb: Breadcrumb[];
}
@Injectable({
  providedIn: 'root',
})
export class ApiDocumentationV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}
  createDocumentationPage(apiId: string, createDocumentation: CreateDocumentation): Observable<Page> {
    return this.http.post<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages`, createDocumentation);
  }
  getApiPages(apiId: string, parentId?: string): Observable<ApiDocumentationPageResult> {
    const url = parentId
      ? `${this.constants.env.v2BaseURL}/apis/${apiId}/pages?parentId=${parentId}`
      : `${this.constants.env.v2BaseURL}/apis/${apiId}/pages`;

    return this.http.get<ApiDocumentationPageResult>(url).pipe(
      map((result: ApiDocumentationPageResult) => {
        const filteredResult: ApiDocumentationPageResult = {
          pages: result.pages.filter((page) => page.type === 'FOLDER' || SUPPORTED_FOR_EDIT.includes(page.type)),
          breadcrumb: result.breadcrumb,
        };
        return filteredResult;
      }),
      map((result: ApiDocumentationPageResult) => {
        const sortedResult: ApiDocumentationPageResult = {
          pages: result.pages,
          breadcrumb: result.breadcrumb?.sort((b1, b2) => b1.position - b2.position),
        };
        return sortedResult;
      }),
    );
  }

  getApiPage(apiId: string, pageId: string): Observable<Page> {
    return this.http.get<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}`);
  }

  updateDocumentationPage(apiId: string, pageId: string, editPage: EditDocumentation): Observable<Page> {
    return this.http.put<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}`, editPage);
  }

  publishDocumentationPage(apiId: string, pageId: string): Observable<Page> {
    return this.http.post<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}/_publish`, {});
  }

  unpublishDocumentationPage(apiId: any, pageId: string): Observable<Page> {
    return this.http.post<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}/_unpublish`, {});
  }

  fetchDocumentationPage(apiId: any, pageId: string): Observable<Page> {
    return this.http.post<Page>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}/_fetch`, {});
  }

  deleteDocumentationPage(apiId: string, pageId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${apiId}/pages/${pageId}`, {});
  }

  getApiNotInPortalTooltip(apiLifecycleState: ApiLifecycleState): string {
    switch (apiLifecycleState) {
      case 'DEPRECATED':
        return 'Deprecated APIs do not appear in the Developer Portal';
      case 'ARCHIVED':
        return 'Archived APIs do not appear in the Developer Portal';
      case 'CREATED':
      case 'UNPUBLISHED':
        return "Activate the Developer Portal by publishing your API under 'General > Info'";
      case 'PUBLISHED':
      default:
        return undefined;
    }
  }
  getApiPortalUrl(apiId: string): string {
    const portalUrl = this.constants.env.settings?.portal?.url;
    if (apiId && portalUrl) {
      const apiPath = 'catalog/api/' + apiId;
      const connector = '/';

      return portalUrl.endsWith(connector) ? portalUrl + apiPath : portalUrl + connector + apiPath;
    }
    return undefined;
  }
}
