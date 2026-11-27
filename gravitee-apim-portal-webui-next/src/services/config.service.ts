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
import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { Configuration } from '../entities/configuration/configuration';

interface Config {
  baseURL: string;
  environmentId?: string;
  portalId?: unknown;
}

interface BootstrapConfig extends Config {
  portalId?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private readonly httpClient = inject(HttpClient);
  private readonly document = inject(DOCUMENT);
  private _baseURL: string = '';
  private _portalId = '';
  private _configuration: Configuration = {};

  public get baseURL(): string {
    return this._baseURL;
  }

  public get portalId(): string {
    return this._portalId;
  }

  public get portalBaseURL(): string {
    if (!this.portalId) {
      throw new Error('Portal ID is required');
    }
    return `${this.baseURL}/portal/${encodeURIComponent(this.portalId)}`;
  }

  public get configuration(): Configuration {
    return this._configuration ?? {};
  }

  private set baseURL(baseURL: string) {
    this._baseURL = baseURL;
  }

  private set configuration(configuration: Configuration) {
    this._configuration = configuration;
  }

  public initBaseURL(): Observable<string> {
    return this.httpClient.get<Config>('./assets/config.json').pipe(
      switchMap(configJson => {
        const configuredPortalId = this.readPortalId(configJson.portalId);
        const baseURL = this._sanitizeBaseURLs(configJson);
        const enforcedEnvironmentId = this._getEnforcedEnvironmentId(configJson);
        const bootstrapParams = new URLSearchParams();
        if (enforcedEnvironmentId) {
          bootstrapParams.set('environmentId', enforcedEnvironmentId);
        }
        // The Portal API is on another host, so it cannot tell from the request which address this app was opened at.
        bootstrapParams.set('host', this.document.location.host);
        const bootstrapUrl = `${baseURL}/ui/bootstrap?${bootstrapParams}`;

        return this.httpClient.get<BootstrapConfig>(bootstrapUrl).pipe(
          map(bootstrapConfig => {
            // Read before choosing: an invalid bootstrap portal ID fails startup even when one is configured.
            const bootstrapPortalId = this.readPortalId(bootstrapConfig.portalId);
            const portalId = configuredPortalId ?? bootstrapPortalId;
            if (!portalId) {
              throw new Error('Portal ID is required');
            }
            this._portalId = portalId;
            this.baseURL = `${bootstrapConfig.baseURL.replace(/\/$/, '')}/environments/${bootstrapConfig.environmentId}`;
            return this.baseURL;
          }),
        );
      }),
    );
  }

  public loadConfiguration(): Observable<Configuration> {
    return this.httpClient.get<Configuration>(`${this.baseURL}/configuration`).pipe(
      tap(resp => {
        this.configuration = resp;
      }),
      catchError(_ => {
        this.configuration = {};
        return of({});
      }),
    );
  }

  /**
   * When {@code portal.authentication.expose.accessToken} is disabled, strip IdP access_token from OAuth storage.
   */
  stripIdpAccessTokenFromStorage(): boolean {
    return this.configuration.authentication?.exposeAccessToken?.enabled === false;
  }

  private readPortalId(value: unknown): string | undefined {
    if (value == null) {
      return undefined;
    }
    if (typeof value !== 'string') {
      throw new Error('Portal ID must be a string');
    }
    const portalId = value.trim();
    if (portalId === '.' || portalId === '..') {
      throw new Error('Portal ID must not be a dot segment');
    }
    return portalId || undefined;
  }

  private _sanitizeBaseURLs(config: Config): string {
    let baseURL = config.baseURL;
    if (config.baseURL.endsWith('/')) {
      baseURL = config.baseURL.slice(0, -1);
    }
    const envIndex = baseURL.indexOf('/environments');
    if (envIndex >= 0) {
      baseURL = baseURL.substring(0, envIndex);
    }
    return baseURL;
  }

  private _getEnforcedEnvironmentId(config: Config): string | undefined {
    if (config.environmentId) {
      return config.environmentId;
    }
    const baseURL = config.baseURL;
    const envIndex = baseURL.indexOf('/environments/');
    if (envIndex >= 0) {
      const subPathWithEnv = baseURL.substring(envIndex, baseURL.length);
      const splitArr = subPathWithEnv.split('/');
      if (splitArr.length >= 3) {
        return splitArr[2];
      }
    }
    return '';
  }
}
