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
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { timeFrames } from '../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsWidgetService, ApiAnalyticsWidgetUrlParamsData } from './api-analytics-widget.service';

// Base interface that both filter types extend
// Contains common filters shared by all analytics components
// Individual components define their own supported filter subsets:
// - Native: plans + timeframe (period/from/to)  
// - Proxy: plans + timeframe + httpStatuses + applications
export interface BaseAnalyticsFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans?: string[] | null;
  applications?: string[] | null;
}

// Query params interface that supports all possible filters
interface BaseQueryParams {
  from?: string;
  to?: string;
  period?: string;
  httpStatuses?: string;
  plans?: string;
  applications?: string[] | string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiAnalyticsBaseService {
  private planService = inject(ApiPlanV2Service);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private apiAnalyticsWidgetService = inject(ApiAnalyticsWidgetService);

  /**
   * Get API plans observable for the specified API
   */
  getApiPlans$(apiId: string): Observable<any[]> {
    return this.planService
      .list(apiId, undefined, ['PUBLISHED', 'DEPRECATED', 'CLOSED'], undefined, 1, 9999)
      .pipe(
        map((plans) => plans.data),
        shareReplay(1),
      );
  }

  /**
   * Handle filter changes by updating query parameters
   */
  onFiltersChange<T extends BaseAnalyticsFilters>(filters: T, supportedFilters: (keyof T)[]): void {
    this.updateQueryParamsFromFilters(filters, supportedFilters);
  }

  /**
   * Handle refresh filters by updating widget service with current filters
   */
  onRefreshFilters<T extends BaseAnalyticsFilters>(filters: T, supportedFilters: (keyof T)[]): void {
    this.apiAnalyticsWidgetService.setUrlParamsData(this.mapQueryParamsToUrlParamsData(filters, supportedFilters));
  }

  /**
   * Clear stats cache on destroy
   */
  clearStatsCache(): void {
    this.apiAnalyticsWidgetService.clearStatsCache();
  }

  /**
   * Map query parameters to URL params data for widget service
   */
  mapQueryParamsToUrlParamsData<T extends BaseAnalyticsFilters>(queryParams: unknown, supportedFilters: (keyof T)[]): ApiAnalyticsWidgetUrlParamsData {
    const params = queryParams as BaseQueryParams;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params, supportedFilters);

    const result: ApiAnalyticsWidgetUrlParamsData = {
      timeRangeParams: normalizedPeriod === 'custom' && params.from && params.to 
        ? {
            from: +params.from,
            to: +params.to,
            interval: this.calculateCustomInterval(+params.from, +params.to),
          }
        : (timeFrames.find((tf) => tf.id === normalizedPeriod) || timeFrames.find((tf) => tf.id === '1d')).timeFrameRangesParams(),
      httpStatuses: (filters as any).httpStatuses || [],
      applications: (filters as any).applications || [],
      plans: (filters as any).plans || [],
    };

    return result;
  }

  /**
   * Map query parameters to filters object
   */
  mapQueryParamsToFilters<T extends BaseAnalyticsFilters>(queryParams: unknown, supportedFilters: (keyof T)[]): T {
    const params = queryParams as BaseQueryParams;
    const normalizedPeriod = params.period || '1d';
    const filters = this.getFilterFields(params, supportedFilters);

    if (normalizedPeriod === 'custom' && params.from && params.to) {
      return <T>{
        period: normalizedPeriod,
        from: +params.from,
        to: +params.to,
        ...filters,
      };
    }

    return <T>{
      period: normalizedPeriod,
      from: null,
      to: null,
      ...filters,
    };
  }

  /**
   * Calculate custom interval for time range
   */
  private calculateCustomInterval(from: number, to: number, nbValuesByBucket = 30): number {
    const range: number = to - from;
    return Math.floor(range / nbValuesByBucket);
  }

  /**
   * Update query parameters from filters
   */
  private updateQueryParamsFromFilters<T extends BaseAnalyticsFilters>(filters: T, supportedFilters: (keyof T)[]): void {
    const queryParams = this.createQueryParamsFromFilters(filters, supportedFilters);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'replace',
    });
  }

  /**
   * Create query parameters from filters
   */
  private createQueryParamsFromFilters<T extends BaseAnalyticsFilters>(filters: T, supportedFilters: (keyof T)[]): Record<string, any> {
    const params: Record<string, any> = {};

    if (filters.period === 'custom' && filters.from && filters.to) {
      params.from = filters.from.toString();
      params.to = filters.to.toString();
      params.period = 'custom';
    } else {
      params.period = filters.period;
    }

    // Handle supported filter fields dynamically
    supportedFilters.forEach(filterKey => {
      if (filterKey === 'period' || filterKey === 'from' || filterKey === 'to') {
        return; // Already handled above
      }

      const filterValue = filters[filterKey];
      if (Array.isArray(filterValue) && filterValue.length) {
        params[filterKey as string] = filterValue.join(',');
      }
    });

    return params;
  }

  /**
   * Extract filter fields from query parameters
   */
  private getFilterFields<T>(queryParams: BaseQueryParams, supportedFilters: (keyof T)[]): Partial<T> {
    const result: Partial<T> = {};

    supportedFilters.forEach(filterKey => {
      if (filterKey === 'period' || filterKey === 'from' || filterKey === 'to') {
        return; // These are handled separately
      }

      const paramKey = filterKey as string;
      if (paramKey in queryParams) {
        const value = queryParams[paramKey as keyof BaseQueryParams];
        (result as any)[filterKey] = this.processFilter(value);
      }
    });

    return result;
  }

  /**
   * Process filter value to convert to string array
   */
  private processFilter(value: string | string[] | undefined): string[] | undefined {
    if (value === undefined) {
      return undefined;
    }
    return Array.isArray(value) ? value : value.split(',');
  }
}
