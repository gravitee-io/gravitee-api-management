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
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvLogsTableComponent);
    fixture.componentRef.setInput('logs', defaultLogs);
    fixture.componentRef.setInput('pagination', defaultPagination);
    fixture.detectChanges();

    logsTableHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, EnvLogsTableHarness);
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
});
