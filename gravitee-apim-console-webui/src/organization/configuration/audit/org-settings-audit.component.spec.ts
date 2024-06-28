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

import { OrgSettingsAuditComponent } from './org-settings-audit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { fakeMetadataPageAudit } from '../../../entities/audit/Audit.fixture';
import { fakeEnvironment } from '../../../entities/environment/environment.fixture';
import { Environment } from '../../../entities/environment/environment';
import { Api } from '../../../entities/api/Api';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

describe('OrgSettingsAuditComponent', () => {
  let fixture: ComponentFixture<OrgSettingsAuditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioTestingModule, OrganizationSettingsModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsAuditComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should display audit logs', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);
    expect(rowCells[0]).toEqual({
      date: 'Apr 19, 2022, 3:32:30 PM',
      event: 'THEME_UPDATED',
      patch: '',
      reference: 'DEFAULT',
      referenceType: 'ENVIRONMENT',
      targets: 'THEME:default',
      user: 'system',
    });
  });

  it('should display audit logs with event filter', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    const eventInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=event]' }));
    await eventInput.clickOptions({ text: 'ROLE_UPDATED' });
    expectAuditListRequest({ event: 'ROLE_UPDATED' });
  });

  it('should display audit logs with type filter', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    const referenceTypeInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=referenceType]' }));
    await referenceTypeInput.clickOptions({ text: 'ORGANIZATION' });
    expectAuditListRequest({ referenceType: 'ORGANIZATION' });
  });

  it('should display audit logs with api filter', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    // 1. Expect initial page
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    // 2. Expect display of APIs select field
    const referenceTypeSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=referenceType]' }));
    await referenceTypeSelect.clickOptions({ text: 'API' });

    expectAuditListRequest({ referenceType: 'API' });

    expectEnvironmentGetAllRequest([fakeEnvironment({ id: 'envA', name: 'envA' }), fakeEnvironment({ id: 'envB', name: 'envB' })]);
    expectApiGetAllByEnvRequest('envA', [fakeApi({ id: 'envA_api1', name: 'envA_api1' })]);
    expectApiGetAllByEnvRequest('envB', [fakeApi({ id: 'envB_api1', name: 'envB_api1' })]);

    // 3. Expect API selection works and trigger new AuditListRequest
    const apiIdSelect = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=apiId]' }));

    await apiIdSelect.clickOptions({ text: 'envB_api1' });
    expectAuditListRequest({ referenceType: 'API', apiId: 'envB_api1' });
  });

  it('should display audit logs with from & to range', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    // 1. Expect initial page
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    // 2. Expect date range works and trigger new AuditListRequest
    const rangeSelect = await loader.getHarness(MatDateRangeInputHarness.with({ selector: '[formGroupName=range]' }));
    await (await rangeSelect.getStartInput()).setValue('4/11/2022');
    expectAuditListRequest({ from: 1649635200000 });

    await (await rangeSelect.getEndInput()).setValue('4/20/2022');
    expectAuditListRequest({ from: 1649635200000, to: 1650412800000 });
  });

  it('should reset pagination on filter change', async () => {
    expectAuditListRequest();
    expectAuditEventsNameRequest();

    // 1. Expect initial page
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);

    // 2. Expect new call on page 2
    const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

    await (await tableWrapper.getPaginator()).goToNextPage();
    expectAuditListRequest(undefined, { page: 2, size: 10 });

    // 3. Expect change filter reset to page 1
    const referenceTypeInput = await loader.getHarness(MatSelectHarness.with({ selector: '[formControlName=referenceType]' }));
    await referenceTypeInput.clickOptions({ text: 'ORGANIZATION' });
    expectAuditListRequest({ referenceType: 'ORGANIZATION' }, { page: 1, size: 10 });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectAuditListRequest(
    filters: {
      event?: string;
      referenceType?: string;
      environmentId?: string;
      applicationId?: string;
      apiId?: string;
      from?: number;
      to?: number;
    } = {},
    pagination: {
      page: number;
      size: number;
    } = { page: 1, size: 10 },
  ) {
    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.org.baseURL}/audit?page=${pagination.page}&size=${pagination.size}${
        filters.event ? '&event=' + filters.event : ''
      }${filters.referenceType ? '&type=' + filters.referenceType : ''}${
        filters.environmentId ? '&environment=' + filters.environmentId : ''
      }${filters.applicationId ? '&application=' + filters.applicationId : ''}${filters.apiId ? '&api=' + filters.apiId : ''}${
        filters.from ? '&from=' + filters.from : ''
      }${filters.to ? '&to=' + filters.to : ''}`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(fakeMetadataPageAudit());
  }

  function expectAuditEventsNameRequest() {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/audit/events`);
    expect(req.request.method).toEqual('GET');
    req.flush(['TENANT_CREATED', 'ROLE_UPDATED']);
  }

  function expectEnvironmentGetAllRequest(envs: Environment[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments`);
    expect(req.request.method).toEqual('GET');
    req.flush(envs);
  }

  function expectApiGetAllByEnvRequest(envId: string, apis: Api[]) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/environments/${envId}/apis`);
    expect(req.request.method).toEqual('GET');
    req.flush(apis);
  }
});
