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
import { isNil } from 'lodash';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { Page, PageType } from '../entities/page';

export interface DocumentationQuery {
  api?: string;
  name?: string;
  type?: string;
  homepage?: boolean;
  published?: boolean;
  parent?: string;
  root?: boolean;
  translated?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class DocumentationService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  apiSearch(apiId: string, q?: DocumentationQuery): Observable<Page[]> {
    const queryParams = Object.entries(q ?? []).map(([key, value]) => (isNil(value) ? null : `${key}=${value}`));

    return this.http.get<Page[]>(
      `${this.constants.env.baseURL}/apis/${apiId}/pages${queryParams.length ? `?${queryParams.join('&')}` : ''}`,
    );
  }

  listPortalPages(type: PageType, published: boolean): Observable<Page[]> {
    const queryParams = [];
    if (type) {
      queryParams.push(`type=${type}`);
    }
    if (published) {
      queryParams.push(`published=${published}`);
    }

    return this.http.get<Page[]>(`${this.constants.env.baseURL}/portal/pages${queryParams.length ? `?${queryParams.join('&')}` : ''}`);
  }
}
