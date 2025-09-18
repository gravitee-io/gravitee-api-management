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
import { Component, computed, inject, input, output, Signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import {
  GioWidgetLayoutComponent,
  GioWidgetLayoutState,
} from '../../../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartPieInput } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import {
  GioChartBarData,
  GioChartBarOptions,
  GioChartBarComponent,
} from '../../../../../../shared/components/gio-chart-bar/gio-chart-bar.component';
import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { GioChartLineModule } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import {
  ApiAnalyticsWidgetTableComponent,
  ApiAnalyticsWidgetTableDataColumn,
  ApiAnalyticsWidgetTableRowData,
} from '../api-analytics-widget-table/api-analytics-widget-table.component';
import { AnalyticsStatsComponent, StatsWidgetData } from '../../../../../../shared/components/analytics-stats/analytics-stats.component';
import {
  AnalyticsMultiStatsComponent,
  MultiStatsWidgetData,
} from '../../../../../../shared/components/analytics-multi-stats/analytics-multi-stats.component';

type PieWidgetData = GioChartPieInput[];
type LineWidgetData = { data: GioChartLineData[]; options?: GioChartLineOptions };
export type TableWidgetData = {
  columns: ApiAnalyticsWidgetTableDataColumn[];
  data: ApiAnalyticsWidgetTableRowData[];
  keyIdentifier?: string;
  firstColumnClickable?: boolean;
  relativePath?: string;
};
type BarWidgetData = { data: GioChartBarData[]; options?: GioChartBarOptions };

interface BaseApiAnalyticsWidgetConfig {
  title: string;
  tooltip?: string;
  state: GioWidgetLayoutState;
  errors?: string[];
}

type ApiAnalyticsWidgetStatsConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'stats';
  widgetData: StatsWidgetData;
};

type ApiAnalyticsWidgetMultiStatsConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'multi-stats';
  widgetData: MultiStatsWidgetData;
};

type ApiAnalyticsWidgetPieConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'pie';
  widgetData: PieWidgetData;
};

type ApiAnalyticsWidgetLineConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'line';
  widgetData: LineWidgetData;
};

type ApiAnalyticsWidgetBarConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'bar';
  widgetData: BarWidgetData;
};

export type ApiAnalyticsWidgetTableConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'table';
  widgetData: TableWidgetData;
};

export type ApiAnalyticsWidgetConfig =
  | ApiAnalyticsWidgetStatsConfig
  | ApiAnalyticsWidgetMultiStatsConfig
  | ApiAnalyticsWidgetPieConfig
  | ApiAnalyticsWidgetLineConfig
  | ApiAnalyticsWidgetBarConfig
  | ApiAnalyticsWidgetTableConfig;
export type ApiAnalyticsWidgetType = 'pie' | 'line' | 'bar' | 'table' | 'stats' | 'multi-stats';

@Component({
  selector: 'api-analytics-widget',
  imports: [
    GioWidgetLayoutComponent,
    GioChartPieModule,
    GioChartLineModule,
    GioChartBarComponent,
    ApiAnalyticsWidgetTableComponent,
    NgClass,
    AnalyticsStatsComponent,
    AnalyticsMultiStatsComponent,
  ],
  templateUrl: './api-analytics-widget.component.html',
  styleUrl: './api-analytics-widget.component.scss',
})
export class ApiAnalyticsWidgetComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  config = input.required<ApiAnalyticsWidgetConfig>();

  tableFirstColumnClick = output<{ row: ApiAnalyticsWidgetTableRowData; keyIdentifier?: string }>();

  statsData: Signal<StatsWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'stats' ? currentConfig.widgetData : null;
  });

  multiStatsData: Signal<MultiStatsWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'multi-stats' ? currentConfig.widgetData : null;
  });

  pieData: Signal<PieWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'pie' ? currentConfig.widgetData : null;
  });

  lineData: Signal<LineWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'line' ? currentConfig.widgetData : null;
  });

  barData: Signal<BarWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'bar' ? currentConfig.widgetData : null;
  });

  tableData: Signal<TableWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'table' ? currentConfig.widgetData : null;
  });

  firstColumnClickable = computed(() => {
    const td = this.tableData();
    if (!td) return false;
    return Boolean(td.relativePath);
  });

  onChildTableFirstColumnClick(row: ApiAnalyticsWidgetTableRowData) {
    const td = this.tableData();
    if (!td) {
      return;
    }
    const key = row && (row['__key'] as string | undefined);
    if (!key || row['unknown'] === 'true') {
      return;
    }
    if (td.relativePath) {
      this.router.navigate([td.relativePath, key], { relativeTo: this.activatedRoute });
    }
    const keyIdentifier = td.keyIdentifier;
    this.tableFirstColumnClick.emit({ row, keyIdentifier });
  }

  public pieChartLabelFormatter = function () {
    const value = this.point.y;
    return `<div style="text-align: center;">
            <div style="font-size: 14px; color: #666; font-weight: 700;">${value}</div>
          </div>`;
  };
}
