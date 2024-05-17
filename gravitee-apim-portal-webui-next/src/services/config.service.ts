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
import { Injectable } from '@angular/core';
import { catchError, map, Observable, switchMap, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { Configuration } from '../entities/configuration/configuration';
import { ConfigurationPortalNext } from '../entities/configuration/configuration-portal-next';

/**
 * Object used in `config.json` file and `/ui/bootstrap` response
 */
interface Config {
  baseURL: string;
  environmentId: string;
}

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private _baseURL: string = '';
  private _configuration: Configuration = {};

  constructor(private httpClient: HttpClient) {}

  public get baseURL(): string {
    return this._baseURL;
  }

  public get portalNext(): ConfigurationPortalNext {
    return this._configuration.portalNext ?? {};
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
        const baseURL = this._sanitizeBaseURLs(configJson);
        const enforcedEnvironmentId = this._getEnforcedEnvironmentId(configJson);
        const bootstrapUrl = `${baseURL}/ui/bootstrap${enforcedEnvironmentId ? `?environmentId=${enforcedEnvironmentId}` : ''}`;

        return this.httpClient.get<Config>(bootstrapUrl);
      }),
      map((bootstrapConfig: Config) => {
        this.baseURL = `${bootstrapConfig.baseURL}/environments/${bootstrapConfig.environmentId}`;
        return this.baseURL;
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
