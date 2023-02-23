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

import { ConnectorListItem } from '../entities/connector/connector-list-item';
import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class EndpointService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  v4ListEndpointPlugins(): Observable<ConnectorListItem[]> {
    return this.http.get<ConnectorListItem[]>(`${this.constants.env.baseURL}/v4/endpoints`);
  }

  v4Get(id: string): Observable<ConnectorListItem> {
    return this.http.get<ConnectorListItem>(`${this.constants.env.baseURL}/v4/endpoints/${id}`);
  }

  v4GetSchema(id: string): Observable<GioJsonSchema> {
    return this.http.get<GioJsonSchema>(`${this.constants.env.baseURL}/v4/endpoints/${id}/schema`);
  }
}
