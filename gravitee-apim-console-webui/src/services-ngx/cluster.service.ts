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
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';

import {
  Cluster,
  ClustersSortByParam,
  ClusterType,
  CreateCluster,
  DeployedCluster,
  PagedResult,
  UpdateCluster,
} from '../entities/management-api-v2';
import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class ClusterService {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);

  list(searchQuery?: string, sortBy?: ClustersSortByParam, page = 1, perPage = 25, type?: ClusterType): Observable<PagedResult<Cluster>> {
    let params = new HttpParams();
    params = params.append('page', page);
    params = params.append('perPage', perPage);
    if (searchQuery) {
      params = params.append('q', searchQuery);
    }
    if (sortBy) {
      params = params.append('sortBy', sortBy);
    }
    if (type) {
      params = params.append('type', type);
    }

    return this.http.get<PagedResult<Cluster>>(`${this.constants.env.v2BaseURL}/clusters`, { params });
  }

  get(id: string): Observable<Cluster> {
    return this.http.get<Cluster>(`${this.constants.env.v2BaseURL}/clusters/${id}`);
  }

  create(createCluster: CreateCluster): Observable<Cluster> {
    return this.http.post<Cluster>(`${this.constants.env.v2BaseURL}/clusters`, createCluster);
  }

  update(id: string, cluster: UpdateCluster): Observable<Cluster> {
    return this.http.put<Cluster>(`${this.constants.env.v2BaseURL}/clusters/${id}`, cluster);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/clusters/${id}`);
  }

  updateGroups(id: string, groups: string[]): Observable<Cluster> {
    return this.http.put<Cluster>(`${this.constants.env.v2BaseURL}/clusters/${id}/groups`, groups);
  }

  getPermissions(clusterId: string): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.constants.env.v2BaseURL}/clusters/${clusterId}/permissions`);
  }

  deploy(id: string): Observable<Cluster> {
    return this.http.post<Cluster>(`${this.constants.env.v2BaseURL}/clusters/${id}/_deploy`, {});
  }

  undeploy(id: string): Observable<Cluster> {
    return this.http.post<Cluster>(`${this.constants.env.v2BaseURL}/clusters/${id}/_undeploy`, {});
  }

  listDeployed(): Observable<DeployedCluster[]> {
    return this.http.get<DeployedCluster[]>(`${this.constants.env.v2BaseURL}/clusters/deployed`);
  }

  getConfigurationSchema(type?: ClusterType): Observable<unknown> {
    let params = new HttpParams();
    if (type) {
      params = params.append('type', type);
    }
    return this.http.get<unknown>(`${this.constants.env.v2BaseURL}/clusters/schema/configuration`, { params });
  }
}
