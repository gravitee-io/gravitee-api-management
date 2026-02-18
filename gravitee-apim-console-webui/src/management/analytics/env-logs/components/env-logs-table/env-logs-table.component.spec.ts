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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { EnvLogsTableComponent } from './env-logs-table.component';
import { EnvLogsTableHarness } from './env-logs-table.harness';

import { fakeEnvLog, fakeEnvLogs } from '../../models/env-log.fixture';

describe('EnvLogsTableComponent', () => {
  let fixture: ComponentFixture<EnvLogsTableComponent>;
  let logsTableHarness: EnvLogsTableHarness;

  const defaultLogs = fakeEnvLogs();
  const defaultPagination = { page: 1, perPage: 10, totalCount: defaultLogs.length };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvLogsTableComponent, NoopAnimationsModule],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvLogsTableComponent);
    fixture.componentRef.setInput('logs', defaultLogs);
    fixture.componentRef.setInput('pagination', defaultPagination);
    fixture.detectChanges();

    logsTableHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsTableHarness);
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display logs with correct data', async () => {
    const rowsData = await logsTableHarness.getRowsData();

    expect(rowsData.length).toEqual(5);

    const firstRow = rowsData[0];
    expect(firstRow['timestamp']).toEqual('15/06/2025 12:00:00');
    expect(firstRow['method']).toEqual('PATCH');
    expect(firstRow['status']).toEqual('200');
    expect(firstRow['api']).toContain('API Name');
    expect(firstRow['plan']).toContain('Keyless');
  });

  it('should display method badges correctly', async () => {
    const methodBadge = await logsTableHarness.getMethodBadge(0);
    expect(methodBadge).toEqual('PATCH');
  });

  it('should display status badges correctly', async () => {
    const statusBadge = await logsTableHarness.getStatusBadge(0);
    expect(statusBadge).toEqual('200');
  });

  it('should display hyphen when generic column data is missing', async () => {
    const logWithMissingGateway = fakeEnvLog({ gateway: undefined });

    fixture.componentRef.setInput('logs', [logWithMissingGateway]);
    fixture.componentRef.setInput('pagination', { page: 1, perPage: 10, totalCount: 1 });
    fixture.detectChanges();

    const rowsData = await logsTableHarness.getRowsData();
    expect(rowsData[0]['gateway']).toEqual('—');
  });

  it('should display plan name', async () => {
    const rowsData = await logsTableHarness.getRowsData();
    expect(rowsData[0]['plan']).toContain('Keyless');
  });

  it('should display hyphen when plan is missing', async () => {
    const rowsData = await logsTableHarness.getRowsData();
    // log-2 has undefined plan
    expect(rowsData[1]['plan']).toEqual('—');
  });

  it('should display endpoint reached icon when requestEnded is true', async () => {
    // log-1 has requestEnded: true
    const icon = await logsTableHarness.getEndpointReachedIcon(0);
    expect(icon).toBeTruthy();
  });

  it('should not display endpoint reached icon when requestEnded is false', async () => {
    // log-2 has requestEnded: false
    const icon = await logsTableHarness.getEndpointReachedIcon(1);
    expect(icon).toBeFalsy();
  });

  it('should display preview button for each row', async () => {
    const previewButton = await logsTableHarness.getPreviewButton(0);
    expect(previewButton).toBeTruthy();
  });

  it('should display error icon when log has errorKey', async () => {
    const errorIcon = await logsTableHarness.getErrorIcon(1);
    expect(errorIcon).toBeTruthy();
  });

  it('should not display error icon when log has no errorKey', async () => {
    const errorIcon = await logsTableHarness.getErrorIcon(0);
    expect(errorIcon).toBeFalsy();
  });

  it('should display warning icon when log has warnings', async () => {
    const warningIcon = await logsTableHarness.getWarningIcon(2);
    expect(warningIcon).toBeTruthy();
  });
});
