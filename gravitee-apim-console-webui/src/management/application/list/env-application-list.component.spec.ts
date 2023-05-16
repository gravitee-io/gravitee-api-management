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
import { UIRouterModule } from '@uirouter/angular';

import { EnvApplicationListComponent } from './env-application-list.component';

import { EnvironmentApplicationModule } from '../environment-application.module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakePagedResult } from '../../../entities/pagedResult';
import { User as DeprecatedUser } from '../../../entities/user';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { Application } from '../../../entities/application/application';
import { fakeApplication } from '../../../entities/application/Application.fixture';

describe('EnvApplicationListComponent', () => {
  let fixture: ComponentFixture<EnvApplicationListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  describe('with ACTIVE status', () => {
    beforeEach(() => {
      const currentUser = new DeprecatedUser();
      currentUser.roles = [{ scope: 'ORGANIZATION', name: 'ADMIN' }];

      TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          EnvironmentApplicationModule,
          GioHttpTestingModule,
          UIRouterModule.forRoot({
            useHash: true,
          }),
        ],
        providers: [
          { provide: UIRouterState, useValue: { go: jest.fn() } },
          { provide: UIRouterStateParams, useValue: {} },
          { provide: CurrentUserService, useValue: { currentUser } },
        ],
      }).overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      });
    });

    describe('without query params', () => {
      beforeEach(() => {
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      it('should display an empty table', fakeAsync(async () => {
        expectApplicationsListRequest();

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));

        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            type: 'Type',
            owner: 'Owner',
          },
        ]);
        expect(rowCells).toEqual([['There is no application (yet).']]);
      }));

      it('should display table with data', fakeAsync(async () => {
        expectApplicationsListRequest([fakeApplication()]);

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));
        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            type: 'Type',
            owner: 'Owner',
          },
        ]);
        expect(rowCells).toEqual([
          {
            actions: 'edit',
            name: 'Default application',
            applicationPicture: '',
            type: 'Simple',
            owner: '',
          },
        ]);
      }));

      it('should search applications', fakeAsync(async () => {
        expectApplicationsListRequest();
        const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

        await tableWrapper.setSearchValue('a');
        expectApplicationsListRequest([fakeApplication()], 'a');

        await tableWrapper.setSearchValue('ad');
        expectApplicationsListRequest([fakeApplication()], 'ad');

        await tableWrapper.setSearchValue('');
        expectApplicationsListRequest([], '');
      }));
    });

    describe('with query params', () => {
      beforeEach(() => {
        TestBed.overrideProvider(UIRouterStateParams, { useValue: { page: 2, q: 'Hello' } });
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });
      it('should init search with url params', fakeAsync(async () => {
        // Directly with page=2 and q=Hello
        expectApplicationsListRequest([fakeApplication()], 'Hello', 2);
      }));
    });
  });

  describe('with ARCHIVED status', () => {
    beforeEach(() => {
      const currentUser = new DeprecatedUser();
      currentUser.roles = [{ scope: 'ORGANIZATION', name: 'ADMIN' }];

      TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          EnvironmentApplicationModule,
          GioHttpTestingModule,
          UIRouterModule.forRoot({
            useHash: true,
          }),
        ],
        providers: [
          { provide: UIRouterState, useValue: { go: jest.fn() } },
          { provide: UIRouterStateParams, useValue: { status: 'ARCHIVED' } },
          { provide: CurrentUserService, useValue: { currentUser } },
        ],
      }).overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      });
    });

    describe('without query params', () => {
      beforeEach(() => {
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      it('should display an empty table', fakeAsync(async () => {
        expectApplicationsListRequest([], '', 1, 'ARCHIVED');

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));

        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            updated_at: 'Archived at',
          },
        ]);
        expect(rowCells).toEqual([['There is no archived application.']]);
      }));

      it('should display table with data', fakeAsync(async () => {
        expectApplicationsListRequest([fakeApplication()], '', 1, 'ARCHIVED');

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));
        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map((row) => row.getCellTextByColumnName()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            updated_at: 'Archived at',
          },
        ]);
        expect(rowCells).toEqual([
          {
            actions: 'edit',
            name: 'Default application',
            applicationPicture: '',
            updated_at: 'Nov 10, 2021, 9:26:15 AM',
          },
        ]);
      }));

      it('should search applications', fakeAsync(async () => {
        expectApplicationsListRequest([], '', 1, 'ARCHIVED');
        const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

        await tableWrapper.setSearchValue('a');
        expectApplicationsListRequest([fakeApplication()], 'a', 1, 'ARCHIVED');

        await tableWrapper.setSearchValue('ad');
        expectApplicationsListRequest([fakeApplication()], 'ad', 1, 'ARCHIVED');

        await tableWrapper.setSearchValue('');
        expectApplicationsListRequest([], '', 1, 'ARCHIVED');
      }));

      xit('should confirm and restore application', fakeAsync(async () => {
        const application = fakeApplication({ status: 'ARCHIVED' });

        expectApplicationsListRequest([application], '', 1, 'ARCHIVED');

        fixture.componentInstance.onRestoreActionClicked({
          applicationId: application.id,
          applicationPicture: application.picture_url,
          name: application.name,
          type: application.type,
          owner: application.owner,
          updated_at: application.updated_at,
          status: application.status,
          origin: application.origin,
        });

        const dialog = await rootLoader.getHarness(MatDialogHarness);

        await (await dialog.getHarness(MatButtonHarness.with({ text: /^Restore/ }))).click();

        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/applications/${application.id}/_restore`);
        expect(req.request.method).toEqual('POST');
        req.flush(null);

        expectApplicationsListRequest();
      }));
    });

    describe('with query params', () => {
      beforeEach(() => {
        TestBed.overrideProvider(UIRouterStateParams, { useValue: { page: 2, q: 'Hello', status: 'ARCHIVED' } });
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });
      it('should init search with url params', fakeAsync(async () => {
        // Directly with page=2 and q=Hello
        expectApplicationsListRequest([fakeApplication()], 'Hello', 2, 'ARCHIVED');
      }));
    });
  });

  function expectApplicationsListRequest(applicationsResponse: Application[] = [], q?: string, page = 1, status = 'ACTIVE') {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=${page}&size=10&status=${status}${q ? `&query=${q}` : ''}`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(applicationsResponse));
    httpTestingController.verify();
  }
});
