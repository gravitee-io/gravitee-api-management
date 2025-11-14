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
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { PortalMenuLink } from '../entities/management-api-v2';

@Injectable({ providedIn: 'root' })
export class PortalContentService {
  constructor(@Inject(HttpClient) private readonly http: HttpClient) {}

  getMenuLinks(): Observable<PortalMenuLink[]> {
    return this.http.get<{ data: PortalMenuLink[] }>('assets/mocks/portal-menu-links.json').pipe(
      map((resp) => resp?.data ?? []),
      catchError(() => of([])),
    );
  }

  getPageContent(pageId: string, label: string): Observable<string> {
    const defaultContentOnError = `# ${label}\n\nFailed to load content.`;
    return this.http.get<{ data: Record<string, string> }>('assets/mocks/page-contents.json').pipe(
      map((resp) => resp?.data?.[pageId] ?? `# ${label}\n\nNo content available for id: ${pageId}.`),
      catchError(() => of(defaultContentOnError)),
    );
  }
}
