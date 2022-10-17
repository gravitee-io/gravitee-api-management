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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { IScope } from 'angular';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { AjsRootScope } from '../ajs-upgraded-providers';
import { ConnectorListItem } from '../entities/connector/connector-list-item';

@Injectable({
  providedIn: 'root',
})
export class ConnectorService {
  constructor(
    private readonly http: HttpClient,
    @Inject('Constants') private readonly constants: Constants,
    @Inject(AjsRootScope) private readonly ajsRootScope: IScope,
  ) {}

  public list(expandSchema = false, expandIcon = false): Observable<ConnectorListItem[]> {
    let queryParams = new HttpParams();
    if (expandSchema) {
      queryParams = queryParams.append('expand', 'schema');
    }
    if (expandIcon) {
      queryParams = queryParams.append('expand', 'icon');
    }
    return this.http.get<ConnectorListItem[]>(`${this.constants.env.baseURL}/connectors`, { params: queryParams });
  }
}
