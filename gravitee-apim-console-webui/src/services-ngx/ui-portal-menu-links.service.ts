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
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { CreatePortalMenuLink, PortalMenuLink, PortalMenuLinksResponse, UpdatePortalMenuLink } from '../entities/management-api-v2';
import { Constants } from '../entities/Constants';

@Injectable({ providedIn: 'root' })
export class UiPortalMenuLinksService {
  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly http: HttpClient,
  ) {}

  list(page = 1, perPage = 10): Observable<PortalMenuLinksResponse> {
    return this.http.get<PortalMenuLinksResponse>(`${this.constants.env.v2BaseURL}/ui/portal-menu-links`, {
      params: {
        page,
        perPage,
      },
    });
  }

  get(portalMenuLinkId: string): Observable<PortalMenuLink> {
    return this.http.get<PortalMenuLink>(`${this.constants.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLinkId}`);
  }

  update(portalMenuLinkId: string, updatePortalMenuLink: UpdatePortalMenuLink): Observable<PortalMenuLink> {
    return this.http.put<PortalMenuLink>(`${this.constants.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLinkId}`, updatePortalMenuLink);
  }

  create(createPortalMenuLink: CreatePortalMenuLink): Observable<PortalMenuLink> {
    return this.http.post<PortalMenuLink>(`${this.constants.env.v2BaseURL}/ui/portal-menu-links`, createPortalMenuLink);
  }

  delete(portalMenuLinkId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/ui/portal-menu-links/${portalMenuLinkId}`);
  }
}
