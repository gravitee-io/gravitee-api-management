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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatSortHeaderHarness } from '@angular/material/sort/testing';

import { ApiListModule } from './api-list.module';
import { ApiListComponent } from './api-list.component';

import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';
import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../entities/user';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { fakePagedResult } from '../../../entities/pagedResult';
import { Api, ApiLifecycleState, ApiState } from '../../../entities/api';

describe('ApisListComponent', () => {
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiListComponent>;
  let apiListComponent: ApiListComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    const currentUser = new DeprecatedUser();
    currentUser.userPermissions = ['environment-api-c'];

    await TestBed.configureTestingModule({
      imports: [ApiListModule, MatIconTestingModule, GioUiRouterTestingModule, NoopAnimationsModule, GioHttpTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: {} },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiListComponent);
    apiListComponent = await fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display an empty table', fakeAsync(async () => {
    await initComponent([]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        contextPath: 'Context paths',
        name: 'Name',
        owner: 'Owner',
        picture: '',
        states: '',
        tags: 'Tags',
      },
    ]);
    expect(rowCells).toEqual([['There is no apis (yet).']]);
  }));

  it('should display a table with one row', fakeAsync(async () => {
    await initComponent([fakeApi()]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        contextPath: 'Context paths',
        name: 'Name',
        owner: 'Owner',
        picture: '',
        states: '',
        tags: 'Tags',
      },
    ]);
    expect(rowCells).toEqual([['', '🪐 Planets', 'play_circlecloud_done', '/planets', '', 'admin']]);
  }));

  it('should order rows by name', fakeAsync(async () => {
    const apis = [fakeApi({ name: 'Planets 🪐' }), fakeApi({ name: 'Unicorns 🦄' })];
    await initComponent(apis);

    const nameSort = await loader.getHarness(MatSortHeaderHarness.with({ selector: '#name' })).then((sortHarness) => sortHarness.host());
    await nameSort.click();
    expectApisListRequest(apis, null, 'name');

    fixture.detectChanges();
    await nameSort.click();
    expectApisListRequest(apis, null, '-name');
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
      };

      apiListComponent.onEditActionClicked(api);

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.portal.general', { apiId: api.id });
    });
  });

  async function initComponent(apis: Api[]) {
    expectApisListRequest(apis);
    loader = TestbedHarnessEnvironment.loader(fixture);
  }

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
      `${CONSTANTS_TESTING.env.baseURL}/apis/_paged?page=${page}&size=10${q ? `&query=${q}` : ''}${order ? `&order=${order}` : ''}`,
    );
    expect(req.request.method).toEqual('GET');
    req.flush(fakePagedResult(apis));
    httpTestingController.verify();
  }
});
