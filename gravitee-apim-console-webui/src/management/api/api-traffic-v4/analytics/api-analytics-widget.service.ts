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
import { BehaviorSubject, catchError, map, merge, Observable, of, switchMap } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { isFunction } from 'lodash';

import { ApiAnalyticsWidgetConfig } from './components/api-analytics-widget/api-analytics-widget.component';
import { ApiAnalyticsDashboardWidgetConfig } from './api-analytics-proxy/api-analytics-proxy.component';

import { ApiAnalyticsV2Service, UrlQueryParamsData } from '../../../../services-ngx/api-analytics-v2.service';
import { GroupByResponse } from '../../../../entities/management-api-v2/analytics/analyticsGroupBy';
import { HistogramAnalyticsResponse } from '../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { GioWidgetLayoutState } from '../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartPieInput } from '../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { GioChartBarData, GioChartBarOptions } from '../../../../shared/components/gio-chart-bar/gio-chart-bar.component';
import { TimeRangeParams } from '../../../../shared/utils/timeFrameRanges';
import { AnalyticsStatsResponse } from '../../../../entities/management-api-v2/analytics/analyticsStats';
import { EsFilter, toQuery } from '../../../../shared/utils/esQuery';
import { MultiStatsWidgetData } from '../../../../shared/components/analytics-multi-stats/analytics-multi-stats.component';

// Interface expected from component that transforms query params to UrlParamsData
export interface ApiAnalyticsWidgetUrlParamsData {
  timeRangeParams: TimeRangeParams;
  httpStatuses?: string[];
  applications?: string[];
  plans?: string[];
}

