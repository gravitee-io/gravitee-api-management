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
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { Constants } from '../entities/Constants';
import { ApiType, ConnectorPlugin, ConnectorVM, MoreInformation } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ConnectorPluginsV2Service {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  listEndpointPlugins(): Observable<ConnectorPlugin[]> {
    return this.http.get<ConnectorPlugin[]>(`${this.constants.org.v2BaseURL}/plugins/endpoints`);
  }

  listEndpointPluginsByApiType(apiType: ApiType): Observable<ConnectorPlugin[]> {
    return this.listEndpointPlugins().pipe(
      map(endpointPlugins =>
        endpointPlugins
          .filter(endpoint => endpoint.supportedApiType === apiType)
          .sort((endpoint1, endpoint2) => {
            const name1 = endpoint1.name.toUpperCase();
            const name2 = endpoint2.name.toUpperCase();
            return name1 < name2 ? -1 : name1 > name2 ? 1 : 0;
          }),
      ),
    );
  }

  listEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.http.get<ConnectorPlugin[]>(`${this.constants.org.v2BaseURL}/plugins/entrypoints`);
  }

  listSyncEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.listEntrypointPlugins().pipe(
      map(entrypointPlugins => entrypointPlugins.filter(entrypoint => entrypoint.supportedApiType === 'PROXY')),
    );
  }

  listAsyncEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.listEntrypointPlugins().pipe(
      map(entrypointPlugins => entrypointPlugins.filter(entrypoint => entrypoint.supportedApiType === 'MESSAGE')),
    );
  }

  listAIEntrypointPlugins(): Observable<ConnectorPlugin[]> {
    return this.listEntrypointPlugins().pipe(
      map(entrypointPlugins =>
        entrypointPlugins.filter(entrypoint => {
          return ['LLM_PROXY', 'MCP_PROXY', 'A2A_PROXY'].includes(entrypoint.supportedApiType);
        }),
      ),
    );
  }

  getEndpointPlugin(id: string): Observable<ConnectorPlugin> {
    return this.http.get<ConnectorPlugin>(`${this.constants.org.v2BaseURL}/plugins/endpoints/${id}`);
  }

  getEndpointPluginSchema(id: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.org.v2BaseURL}/plugins/endpoints/${id}/schema`);
  }

  getEndpointPluginMoreInformation(endpointId: string): Observable<MoreInformation> {
    return this.http.get<MoreInformation>(`${this.constants.org.v2BaseURL}/plugins/endpoints/${endpointId}/more-information`);
  }

  getEndpointPluginSharedConfigurationSchema(id: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.org.v2BaseURL}/plugins/endpoints/${id}/shared-configuration-schema`);
  }

  getEntrypointPlugin(entrypointId: string): Observable<ConnectorPlugin> {
    return this.http.get<ConnectorPlugin>(`${this.constants.org.v2BaseURL}/plugins/entrypoints/${entrypointId}`);
  }

  getEntrypointPluginSchema(entrypointId: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.org.v2BaseURL}/plugins/entrypoints/${entrypointId}/schema`);
  }

  getEntrypointPluginSubscriptionSchema(entrypointId: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.org.v2BaseURL}/plugins/entrypoints/${entrypointId}/subscription-schema`);
  }

  getEntrypointPluginMoreInformation(entrypointId: string): Observable<MoreInformation> {
    return this.http.get<MoreInformation>(`${this.constants.org.v2BaseURL}/plugins/entrypoints/${entrypointId}/more-information`);
  }

  selectedPluginsNotAvailable(selectedIds: string[], connectorPlugins: ConnectorVM[]): boolean {
    if (!selectedIds || isEmpty(selectedIds)) {
      return false;
    }
    return selectedIds
      .map(id => connectorPlugins.find(connectorPlugin => connectorPlugin.id === id))
      .some(connectorPlugin => !connectorPlugin.deployed);
  }
}
