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

import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';

import { Dashboard } from '@gravitee/gravitee-dashboard';

import { analyticsListBreadcrumb } from './analytics-breadcrumbs';
import { AnalyticsDashboardCardComponent } from '../../../components/analytics-dashboard-card/analytics-dashboard-card.component';
import { BannerComponent } from '../../../components/banner/banner.component';
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
  imports: [AnalyticsDashboardCardComponent, CardsGridComponent, PaginationComponent, LoaderComponent, BannerComponent],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export default class AnalyticsComponent {
  private static readonly MAX_PINNED = 4;
  private static readonly PINNED_KEY = 'analytics-pinned-dashboards';

  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly dashboardService = inject(AnalyticsDashboardService);
  private readonly router = inject(Router);
  private readonly breadcrumbService = inject(BreadcrumbService);

  readonly pageSize = 20;
  private readonly currentPage = signal(1);

  readonly pinnedIds = signal<string[]>(this.loadPinnedIds());
  readonly pinnedIdsSet = computed(() => new Set(this.pinnedIds()));
  readonly canPinMore = computed(() => this.pinnedIds().length < AnalyticsComponent.MAX_PINNED);

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

  protected readonly pinnedResource = rxResource<Dashboard[], string[]>({
    params: () => this.pinnedIds(),
    stream: ({ params: ids }) => {
      if (ids.length === 0) return of([]);
      return forkJoin(ids.map(id => this.dashboardService.getById(id).pipe(catchError(() => of(null))))).pipe(
        map(results => results.filter((dashboard): dashboard is Dashboard => dashboard !== null)),
      );
    },
  });

  private readonly cachedPinnedDashboards = signal<Dashboard[]>([]);
  readonly pinnedDashboards = computed(() => this.pinnedResource.value() ?? this.cachedPinnedDashboards());

  constructor() {
    this.breadcrumbService.set([analyticsListBreadcrumb()]);
    // Cache last loaded set to keep the pinned row stable during rxResource reloads.
    effect(() => {
      const value = this.pinnedResource.value();
      if (value !== undefined) {
        this.cachedPinnedDashboards.set(value);
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage.set(page);
  }

  navigateToDashboard(dashboardId: string): void {
    this.router.navigate([dashboardId], { relativeTo: this.activatedRoute });
  }

  togglePin(dashboardId: string): void {
    const current = this.pinnedIds();
    const isPinned = current.includes(dashboardId);
    if (!isPinned && current.length >= AnalyticsComponent.MAX_PINNED) return;
    const updated = isPinned ? current.filter(id => id !== dashboardId) : [...current, dashboardId];
    this.pinnedIds.set(updated);
    localStorage.setItem(AnalyticsComponent.PINNED_KEY, JSON.stringify(updated));
  }

  private loadPinnedIds(): string[] {
    try {
      const stored = localStorage.getItem(AnalyticsComponent.PINNED_KEY);
      if (!stored) return [];
      const parsed: unknown = JSON.parse(stored);
      return Array.isArray(parsed) && parsed.every(value => typeof value === 'string') ? parsed : [];
    } catch (error) {
      console.warn('Failed to read pinned analytics dashboards from localStorage', error);
      return [];
    }
  }
}
