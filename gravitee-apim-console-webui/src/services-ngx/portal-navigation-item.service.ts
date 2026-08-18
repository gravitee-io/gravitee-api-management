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

import { Constants } from '../entities/Constants';
import {
  FetchPortalNavigationItemResponse,
  NewPortalNavigationItem,
  PortalArea,
  PortalNavigationItem,
  PortalNavigationItemsResponse,
  UpdatePortalNavigationItem,
} from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class PortalNavigationItemService {
  constructor(
    private http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getNavigationItems(portalArea: PortalArea, includes?: string[]): Observable<PortalNavigationItemsResponse> {
    const includesParam = includes?.length ? `&includes=${includes.join(',')}` : '';
    return this.http.get<PortalNavigationItemsResponse>(
      `${this.constants.env.v2BaseURL}/portal-navigation-items?area=${portalArea}${includesParam}`,
    );
  }

  public createNavigationItem(newPortalNavigationItem: NewPortalNavigationItem): Observable<PortalNavigationItem> {
    return this.http.post<PortalNavigationItem>(`${this.constants.env.v2BaseURL}/portal-navigation-items`, newPortalNavigationItem);
  }

  public createNavigationItemsInBulk(items: NewPortalNavigationItem[]): Observable<PortalNavigationItemsResponse> {
    return this.http.post<PortalNavigationItemsResponse>(`${this.constants.env.v2BaseURL}/portal-navigation-items/_bulk`, { items });
  }

  public seedDefaultPages(ids: string[]): Observable<void> {
    return this.http.post<void>(`${this.constants.env.v2BaseURL}/portal-navigation-items/_default-pages`, { ids });
  }

  public updateNavigationItem(
    portalNavigationItemId: string,
    updatePortalNavigationItem: UpdatePortalNavigationItem,
    propagatePublishToChildren = false,
  ): Observable<PortalNavigationItem> {
    const params = propagatePublishToChildren ? { propagatePublishToChildren } : {};

    return this.http.put<PortalNavigationItem>(
      `${this.constants.env.v2BaseURL}/portal-navigation-items/${portalNavigationItemId}`,
      updatePortalNavigationItem,
      { params },
    );
  }

  public fetchNavigationItem(portalNavigationItemId: string): Observable<FetchPortalNavigationItemResponse> {
    return this.http.post<FetchPortalNavigationItemResponse>(
      `${this.constants.env.v2BaseURL}/portal-navigation-items/${portalNavigationItemId}/_fetch`,
      null,
    );
  }

  public deleteNavigationItem(portalNavigationItemId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/portal-navigation-items/${portalNavigationItemId}`);
  }
}
