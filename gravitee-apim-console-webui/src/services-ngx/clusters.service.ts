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
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { Cluster, ClustersSortByParam, CreateCluster, fakePagedResult, PagedResult, UpdateCluster } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ClustersService {
  private fakeClusters: Cluster[] = [
    {
      id: 'clusterId',
      name: 'Production Cluster',
      bootstrapServer: 'kafka-prod.example.com:9092',
      security: 'SSL',
      updatedAt: new Date('2023-01-15'),
      createdAt: new Date('2022-12-01'),
    },
    {
      id: '2',
      name: 'Development Cluster',
      bootstrapServer: 'kafka-dev.example.com:9092',
      security: 'SASL_PLAINTEXT',
      updatedAt: new Date('2023-02-20'),
      createdAt: new Date('2022-11-15'),
    },
    {
      id: '3',
      name: 'Testing Cluster',
      bootstrapServer: 'kafka-test.example.com:9092',
      security: 'PLAINTEXT',
      updatedAt: new Date('2023-03-10'),
      createdAt: new Date('2022-10-05'),
    },
    {
      id: '4',
      name: 'Staging Cluster',
      bootstrapServer: 'kafka-staging.example.com:9092',
      security: 'SASL_SSL',
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
          cluster.bootstrapServer.toLowerCase().includes(lowerCaseQuery) ||
          cluster.security.toLowerCase().includes(lowerCaseQuery),
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
    const cluster = this.fakeClusters.find((c) => c.id === id);
    if (!cluster) {
      throw new Error(`Cluster with id ${id} not found`);
    }
    return of(cluster).pipe(delay(300));
  }

  create(cluster: CreateCluster): Observable<Cluster> {
    const newCluster: Cluster = {
      ...cluster,
      id: Math.random().toString(36).substring(2, 11),
      updatedAt: new Date(),
      createdAt: new Date(),
      security: 'PLAINTEXT',
    };
    this.fakeClusters.push(newCluster);
    return of(newCluster).pipe(delay(300));
  }

  update(id: string, cluster: UpdateCluster): Observable<Cluster> {
    const index = this.fakeClusters.findIndex((c) => c.id === id);
    if (index === -1) {
      throw new Error(`Cluster with id ${id} not found`);
    }
    const updatedCluster: Cluster = {
      ...this.fakeClusters[index],
      ...cluster,
      updatedAt: new Date(),
    };
    this.fakeClusters[index] = updatedCluster;
    return of(updatedCluster).pipe(delay(300));
  }

  delete(id: string): Observable<void> {
    const index = this.fakeClusters.findIndex((c) => c.id === id);
    if (index === -1) {
      throw new Error(`Cluster with id ${id} not found`);
    }
    this.fakeClusters.splice(index, 1);
    return of(undefined).pipe(delay(300));
  }
}
