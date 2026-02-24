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

import { computed, Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { PROXY_DASHBOARD_TEMPLATE } from './templates';
import { DashboardTemplate } from './templates/dashboard-template.model';

import { Constants } from '../../../entities/Constants';
import { PagedResult } from '../../../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) { }

  readonly overviewDashboard = computed(() => {
    return this.createInitialOverview();
  });

  public list(page: number, perPage: number): Observable<PagedResult<Dashboard>> {
    return this.http.get<PagedResult<Dashboard>>(`${this.constants.org.v2BaseURL}/analytics/dashboards`, {
      params: {
        page: page.toString(),
        perPage: perPage.toString(),
      },
    });
  }

  public create(dashboard: Partial<Dashboard>): Observable<Dashboard> {
    return this.http.post<Dashboard>(`${this.constants.org.v2BaseURL}/analytics/dashboards`, dashboard);
  }

  public getById(id: string): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.constants.org.v2BaseURL}/analytics/dashboards/${id}`);
  }

  public toCreateDashboard(template: DashboardTemplate): Partial<Dashboard> {
    const now = new Date().toISOString();
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();
    const defaultTimeRange = { from: fiveMinutesAgo, to: now };
    const defaultInterval = Math.floor((5 * 60 * 1000) / 30); // 5 min / 30 buckets = 10000ms

    const widgets = (template.initialConfig.widgets ?? []).map(widget => {
      if (!widget.request) return widget;

      const request = { ...widget.request, timeRange: widget.request.timeRange ?? defaultTimeRange };

      if (request.type === 'time-series') {
        return { ...widget, request: { ...request, interval: (request as any).interval ?? defaultInterval } };
      }

      return { ...widget, request };
    });

    return {
      name: `${template.name} - ${new Date().toLocaleString()}`,
      labels: template.initialConfig.labels ?? {},
      widgets,
    };
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

