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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { Constants } from '../../../../entities/Constants';
import { ListParams } from '../models/ListParams';

@Injectable({
  providedIn: 'root',
})
export class PolicyStudioResourcesService {
  constructor(readonly http: HttpClient, @Inject('Constants') readonly constants: Constants) {}

  listResources(params: ListParams): Observable<ResourceListItem[]> {
    let httpParams = new HttpParams();

    if (params.expandSchema) {
      httpParams = httpParams.append('expand', 'schema');
    }
    if (params.expandIcon) {
      httpParams = httpParams.append('expand', 'icon');
    }

    return this.http.get<ResourceListItem[]>(`${this.constants.env.baseURL}/resources`, {
      params: httpParams,
    });
  }
}
