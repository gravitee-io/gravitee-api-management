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
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { ConnectorPlugin, MoreInformation } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ConnectorPluginsV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject('Constants') private readonly constants: Constants,
    private readonly matIconRegistry: MatIconRegistry,
    private _sanitizer: DomSanitizer,
  ) {}

  listEndpointPlugins(): Observable<ConnectorPlugin[]> {
    return this.http.get<ConnectorPlugin[]>(`${this.constants.v2BaseURL}/plugins/endpoints`);
  }

  private listEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.http.get<ConnectorPlugin[]>(`${this.constants.v2BaseURL}/plugins/entrypoints`);
  }

  listSyncEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.listEntrypointPlugins().pipe(
      map((entrypointPlugins) => entrypointPlugins.filter((entrypoint) => entrypoint.supportedApiType === 'PROXY')),
    );
  }

  listAsyncEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.listEntrypointPlugins().pipe(
      map((entrypointPlugins) => entrypointPlugins.filter((entrypoint) => entrypoint.supportedApiType === 'MESSAGE')),
    );
  }

  getEndpointPlugin(id: string): Observable<ConnectorPlugin> {
    return this.http.get<ConnectorPlugin>(`${this.constants.v2BaseURL}/plugins/endpoints/${id}`);
  }

  getEndpointPluginSchema(id: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.v2BaseURL}/plugins/endpoints/${id}/schema`);
  }

  getEndpointPluginMoreInformation(endpointId: string): Observable<MoreInformation> {
    return this.http.get<MoreInformation>(`${this.constants.v2BaseURL}/plugins/endpoints/${endpointId}/more-information`);
  }

  getEndpointPluginSharedConfigurationSchema(id: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.v2BaseURL}/plugins/endpoints/${id}/shared-configuration-schema`);
  }

  getEntrypointPluginSchema(entrypointId: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.v2BaseURL}/plugins/entrypoints/${entrypointId}/schema`);
  }

  getEntrypointPluginMoreInformation(entrypointId: string): Observable<MoreInformation> {
    return this.http.get<MoreInformation>(`${this.constants.v2BaseURL}/plugins/entrypoints/${entrypointId}/moreInformation`);
  }
}
