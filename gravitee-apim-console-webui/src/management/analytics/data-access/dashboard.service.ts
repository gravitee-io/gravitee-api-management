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
import { Dashboard } from '@gravitee/gravitee-dashboard';

import { computed, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { PROXY_DASHBOARD_TEMPLATE } from './templates';

import { PagedResult } from '../../../entities/management-api-v2';
import { Sort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';

export interface DashboardListItem {
  id: string;
  name: string;
  createdBy: string;
  lastUpdated: string;
  labels?: Record<string, string>;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly dashboards: DashboardListItem[] = [
    {
      id: '1',
      name: 'V4 Proxy Dashboard',
      createdBy: 'Admin',
      lastUpdated: 'Aug 12, 2025',
      labels: { Focus: 'HTTP / TCP', Theme: 'Proxy' },
    },
    { id: '2', name: 'Kafka Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025' },
    { id: '3', name: 'Message API Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025' },
    {
      id: '4',
      name: 'AI Dashboard',
      createdBy: 'Thomas Pesquet',
      lastUpdated: 'Aug 12, 2025',
      labels: { Focus: 'LLM / MCP', Theme: 'AI' },
    },
    { id: '5', name: 'LLM Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025' },
    { id: '6', name: 'MCP Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025', labels: { team: 'Ops', group: 'France' } },
    { id: '7', name: 'Adoption Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025' },
    { id: '8', name: 'Application Dashboard', createdBy: 'Admin', lastUpdated: 'Aug 12, 2025', labels: { geo: 'EU-west' } },
  ];

  readonly overviewDashboard = computed(() => {
    return this.createInitialOverview();
  });

  public list(searchTerm: string, sort: Sort, page: number, size: number): Observable<PagedResult<DashboardListItem>> {
    let list = this.dashboards;

    // Filtering
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      list = list.filter(d => d.name.toLowerCase().includes(term));
    }

    // Sorting
    if (sort && sort.direction) {
      list = [...list].sort((a, b) => {
        const isAsc = sort.direction === 'asc';
        switch (sort.active) {
          case 'name':
            return this.compare(a.name, b.name, isAsc);
          case 'createdBy':
            return this.compare(a.createdBy, b.createdBy, isAsc);
          case 'lastUpdated':
            return this.compare(a.lastUpdated, b.lastUpdated, isAsc);
          case 'labels': {
            const labelsA = Object.entries(a.labels ?? {})
              .map(([k, v]) => `${k}:${v}`)
              .sort((a, b) => a.localeCompare(b))
              .join(',');
            const labelsB = Object.entries(b.labels ?? {})
              .map(([k, v]) => `${k}:${v}`)
              .sort((a, b) => a.localeCompare(b))
              .join(',');
            return this.compare(labelsA, labelsB, isAsc);
          }
          default:
            return 0;
        }
      });
    }

    // Pagination
    const totalCount = list.length;
    const startIndex = (page - 1) * size;
    const endIndex = Math.min(startIndex + size, totalCount);
    const data = list.slice(startIndex, endIndex);

    const result = new PagedResult<DashboardListItem>();
    result.data = data;
    result.pagination = {
      totalCount,
      pageCount: Math.ceil(totalCount / size),
      page,
      perPage: size,
      pageItemsCount: data.length,
    } as any;
    result.links = {};

    return of(result);
  }

  private compare(a: number | string, b: number | string, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }

  private createInitialOverview(): Dashboard {
    return {
      ...PROXY_DASHBOARD_TEMPLATE.initialConfig,
      id: 'default-overview',
      name: PROXY_DASHBOARD_TEMPLATE.name,
      createdBy: 'System',
      createdAt: new Date().toDateString(),
      lastModified: new Date().toDateString(),
      labels: PROXY_DASHBOARD_TEMPLATE.initialConfig.labels ?? {},
      widgets: PROXY_DASHBOARD_TEMPLATE.initialConfig.widgets ?? [],
    } satisfies Dashboard;
  }
}
