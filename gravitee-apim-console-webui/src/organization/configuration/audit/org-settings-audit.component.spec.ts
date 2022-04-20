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

import { OrgSettingsAuditComponent } from './org-settings-audit.component';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { fakeMetadataPageAudit } from '../../../entities/audit/Audit.fixture';

describe('OrgSettingsAuditComponent', () => {
  let fixture: ComponentFixture<OrgSettingsAuditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, GioHttpTestingModule, OrganizationSettingsModule],
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

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#auditTable' }));
    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));
    expect(rowCells.length).toEqual(20);
    expect(rowCells[0]).toEqual({
      date: 'Apr 19, 2022, 3:32:30 PM',
      event: 'THEME_UPDATED',
      patch: '',
      reference: 'system',
      referenceType: 'ENVIRONMENT',
      targets: 'THEME:default',
      user: 'system',
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function expectAuditListRequest() {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/audit`);
    expect(req.request.method).toEqual('GET');
    req.flush(fakeMetadataPageAudit());
  }
});
