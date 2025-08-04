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
import { catchError, map, merge, Observable, of, switchMap } from 'rxjs';
import { shareReplay } from 'rxjs/operators';

import { ApiAnalyticsWidgetConfig } from './components/api-analytics-widget/api-analytics-widget.component';
import { ApiAnalyticsDashboardWidgetConfig } from './api-analytics-proxy/api-analytics-proxy.component';
import { ApiAnalyticsWidgetTableDataColumn } from './components/api-analytics-widget-table/api-analytics-widget-table.component';

import { ApiAnalyticsV2Service } from '../../../../services-ngx/api-analytics-v2.service';
import { GroupByResponse } from '../../../../entities/management-api-v2/analytics/analyticsGroupBy';
import { HistogramAnalyticsResponse } from '../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { GioWidgetLayoutState } from '../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartPieInput } from '../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { TimeRangeParams } from '../../../../shared/utils/timeFrameRanges';
import { AnalyticsStatsResponse } from '../../../../entities/management-api-v2/analytics/analyticsStats';

// Colors for charts
const defaultColors = ['#2B72FB', '#64BDC6', '#EECA34', '#FA4B42', '#FE6A35'];

@Injectable({
  providedIn: 'root',
})
export class ApiAnalyticsWidgetService {
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

  clearStatsCache(): void {
    this.statsCache.clear();
  }

  getApiAnalyticsWidgetConfig$(widgetConfig: ApiAnalyticsDashboardWidgetConfig): Observable<ApiAnalyticsWidgetConfig> {
    return this.apiAnalyticsV2Service
      .timeRangeFilter()
      .pipe(
        switchMap(() =>
          merge(of(this.createLoadingConfig(widgetConfig)), this.getApiAnalyticsWidgetConfigFromAnalyticsType$(widgetConfig)),
        ),
      );
  }

  private getApiAnalyticsWidgetConfigFromAnalyticsType$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): Observable<ApiAnalyticsWidgetConfig> {
    return this.apiAnalyticsV2Service.timeRangeFilter().pipe(
      switchMap((timeRangeParams) => {
        if (!timeRangeParams) {
          return of(this.createErrorConfig(widgetConfig, 'No time range selected'));
        }

        if (widgetConfig.analyticsType === 'STATS') {
          return this.handleStatsAnalytics$(widgetConfig, timeRangeParams);
        } else if (widgetConfig.analyticsType === 'GROUP_BY') {
          return this.handleGroupByAnalytics$(widgetConfig, timeRangeParams);
        } else if (widgetConfig.analyticsType === 'HISTOGRAM') {
          return this.handleHistogramAnalytics$(widgetConfig, timeRangeParams);
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
    timeRangeParams: TimeRangeParams,
  ): Observable<ApiAnalyticsWidgetConfig> {
    const urlParamsData: any = {};

    if (widgetConfig.statsField) {
      urlParamsData.field = widgetConfig.statsField;
    }

    const cacheKey = this.createStatsCacheKey(widgetConfig.apiId, timeRangeParams, urlParamsData);

    if (!this.statsCache.has(cacheKey)) {
      const statsRequest$ = this.apiAnalyticsV2Service.getStats(widgetConfig.apiId, timeRangeParams, urlParamsData).pipe(
        shareReplay(1),
        catchError((error) => {
          this.statsCache.delete(cacheKey);
          throw error;
        }),
      );
      this.statsCache.set(cacheKey, statsRequest$);
    }

    return this.statsCache.get(cacheKey)!.pipe(map((response) => this.transformStatsResponseToStatsConfig(response, widgetConfig)));
  }

  private transformStatsResponseToStatsConfig(
    statsResponse: AnalyticsStatsResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    return {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
      widgetType: 'stats' as const,
      widgetData: { stats: statsResponse[widgetConfig.statsKey], statsUnit: widgetConfig.statsUnit },
    };
  }

  /**
   *
   *   GROUP BY ANALYTICS
   *
   */

  private handleGroupByAnalytics$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    timeRangeParams: any,
  ): Observable<ApiAnalyticsWidgetConfig> {
    const urlParamsData: any = {};

    if (widgetConfig.groupByField) {
      urlParamsData.field = widgetConfig.groupByField;
    }

    if (widgetConfig.ranges) {
      urlParamsData.ranges = widgetConfig.ranges.map((range) => `${range.value}`).join(';');
    }

    if (widgetConfig.orderBy) {
      urlParamsData.order = widgetConfig.orderBy;
    }

    return this.apiAnalyticsV2Service
      .getGroupBy(widgetConfig.apiId, timeRangeParams, urlParamsData)
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
    const tableData = Object.entries(groupByResponse.values)
      .filter(([_, value]) => value > 0)
      .map(([label, value]) => {
        const metadata = groupByResponse.metadata[label];
        return {
          name: metadata?.name || label,
          count: value,
          id: label,
          isUnknown: metadata?.unknown || false,
          order: Number(metadata?.order ?? Number.MAX_SAFE_INTEGER),
        };
      })
      .sort((a, b) => a.order - b.order);

    const columns: ApiAnalyticsWidgetTableDataColumn[] = [
      { name: 'name', label: 'Name', isSortable: true, dataType: 'string' },
      { name: 'count', label: 'Count', isSortable: true, dataType: 'number' },
    ];

    return {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
      widgetType: 'table' as const,
      widgetData: { columns, data: tableData },
    };
  }

  /**
   *
   *   HISTOGRAM ANALYTICS
   *
   */

  private handleHistogramAnalytics$(
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
    timeRangeParams: any,
  ): Observable<ApiAnalyticsWidgetConfig> {
    if (!widgetConfig.aggregations || widgetConfig.aggregations.length === 0) {
      return of(this.createErrorConfig(widgetConfig, 'No aggregations specified for histogram'));
    }

    const aggregationsString = widgetConfig.aggregations.map((agg) => `${agg.type}:${agg.field}`).join(',');

    return this.apiAnalyticsV2Service
      .getHistogramAnalytics(widgetConfig.apiId, aggregationsString, timeRangeParams)
      .pipe(
        map((response: HistogramAnalyticsResponse) => this.transformHistogramResponseToApiAnalyticsWidgetConfig(response, widgetConfig)),
      );
  }

  private transformHistogramResponseToApiAnalyticsWidgetConfig(
    histogramResponse: HistogramAnalyticsResponse,
    widgetConfig: ApiAnalyticsDashboardWidgetConfig,
  ): ApiAnalyticsWidgetConfig {
    const baseConfig = {
      title: widgetConfig.title,
      tooltip: widgetConfig.tooltip,
      state: 'success',
    };

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
            return value.buckets.map((bucket) => ({
              name: value.metadata?.[bucket.name]?.name || bucket.name,
              values: bucket.data || [],
            }));
          }
        })
        .flat(); // Flatten the array since single aggregation returns multiple series

      const options: GioChartLineOptions = {
        pointStart: histogramResponse.timestamp.from,
        pointInterval: histogramResponse.timestamp.interval,
      };

      return {
        title: baseConfig.title,
        tooltip: baseConfig.tooltip,
        state: 'success',
        widgetType: 'line' as const,
        widgetData: { data: lineData, options },
      };
    }

    // Default fallback for unsupported widget types
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
      ...(errors && { errors }),
    };

    if (widgetConfig.type === 'stats') {
      return {
        ...baseConfig,
        widgetType: 'stats' as const,
        widgetData: null,
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
}
