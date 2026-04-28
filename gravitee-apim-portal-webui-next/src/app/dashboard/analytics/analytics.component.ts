/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { Dashboard } from '@gravitee/gravitee-dashboard';

import { analyticsListBreadcrumb } from './analytics-breadcrumbs';
import { AnalyticsDashboardCardComponent } from '../../../components/analytics-dashboard-card/analytics-dashboard-card.component';
import { CardsGridComponent } from '../../../components/cards-grid/cards-grid.component';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { PaginationComponent } from '../../../components/pagination/pagination.component';
import { AnalyticsDashboardsResponse } from '../../../entities/analytics-dashboard/analytics-dashboard';
import { AnalyticsDashboardService } from '../../../services/analytics-dashboard.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';

interface DashboardsListParams {
  page: number;
  pageSize: number;
}

export interface DashboardPaginatorVM {
  data: Dashboard[];
  page: number;
  totalResults: number;
}

@Component({
  selector: 'app-analytics',
  imports: [AnalyticsDashboardCardComponent, CardsGridComponent, PaginationComponent, LoaderComponent],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export default class AnalyticsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly dashboardService = inject(AnalyticsDashboardService);
  private readonly router = inject(Router);
  private readonly breadcrumbService = inject(BreadcrumbService);

  readonly pageSize = 20;
  private readonly currentPage = signal(1);

  protected readonly dashboardsResource = rxResource<AnalyticsDashboardsResponse | undefined, DashboardsListParams>({
    params: () => ({ page: this.currentPage(), pageSize: this.pageSize }),
    stream: ({ params }) => this.dashboardService.list(params.page, params.pageSize),
  });

  protected readonly dashboardPaginator = computed((): DashboardPaginatorVM => {
    if (this.dashboardsResource.error()) {
      return { data: [], page: 1, totalResults: 0 };
    }
    const resp = this.dashboardsResource.value();
    const data = resp?.data ?? [];
    const page = resp?.metadata?.pagination?.current_page ?? 1;
    const totalResults = resp?.metadata?.pagination?.total ?? 0;
    return { data, page, totalResults };
  });

  constructor() {
    this.breadcrumbService.set([analyticsListBreadcrumb()]);
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  navigateToDashboard(dashboardId: string): void {
    this.router.navigate([dashboardId], { relativeTo: this.activatedRoute });
  }
}