// Colors for charts
const defaultColors = ['#2B72FB', '#64BDC6', '#EECA34', '#FA4B42', '#FE6A35'];
const successColor = '#00D4AA';
const failureColor = '#FF6B6B';

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsWidgetService {
  private urlParamsData = new BehaviorSubject<ApiAnalyticsWidgetUrlParamsData>({
    timeRangeParams: null,
    httpStatuses: [],
    applications: [],
    plans: [],
  });

  // Cache for stats requests to avoid multiple backend calls
  private statsCache = new Map<string, Observable<AnalyticsStatsResponse>>();

  private createStatsCacheKey(apiId: string, timeRangeParams: any, urlParamsData: any): string {
    const params = {
      apiId,
      from: timeRangeParams.from,
      to: timeRangeParams.to,
      interval: timeRangeParams.interval,
      ...urlParamsData,
    };
    return JSON.stringify(params);
  }

  constructor(private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service) {}

  urlParamsData$(): Observable<ApiAnalyticsWidgetUrlParamsData> {
    return this.urlParamsData.asObservable();
  }

  setUrlParamsData(urlParamsData: ApiAnalyticsWidgetUrlParamsData): void {
    this.urlParamsData.next(urlParamsData);
  }

  clearStatsCache(): void {
    this.statsCache.clear();
  }

  getApiAnalyticsWidgetConfig$(widgetConfig: ApiAnalyticsDashboardWidgetConfig): Observable<ApiAnalyticsWidgetConfig> {
    return this.urlParamsData$().pipe(
      switchMap(() => merge(of(this.createLoadingConfig(widgetConfig)), this.getApiAnalyticsWidgetConfigFromAnalyticsType$(widgetConfig))),
    );
  }

  private getApiAnalyticsWidgetConfigFromAnalyticsType$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): Observable<ApiAnalyticsWidgetConfig> {
    return this.urlParamsData$().pipe(
      switchMap((urlParamsData: ApiAnalyticsWidgetUrlParamsData) => {
        if (isFunction(widgetConfig.filterQueryParams)) {
          urlParamsData = widgetConfig.filterQueryParams(urlParamsData);
        }

        if (!urlParamsData.timeRangeParams) {
          return of(this.createErrorConfig(widgetConfig, 'No time range selected'));
        }

        if (widgetConfig.analyticsType === 'STATS') {
          return this.handleStatsAnalytics$(widgetConfig, urlParamsData);
        } else if (widgetConfig.analyticsType === 'GROUP_BY') {
          return this.handleGroupByAnalytics$(widgetConfig, urlParamsData);
        } else if (widgetConfig.analyticsType === 'HISTOGRAM') {
          return this.handleHistogramAnalytics$(widgetConfig, urlParamsData);
        } else {
          return of(this.createErrorConfig(widgetConfig, 'Unsupported analytics type'));
        }
      }),
      catchError(({ error }) => {
        return of(this.createErrorConfig(widgetConfig, error?.message || 'An error occurred'));
      }),
    );
  }

  /**
   *
   *   STATS ANALYTICS
   *
   */

  private handleStatsAnalytics$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    urlParamsData: ApiAnalyticsWidgetUrlParamsData,
  ): Observable<ApiAnalyticsWidgetConfig> {
    const { timeRangeParams } = urlParamsData;
    const defaultQueryParams: any = {};

    if (widgetConfig.statsField) {
      defaultQueryParams.field = widgetConfig.statsField;
    }

    const query = this.queryOf(urlParamsData);
    let queryParams: UrlQueryParamsData = {
      ...(query ? { query } : {}),
    };
    if (isFunction(widgetConfig.mapQueryParams)) {
      queryParams = widgetConfig.mapQueryParams(urlParamsData);
    }

    const cacheKey = this.createStatsCacheKey(widgetConfig.apiId, timeRangeParams, { ...defaultQueryParams, ...queryParams });

    if (!this.statsCache.has(cacheKey)) {
      const statsRequest$ = this.apiAnalyticsV2Service
        .getStats(widgetConfig.apiId, timeRangeParams, { ...defaultQueryParams, ...queryParams })
        .pipe(
          shareReplay(1),
          catchError(error => {
            this.statsCache.delete(cacheKey);
            throw error;
          }),
        );
      this.statsCache.set(cacheKey, statsRequest$);
    }

    return this.statsCache.get(cacheKey)!.pipe(map(response => this.transformStatsResponseToStatsConfig(response, widgetConfig)));
  }

  private transformStatsResponseToStatsConfig(
    statsResponse: AnalyticsStatsResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    const statsValue = widgetConfig.statsKey ? statsResponse[widgetConfig.statsKey] : undefined;
    if (statsValue == null || statsValue === 0) {
      return this.createEmptyConfig(widgetConfig);
    }

    return {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
      widgetType: 'stats' as const,
      widgetData: { stats: statsValue, statsUnit: widgetConfig.statsUnit },
    };
  }

  /**
   *
   *   GROUP BY ANALYTICS
   *
   */

  private handleGroupByAnalytics$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    urlParamsData: ApiAnalyticsWidgetUrlParamsData,
  ): Observable<ApiAnalyticsWidgetConfig> {
    const { timeRangeParams } = urlParamsData;
    const defaultQueryParams: UrlQueryParamsData = {};

    if (widgetConfig.groupByField) {
      defaultQueryParams.field = widgetConfig.groupByField;
    }

    if (widgetConfig.ranges) {
      defaultQueryParams.ranges = widgetConfig.ranges.map(range => `${range.value}`).join(';');
    }

    if (widgetConfig.orderBy) {
      defaultQueryParams.order = widgetConfig.orderBy;
    }

    const query = this.queryOf(urlParamsData);
    let queryParams: UrlQueryParamsData = {
      ...(query ? { query } : {}),
    };
    if (isFunction(widgetConfig.mapQueryParams)) {
      queryParams = widgetConfig.mapQueryParams(urlParamsData);
    }

    return this.apiAnalyticsV2Service
      .getGroupBy(widgetConfig.apiId, timeRangeParams, { ...defaultQueryParams, ...queryParams })
      .pipe(map((response: GroupByResponse) => this.transformGroupByResponseToApiAnalyticsWidgetConfig(response, widgetConfig)));
  }

  private transformGroupByResponseToApiAnalyticsWidgetConfig(
    groupByResponse: GroupByResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    if (widgetConfig.type === 'pie') {
      return this.transformGroupByResponseToPieConfig(groupByResponse, widgetConfig);
    }

    if (widgetConfig.type === 'table') {
      return this.transformGroupByResponseToTableConfig(groupByResponse, widgetConfig);
    }

    // Default fallback for unsupported widget types
    return this.createErrorConfig(widgetConfig, 'Unsupported widget type for GROUP_BY analytics');
  }

  private transformGroupByResponseToPieConfig(
    groupByResponse: GroupByResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    const pieData: GioChartPieInput[] = Object.entries(groupByResponse.values)
      .filter(([_label, value]) => value > 0)
      .map(([label, value], index) => {
        // For ranges, use configurable labels and colors
        if (widgetConfig.ranges) {
          // Identify the order index from metadata from label
          const matchingRange = widgetConfig.ranges[groupByResponse.metadata[label]?.order || index];
          return {
            label: matchingRange?.label || label,
            value,
            color: matchingRange?.color || defaultColors[index % defaultColors.length],
          };
        }

        // For other fields, use metadata if available
        const metadata = groupByResponse.metadata[label];
        return {
          label: metadata?.name || label,
          value,
          color: defaultColors[index % defaultColors.length],
        };
      })
      .sort((a, b) => a.label.localeCompare(b.label));

    if (pieData.length === 0) {
      return this.createEmptyConfig(widgetConfig);
    }

    return {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
      widgetType: 'pie' as const,
      widgetData: pieData,
    };
  }

  private transformGroupByResponseToTableConfig(
    groupByResponse: GroupByResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    const columns = widgetConfig.tableData?.columns ?? [
      { label: 'Name', dataType: 'string' },
      { label: 'Value', dataType: 'number' },
    ];
    const transformedColumns = columns.map((col, i) => ({ ...col, name: `col-${i}` }));

    // Keep original index for stable sorting fallback
    const entries = Object.entries(groupByResponse.values)
      .filter(([, value]) => value > 0)
      .map(([label, value], index) => ({ label, value, index }));

    // Sort using metadata.order or original index
    entries.sort((a, b) => {
      const orderA = groupByResponse.metadata[a.label]?.order ?? a.index;
      const orderB = groupByResponse.metadata[b.label]?.order ?? b.index;
      return orderA - orderB;
    });

    const tableData = entries.map(({ label, value }) => {
      const meta = groupByResponse.metadata?.[label];
      const row: any = {
        __key: label,
        [transformedColumns[0].name]: meta?.name || label,
        [transformedColumns[1].name]: value,
        unknown: !!meta?.unknown,
      };
      return row;
    });

    if (!tableData.length) {
      return this.createEmptyConfig(widgetConfig);
    }

    return {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
      minHeight: widgetConfig.minHeight,
      widgetType: 'table',
      widgetData: {
        columns: transformedColumns,
        data: tableData,
        keyIdentifier: widgetConfig.groupByField,
        firstColumnClickable:
          typeof widgetConfig.isClickable === 'boolean'
            ? widgetConfig.isClickable
            : widgetConfig.groupByField === 'application-id' || widgetConfig.groupByField === 'plan-id',
        relativePath: widgetConfig.relativePath,
      },
    };
  }

  /**
   *
   *   HISTOGRAM ANALYTICS
   *
   */

  private handleHistogramAnalytics$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    urlParamsData: ApiAnalyticsWidgetUrlParamsData,
  ): Observable<ApiAnalyticsWidgetConfig> {
    const { timeRangeParams } = urlParamsData;
    if (!widgetConfig.aggregations || widgetConfig.aggregations.length === 0) {
      return of(this.createErrorConfig(widgetConfig, 'No aggregations specified for histogram'));
    }

    const aggregationsString = widgetConfig.aggregations.map(agg => `${agg.type}:${agg.field}`).join(',');

    const query = this.queryOf(urlParamsData);
    let queryParams: UrlQueryParamsData = {
      ...(query ? { query } : {}),
    };
    if (isFunction(widgetConfig.mapQueryParams)) {
      queryParams = widgetConfig.mapQueryParams(urlParamsData);
    }

    return this.apiAnalyticsV2Service
      .getHistogramAnalytics(widgetConfig.apiId, aggregationsString, timeRangeParams, queryParams)
      .pipe(
        map((response: HistogramAnalyticsResponse) => this.transformHistogramResponseToApiAnalyticsWidgetConfig(response, widgetConfig)),
      );
  }

  private queryOf(urlParamsData: ApiAnalyticsWidgetUrlParamsData): string | null {
    const filters: EsFilter[] = [];
    if (urlParamsData.httpStatuses && urlParamsData.httpStatuses.length > 0) {
      filters.push({ type: 'isin', field: 'status', values: urlParamsData.httpStatuses });
    }
    if (urlParamsData.plans && urlParamsData.plans.length > 0) {
      filters.push({ type: 'isin', field: 'plan-id', values: urlParamsData.plans });
    }
    if (urlParamsData.applications && urlParamsData.applications.length > 0) {
      filters.push({ type: 'isin', field: 'application-id', values: urlParamsData.applications });
    }

    const queryString = toQuery(filters);

    // if (urlParamsData.terms && urlParamsData.terms.length > 0) {
    //   queryString = queryString + '&terms=' + urlParamsData.terms;
    // }

    return queryString;
  }

  private transformHistogramResponseToApiAnalyticsWidgetConfig(
    histogramResponse: HistogramAnalyticsResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    if (widgetConfig.type === 'multi-stats') {
      if (!histogramResponse.values || histogramResponse.values.length === 0) {
        return this.createEmptyConfig(widgetConfig);
      }

      // Transform multiple aggregations into multi-stats format
      const items = histogramResponse.values
        .map((value, index) => {
          const aggregation = widgetConfig.aggregations![index];
          const bucket = value.buckets?.[0];
          if (!bucket || !bucket.data || bucket.data.length === 0) {
            return null;
          }
          const stat = bucket.data[bucket.data.length - 1]; // Get the latest value
          return {
            label: aggregation.label || aggregation.field,
            value: stat,
            unit: '',
          };
        })
        .filter(item => item !== null);

      if (items.length === 0) {
        return this.createEmptyConfig(widgetConfig);
      }

      const multiStatsData: MultiStatsWidgetData = {
        items: items,
      };

      return {
        title: widgetConfig.title,
        tooltip: widgetConfig.tooltip,
        state: 'success',
        minHeight: widgetConfig.minHeight,
        widgetType: 'multi-stats' as const,
        widgetData: multiStatsData,
      };
    }

    if (widgetConfig.type === 'line') {
      const hasMultipleAggregations = widgetConfig.aggregations && widgetConfig.aggregations.length > 1;

      const lineData: GioChartLineData[] = histogramResponse.values
        .map((value, index) => {
          if (hasMultipleAggregations) {
            // For multiple aggregations, use the aggregation label as the name
            const aggregation = widgetConfig.aggregations![index];
            return {
              name: aggregation.label || aggregation.field,
              values: value.buckets[0]?.data || [],
            };
          } else {
            // For single aggregation, use the bucket names as the series names
            return value.buckets.map(bucket => ({
              name: value.metadata?.[bucket.name]?.name || bucket.name,
              values: bucket.data || [],
            }));
          }
        })
        .flat(); // Flatten the array since single aggregation returns multiple series

      const options: GioChartLineOptions = {
        pointStart: histogramResponse.timestamp.from,
        pointInterval: histogramResponse.timestamp.interval,
        enableMarkers: widgetConfig.lineEnableMarkers ?? false,
      };

      if (lineData.length === 0 || lineData.every(bucket => bucket.values.every(value => value === 0))) {
        return this.createEmptyConfig(widgetConfig);
      }

      return {
        title: widgetConfig.title,
        tooltip: widgetConfig.tooltip,
        state: 'success',
        minHeight: widgetConfig.minHeight,
        widgetType: 'line' as const,
        widgetData: { data: lineData, options },
      };
    }

    if (widgetConfig.type === 'bar') {
      const hasMultipleAggregations = widgetConfig.aggregations && widgetConfig.aggregations.length > 1;

      // TODO: Improve it to move this logic to native-analytics component side
      const isAuthenticationChart = widgetConfig.aggregations?.some(
        agg => agg.field.includes('authentication-successes') || agg.field.includes('authentication-failures'),
      );

      let barData: GioChartBarData[];

      if (isAuthenticationChart && widgetConfig.aggregations?.length === 4) {
        const downstreamFailure =
          histogramResponse.values.find(v => v.field === 'downstream-authentication-failures-count-increment')?.buckets[0]?.data || [];
        const upstreamFailure =
          histogramResponse.values.find(v => v.field === 'upstream-authentication-failures-count-increment')?.buckets[0]?.data || [];
        const upstreamSuccess =
          histogramResponse.values.find(v => v.field === 'upstream-authentication-successes-count-increment')?.buckets[0]?.data || [];
        const downstreamSuccess =
          histogramResponse.values.find(v => v.field === 'downstream-authentication-successes-count-increment')?.buckets[0]?.data || [];

        // Sum downstream + upstream for success and failure for each time point
        const totalSuccess = downstreamSuccess.map((value, index) => value + (upstreamSuccess[index] || 0));
        const totalFailure = downstreamFailure.map((value, index) => value + (upstreamFailure[index] || 0));

        barData = [
          {
            name: 'Success',
            values: totalSuccess,
            color: successColor,
          },
          {
            name: 'Failure',
            values: totalFailure,
            color: failureColor,
          },
        ];
      } else {
        barData = histogramResponse.values
          .map((value, index) => {
            if (hasMultipleAggregations) {
              // For multiple aggregations, use the aggregation label as the name
              const aggregation = widgetConfig.aggregations![index];
              return {
                name: aggregation.label || aggregation.field,
                values: value.buckets[0]?.data || [],
                color: defaultColors[index % defaultColors.length],
              };
            } else {
              // For single aggregation, use the bucket names as the series names
              return value.buckets.map((bucket, bucketIndex) => ({
                name: value.metadata?.[bucket.name]?.name || bucket.name,
                values: bucket.data || [],
                color: defaultColors[bucketIndex % defaultColors.length],
              }));
            }
          })
          .flat();
      }

      // Create categories based on time intervals
      const totalDataPoints = barData[0]?.values.length || 0;
      const categories: string[] = [];
      for (let i = 0; i < totalDataPoints; i++) {
        const timestamp = histogramResponse.timestamp.from + i * histogramResponse.timestamp.interval;
        const date = new Date(timestamp);

        // Format based on interval length to show appropriate granularity
        const intervalHours = histogramResponse.timestamp.interval / (1000 * 60 * 60);
        if (intervalHours >= 24) {
          categories.push(date.toLocaleDateString());
        } else if (intervalHours >= 1) {
          categories.push(date.toLocaleDateString() + ' ' + date.getHours().toString().padStart(2, '0') + ':00');
        } else {
          categories.push(date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
        }
      }

      const options: GioChartBarOptions = {
        categories: categories,
        stacked: true,
        reverseStack: true,
      };

      if (barData.length === 0 || barData.every(series => series.values.every(value => value === 0))) {
        return this.createEmptyConfig(widgetConfig);
      }

      return {
        title: widgetConfig.title,
        tooltip: widgetConfig.tooltip,
        state: 'success',
        minHeight: widgetConfig.minHeight,
        widgetType: 'bar' as const,
        widgetData: { data: barData, options },
      };
    }

    return this.createErrorConfig(widgetConfig, 'Unsupported widget type for HISTOGRAM analytics');
  }

  /**
   *
   *   GENERAL UTILITY METHODS
   *
   */

  private createConfigWithBlankData(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    state: GioWidgetLayoutState,
    errors?: string[],
  ): ApiAnalyticsWidgetConfig {
    const baseConfig = {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state,
      minHeight: widgetConfig.minHeight,
      ...(errors && { errors }),
    };

    if (widgetConfig.type === 'stats') {
      return {
        ...baseConfig,
        widgetType: 'stats' as const,
        widgetData: null,
      };
    }

    if (widgetConfig.type === 'multi-stats') {
      return {
        ...baseConfig,
        widgetType: 'multi-stats' as const,
        widgetData: { items: [] },
      };
    }

    if (widgetConfig.type === 'table') {
      return {
        ...baseConfig,
        widgetType: 'table' as const,
        widgetData: { columns: [], data: [] },
      };
    }

    if (widgetConfig.type === 'line') {
      return {
        ...baseConfig,
        widgetType: 'line' as const,
        widgetData: { data: [], options: { pointStart: 0, pointInterval: 0 } },
      };
    }

    if (widgetConfig.type === 'bar') {
      return {
        ...baseConfig,
        widgetType: 'bar' as const,
        widgetData: { data: [], options: { categories: [], stacked: true } },
      };
    }

    return {
      ...baseConfig,
      widgetType: 'pie' as const,
      widgetData: [],
    };
  }

  private createLoadingConfig(widgetConfig: ApiAnalyticsDashboardWidgetConfig): ApiAnalyticsWidgetConfig {
    return this.createConfigWithBlankData(widgetConfig, 'loading');
  }

  private createErrorConfig(widgetConfig: ApiAnalyticsDashboardWidgetConfig, errorMessage: string): ApiAnalyticsWidgetConfig {
    return this.createConfigWithBlankData(widgetConfig, 'error', [errorMessage]);
  }

  private createEmptyConfig(widgetConfig: ApiAnalyticsDashboardWidgetConfig): ApiAnalyticsWidgetConfig {
    return this.createConfigWithBlankData(widgetConfig, 'empty');
  }
}
