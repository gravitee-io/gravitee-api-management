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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { FailedHealthChecksComponent } from './failed-health-checks.component';
import { FailedHealthChecksHarness } from './failed-health-checks.harness';
import { FailedHealthCheckDetailsDialogHarness } from './failed-health-check-details-dialog/failed-health-check-details-dialog.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { fakeApiHealthCheckLogs, fakeHealthCheckStep } from '../../../../../entities/management-api-v2/api/v4/healthCheck.fixture';
import { HealthCheckLogsResponse } from '../../../../../entities/management-api-v2/api/v4/healthCheck';

describe('FailedHealthChecksComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<FailedHealthChecksComponent>;
  let componentHarness: FailedHealthChecksHarness;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FailedHealthChecksComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { apiId: API_ID } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FailedHealthChecksComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FailedHealthChecksHarness);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.autoDetectChanges(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectGetApiHealthCheckLogs(response: HealthCheckLogsResponse = fakeApiHealthCheckLogs()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/health/logs`;
    httpTestingController.expectOne((request) => request.method === 'GET' && request.url.startsWith(url)).flush(response);
  }

  describe('table columns', () => {
    it('should_display_response_time_and_actions_columns', async () => {
      expectGetApiHealthCheckLogs();

      const table = await componentHarness.tableHarness();
      const headerRows = await table.getHeaderRows();
      const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

      expect(headerCells).toEqual([
        {
          timestamp: 'Timestamp',
          endpoint: 'Endpoint',
          gateway: 'Gateway',
          responseTime: 'Response Time',
          actions: '',
        },
      ]);
    });

    it('should_display_response_time_in_milliseconds', async () => {
      expectGetApiHealthCheckLogs();

      const table = await componentHarness.tableHarness();
      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

      expect(rowCells[0].responseTime).toEqual('150ms');
    });
  });

  describe('view details button', () => {
    it('should_enable_view_details_button_when_log_has_steps', async () => {
      expectGetApiHealthCheckLogs();

      expect(await componentHarness.getViewDetailsButtonCount()).toEqual(1);
      expect(await componentHarness.isViewDetailsButtonDisabled(0)).toBe(false);
    });

    it('should_disable_view_details_button_when_steps_are_empty', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({ ...logs, data: [{ ...logs.data[0], steps: [] }] });

      expect(await componentHarness.getViewDetailsButtonCount()).toEqual(1);
      expect(await componentHarness.isViewDetailsButtonDisabled(0)).toBe(true);
    });

    it('should_disable_view_details_button_when_steps_are_undefined', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({ ...logs, data: [{ ...logs.data[0], steps: undefined }] });

      expect(await componentHarness.isViewDetailsButtonDisabled(0)).toBe(true);
    });

    it('should_explain_through_tooltip_why_disabled_button_cannot_be_used', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({ ...logs, data: [{ ...logs.data[0], steps: [] }] });

      expect(await componentHarness.getViewDetailsTooltip(0)).toEqual('No details available for this probe');
    });

    it('should_keep_disabled_button_hoverable_so_its_tooltip_stays_reachable', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({ ...logs, data: [{ ...logs.data[0], steps: [] }] });

      // A native `disabled` attribute would suppress pointer events in a real browser and kill the tooltip.
      expect(await componentHarness.getViewDetailsDisabledAttributes(0)).toEqual({
        nativeDisabled: null,
        ariaDisabled: 'true',
      });
    });

    it('should_not_open_dialog_when_disabled_button_is_clicked', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({ ...logs, data: [{ ...logs.data[0], steps: [] }] });

      await componentHarness.clickViewDetails(0);

      expect(await rootLoader.getAllHarnesses(FailedHealthCheckDetailsDialogHarness)).toHaveLength(0);
    });
  });

  describe('details dialog', () => {
    it('should_open_details_dialog_with_log_data_when_clicking_view_details', async () => {
      const logs = fakeApiHealthCheckLogs();
      expectGetApiHealthCheckLogs({
        ...logs,
        data: [{ ...logs.data[0], steps: [fakeHealthCheckStep({ name: 'probe-step' })] }],
      });

      await componentHarness.clickViewDetails(0);

      const dialog = await rootLoader.getHarness(FailedHealthCheckDetailsDialogHarness);
      expect(await dialog.getStepNames()).toEqual(['probe-step']);
      expect(await dialog.getSummaryValue('endpoint')).toEqual('sample-endpoint-name');

      await dialog.close();
    });

    it('should_not_trigger_additional_http_call_when_opening_details_dialog', async () => {
      expectGetApiHealthCheckLogs();

      await componentHarness.clickViewDetails(0);

      const dialog = await rootLoader.getHarness(FailedHealthCheckDetailsDialogHarness);
      httpTestingController.verify();
      await dialog.close();
    });
  });

  describe('empty state', () => {
    it('should_display_no_logs_row_when_response_is_empty', async () => {
      expectGetApiHealthCheckLogs(
        fakeApiHealthCheckLogs({
          data: [],
          pagination: { totalCount: 0, page: 1, pageCount: 0, pageItemsCount: 0, perPage: 10 },
        }),
      );

      const table = await componentHarness.tableHarness();
      // Material 19's row harness also matches the *matNoDataRow, so data rows are filtered explicitly.
      expect(await table.getRows({ selector: ':not(.mat-mdc-no-data-row)' })).toHaveLength(0);
      expect(await componentHarness.getViewDetailsButtonCount()).toEqual(0);
    });
  });
});
