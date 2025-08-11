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
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';

import { Cluster, ClustersSortByParam, CreateCluster, fakePagedResult, PagedResult, UpdateCluster } from '../entities/management-api-v2';
import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class ClustersService {
  private readonly http = inject(HttpClient);
  private readonly constants = inject(Constants);

  private fakeClusters: Cluster[] = [
    {
      id: 'clusterId',
      name: 'Production Cluster',
      configuration: {
        bootstrapServers: 'kafka-prod.example.com:9092',
        security: { protocol: 'SSL' },
      },
      updatedAt: new Date('2023-01-15'),
      createdAt: new Date('2022-12-01'),
    },
    {
      id: '2',
      name: 'Development Cluster',
      configuration: {
        bootstrapServers: 'kafka-dev.example.com:9092',
        security: { protocol: 'SASL_PLAINTEXT' },
      },
      updatedAt: new Date('2023-02-20'),
      createdAt: new Date('2022-11-15'),
    },
    {
      id: '3',
      name: 'Testing Cluster',
      configuration: {
        bootstrapServers: 'kafka-test.example.com:9092',
        security: { protocol: 'PLAINTEXT' },
      },
      updatedAt: new Date('2023-03-10'),
      createdAt: new Date('2022-10-05'),
    },
    {
      id: '4',
      name: 'Staging Cluster',
      configuration: {
        bootstrapServers: 'kafka-staging.example.com:9092',
        security: { protocol: 'SASL_SSL' },
      },
      updatedAt: new Date('2023-04-05'),
      createdAt: new Date('2022-09-20'),
    },
  ];

  list(searchQuery?: string, sortBy?: ClustersSortByParam, page = 1, perPage = 25): Observable<PagedResult<Cluster>> {
    // Filter by search query if provided
    let filteredClusters = this.fakeClusters;
    if (searchQuery) {
      const lowerCaseQuery = searchQuery.toLowerCase();
      filteredClusters = this.fakeClusters.filter(
        (cluster) =>
          cluster.name.toLowerCase().includes(lowerCaseQuery) ||
          cluster.configuration.bootstrapServers.toLowerCase().includes(lowerCaseQuery),
      );
    }

    // Sort if sortBy is provided
    if (sortBy) {
      const isDesc = sortBy.startsWith('-');
      const field = isDesc ? (sortBy.substring(1) as keyof Cluster) : (sortBy as keyof Cluster);
      filteredClusters = [...filteredClusters].sort((a, b) => {
        if (a[field] < b[field]) return isDesc ? 1 : -1;
        if (a[field] > b[field]) return isDesc ? -1 : 1;
        return 0;
      });
    }

    // Paginate
    const startIndex = (page - 1) * perPage;
    const paginatedClusters = filteredClusters.slice(startIndex, startIndex + perPage);

    // Return as Observable with a small delay to simulate
    return of(
      fakePagedResult(filteredClusters, {
        page,
        perPage,
        pageCount: Math.ceil(filteredClusters.length / perPage),
        pageItemsCount: paginatedClusters.length,
        totalCount: filteredClusters.length,
      }),
    ).pipe(delay(300));
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
}
