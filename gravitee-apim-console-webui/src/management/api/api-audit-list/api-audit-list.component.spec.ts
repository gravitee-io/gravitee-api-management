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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatDateRangeInputHarness } from '@angular/material/datepicker/testing';
import { ActivatedRoute } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiAuditListComponent } from './api-audit-list.component';
import { ApiAuditListModule } from './api-audit-list.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { fakeMetadataPageAudit } from '../../../entities/audit/Audit.fixture';

describe('ApiAuditListComponent', () => {
  let fixture: ComponentFixture<ApiAuditListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const API_ID = 'id_test';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, ApiAuditListModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { API_ID } } } }],
      schemas: [NO_ERRORS_SCHEMA],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        isTabbable: () => true,
      },
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiAuditListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should display audit table', async () => {
    expectGetApiAuditListRequest();
    expectGetApiEventsListRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);
    expect(rowCells[0]).toEqual({
      date: 'Apr 19, 2022, 3:32:30 PM',
      event: 'THEME_UPDATED',
      patch: '',
      targets: 'THEME:default',
      user: 'system',
    });
  });

  it('should display audit logs with event filter', async () => {
    expectGetApiAuditListRequest();
    expectGetApiEventsListRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    const eventInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=event]' }));
    await eventInput.clickOptions({ text: 'ROLE_UPDATED' });
    expectGetApiAuditListRequest({ event: 'ROLE_UPDATED' });
  });

  it('should display audit logs with from & to range', async () => {
    expectGetApiAuditListRequest();
    expectGetApiEventsListRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    const rangeSelect = await loader.getHarness(MatDateRangeInputHarness.with({ selector: '[formGroupName=range]' }));
    await (await rangeSelect.getStartInput()).setValue('4/11/2022');
    expectGetApiAuditListRequest({ from: 1649635200000 });

    await (await rangeSelect.getEndInput()).setValue('4/20/2022');
    expectGetApiAuditListRequest({ from: 1649635200000, to: 1650499199999 });
  });

  it('should reset pagination on filter change', async () => {
    expectGetApiAuditListRequest();
    expectGetApiEventsListRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

    await (await tableWrapper.getPaginator()).goToNextPage();
    expectGetApiAuditListRequest(undefined, { page: 2, size: 10 });

    const eventInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=event]' }));
    await eventInput.clickOptions({ text: 'ROLE_UPDATED' });
    expectGetApiAuditListRequest({ event: 'ROLE_UPDATED' });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectGetApiAuditListRequest(
    filters: {
      event?: string;
      from?: number;
      to?: number;
    } = {},
    pagination: {
      page: number;
      size: number;
    } = { page: 1, size: 10 },
  ) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/undefined/audit?page=${pagination.page}&size=${pagination.size}${
          filters.event ? '&event=' + filters.event : ''
        }${filters.from ? '&from=' + filters.from : ''}${filters.to ? '&to=' + filters.to : ''}`,
        method: 'GET',
      })
      .flush(fakeMetadataPageAudit());
  }

  function expectGetApiEventsListRequest() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/undefined/audit/events`,
        method: 'GET',
      })
      .flush(['TENANT_CREATED', 'ROLE_UPDATED']);
  }
});
