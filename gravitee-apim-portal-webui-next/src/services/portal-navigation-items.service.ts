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
import { Injectable, signal, WritableSignal } from '@angular/core';
import { catchError, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import { PortalArea, PortalNavigationItem } from '../entities/portal-navigation/portal-navigation';

@Injectable({
  providedIn: 'root',
})
export class PortalNavigationItemsService {
  public topNavbar: WritableSignal<PortalNavigationItem[]> = signal([]);

  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  loadNavigationItems(area: PortalArea, loadChildren: boolean = true, parentId?: string): Observable<PortalNavigationItem[]> {
    let params = new HttpParams();
    params = params.set('area', area);
    if (loadChildren !== undefined) {
      params = params.set('loadChildren', String(loadChildren));
    }
    if (parentId) {
      params = params.set('parentId', parentId);
    }

    return this.http
      .get<PortalNavigationItem[]>(`${this.configService.baseURL}/portal-navigation-items`, { params })
      .pipe(catchError(_ => of([])));
  }

  loadTopNavBarItems(): Observable<PortalNavigationItem[]> {
    return this.loadNavigationItems('TOP_NAVBAR', true).pipe(tap(value => this.topNavbar.set(value)));
  }
}
