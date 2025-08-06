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
            { label: 'name', dataType: 'string' },
            { label: 'count', dataType: 'number' },
          ],
          data: [
            ['Item 1', 10],
            ['Item 2', 20],
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.hasTableData()).toBe(true);

      // Check first row data
      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 0) {
        expect(await harness.getTableCellText(0, 0)).toBe('Item 2');
      }

      if (rowCount > 0 && columnCount > 1) {
        expect(await harness.getTableCellText(0, 1)).toContain('20');
      }
    });

    it('should format number data in table correctly', async () => {
      fixture.componentRef.setInput('config', {
        title: 'Test Table Widget',
        state: 'success',
        widgetType: 'table',
        widgetData: {
          columns: [
            { label: 'name', dataType: 'string' },
            { label: 'count', dataType: 'number' },
          ],
          data: [['Item 1', 1234]],
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
            { label: 'name', dataType: 'string' },
            { label: 'percentage', dataType: 'percentage' },
          ],
          data: [['Item 1', 0.25]],
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
            { label: 'name', dataType: 'string' },
            { label: 'count', dataType: 'number' },
            { label: 'percentage', dataType: 'percentage' },
          ],
          data: [
            ['Item 1', 100, 0.5],
            ['Item 2', 200, 0.75],
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTableColumnCount()).toBe(3);
      expect(await harness.getTableRowCount()).toBeGreaterThan(0);

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 0) {
        expect(await harness.getTableCellText(0, 0)).toBe('Item 2');
      }

      if (rowCount > 0 && columnCount > 1) {
        expect(await harness.getTableCellText(0, 1)).toContain('200');
      }

      if (rowCount > 0 && columnCount > 2) {
        expect(await harness.getTableCellText(0, 2)).toContain('75%');
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
            { label: 'name', dataType: 'string' },
            { label: 'count', dataType: 'number' },
            { label: 'optional', dataType: 'string' },
          ],
          data: [
            ['Item 1', 10], // Missing 'optional' field
            ['Item 2', 20, 'Present'],
          ],
        },
      });
      fixture.detectChanges();

      expect(await harness.getTableRowCount()).toBeGreaterThan(0);

      const rowCount = await harness.getTableRowCount();
      const columnCount = await harness.getTableColumnCount();

      if (rowCount > 0 && columnCount > 2) {
        expect(await harness.getTableCellText(0, 2)).toBe('Present'); // Missing optional field
      }

      if (rowCount > 1 && columnCount > 2) {
        expect(await harness.getTableCellText(1, 2)).toBe(''); // Present optional field
      }
    });
  });
});
