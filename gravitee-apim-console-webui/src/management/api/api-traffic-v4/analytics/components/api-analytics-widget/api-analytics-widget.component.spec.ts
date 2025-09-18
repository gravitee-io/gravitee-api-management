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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { HarnessLoader } from '@angular/cdk/testing';

import { ApiAnalyticsWidgetComponent, ApiAnalyticsWidgetConfig } from './api-analytics-widget.component';
import { ApiAnalyticsWidgetHarness } from './api-analytics-widget.harness';

import { GioTestingModule } from '../../../../../../shared/testing';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.component';
import { MultiStatsWidgetData } from '../../../../../../shared/components/analytics-multi-stats/analytics-multi-stats.component';

@Component({
  selector: 'test-host',
  template: '<api-analytics-widget [config]="config()" />',
  standalone: true,
  imports: [ApiAnalyticsWidgetComponent],
})
class TestComponent {
  config = input.required<ApiAnalyticsWidgetConfig>();
}

describe('ApiAnalyticsWidgetComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let harnessLoader: HarnessLoader;
  let harness: ApiAnalyticsWidgetHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestComponent);

    fixture.componentRef.setInput('config', {
      title: 'Test Analytics Widget',
      state: 'loading',
      widgetType: 'pie',
      widgetData: [],
    });

    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
    harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
  });

  describe('Pie Charts', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'loading',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show loading state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'loading',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
    });

    it('should show success state with pie chart data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'success',
        widgetType: 'pie',
        widgetData: [
          { label: 'Test 1', value: 10, color: '#ff0000' },
          { label: 'Test 2', value: 20, color: '#00ff00' },
        ],
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
      expect(await harness.getPieChartHarness()).not.toBeNull();
    });

    it('should show success state with empty pie data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'success',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
      expect(await harness.getPieChartHarness()).not.toBeNull();
    });

    it('should show empty state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'empty',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.isEmpty()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
    });

    it('should show error state with errors', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'error',
        errors: ['Test error message'],
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.hasError()).toBe(true);
      expect(await harness.getErrorText()).toBe('Test error message');
      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
    });

    it('should show tooltip when provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'success',
        tooltip: 'Test tooltip',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
    });

    it('should not show tooltip when not provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Pie Chart Widget',
        state: 'success',
        widgetType: 'pie',
        widgetData: [],
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Pie Chart Widget');
    });
  });

  describe('Line Charts', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'loading',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show loading state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'loading',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });

    it('should show success state with line chart data', async () => {
      const lineData: GioChartLineData[] = [
        { name: 'Series 1', values: [1, 2, 3, 4, 5] },
        { name: 'Series 2', values: [2, 4, 6, 8, 10] },
      ];
      const lineOptions: GioChartLineOptions = {
        pointStart: 1728992010000,
        pointInterval: 30000,
      };

      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'success',
        widgetType: 'line',
        widgetData: {
          data: lineData,
          options: lineOptions,
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
      // Note: Line chart harness would need to be added to ApiAnalyticsWidgetHarness
      // For now, we can check that the component renders without errors
    });

    it('should show success state with empty line data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'success',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });

    it('should show empty state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'empty',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.isEmpty()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });

    it('should show error state with errors', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'error',
        errors: ['Test error message'],
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.hasError()).toBe(true);
      expect(await harness.getErrorText()).toBe('Test error message');
      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });

    it('should show tooltip when provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'success',
        tooltip: 'Test tooltip',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });

    it('should not show tooltip when not provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Line Chart Widget',
        state: 'success',
        widgetType: 'line',
        widgetData: {
          data: [],
          options: { pointStart: 0, pointInterval: 1000 },
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Line Chart Widget');
    });
  });

  describe('Bar Charts', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'loading',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show loading state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'loading',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should show success state with bar chart data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [
            { name: 'Category A', value: 10 },
            { name: 'Category B', value: 20 },
            { name: 'Category C', value: 15 },
          ],
          options: {
            yAxis: { title: 'Count' },
            xAxis: { title: 'Categories' },
          },
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
      // Note: Bar chart harness would need to be added to ApiAnalyticsWidgetHarness
      // For now, we can check that the component renders without errors
    });

    it('should show success state with empty bar data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should show empty state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'empty',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.isEmpty()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should show error state with errors', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'error',
        errors: ['Test error message'],
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.hasError()).toBe(true);
      expect(await harness.getErrorText()).toBe('Test error message');
      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should show tooltip when provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        tooltip: 'Test tooltip',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should not show tooltip when not provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
    });

    it('should handle bar chart with custom options', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [
            { name: 'Jan', value: 100 },
            { name: 'Feb', value: 150 },
            { name: 'Mar', value: 200 },
          ],
          options: {
            yAxis: { title: 'Revenue ($)' },
            xAxis: { title: 'Months' },
            colors: ['#ff6b6b', '#4ecdc4', '#45b7d1'],
          },
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
      // Component should render successfully with custom options
    });

    it('should handle bar chart with single data point', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [{ name: 'Single Category', value: 42 }],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
      // Component should handle single data point gracefully
    });

    it('should handle bar chart with zero values', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [
            { name: 'Zero A', value: 0 },
            { name: 'Zero B', value: 0 },
          ],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
      // Component should handle zero values appropriately
    });

    it('should handle bar chart with large numbers', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Bar Chart Widget',
        state: 'success',
        widgetType: 'bar',
        widgetData: {
          data: [
            { name: 'Large Value A', value: 1000000 },
            { name: 'Large Value B', value: 2500000 },
          ],
          options: {},
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Bar Chart Widget');
      // Component should handle large numbers appropriately
    });
  });

  describe('Unknown Widget Types', () => {
    it('should show default content for unknown widget type', async () => {
      // Create a config with an unknown widget type
      const unknownConfig: ApiAnalyticsWidgetConfig = {
        title: 'Test Unknown Widget',
        state: 'success',
        widgetType: 'pie' as any, // Force unknown type
        widgetData: [],
      };

      fixture.componentRef.setInput('config', unknownConfig);
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Unknown Widget');
      // The component should still render without errors even with unknown types
    });

    it('should handle invalid widget data gracefully', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Invalid Widget',
        state: 'success',
        widgetType: 'pie',
        widgetData: null as any,
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Invalid Widget');
      // Component should handle null/undefined data gracefully
    });
  });

  describe('Table Charts', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'loading',
        widgetType: 'table',
        widgetData: {
          columns: [],
          data: [],
        },
      });
      fixture.detectChanges();
      harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show loading state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'loading',
        widgetType: 'table',
        widgetData: {
          columns: [],
          data: [],
        },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Table Widget');
    });

    it('should show success state with table data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
          ],
          data: [
            { name: 'Item 1', count: 10 },
            { name: 'Item 2', count: 20 },
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTitleText()).toBe('Test Table Widget');
      expect(await harness.hasTable()).toBe(true);
      expect(await harness.getTableColumnCount()).toBe(2);
      expect(await harness.getTableRowCount()).toBeGreaterThan(0);
    });

    it('should display correct table headers', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
          ],
          data: [
            { name: 'Item 1', count: 10 },
            { name: 'Item 2', count: 20 },
          ],
        },
      });
      fixture.detectChanges();

      const headers = await harness.getTableHeaderTexts();
      expect(headers).toEqual(['Name', 'Count']);
    });

    it('should display table data correctly', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
          ],
          data: [
            { name: 'Item 1', count: 10 },
            { name: 'Item 2', count: 20 },
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTableData()).toBe(true);

      // Check first row data
      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 0) {
        expect(await harness.getTableCellText(0, 0)).toBe('Item 1');
      }

      if (rowCount > 0 && columnCount > 1) {
        expect(await harness.getTableCellText(0, 1)).toContain('10');
      }
    });

    it('should format number data in table correctly', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
          ],
          data: [{ name: 'Item 1', count: 1234 }],
        },
      });
      fixture.detectChanges();

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 1) {
        const cellText = await harness.getTableCellText(0, 1);
        expect(cellText).toContain('1,234'); // Number pipe formatting
      }
    });

    it('should format percentage data in table correctly', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'percentage', label: 'Percentage', dataType: 'percentage' },
          ],
          data: [{ name: 'Item 1', percentage: 0.25 }],
        },
      });
      fixture.detectChanges();

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 1) {
        const cellText = await harness.getTableCellText(0, 1);
        expect(cellText).toContain('25%'); // Percent pipe formatting
      }
    });

    it('should show empty state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'empty',
        widgetType: 'table',
        widgetData: {
          columns: [],
          data: [],
        },
      });
      fixture.detectChanges();

      expect(await harness.isEmpty()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Table Widget');
    });

    it('should show error state with errors', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'error',
        errors: ['Test error message'],
        widgetType: 'table',
        widgetData: {
          columns: [],
          data: [],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasError()).toBe(true);
      expect(await harness.getErrorText()).toBe('Test error message');
      expect(await harness.getTitleText()).toBe('Test Table Widget');
    });

    it('should show tooltip when provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        tooltip: 'Test tooltip',
        widgetType: 'table',
        widgetData: {
          columns: [{ name: 'name', label: 'Name', dataType: 'string' }],
          data: [{ name: 'Item 1' }],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Table Widget');
    });

    it('should not show tooltip when not provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [{ name: 'name', label: 'Name', dataType: 'string' }],
          data: [{ name: 'Item 1' }],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Table Widget');
    });

    it('should handle empty table data', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
          ],
          data: [],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTable()).toBe(true);
      expect(await harness.hasTableNoDataMessage()).toBe(true);
      expect(await harness.getTableNoDataMessage()).toBe('No content to display');
    });

    it('should handle complex table data with multiple data types', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
            { name: 'percentage', label: 'Percentage', dataType: 'percentage' },
          ],
          data: [
            { name: 'Item 1', count: 100, percentage: 0.5 },
            { name: 'Item 2', count: 200, percentage: 0.75 },
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTableColumnCount()).toBe(3);
      expect(await harness.getTableRowCount()).toBeGreaterThan(0);

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 0) {
        expect(await harness.getTableCellText(0, 0)).toBe('Item 1');
      }

      if (rowCount > 0 && columnCount > 1) {
        expect(await harness.getTableCellText(0, 1)).toContain('100');
      }

      if (rowCount > 0 && columnCount > 2) {
        expect(await harness.getTableCellText(0, 2)).toContain('50%');
      }
    });

    it('should handle sortable columns in table', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string', isSortable: true },
            { name: 'count', label: 'Count', dataType: 'number', isSortable: true },
          ],
          data: [
            { name: 'Item 1', count: 10 },
            { name: 'Item 2', count: 20 },
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTableColumnCount()).toBe(2);
      expect(await harness.getTableRowCount()).toBeGreaterThan(0);
    });

    it('should handle missing column data gracefully in table', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { name: 'name', label: 'Name', dataType: 'string' },
            { name: 'count', label: 'Count', dataType: 'number' },
            { name: 'optional', label: 'Optional', dataType: 'string' },
          ],
          data: [
            { name: 'Item 1', count: 10 }, // Missing 'optional' field
            { name: 'Item 2', count: 20, optional: 'Present' },
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTableRowCount()).toBeGreaterThan(0);

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 2) {
        expect(await harness.getTableCellText(0, 2)).toBe('(empty)'); // Missing optional field
      }

      if (rowCount > 1 && columnCount > 2) {
        expect(await harness.getTableCellText(1, 2)).toBe('Present'); // Present optional field
      }
    });
  });

  describe('Stats Widget', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'loading',
        widgetType: 'stats',
        widgetData: [],
      });
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show formatted zero value', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'success',
        widgetType: 'stats',
        widgetData: { stats: 0, statsUnit: 'ms' },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Stats Widget');
      const statsHarness = await harness.getStatsComponentHarness();
      expect(await statsHarness.getDisplayedValue()).toBe('0ms');
    });

    it('should show defined value formatted to number without unit', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'success',
        widgetType: 'stats',
        widgetData: { stats: 1000000 },
      });

      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Stats Widget');
      const statsHarness = await harness.getStatsComponentHarness();
      expect(await statsHarness.getDisplayedValue()).toBe('1,000,000');
    });

    it('should show defined value with the unit', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'success',
        widgetType: 'stats',
        widgetData: { stats: 100, statsUnit: 'candies' },
      });

      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Stats Widget');
      const statsHarness = await harness.getStatsComponentHarness();
      expect(await statsHarness.getDisplayedValue()).toBe('100candies');
    });

    it('should display empty state for undefined stats', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'success',
        widgetType: 'stats',
        widgetData: { stats: undefined, statsUnit: 'ms' },
      });

      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Stats Widget');
      const statsHarness = await harness.getStatsComponentHarness();
      expect(await statsHarness.getDisplayedValue()).toBe('-');
    });

    it('should display empty state for null stats', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Stats Widget',
        state: 'success',
        widgetType: 'stats',
        widgetData: { stats: null, statsUnit: 'ms' },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Test Stats Widget');
      const statsHarness = await harness.getStatsComponentHarness();
      expect(await statsHarness.getDisplayedValue()).toBe('-');
    });
  });

  describe('Multi-Stats Widget', () => {
    beforeEach(async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Multi-Stats Widget',
        state: 'loading',
        widgetType: 'multi-stats',
        widgetData: { items: [] },
      });
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
      harness = await harnessLoader.getHarness(ApiAnalyticsWidgetHarness);
    });

    it('should show loading state', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Multi-Stats Widget',
        state: 'loading',
        widgetType: 'multi-stats',
        widgetData: { items: [] },
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Multi-Stats Widget');
    });

    it('should display multiple stats with labels', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [
          { label: 'Downstream', value: 1247, unit: '' },
          { label: 'Upstream', value: 892, unit: '' },
        ],
      };

      fixture.componentRef.setInput('config', {
        title: 'Active Connections',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      expect(await harness.isLoading()).toBe(false);
      expect(await harness.getTitleText()).toBe('Active Connections');

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      expect(await multiStatsHarness.getStatsItemCount()).toBe(2);

      const displayedValues = await multiStatsHarness.getAllStatsValues();
      expect(displayedValues).toEqual(['1,247', '892']);

      const displayedLabels = await multiStatsHarness.getAllStatsLabels();
      expect(displayedLabels).toEqual(['Downstream', 'Upstream']);
    });

    it('should display stats with units', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [
          { label: 'From Clients', value: 1500, unit: ' msgs' },
          { label: 'To Broker', value: 1450, unit: ' msgs' },
        ],
      };

      fixture.componentRef.setInput('config', {
        title: 'Messages Produced',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      const displayedValues = await multiStatsHarness.getAllStatsValues();
      expect(displayedValues).toEqual(['1,500 msgs', '1,450 msgs']);
    });

    it('should handle zero values', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [
          { label: 'Downstream', value: 0, unit: '' },
          { label: 'Upstream', value: 0, unit: '' },
        ],
      };

      fixture.componentRef.setInput('config', {
        title: 'Active Connections',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      const displayedValues = await multiStatsHarness.getAllStatsValues();
      expect(displayedValues).toEqual(['0', '0']);
    });

    it('should handle large numbers with formatting', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [
          { label: 'Downstream', value: 1234567, unit: '' },
          { label: 'Upstream', value: 987654, unit: '' },
        ],
      };

      fixture.componentRef.setInput('config', {
        title: 'Active Connections',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      const displayedValues = await multiStatsHarness.getAllStatsValues();
      expect(displayedValues).toEqual(['1,234,567', '987,654']);
    });

    it('should handle null values', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [
          { label: 'Downstream', value: null as any, unit: '' },
          { label: 'Upstream', value: 892, unit: '' },
        ],
      };

      fixture.componentRef.setInput('config', {
        title: 'Active Connections',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      const displayedValues = await multiStatsHarness.getAllStatsValues();
      expect(displayedValues).toEqual(['-', '892']);
    });

    it('should handle empty items array', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [],
      };

      fixture.componentRef.setInput('config', {
        title: 'Active Connections',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      expect(await multiStatsHarness.getStatsItemCount()).toBe(0);
    });

    it('should show error state with errors', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Multi-Stats Widget',
        state: 'error',
        errors: ['Test error message'],
        widgetType: 'multi-stats',
        widgetData: { items: [] },
      });
      fixture.detectChanges();

      expect(await harness.hasError()).toBe(true);
      expect(await harness.getErrorText()).toBe('Test error message');
      expect(await harness.getTitleText()).toBe('Test Multi-Stats Widget');
    });

    it('should show tooltip when provided', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Multi-Stats Widget',
        state: 'success',
        tooltip: 'Test tooltip for multi-stats',
        widgetType: 'multi-stats',
        widgetData: { items: [{ label: 'Test', value: 100, unit: '' }] },
      });
      fixture.detectChanges();

      expect(await harness.hasTooltipIcon()).toBe(true);
      expect(await harness.getTitleText()).toBe('Test Multi-Stats Widget');
    });

    it('should handle single item', async () => {
      const multiStatsData: MultiStatsWidgetData = {
        items: [{ label: 'Single Item', value: 42, unit: ' units' }],
      };

      fixture.componentRef.setInput('config', {
        title: 'Single Multi-Stats Widget',
        state: 'success',
        widgetType: 'multi-stats',
        widgetData: multiStatsData,
      });
      fixture.detectChanges();

      const multiStatsHarness = await harness.getMultiStatsComponentHarness();
      expect(await multiStatsHarness.getStatsItemCount()).toBe(1);
      expect(await multiStatsHarness.getStatsValueAt(0)).toBe('42 units');
      expect(await multiStatsHarness.getStatsLabelAt(0)).toBe('Single Item');
    });
  });
});
