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
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { map } from 'rxjs/operators';

import { ConnectorListItem } from '../entities/connector/connector-list-item';
import { Constants } from '../entities/Constants';
import { Entrypoint } from '../entities/entrypoint/entrypoint';
import { PluginMoreInformation } from '../entities/plugin/PluginMoreInformation';
import { toQueryParams, ListPluginsExpand } from '../entities/plugin/ListPluginsExpand';

@Injectable({
  providedIn: 'root',
})
export class EntrypointService {
  constructor(
    private readonly http: HttpClient,
    @Inject('Constants') private readonly constants: Constants,
    private readonly matIconRegistry: MatIconRegistry,
    private _sanitizer: DomSanitizer,
  ) {}

  list(): Observable<Entrypoint[]> {
    return this.http.get<Entrypoint[]>(`${this.constants.org.baseURL}/configuration/entrypoints`);
  }

  create(entrypoint: Entrypoint) {
    return this.http.post<void>(`${this.constants.org.baseURL}/configuration/entrypoints/`, entrypoint);
  }

  update(entrypoint: Entrypoint): Observable<void> {
    return this.http.put<void>(`${this.constants.org.baseURL}/configuration/entrypoints/`, entrypoint);
  }

  delete(entrypointId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/configuration/entrypoints/${entrypointId}`);
  }

  private v4ListEntrypointPlugins(expands: ListPluginsExpand[] = ['icon']): Observable<ConnectorListItem[]> {
    return this.http.get<ConnectorListItem[]>(`${this.constants.env.baseURL}/v4/entrypoints${toQueryParams(expands)}`);
  }

  v4ListSyncEntrypointPlugins(expands: ListPluginsExpand[] = ['icon']): Observable<ConnectorListItem[]> {
    return this.v4ListEntrypointPlugins(expands).pipe(
      map((entrypointPlugins) => entrypointPlugins.filter((entrypoint) => entrypoint.supportedApiType === 'proxy')),
    );
  }

  v4ListAsyncEntrypointPlugins(expands: ListPluginsExpand[] = ['icon']): Observable<ConnectorListItem[]> {
    return this.v4ListEntrypointPlugins(expands).pipe(
      map((entrypointPlugins) => entrypointPlugins.filter((entrypoint) => entrypoint.supportedApiType === 'message')),
    );
  }

  v4GetSchema(entrypointId: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.env.baseURL}/v4/entrypoints/${entrypointId}/schema`);
  }

  v4GetMoreInformation(entrypointId: string): Observable<PluginMoreInformation> {
    return this.http.get<PluginMoreInformation>(`${this.constants.env.baseURL}/v4/entrypoints/${entrypointId}/moreInformation`);
  }
}
