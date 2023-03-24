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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { HomeApiStatusComponent } from './home-api-status.component';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { User } from '../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { HomeModule } from '../home.module';
import { Api } from '../../../entities/api';
import { fakePagedResult } from '../../../entities/pagedResult';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';

describe('HomeApiStatusComponent', () => {
  let fixture: ComponentFixture<HomeApiStatusComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const currentUser = new User();
  const fakeUiRouter = { go: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioUiRouterTestingModule, GioHttpTestingModule, HomeModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
        { provide: UIRouterStateParams, useValue: {} },
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeApiStatusComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should display an empty table', fakeAsync(async () => {
    fixture.detectChanges();
    expectApisListRequest([]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        status: 'API Status',
      },
    ]);
    expect(rowCells).toEqual([['No APIs to display.']]);
  }));

  it('should display a table with one row', fakeAsync(async () => {
    fixture.detectChanges();
    const api = fakeApi({
      healthcheck_enabled: false,
    });
    expectApisListRequest([api]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        status: 'API Status',
      },
    ]);
    expect(rowCells).toEqual([['', '🪐 Planets (1.0)', '', 'Health-check has not been configured', '']]);
  }));

  it('should display api with HeathCheck configured', fakeAsync(async () => {
    fixture.detectChanges();
    const api = fakeApi({
      healthcheck_enabled: true,
    });
    expectApisListRequest([api]);
    fixture.detectChanges();
    expectGetApiHealth(api.id);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        status: 'API Status',
      },
    ]);
    expect(rowCells).toEqual([['', '🪐 Planets (1.0)', '', '50%', '']]);

    // Expect HealthCheck TimeFrame select changes
    const healthCheckTimeFrameSelect = await loader.getHarness(MatSelectHarness);
    await healthCheckTimeFrameSelect.clickOptions({ text: 'last week' });

    const { rowCells: rowCells_2 } = await computeApisTableCells();
    expect(rowCells_2).toEqual([['', '🪐 Planets (1.0)', '', '20%', '']]);
  }));

  describe('onAddApiClick', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      expectApisListRequest([fakeApi()]);
    }));

    it('should navigate to view API HealthCheck', async () => {
      await loader
        .getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to view API HealthCheck"]' }))
        .then((button) => button.click());

      expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.detail.proxy.healthCheckDashboard');
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

  function expectApisListRequest(apis: Api[] = [], q?: string, order?: string, page = 1) {
    // wait debounceTime
    tick(400);

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/apis/_search/_paged?page=${page}&size=10&q=${q ? q : '*'}${order ? `&order=${order}` : ''}`,
    );
    expect(req.request.method).toEqual('POST');
    req.flush(fakePagedResult(apis));
  }

  function expectGetApiHealth(apiId: string) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/health?type=availability`,
      method: 'GET',
    });
    req.flush({
      global: {
        '1d': 10,
        '1w': 20,
        '1h': 30,
        '1M': 40,
        '1m': 50,
      },
      buckets: {
        default: {
          '1d': 10,
          '1w': 20,
          '1h': 30,
          '1M': 40,
          '1m': 50,
        },
      },
      metadata: {
        default: {
          target: 'https://apim-master-gateway.team-apim.gravitee.dev',
        },
      },
    });
  }
});
