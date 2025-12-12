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
import { catchError, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import { PortalArea, PortalNavigationItem } from '../entities/portal-navigation/portal-navigation-item';

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

  getNavigationItemContent(id: string): Observable<string> {
    return this.http
      .get(`${this.configService.baseURL}/portal-navigation-items/${id}/content`, { responseType: 'text' })
      .pipe(catchError(_ => of('')));
  }

  loadTopNavBarItems(): Observable<void> {
    return this.getNavigationItems('TOP_NAVBAR', false).pipe(
      switchMap(value => {
        this.topNavbarItems.set(value);
        return of(undefined);
      }),
    );
  }
}
