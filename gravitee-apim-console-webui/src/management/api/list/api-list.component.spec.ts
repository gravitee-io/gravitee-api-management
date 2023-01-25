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
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSortHeaderHarness } from '@angular/material/sort/testing';
import { By } from '@angular/platform-browser';

import { ApiListModule } from './api-list.module';
import { ApiListComponent } from './api-list.component';

import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../entities/user';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakePagedResult } from '../../../entities/pagedResult';
import { Api, ApiLifecycleState, ApiOrigin, ApiState } from '../../../entities/api';

describe('ApisListComponent', () => {
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiListComponent>;
  let apiListComponent: ApiListComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  const fakeConstants = CONSTANTS_TESTING;
  const currentUser = new DeprecatedUser();
  currentUser.userPermissions = ['environment-api-c'];

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('without quality score', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ApiListModule, MatIconTestingModule, GioUiRouterTestingModule, NoopAnimationsModule, GioHttpTestingModule],
        providers: [
          { provide: UIRouterState, useValue: fakeUiRouter },
          { provide: UIRouterStateParams, useValue: {} },
          { provide: CurrentUserService, useValue: { currentUser } },
          { provide: 'Constants', useValue: fakeConstants },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiListComponent);
      apiListComponent = await fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    it('should display an empty table', fakeAsync(async () => {
      await initComponent([]);

      const { headerCells, rowCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          contextPath: 'Context Path',
          definitionVersion: 'Mode',
          name: 'Name',
          owner: 'Owner',
          picture: '',
          states: '',
          tags: 'Tags',
          visibility: 'Visibility',
        },
      ]);
      expect(rowCells).toEqual([['There is no API (yet).']]);
    }));

    it('should display a table with one row', fakeAsync(async () => {
      const api = fakeApi();
      await initComponent([api]);

      const { headerCells, rowCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          contextPath: 'Context Path',
          definitionVersion: 'Mode',
          name: 'Name',
          owner: 'Owner',
          picture: '',
          states: '',
          tags: 'Tags',
          visibility: 'Visibility',
        },
      ]);
      expect(rowCells).toEqual([['', '🪐 Planets', '', '/planets', '', 'admin', 'Policy studio', 'public', 'edit']]);
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
    }));

    it('should display one row with kubernetes icon', fakeAsync(async () => {
      await initComponent([fakeApi({ definition_context: { origin: 'kubernetes' } })]);
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-origin' }))).toBeTruthy();
    }));

    it('should display one row without kubernetes icon', fakeAsync(async () => {
      await initComponent([fakeApi({ definition_context: { origin: 'management' } })]);
      expect(await loader.getAllHarnesses(MatIconHarness.with({ selector: '.states__api-origin' }))).toHaveLength(0);
    }));

    it('should order rows by name', fakeAsync(async () => {
      const planetsApi = fakeApi({ id: '1', name: 'Planets 🪐' });
      const unicornsApi = fakeApi({ id: '2', name: 'Unicorns 🦄' });
      const apis = [planetsApi, unicornsApi];
      await initComponent(apis);

      const nameSort = await loader.getHarness(MatSortHeaderHarness.with({ selector: '#name' })).then((sortHarness) => sortHarness.host());
      await nameSort.click();
      apis.map((api) => expectSyncedApi(api.id, true));
      expectApisListRequest(apis, null, 'name');

      fixture.detectChanges();
      await nameSort.click();
      apis.map((api) => expectSyncedApi(api.id, true));
      expectApisListRequest(apis, null, '-name');
    }));

    it('should display out of sync api icon', fakeAsync(async () => {
      const api = fakeApi();
      const apis = [api];
      await initComponent(apis);
      expect(await loader.getAllHarnesses(MatIconHarness.with({ selector: '.states__api-is-not-synced' }))).toHaveLength(0);

      const nameSort = await loader.getHarness(MatSortHeaderHarness.with({ selector: '#name' })).then((sortHarness) => sortHarness.host());
      await nameSort.click();

      expectSyncedApi(api.id, false);
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-is-not-synced' }))).toBeTruthy();
      expectApisListRequest(apis, null, 'name');
    }));

    describe('onAddApiClick', () => {
      beforeEach(fakeAsync(() => initComponent([fakeApi()])));
      it('should navigate to new apis page on click to add button', async () => {
        const routerSpy = jest.spyOn(fakeUiRouter, 'go');

        await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="add-api"]' })).then((button) => button.click());

        expect(routerSpy).toHaveBeenCalledWith('management.apis.new');
      });
    });

    describe('onEditApiClick', () => {
      beforeEach(fakeAsync(() => initComponent([fakeApi()])));
      it('should navigate to new apis page on click to add button', () => {
        const routerSpy = jest.spyOn(fakeUiRouter, 'go');
        const api = {
          id: 'api-id',
          name: 'api#1',
          contextPath: '/api-1',
          tags: null,
          owner: 'admin',
          ownerEmail: 'admin@gio.com',
          picture: null,
          state: 'CREATED' as ApiState,
          lifecycleState: 'PUBLISHED' as ApiLifecycleState,
          workflowState: 'REVIEW_OK',
          visibility: { label: 'PUBLIC', icon: 'public' },
          origin: 'management' as ApiOrigin,
          readonly: false,
          definitionVersion: { label: 'Policy studio', icon: '' },
        };

        apiListComponent.onEditActionClicked(api);

        expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.portal.general', { apiId: api.id });
      });
    });

    async function initComponent(apis: Api[]) {
      expectApisListRequest(apis);
      loader = TestbedHarnessEnvironment.loader(fixture);
      fixture.detectChanges();
    }
  });

  describe('with quality score', () => {
    beforeEach(async () => {
      const withQualityEnabled = CONSTANTS_TESTING;
      withQualityEnabled.env.settings.apiQualityMetrics.enabled = true;

      await TestBed.configureTestingModule({
        imports: [ApiListModule, MatIconTestingModule, GioUiRouterTestingModule, NoopAnimationsModule, GioHttpTestingModule],
        providers: [
          { provide: UIRouterState, useValue: fakeUiRouter },
          { provide: UIRouterStateParams, useValue: {} },
          { provide: CurrentUserService, useValue: { currentUser } },
          { provide: 'Constants', useValue: withQualityEnabled },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ApiListComponent);
      apiListComponent = await fixture.componentInstance;
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    it('should display quality columns', fakeAsync(async () => {
      await initComponent(fakeApi());
      const { headerCells, rowCells } = await computeApisTableCells();
      expect(headerCells).toEqual([
        {
          actions: '',
          contextPath: 'Context Path',
          definitionVersion: 'Mode',
          name: 'Name',
          owner: 'Owner',
          picture: '',
          qualityScore: 'Quality',
          states: '',
          tags: 'Tags',
          visibility: 'Visibility',
        },
      ]);
      expect(rowCells).toEqual([['', '🪐 Planets', '', '/planets', '', '100%', 'admin', 'Policy studio', 'public', 'edit']]);
      expect(fixture.debugElement.query(By.css('.quality-score__good'))).toBeTruthy();
      expect(await loader.getHarness(MatIconHarness.with({ selector: '.states__api-started' }))).toBeTruthy();
    }));

    it('should display bad quality score', fakeAsync(async () => {
      await initComponent(fakeApi(), 0);
      expect(fixture.debugElement.query(By.css('.quality-score__bad'))).toBeTruthy();
    }));

    it('should medium quality score', fakeAsync(async () => {
      await initComponent(fakeApi(), 0.51);
      expect(fixture.debugElement.query(By.css('.quality-score__medium'))).toBeTruthy();
    }));

    async function initComponent(api: Api, score = 1) {
      expectApisListRequest([api]);
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
      httpTestingController.verify();
    }
  });

  async function computeApisTableCells() {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#apisTable' }));

    const headerRows = await table.getHeaderRows();
    const headerCells = await parallel(() => headerRows.map((row) => row.getCellTextByColumnName()));

    const rows = await table.getRows();
    const rowCells = await parallel(() => rows.map((row) => row.getCellTextByIndex()));
    return { headerCells, rowCells };
  }

  function expectApisListRequest(apis: Api[] = [], q?: string, order?: string, page = 1) {
    // wait debounceTime
    fixture.detectChanges();
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/apis/_search/_paged?page=${page}&size=10&q=${q ? q : '*'}${order ? `&order=${order}` : ''}`,
    );
    expect(req.request.method).toEqual('POST');
    req.flush(fakePagedResult(apis));
    httpTestingController.verify();
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
