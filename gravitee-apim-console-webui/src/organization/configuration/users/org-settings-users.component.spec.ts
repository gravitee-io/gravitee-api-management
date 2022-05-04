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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { OrgSettingsUsersComponent } from './org-settings-users.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { UIRouterStateParams, UIRouterState, CurrentUserService } from '../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { User } from '../../../entities/user/user';
import { fakePagedResult } from '../../../entities/pagedResult';
import { fakeAdminUser } from '../../../entities/user/user.fixture';
import { User as DeprecatedUser } from '../../../entities/user';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

describe('OrgSettingsUsersComponent', () => {
  let fixture: ComponentFixture<OrgSettingsUsersComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OrganizationSettingsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: UIRouterStateParams, useValue: {} },
        { provide: CurrentUserService, useValue: { currentUser: new DeprecatedUser() } },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
      },
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
  describe('without query params', () => {
    beforeEach(() => {
      TestBed.compileComponents();

      httpTestingController = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(OrgSettingsUsersComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    it('should display an empty table', fakeAsync(async () => {
      expectUsersListRequest();

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#usersTable' }));

      const headerRows = await table.getHeaderRows();
      const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));

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
    }));

    it('should display table with data', fakeAsync(async () => {
      expectUsersListRequest([fakeAdminUser()]);

      const table = await loader.getHarness(MatTableHarness.with({ selector: '#usersTable' }));
      const headerRows = await table.getHeaderRows();
      const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

      const rows = await table.getRows();
      const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

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
          actions: '',
          displayName: 'adminPrimary Owner Active Token',
          email: '',
          source: 'memory',
          status: 'Active',
          userPicture: '',
        },
      ]);
    }));

    it('should confirm and delete user', fakeAsync(async () => {
      expectUsersListRequest([
        {
          id: 'c0716036-5ed0-46ef-b160-365ed026ef07',
          firstname: 'Joe',
          lastname: 'Bar',
          email: 'joe.bar@noop.com',
          source: 'gravitee',
          sourceId: 'joe.bar@noop.com',
          status: 'ACTIVE',
          loginCount: 0,
          displayName: 'Joe Bar',
          created_at: 1631603626469,
          updated_at: 1631603626469,
          primary_owner: false,
          number_of_active_tokens: 0,
        },
      ]);

      fixture.componentInstance.onDeleteUserClick({
        userId: '42',
        displayName: 'Joo',
        email: '',
        userPicture: '',
        status: '',
        source: '',
        number_of_active_tokens: 0,
        primary_owner: true,
        badgeCSSClass: '',
      });

      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await (await dialog.getHarness(MatButtonHarness.with({ text: /^Delete/ }))).click();

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users/42`);
      expect(req.request.method).toEqual('DELETE');
      req.flush(null);

      expectUsersListRequest();
    }));

    it('should search users', fakeAsync(async () => {
      expectUsersListRequest();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      await tableWrapper.setSearchValue('a');
      expectUsersListRequest([fakeAdminUser()], 'a');

      await tableWrapper.setSearchValue('ad');
      expectUsersListRequest([fakeAdminUser()], 'ad');

      await tableWrapper.setSearchValue('');
      expectUsersListRequest([], '');
    }));
  });

  describe('with query params', () => {
    beforeEach(() => {
      TestBed.overrideProvider(UIRouterStateParams, { useValue: { page: 2, q: 'Hello' } });
      TestBed.compileComponents();

      httpTestingController = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(OrgSettingsUsersComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    });

    it('should init search with url params', fakeAsync(async () => {
      // Directly with page=2 and q=Hello
      expectUsersListRequest([fakeAdminUser()], 'Hello', 2);
    }));
  });

  function expectUsersListRequest(usersResponse: User[] = [], q?: string, page = 1) {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/users?page=${page}&size=10${q ? `&q=${q}` : ''}`);
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(usersResponse));
  }
});
