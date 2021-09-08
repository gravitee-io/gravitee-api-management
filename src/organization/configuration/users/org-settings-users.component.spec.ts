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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { OrgSettingsUsersComponent } from './org-settings-users.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { UIRouterStateParams, UIRouterState } from '../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { User } from '../../../entities/user/user';
import { fakePagedResult } from '../../../entities/pagedResult';
import { fakeAdminUser } from '../../../entities/user/user.fixture';

describe('OrgSettingsUsersComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUsersComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: UIRouterStateParams, useValue: {} },
      ],
    });
    fixture = TestBed.createComponent(OrgSettingsUsersComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display an empty table', async () => {
    expectUsersListRequest();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#usersTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await await parallel(() => rows.map((row) => row.getCellTextByIndex()));

    expect(headerCells).toEqual([
      {
        actions: '',
        displayName: 'Display name',
        email: 'Email',
        source: 'Source',
        status: 'Status',
        userPicture: '',
      },
    ]);
    expect(rowCells).toEqual([['No user']]);
  });

  it('should display table with data', async () => {
    expectUsersListRequest([fakeAdminUser()]);
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#usersTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

    expect(headerCells).toEqual([
      {
        actions: '',
        displayName: 'Display name',
        email: 'Email',
        source: 'Source',
        status: 'Status',
        userPicture: '',
      },
    ]);
    expect(rowCells).toEqual([
      {
        actions: 'delete',
        displayName: 'admin',
        email: '',
        source: 'memory',
        status: 'memory',
        userPicture: '🦊',
      },
    ]);
  });

  function expectUsersListRequest(usersResponse: User[] = []) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=1&size=10`);
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(usersResponse));
  }
});
