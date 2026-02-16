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
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { ActivatedRoute } from '@angular/router';

import { EnvApplicationListComponent } from './env-application-list.component';

import { ApplicationsModule } from '../applications.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakePagedResult } from '../../../entities/pagedResult';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { Application } from '../../../entities/application/Application';
import { fakeApplication } from '../../../entities/application/Application.fixture';
import { GioTestingRoleProvider } from '../../../shared/components/gio-role/gio-role.service';

describe('EnvApplicationListComponent', () => {
  let fixture: ComponentFixture<EnvApplicationListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  describe('with ACTIVE status', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, ApplicationsModule, GioTestingModule],
        providers: [{ provide: GioTestingRoleProvider, useValue: [{ scope: 'ORGANIZATION', name: 'ADMIN' }] }],
      }).overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      });
    });

    describe('without query params', () => {
      beforeEach(() => {
        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      it('should display an empty table', fakeAsync(async () => {
        expectActiveApplicationsListRequest();

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));

        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            type: 'Type',
            owner: 'Owner',
          },
        ]);
        expect(rowCells).toHaveLength(0);

        const tableElement = await table.host();
        expect(await tableElement.text()).toContain('There is no application (yet).');
      }));

      it('should display table with data', fakeAsync(async () => {
        expectActiveApplicationsListRequest([fakeApplication()]);

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));
        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

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
        expectActiveApplicationsListRequest();
        const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

        await tableWrapper.setSearchValue('a');
        expectActiveApplicationsListRequest([fakeApplication()], 'a');

        await tableWrapper.setSearchValue('ad');
        expectActiveApplicationsListRequest([fakeApplication()], 'ad');

        await tableWrapper.setSearchValue('');
        expectActiveApplicationsListRequest([], '');
      }));
    });

    describe('with query params', () => {
      beforeEach(() => {
        TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParams: { page: 2, q: 'Hello' } } } });
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });
      it('should init search with url params', fakeAsync(async () => {
        // Directly with page=2 and q=Hello
        expectActiveApplicationsListRequest([fakeApplication()], 'Hello', 2);
      }));
    });
  });

  describe('with ARCHIVED status', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, ApplicationsModule, GioTestingModule],
        providers: [{ provide: GioTestingRoleProvider, useValue: [{ scope: 'ORGANIZATION', name: 'ADMIN' }] }],
      }).overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This checks focus trap, set it to true to  avoid the warning
        },
      });
    });

    describe('without query params', () => {
      beforeEach(() => {
        TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParams: { page: 1, q: '', status: 'ARCHIVED' } } } });
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });

      it('should display an empty table', fakeAsync(async () => {
        expectArchivedApplicationsListRequest([], '', 1, 'ARCHIVED');

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));

        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));

        expect(headerCells).toEqual([
          {
            actions: '',
            name: 'Name',
            applicationPicture: '',
            updated_at: 'Archived at',
          },
        ]);
        expect(rowCells).toHaveLength(0);

        const tableElement = await table.host();
        expect(await tableElement.text()).toContain('There is no archived application.');
      }));

      it('should display table with data', fakeAsync(async () => {
        expectArchivedApplicationsListRequest([fakeApplication()], '', 1, 'ARCHIVED');

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#applicationsTable' }));
        const headerRows = await table.getHeaderRows();
        const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

        const rows = await table.getRows();
        const rowCells = await parallel(() => rows.map(row => row.getCellTextByColumnName()));

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
        expectArchivedApplicationsListRequest([], '', 1, 'ARCHIVED');
        const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

        await tableWrapper.setSearchValue('a');
        expectArchivedApplicationsListRequest([fakeApplication()], 'a', 1, 'ARCHIVED');

        await tableWrapper.setSearchValue('ad');
        expectArchivedApplicationsListRequest([fakeApplication()], 'ad', 1, 'ARCHIVED');

        await tableWrapper.setSearchValue('');
        expectArchivedApplicationsListRequest([], '', 1, 'ARCHIVED');
      }));

      it('should confirm and restore application', fakeAsync(async () => {
        const application = fakeApplication({ status: 'ARCHIVED' });

        expectArchivedApplicationsListRequest([application], '', 1, 'ARCHIVED');

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

        expectArchivedApplicationsListRequest();
      }));
    });

    describe('with query params', () => {
      beforeEach(() => {
        TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { queryParams: { page: 2, q: 'Hello', status: 'ARCHIVED' } } } });
        TestBed.compileComponents();

        httpTestingController = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(EnvApplicationListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      });
      it('should init search with url params', fakeAsync(async () => {
        // Directly with page=2 and q=Hello
        expectArchivedApplicationsListRequest([fakeApplication()], 'Hello', 2, 'ARCHIVED');
      }));
    });
  });

  function expectActiveApplicationsListRequest(applicationsResponse: Application[] = [], q?: string, page = 1, status = 'ACTIVE') {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=${page}&size=25&status=${status}${q ? `&query=${q}` : ''}`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(applicationsResponse));
    httpTestingController.verify();
  }

  function expectArchivedApplicationsListRequest(applicationsResponse: Application[] = [], q?: string, page = 1, status = 'ARCHIVED') {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/applications/_paged?page=${page}&size=25&status=${status}${q ? `&query=${q}` : ''}`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(applicationsResponse));
    httpTestingController.verify();
    flush(); // Ensure all pending async tasks/timers complete
  }
});
