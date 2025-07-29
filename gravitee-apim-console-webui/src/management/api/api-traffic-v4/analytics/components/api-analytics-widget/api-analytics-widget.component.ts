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
import { Component, input, computed, Signal } from '@angular/core';
import { NgClass } from '@angular/common';

import {
  GioWidgetLayoutComponent,
  GioWidgetLayoutState,
} from '../../../../../../shared/components/gio-widget-layout/gio-widget-layout.component';
import { GioChartPieInput } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.component';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';
import { GioChartLineModule } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import {
  ApiAnalyticsWidgetTableComponent,
  ApiAnalyticsWidgetTableDataColumn,
  ApiAnalyticsWidgetTableRowData,
} from '../api-analytics-widget-table/api-analytics-widget-table.component';

type PieWidgetData = GioChartPieInput[];
type LineWidgetData = { data: GioChartLineData[]; options?: GioChartLineOptions };
type TableWidgetData = { columns: ApiAnalyticsWidgetTableDataColumn[]; data: ApiAnalyticsWidgetTableRowData[] };

interface BaseApiAnalyticsWidgetConfig {
  title: string;
  tooltip?: string;
  state: GioWidgetLayoutState;
  errors?: string[];
}

type ApiAnalyticsWidgetPieConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'pie';
  widgetData: PieWidgetData;
};

type ApiAnalyticsWidgetLineConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'line';
  widgetData: LineWidgetData;
};

type ApiAnalyticsWidgetTableConfig = BaseApiAnalyticsWidgetConfig & {
  widgetType: 'table';
  widgetData: TableWidgetData;
};

export type ApiAnalyticsWidgetConfig = ApiAnalyticsWidgetPieConfig | ApiAnalyticsWidgetLineConfig | ApiAnalyticsWidgetTableConfig;
export type ApiAnalyticsWidgetType = 'pie' | 'line' | 'table';

@Component({
  selector: 'api-analytics-widget',
  imports: [GioWidgetLayoutComponent, GioChartPieModule, GioChartLineModule, ApiAnalyticsWidgetTableComponent, NgClass],
  templateUrl: './api-analytics-widget.component.html',
  styleUrl: './api-analytics-widget.component.scss',
})
export class ApiAnalyticsWidgetComponent {
  config = input.required<ApiAnalyticsWidgetConfig>();

  pieData: Signal<PieWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'pie' ? currentConfig.widgetData : null;
  });

  lineData: Signal<LineWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'line' ? currentConfig.widgetData : null;
  });

  tableData: Signal<TableWidgetData | null> = computed(() => {
    const currentConfig = this.config();
    return currentConfig.widgetType === 'table' ? currentConfig.widgetData : null;
  });

  public pieChartLabelFormatter = function () {
    const value = this.point.y;
    return `<div style="text-align: center;">
            <div style="font-size: 14px; color: #666; font-weight: 700;">${value}</div>
          </div>`;
  };
}
