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

import { Component, OnInit } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable, of } from 'rxjs';

import { ApiAnalyticsNativeFilterBarComponent } from '../components/api-analytics-native-filter-bar/api-analytics-native-filter-bar.component';
import { ApiAnalyticsWidgetComponent, ApiAnalyticsWidgetConfig } from '../components/api-analytics-widget/api-analytics-widget.component';
import { GioChartPieModule } from '../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { AnalyticsLayoutComponent } from '../components/analytics-layout/analytics-layout.component';
import { FILTER_FIELDS } from '../components/common/common-filters.types';
import { GioWidgetLayoutState } from '../../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { GioChartBarData, GioChartBarOptions } from '../../../../../shared/components/gio-chart-bar/gio-chart-bar.component';

@Component({
  selector: 'api-analytics-native',
  imports: [
    CommonModule,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsNativeFilterBarComponent,
    ApiAnalyticsWidgetComponent,
    GioChartPieModule,
    AnalyticsLayoutComponent,
  ],
  templateUrl: './api-analytics-native.component.html',
  styleUrl: './api-analytics-native.component.scss',
})
export class ApiAnalyticsNativeComponent implements OnInit {
  private readonly apiId: string = this.activatedRoute.snapshot.params.apiId;

  filterFields = FILTER_FIELDS.NATIVE;

  public topRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public leftColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public rightColumnTransformed$: Observable<ApiAnalyticsWidgetConfig>[];
  public bottomRowTransformed$: Observable<ApiAnalyticsWidgetConfig>[];

  constructor(private readonly activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.topRowTransformed$ = [
      of(this.createMockStatsWidget('Downstream Active Connections', 856, '')),
      of(this.createMockStatsWidget('Upstream Active Connections', 391, '')),
      of(this.createMockStatsWidget('Messages Produced from Clients', 1234567, '')),
      of(this.createMockStatsWidget('Messages Produced to Broker', 2345678, '')),
      of(this.createMockStatsWidget('Messages Consumed from Broker', 987654, '')),
      of(this.createMockStatsWidget('Messages Consumed to Clients', 876543, '')),
    ];

    this.leftColumnTransformed$ = [
      of(this.createMockLineWidget('Message Production Rate', 'Message production rate over time')),
      of(this.createMockLineWidget('Data Production Rate', 'Data production rate over time')),
    ];

    this.rightColumnTransformed$ = [
      of(this.createMockLineWidget('Message Consumption Rate', 'Message consumption rate over time')),
      of(this.createMockLineWidget('Data Consumption Rate', 'Data consumption rate over time')),
    ];

    this.bottomRowTransformed$ = [
      of(this.createMockStackedBarWidget('Authentication Success vs. Failure', 'Authentication success and failure rates over time')),
    ];
  }

  private createMockStatsWidget(title: string, value: number, unit: string): ApiAnalyticsWidgetConfig {
    return {
      title,
      tooltip: `Mock ${title.toLowerCase()} data`,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'stats',
      widgetData: { stats: value, statsUnit: unit },
    };
  }

  private createMockLineWidget(title: string, tooltip: string): ApiAnalyticsWidgetConfig {
    const fromClientData = [45, 52, 38, 67, 44, 59, 72, 48, 63, 41, 56, 39, 74, 51, 42];

    const toBrokerData = [62, 48, 71, 34, 56, 43, 68, 52, 39, 65, 47, 73, 41, 58, 44];

    const lineData: GioChartLineData[] = [
      {
        name: 'From Client',
        values: fromClientData,
      },
      {
        name: 'To Broker',
        values: toBrokerData,
      },
    ];

    const lineOptions: GioChartLineOptions = {
      pointStart: Date.now() - 14 * 24 * 60 * 60 * 1000,
      pointInterval: 24 * 60 * 60 * 1000,
      enableMarkers: true,
      useSharpCorners: true,
    };

    return {
      title,
      tooltip,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'line',
      widgetData: { data: lineData, options: lineOptions },
    };
  }

  private createMockStackedBarWidget(title: string, tooltip: string): ApiAnalyticsWidgetConfig {
    const generateHourlyData = () => {
      const successData = [
        1245, 1567, 2134, 1876, 2456, 1834, 2678, 2123, 1945, 2345, 1687, 2789, 2567, 1923, 2145, 1756, 2434, 1865, 2678, 1543, 2234, 1876,
        2456, 1834,
      ];

      const failureData = [37, 47, 64, 56, 74, 55, 80, 64, 58, 70, 51, 84, 77, 58, 64, 53, 73, 56, 80, 46, 67, 56, 74, 55];

      const categories: string[] = [];

      for (let i = 0; i < 24; i++) {
        const hour = new Date(Date.now() - (23 - i) * 60 * 60 * 1000).getHours();
        categories.push(`${hour.toString().padStart(2, '0')}:00`);
      }

      return { successData, failureData, categories };
    };

    const { successData, failureData, categories } = generateHourlyData();

    const barData: GioChartBarData[] = [
      {
        name: 'Success',
        values: successData,
        color: '#00D4AA',
      },
      {
        name: 'Failure',
        values: failureData,
        color: '#FF6B6B',
      },
    ];

    const barOptions: GioChartBarOptions = {
      categories: categories,
      stacked: true,
      reverseStack: true,
      customTooltip: {
        formatter: function () {
          const x = this.x;
          const pointIndex = this.point.index;
          const success = successData[pointIndex];
          const failure = failureData[pointIndex];
          const failureRate = ((failure / (success + failure)) * 100).toFixed(2);

          return `
            <div style="text-align: left;">
              <strong>Time:</strong> ${x}<br/>
              <strong style="color: #00D4AA;">Success:</strong> <span style="color: #00D4AA;">${success.toLocaleString()}</span><br/>
              <strong style="color: #FF6B6B;">Failure:</strong> <span style="color: #FF6B6B;">${failure.toLocaleString()}</span><br/>
              <strong>Failure Rate:</strong> ${failureRate}%
            </div>
          `;
        },
      },
    };

    return {
      title,
      tooltip,
      state: 'success' as GioWidgetLayoutState,
      widgetType: 'bar',
      widgetData: { data: barData, options: barOptions },
    };
  }
}
