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
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';

export interface AimA2aProxyDetail {
  agentId?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class AimA2aProxyService {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);

  getA2aProxy(apiId: string): Observable<AimA2aProxyDetail> {
    return this.http.get<AimA2aProxyDetail>(`${this.aimBaseUrl()}/a2a-proxies/${encodeURIComponent(apiId)}`);
  }

  private aimBaseUrl(): string {
    const orgBase = this.constants.org.baseURL;
    const hostEnd = orgBase.indexOf('/management');
    const host = hostEnd >= 0 ? orgBase.substring(0, hostEnd) : orgBase;
    return `${host}/gamma/organizations/${this.constants.org.id}/environments/${this.constants.org.currentEnv.id}/modules/aim`;
  }
}
