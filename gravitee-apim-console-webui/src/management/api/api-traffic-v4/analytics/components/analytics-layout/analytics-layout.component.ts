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

import { Component, computed, effect, inject, OnDestroy, Signal, input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, shareReplay } from 'rxjs/operators';

import { timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsWidgetService, ApiAnalyticsWidgetUrlParamsData } from '../../api-analytics-widget.service';
import { ApiPlanV2Service } from '../../../../../../services-ngx/api-plan-v2.service';
import { FILTER_KEYS } from '../common/common-filters.types';

interface BaseQueryParams {
  from?: string;
  to?: string;
  period?: string;
  [FILTER_KEYS.HTTP_STATUSES]?: string;
  [FILTER_KEYS.PLANS]?: string;
  [FILTER_KEYS.APPLICATIONS]?: string[];
}

@Component({
  selector: 'analytics-layout',
  templateUrl: './analytics-layout.component.html',
  styleUrl: './analytics-layout.component.scss',
})
export class AnalyticsLayoutComponent implements OnDestroy {
  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;
  private activatedRouteQueryParams = toSignal(this.activatedRoute.queryParams);
  private planService = inject(ApiPlanV2Service);

  filterFields = input<string[]>([FILTER_KEYS.PLANS]);

  public activeFilters: Signal<any> = computed(() => this.mapQueryParamsToFilters(this.activatedRouteQueryParams()));

  apiPlans$ = this.planService
    .list(this.activatedRoute.snapshot.params.apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, 1, 9999)
    .pipe(
      map((plans) => plans.data),
      shareReplay(1),
    );

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiAnalyticsWidgetService: ApiAnalyticsWidgetService,
  ) {
    effect(() => {
      this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activatedRouteQueryParams()));
    });
  }

  onFiltersChange(filters: any): void {
    this.updateQueryParamsFromFilters(filters);
  }

  onRefreshFilters(): void {
    this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(this.activeFilters()));
  }

  ngOnDestroy(): void {
    this.apiAnalyticsWidgetService.clearStatsCache();
  }

  private updateQueryParamsFromFilters(filters: any): void {
    const queryParams = this.createQueryParamsFromFilters(filters);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'replace',
    });
  }

  private createQueryParamsFromFilters(filters: any): Record<string, any> {
    const params: Record<string, any> = {};

    if (filters.period === 'custom' && filters.from && filters.to) {
      params.from = filters.from;
      params.to = filters.to;
      params.period = 'custom';
    } else {
      params.period = filters.period;
    }

    const fields = this.filterFields();
    if (fields.includes(FILTER_KEYS.PLANS) && (filters as any)[FILTER_KEYS.PLANS]?.length) {
      params[FILTER_KEYS.PLANS] = (filters as any)[FILTER_KEYS.PLANS].join(',');
    }
    if (fields.includes(FILTER_KEYS.HTTP_STATUSES) && (filters as any)[FILTER_KEYS.HTTP_STATUSES]?.length) {
      params[FILTER_KEYS.HTTP_STATUSES] = (filters as any)[FILTER_KEYS.HTTP_STATUSES].join(',');
    }
    if (fields.includes(FILTER_KEYS.APPLICATIONS) && (filters as any)[FILTER_KEYS.APPLICATIONS]?.length) {
      params[FILTER_KEYS.APPLICATIONS] = (filters as any)[FILTER_KEYS.APPLICATIONS].join(',');
    }

    return params;
  }

  private mapQueryParamsToUrlParamsData(queryParams: unknown): ApiAnalyticsWidgetUrlParamsData {
    const params = queryParams as BaseQueryParams;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return {
        timeRangeParams: {
          from: +params.from,
          to: +params.to,
          interval: this.calculateCustomInterval(+params.from, +params.to),
        },
        ...filters,
      };
    }

    const timeFrame = timeFrames.find((tf) => tf.id === normalizedPeriod) || timeFrames.find((tf) => tf.id === '1d');
    return {
      timeRangeParams: timeFrame.timeFrameRangesParams(),
      ...filters,
    };
  }

  private mapQueryParamsToFilters(queryParams: unknown): any {
    const params = queryParams as BaseQueryParams;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return {
        period: normalizedPeriod,
        from: +params.from,
        to: +params.to,
        ...filters,
      } as any;
    }

    return {
      period: normalizedPeriod,
      from: null,
      to: null,
      ...filters,
    } as any;
  }

  private getFilterFields(queryParams: BaseQueryParams) {
    const fields = this.filterFields();
    const result: any = {};

    if (fields.includes(FILTER_KEYS.PLANS)) {
      result[FILTER_KEYS.PLANS] = this.processFilter(queryParams[FILTER_KEYS.PLANS]);
    }
    if (fields.includes(FILTER_KEYS.HTTP_STATUSES)) {
      result[FILTER_KEYS.HTTP_STATUSES] = this.processFilter(queryParams[FILTER_KEYS.HTTP_STATUSES]);
    }
    if (fields.includes(FILTER_KEYS.APPLICATIONS)) {
      result[FILTER_KEYS.APPLICATIONS] = this.processFilter(queryParams[FILTER_KEYS.APPLICATIONS]);
    }

    return result;
  }

  private processFilter(value: string | string[] | undefined): string[] | undefined {
    if (value === undefined) {
      return undefined;
    }
    return Array.isArray(value) ? value : value.split(',');
  }

  private calculateCustomInterval(from: number, to: number, nbValuesByBucket = 30): number {
    const range: number = to - from;
    return Math.floor(range / nbValuesByBucket);
  }
}
