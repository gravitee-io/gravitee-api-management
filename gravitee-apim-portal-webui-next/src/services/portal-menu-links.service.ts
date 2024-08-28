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
import { catchError, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';

export type PortalMenuLinkType = 'external';

export interface PortalMenuLink {
  id: string;
  type: PortalMenuLinkType;
  name: string;
  target: string;
  order: number;
}

@Injectable({
  providedIn: 'root',
})
export class PortalMenuLinksService {
  public links: WritableSignal<PortalMenuLink[]> = signal([]);

  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  loadCustomLinks(): Observable<unknown> {
    return this.http.get<PortalMenuLink[]>(`${this.configService.baseURL}/portal-menu-links`).pipe(
      catchError(_ => of([])),
      tap(value => this.links.set(value)),
    );
  }
}
