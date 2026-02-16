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
import { ActivatedRoute } from '@angular/router';

import { ApiPathMappingsComponent } from './api-path-mappings.component';
import { ApiPathMappingsModule } from './api-path-mappings.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { Page } from '../../../../entities/page';
import { ApiV2, fakeApiV2 } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ApiPathMappingsComponent', () => {
  const API_ID = 'apiId';

  let fixture: ComponentFixture<ApiPathMappingsComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiPathMappingsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-u', 'api-definition-r'],
        },
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

      const tableCells = await computeApisTableCells();
      expect(tableCells.headerCells).toEqual([
        {
          path: 'Path',
          actions: '',
        },
      ]);

      const table = await loader.getHarness(MatTableHarness);
      const tableHost = await table.host();
      expect(await tableHost.text()).toContain('No Path Mappings');
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

      const { rowCells } = await computeApisTableCells();
      expect(rowCells).toEqual([
        ['/test', ''],
        ['/test/:id', ''],
      ]);

      await loader
        .getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Button to delete a path mapping"]' }))
        .then(elements => elements[1].click());
      await rootLoader
        .getHarness(MatDialogHarness)
        .then(dialog => dialog.getHarness(MatButtonHarness.with({ text: /Delete/ })))
        .then(element => element.click());

      const updatedApi = { ...api, pathMappings: ['/test'] };
      expectApiGetRequest(api);
      expectApiPutRequest(updatedApi);
      expectApiGetRequest(updatedApi);
      expectApiPagesGetRequest(api, []);
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

      const { rowCells } = await computeApisTableCells();
      expect(rowCells).toEqual([
        ['/test', ''],
        ['/test/:id', ''],
      ]);

      await loader
        .getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Button to edit a path mapping"]' }))
        .then(elements => elements[1].click());
      const dialog = await rootLoader.getHarness(MatDialogHarness);
      await dialog
        .getHarness(MatInputHarness.with({ selector: '[aria-label="Path mapping input"]' }))
        .then(input => input.setValue('/updated/:id'));
      expect(
        await dialog
          .getHarness(MatButtonHarness.with({ selector: '[aria-label="Save path mapping"]' }))
          .then(element => element.isDisabled()),
      ).toEqual(false);
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
  }

  async function computeApisTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#pathMappingsTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map(row => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map(row => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }
});
