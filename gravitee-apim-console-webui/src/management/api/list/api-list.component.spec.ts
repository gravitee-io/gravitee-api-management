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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconHarness, MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSortHeaderHarness } from '@angular/material/sort/testing';
import { By } from '@angular/platform-browser';

import { ApiListModule } from './api-list.module';
import { ApiListComponent } from './api-list.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';
import { Api, ApiV4, fakeApiV2, fakeApiV4, fakePagedResult, fakeProxyApiV4 } from '../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { fakeKafkaListener } from '../../../entities/management-api-v2/api/v4/listener.fixture';

describe('ApisListComponent', () => {
  let fixture: ComponentFixture<ApiListComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeConstants = CONSTANTS_TESTING;

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('with HTTP proxy API', () => {
    describe('without quality score', () => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          imports: [ApiListModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
          providers: [
            { provide: GioTestingPermissionProvider, useValue: ['environment-api-c'] },
            { provide: Constants, useValue: fakeConstants },
          ],
        }).compileComponents();

        fixture = TestBed.createComponent(ApiListComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
      });

      it('should display an empty table with cells headers', fakeAsync(async () => {
        await initComponent([]);

        const { headerCells, rowCells } = await computeApisTableCells();
        expect(headerCells).toEqual([
          {
            actions: '',
            access: 'Access',
            categories: 'Categories',
            definitionVersion: 'Definition',
            name: 'Name',
            owner: 'Owner',
            picture: '',
            states: 'Status',
            tags: 'Tags',
            visibility: 'Visibility',
          },
        ]);
        expect(rowCells).toEqual([['There is no API (yet).']]);
      }));

      it('should display a table with one row', fakeAsync(async () => {
        const api = fakeApiV2();
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([['', '🪐 Planets (1.0)', 'V2 Gravitee', '', '/planets', '', '', 'admin', 'public', 'edit']]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v4 api', fakeAsync(async () => {
        const api = fakeApiV4();
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([
          ['', '🪐 Planets (1.0)', 'V4 - Message Gravitee', '', 'No access with this configuration', '', '', 'admin', 'public', 'edit'],
        ]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v2 api', fakeAsync(async () => {
        const api = fakeApiV2();
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([['', '🪐 Planets (1.0)', 'V2 Gravitee', '', '/planets', '', '', 'admin', 'public', 'edit']]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v4 api with multiple context path', fakeAsync(async () => {
        const api = fakeApiV4({
          listeners: [
            {
              type: 'HTTP',
              paths: [
                {
                  path: '/test/ws',
                },
                {
                  path: '/preprod/ws',
                },
                {
                  path: '/prod/ws',
                },
              ],
            },
          ],
        });
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([
          ['', '🪐 Planets (1.0)', 'V4 - Message Gravitee', '', '/test/ws 2 more', '', '', 'admin', 'public', 'edit'],
        ]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v2 api with multiple context path', fakeAsync(async () => {
        const api = fakeApiV2({
          proxy: {
            virtualHosts: [
              {
                path: '/test/ws',
              },
              {
                path: '/preprod/ws',
              },
              {
                path: '/prod/ws',
              },
            ],
          },
        });
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([['', '🪐 Planets (1.0)', 'V2 Gravitee', '', '/test/ws 2 more', '', '', 'admin', 'public', 'edit']]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v4 api with virtual host', fakeAsync(async () => {
        const api = fakeApiV4({
          listeners: [
            {
              type: 'HTTP',
              paths: [
                {
                  path: '/test/ws',
                  host: 'test.domain.com',
                },
                {
                  path: '/preprod/ws',
                  host: 'pp.domain.com',
                },
                {
                  path: '/prod/ws',
                  host: 'domain.com',
                },
              ],
            },
          ],
        });
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([
          ['', '🪐 Planets (1.0)', 'V4 - Message Gravitee', '', 'test.domain.com/test/ws 2 more', '', '', 'admin', 'public', 'edit'],
        ]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display v2 api with virtual host', fakeAsync(async () => {
        const api = fakeApiV2({
          proxy: {
            virtualHosts: [
              {
                path: '/test/ws',
                host: 'test.domain.com',
              },
              {
                path: '/preprod/ws',
                host: 'pp.domain.com',
              },
              {
                path: '/prod/ws',
                host: 'domain.com',
              },
            ],
          },
        });
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([
          ['', '🪐 Planets (1.0)', 'V2 Gravitee', '', 'test.domain.com/test/ws 2 more', '', '', 'admin', 'public', 'edit'],
        ]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should allow new search on request throw', fakeAsync(async () => {
        await initComponent([]);

        await loader.getHarness(GioTableWrapperHarness).then((tableWrapper) => tableWrapper.setSearchValue('bad-search'));
        await tick(400);
        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=1&perPage=25`);
        expect(req.request.body).toEqual({ query: 'bad-search' });

        req.flush('Internal error', { status: 500, statusText: 'Internal error' });

        await loader.getHarness(GioTableWrapperHarness).then((tableWrapper) => tableWrapper.setSearchValue('good-search'));

        expectApisListRequest([], null, 'good-search');
      }));

      it('should display one row with kubernetes icon', fakeAsync(async () => {
        await initComponent([fakeApiV2({ originContext: { origin: 'KUBERNETES' } })]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-origin' }))).toBeTruthy();
      }));

      it('should display one row without kubernetes icon', fakeAsync(async () => {
        await initComponent([fakeApiV2({ originContext: { origin: 'MANAGEMENT' } })]);
        expect(await loader.getAllHarnesses(MatIconHarness.with({ selector: '.states__api-origin' }))).toHaveLength(0);
      }));

      it('should order rows by name', fakeAsync(async () => {
        const planetsApi = fakeApiV2({ id: '1', name: 'Planets 🪐' });
        const unicornsApi = fakeApiV2({ id: '2', name: 'Unicorns 🦄' });
        const apis = [planetsApi, unicornsApi];
        await initComponent(apis);

        const nameSort = await loader
          .getHarness(MatSortHeaderHarness.with({ selector: '#name' }))
          .then((sortHarness) => sortHarness.host());
        await nameSort.click();
        apis.map((api) => expectSyncedApi(api.id, true));
        expectApisListRequest(apis, 'name');

        fixture.detectChanges();
        await nameSort.click();
        apis.map((api) => expectSyncedApi(api.id, true));
        expectApisListRequest(apis, '-name');
      }));

      it('should order rows by access', fakeAsync(async () => {
        const planetsApi = fakeApiV2({ id: '1', name: 'Planets 🪐', contextPath: '/planets' });
        const unicornsApi = fakeApiV2({ id: '2', name: 'Unicorns 🦄', contextPath: '/unicorns' });
        const apis = [planetsApi, unicornsApi];
        await initComponent(apis);

        const accessSort = await loader
          .getHarness(MatSortHeaderHarness.with({ selector: '#access' }))
          .then((sortHarness) => sortHarness.host());
        await accessSort.click();
        apis.map((api) => expectSyncedApi(api.id, true));
        expectApisListRequest(apis, 'paths');

        fixture.detectChanges();
        await accessSort.click();
        apis.map((api) => expectSyncedApi(api.id, true));
        expectApisListRequest(apis, '-paths');
      }));

      it('should display out of sync api icon', fakeAsync(async () => {
        const api = fakeApiV2();
        const apis = [api];
        await initComponent(apis);
        expect(await loader.getAllHarnesses(MatIconHarness.with({ selector: '.states__api-is-not-synced' }))).toHaveLength(0);
        expectSyncedApi(api.id, false);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-is-not-synced' }))).toBeTruthy();
      }));

      async function initComponent(apis: Api[]) {
        // APIs are sorted by name by default
        expectApisListRequest(apis, 'name');
        loader = TestbedHarnessEnvironment.loader(fixture);
        fixture.detectChanges();
      }
    });

    describe('with quality score', () => {
      beforeEach(() => {
        const withQualityEnabled = CONSTANTS_TESTING;
        withQualityEnabled.env.settings.apiQualityMetrics.enabled = true;

        TestBed.configureTestingModule({
          imports: [ApiListModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
          providers: [
            { provide: GioTestingPermissionProvider, useValue: ['environment-api-c'] },
            { provide: Constants, useValue: withQualityEnabled },
          ],
        }).compileComponents();

        fixture = TestBed.createComponent(ApiListComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
      });

      it('should display quality columns', fakeAsync(async () => {
        await initComponent(fakeApiV2());
        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([['', '🪐 Planets (1.0)', 'V2 Gravitee', '', '/planets', '100%', '', '', 'admin', 'public', 'edit']]);
        expect(fixture.debugElement.query(By.css('.quality-score__good'))).toBeTruthy();
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      it('should display bad quality score', fakeAsync(async () => {
        await initComponent(fakeApiV2(), 0);
        expect(fixture.debugElement.query(By.css('.quality-score__bad'))).toBeTruthy();
      }));

      it('should medium quality score', fakeAsync(async () => {
        await initComponent(fakeApiV2(), 0.51);
        expect(fixture.debugElement.query(By.css('.quality-score__medium'))).toBeTruthy();
      }));

      async function initComponent(api: Api, score = 1) {
        expectApisListRequest([api], 'name');
        loader = TestbedHarnessEnvironment.loader(fixture);
        expectSyncedApi(api.id, true);
        expectQualityRequest(api.id, score);
        fixture.detectChanges();
      }

      function expectQualityRequest(apiId: string, score: number) {
        // wait debounceTime
        fixture.detectChanges();
        tick();

        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/quality`);
        expect(req.request.method).toEqual('GET');
        req.flush({ score });

        const reqCat = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/configuration/categories`);
        expect(reqCat.request.method).toEqual('GET');
        reqCat.flush([]);
        httpTestingController.verify();
      }
    });
  });

  describe('with TCP proxy API', () => {
    const api = fakeProxyApiV4({
      listeners: [
        {
          type: 'TCP',
          hosts: ['foo.example.com', 'bar.example.com'],
          entrypoints: [
            {
              type: 'tcp-proxy',
            },
          ],
        },
      ],
    });
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [ApiListModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
        providers: [
          { provide: GioTestingPermissionProvider, useValue: ['environment-api-c'] },
          { provide: Constants, useValue: fakeConstants },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiListComponent);
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    it('should display a table with one row', fakeAsync(async () => {
      await initComponent([api]);

      const { rowCells, headerCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          access: 'Access',
          definitionVersion: 'Definition',
          name: 'Name',
          owner: 'Owner',
          picture: '',
          qualityScore: 'Quality',
          states: 'Status',
          tags: 'Tags',
          categories: 'Categories',
          visibility: 'Visibility',
        },
      ]);
      expect(rowCells).toEqual([
        ['', '🪐 Planets (1.0)', 'V4 - TCP Proxy Gravitee', '', 'foo.example.com 1 more', '', '', '', 'admin', 'public', 'edit'],
      ]);
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
    }));

    async function initComponent(apis: ApiV4[]) {
      // APIs are sorted by name by default
      expectApisListRequest(apis, 'name');
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    }
  });

  describe('with Native Kafka API', () => {
    describe('without quality score', () => {
      beforeEach(() => {
        TestBed.configureTestingModule({
          imports: [ApiListModule, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
          providers: [
            { provide: GioTestingPermissionProvider, useValue: ['environment-api-c'] },
            { provide: Constants, useValue: fakeConstants },
          ],
        }).compileComponents();

        fixture = TestBed.createComponent(ApiListComponent);
        httpTestingController = TestBed.inject(HttpTestingController);
      });

      it('should display a table with one row', fakeAsync(async () => {
        const api = fakeApiV4({ type: 'NATIVE', listeners: [fakeKafkaListener()] });
        await initComponent([api]);

        const { rowCells } = await computeApisTableCells();
        expect(rowCells).toEqual([
          ['', '🪐 Planets (1.0)', 'V4 - Native Kafka Gravitee', '', 'kafka-host:1000', '', '', '', 'admin', 'public', 'edit'],
        ]);
        expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
      }));

      async function initComponent(apis: Api[]) {
        // APIs are sorted by name by default
        expectApisListRequest(apis, 'name');
        loader = TestbedHarnessEnvironment.loader(fixture);
        fixture.detectChanges();
      }
    });
  });

  async function computeApisTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apisTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApisListRequest(apis: Api[] = [], sortBy?: string, query?: string, page = 1) {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.v2BaseURL}/apis/_search?page=${page}&perPage=25${sortBy ? `&sortBy=${sortBy}` : ''}`,
    );
    expect(req.request.method).toEqual('POST');

    if (query) {
      expect(req.request.body).toEqual({ query });
    }

    req.flush(fakePagedResult(apis));
  }

  function expectSyncedApi(apiId: string, isSynced: boolean) {
    // wait debounceTime
    fixture.detectChanges();
    tick();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/state`);
    expect(req.request.method).toEqual('GET');
    req.flush({ api_id: apiId, is_synchronized: isSynced });
  }
});
