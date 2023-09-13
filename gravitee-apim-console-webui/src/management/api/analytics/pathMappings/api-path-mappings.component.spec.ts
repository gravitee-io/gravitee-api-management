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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';

import { ApiPathMappingsComponent } from './api-path-mappings.component';
import { ApiPathMappingsModule } from './api-path-mappings.module';

import { AjsRootScope, CurrentUserService, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { User } from '../../../../entities/user';
import { Page } from '../../../../entities/page';
import { ApiV2, fakeApiV2 } from '../../../../entities/management-api-v2';

describe('ApiPathMappingsComponent', () => {
  const API_ID = 'apiId';

  const currentUser = new User();
  currentUser.userPermissions = ['api-definition-u', 'api-definition-r'];

  let fixture: ComponentFixture<ApiPathMappingsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiPathMappingsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: AjsRootScope, useValue: { $broadcast: jest.fn() } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiPathMappingsComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('table tests', () => {
    it('should display the api path mapping', async () => {
      const api = fakeApiV2({
        id: API_ID,
        pathMappings: ['/test', '/test/:id'],
      });

      expectApiGetRequest(api);
      expectApiPagesGetRequest(api, []);

      const { headerCells, rowCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          path: 'Path',
          actions: '',
        },
      ]);

      expect(rowCells).toEqual([
        ['/test', ''],
        ['/test/:id', ''],
      ]);
    });

    it('should display an empty table', async () => {
      const api = fakeApiV2({
        id: API_ID,
        pathMappings: [],
      });
      expectApiGetRequest(api);
      expectApiPagesGetRequest(api, []);

      const { headerCells, rowCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          path: 'Path',
          actions: '',
        },
      ]);

      expect(rowCells).toEqual([['No Path Mappings']]);
    });
  });

  describe('deletePathMapping', () => {
    it('should delete a path mapping', async () => {
      const api = fakeApiV2({
        id: API_ID,
        pathMappings: ['/test', '/test/:id'],
      });
      expectApiGetRequest(api);
      expectApiPagesGetRequest(api, []);

      let { rowCells } = await computeApisTableCells();
      expect(rowCells).toEqual([
        ['/test', ''],
        ['/test/:id', ''],
      ]);

      await loader
        .getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Button to delete a path mapping"]' }))
        .then((elements) => elements[1].click());
      await rootLoader
        .getHarness(MatDialogHarness)
        .then((dialog) => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
        .then((element) => element.click());

      const updatedApi = { ...api, pathMappings: ['/test'] };
      expectApiGetRequest(api);
      expectApiPutRequest(updatedApi);
      expectApiGetRequest(updatedApi);
      expectApiPagesGetRequest(api, []);

      ({ rowCells } = await computeApisTableCells());
      expect(rowCells).toEqual([['/test', '']]);
    });
  });

  describe('edit path mapping', () => {
    it('should open edit path dialog', async () => {
      const api = fakeApiV2({
        id: API_ID,
        pathMappings: ['/test', '/test/:id'],
      });
      expectApiGetRequest(api);
      expectApiPagesGetRequest(api, []);

      let { rowCells } = await computeApisTableCells();
      expect(rowCells).toEqual([
        ['/test', ''],
        ['/test/:id', ''],
      ]);

      await loader
        .getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Button to edit a path mapping"]' }))
        .then((elements) => elements[1].click());
      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then((input) => input.setValue('/updated/:id'));
      await dialog.getHarness(MatButtonHarness.with({ selector: '[aria-label="Save path mapping"]' })).then((element) => element.click());

      expectApiGetRequest(api);
      const updatedApi = { ...api, path_mappings: ['/test', '/updated/:id'] };
      expectApiPutRequest(updatedApi);

      const snackBars = await rootLoader.getAllHarnesses(MatSnackBarHarness);
      expect(snackBars.length).toBe(1);

      expectApiGetRequest(updatedApi);
      expectApiPagesGetRequest(api, []);
      ({ rowCells } = await computeApisTableCells());
      expect(rowCells).toEqual([
        ['/test', ''],
        ['/updated/:id', ''],
      ]);
    });
  });

  function expectApiGetRequest(api: ApiV2) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'GET' }).flush(api);
  }

  function expectApiPagesGetRequest(api: ApiV2, pages: Page[]) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/pages?type=SWAGGER&api=${api.id}`, method: 'GET' })
      .flush(pages);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV2) {
    const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}` });
    expect(req.request.body).toBeTruthy();
    expect(req.request.body.pathMappings).toStrictEqual(api.pathMappings);
    req.flush(api);
    fixture.detectChanges();
  }

  async function computeApisTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#pathMappingsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }
});
