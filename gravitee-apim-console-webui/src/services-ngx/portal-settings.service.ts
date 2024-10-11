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
import { PortalSettings } from '../entities/portal/portalSettings';

@Injectable({
  providedIn: 'root',
})
export class PortalSettingsService {
  static isReadonly(settings: PortalSettings, property: string): boolean {
    if (settings && settings.metadata && settings.metadata.readonly) {
      return settings.metadata.readonly.some((key) => key === property);
    }
    return false;
  }

  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  get(): Observable<PortalSettings> {
    return this.http.get<PortalSettings>(`${this.constants.env.baseURL}/settings`);
  }

<<<<<<< HEAD
  save(portalSettings: PortalSettings): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/settings`, portalSettings);
=======
  getByEnvironmentId(environmentId: string): Observable<PortalSettings> {
    return this.http.get<PortalSettings>(`${this.constants.org.baseURL}/environments/${environmentId}/settings`);
  }

  save(portalSettings: PortalSettings): Observable<PortalSettings> {
    return this.http.post<PortalSettings>(`${this.constants.env.baseURL}/settings`, portalSettings).pipe(
      tap(() => {
        this.environmentSettingsService.load().subscribe();
      }),
    );
>>>>>>> a633a57155 (feat(console): org settings - allow to change portal default entrypoint for all env)
  }

  saveByEnvironmentId(environmentId: string, portalSettings: PortalSettings): Observable<PortalSettings> {
    return this.http.post<PortalSettings>(`${this.constants.org.baseURL}/environments/${environmentId}/settings`, portalSettings).pipe(
      tap(() => {
        this.environmentSettingsService.load().subscribe();
      }),
    );
  }
}
