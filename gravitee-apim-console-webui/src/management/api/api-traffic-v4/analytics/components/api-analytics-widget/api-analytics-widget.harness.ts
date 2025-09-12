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
import { ComponentHarness } from '@angular/cdk/testing';

import { GioWidgetLayoutHarness } from '../../../../../../shared/components/gio-widget-layout/gio-widget-layout.harness';
import { ApiAnalyticsWidgetTableHarness } from '../api-analytics-widget-table/api-analytics-widget-table.harness';
import { GioChartPieHarness } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.harness';
import { AnalyticsStatsComponentHarness } from '../../../../../../shared/components/analytics-stats/analytics-stats.component.harness';
import { AnalyticsMultiStatsComponentHarness } from '../../../../../../shared/components/analytics-multi-stats/analytics-multi-stats.component.harness';

export class ApiAnalyticsWidgetHarness extends ComponentHarness {
  static hostSelector = 'api-analytics-widget';

  protected getStatsWidget = this.locatorFor(AnalyticsStatsComponentHarness);
  protected getMultiStatsWidget = this.locatorFor(AnalyticsMultiStatsComponentHarness);
  protected getWidgetLayout = this.locatorFor(GioWidgetLayoutHarness);
  protected getTableWidget = this.locatorForOptional(ApiAnalyticsWidgetTableHarness);
  protected getPieChart = this.locatorForOptional(GioChartPieHarness);

  async getStatsComponentHarness(): Promise<AnalyticsStatsComponentHarness> {
    return this.getStatsWidget();
  }

  async getMultiStatsComponentHarness(): Promise<AnalyticsMultiStatsComponentHarness> {
    return this.getMultiStatsWidget();
  }

  /**
   * Gets the widget layout harness
   */
  async getWidgetLayoutHarness(): Promise<GioWidgetLayoutHarness> {
    return this.getWidgetLayout();
  }

  async getPieChartHarness(): Promise<GioChartPieHarness | null> {
    return this.getPieChart();
  }

  /**
   * Gets the table widget harness if present
   */
  async getTableWidgetHarness(): Promise<ApiAnalyticsWidgetTableHarness | null> {
    return this.getTableWidget();
  }

  /**
   * Checks if the widget has a table
   */
  async hasTable(): Promise<boolean> {
    const tableWidget = await this.getTableWidget();
    return tableWidget !== null;
  }

  /**
   * Gets the table widget harness (throws if not present)
   */
  async getTableWidgetHarnessOrThrow(): Promise<ApiAnalyticsWidgetTableHarness> {
    const tableWidget = await this.getTableWidget();
    if (!tableWidget) {
      throw new Error('Table widget not found');
    }
    return tableWidget;
  }

  /**
   * Gets the number of rows in the table
   */
  async getTableRowCount(): Promise<number> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getRowCount();
  }

  /**
   * Gets the number of columns in the table
   */
  async getTableColumnCount(): Promise<number> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getColumnCount();
  }

  /**
   * Gets the table header texts
   */
  async getTableHeaderTexts(): Promise<string[]> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getHeaderTexts();
  }

  /**
   * Gets the table data as a 2D array
   */
  async getTableData(): Promise<string[][]> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getTableData();
  }

  /**
   * Gets the text of a specific table cell
   */
  async getTableCellText(rowIndex: number, columnIndex: number): Promise<string> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getCellText(rowIndex, columnIndex);
  }

  /**
   * Checks if the table has data
   */
  async hasTableData(): Promise<boolean> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.hasData();
  }

  /**
   * Checks if the table shows no data message
   */
  async hasTableNoDataMessage(): Promise<boolean> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.hasNoDataMessage();
  }

  /**
   * Gets the table no data message text
   */
  async getTableNoDataMessage(): Promise<string | null> {
    const tableWidget = await this.getTableWidgetHarnessOrThrow();
    return tableWidget.getNoDataMessage();
  }

  // Delegate to GioWidgetLayoutHarness for common functionality
  async getTitleText(): Promise<string> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.getTitleText();
  }

  async isLoading(): Promise<boolean> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.isLoading();
  }

  async isEmpty(): Promise<boolean> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.isEmpty();
  }

  async hasError(): Promise<boolean> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.hasError();
  }

  async getErrorText(): Promise<string> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.getErrorMessageText();
  }

  async hasTooltipIcon(): Promise<boolean> {
    const widgetLayout = await this.getWidgetLayout();
    return widgetLayout.hasTooltipIcon();
  }
}
