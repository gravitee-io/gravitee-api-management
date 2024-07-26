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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HighchartsChartModule } from 'highcharts-angular';

import { HomeApiHealthCheckComponent } from './home-api-health-check.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { HomeModule } from '../home.module';
import { Api } from '../../../entities/api';
import { fakePagedResult } from '../../../entities/pagedResult';
import { fakeApi } from '../../../entities/api/Api.fixture';
import { GioQuickTimeRangeHarness } from '../components/gio-quick-time-range/gio-quick-time-range.harness';

describe('HomeApiHealthCheckComponent', () => {
  let fixture: ComponentFixture<HomeApiHealthCheckComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, HomeModule, MatIconTestingModule, HighchartsChartModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeApiHealthCheckComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  it('should display an empty table', async () => {
    fixture.detectChanges();

    // For "Health-check of all APIs" check
    await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);
    // For table
    await expectApisListRequest([]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        availability: 'API Availability',
      },
    ]);
    expect(rowCells).toEqual([['No APIs to display.']]);
  });

  it('should display a table with one row', async () => {
    fixture.detectChanges();
    const api = fakeApi({
      healthcheck_enabled: false,
    });
    // For "Health-check of all APIs" check
    await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);
    // For table
    await expectApisListRequest([api]);

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        availability: 'API Availability',
      },
    ]);
    expect(rowCells).toEqual([['', '🪐 Planets (1.0)', '', 'Health check has not been configured', '']]);
  });

  it('should display api with HeathCheck configured', async () => {
    fixture.detectChanges();
    const api = fakeApi({
      healthcheck_enabled: true,
    });
    // For "Health-check of all APIs" check
    await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);

    // For table
    await expectApisListRequest([api]);
    fixture.detectChanges();
    expectGetApiHealth(api.id);
    expectGetApiHealthAverage(api.id);
    fixture.detectChanges();

    const { headerCells, rowCells } = await computeApisTableCells();
    expect(headerCells).toEqual([
      {
        actions: '',
        name: 'Name',
        picture: '',
        states: '',
        availability: 'API Availability',
      },
    ]);
    expect(rowCells).toEqual([
      ['', '🪐 Planets (1.0)', '', '50%Created with Highcharts 9.2.213:21:3013:21:4013:21:5013:22:0013:22:1013:22:20', ''],
    ]);

    // Expect HealthCheck TimeFrame select changes
    const healthCheckTimeFrameSelect = await loader.getHarness(GioQuickTimeRangeHarness);
    await healthCheckTimeFrameSelect.selectTimeRangeByText('last week');
    expectGetApiHealthAverage(api.id);

    // For "Health-check of all APIs" check
    await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);

    const { rowCells: rowCells_2 } = await computeApisTableCells();
    expect(rowCells_2).toEqual([
      ['', '🪐 Planets (1.0)', '', '20%Created with Highcharts 9.2.213:21:3013:21:4013:21:5013:22:0013:22:1013:22:20', ''],
    ]);
  });

  describe('onRefreshClick', () => {
    const api = fakeApi({
      healthcheck_enabled: true,
    });
    beforeEach(async () => {
      fixture.detectChanges();
      // For "Health-check of all APIs" check
      await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);

      // For table
      await expectApisListRequest([api]);
      fixture.detectChanges();
      expectGetApiHealth(api.id);
      expectGetApiHealthAverage(api.id);
      fixture.detectChanges();
    });

    it('should fetch last Health data', async () => {
      await loader.getHarness(MatButtonHarness.with({ selector: '.time-frame__refresh-btn' })).then((button) => button.click());

      expectGetApiHealth(api.id);
      expectGetApiHealthAverage(api.id);
      // For "Health-check of all APIs" check
      await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);
    });
  });

  describe('onOnlyHCConfigured', () => {
    const api = fakeApi({
      healthcheck_enabled: true,
    });
    beforeEach(async () => {
      fixture.detectChanges();
      // For "Health-check of all APIs" check
      await expectApisListRequest([], 'has_health_check:true', undefined, 1, 100);

      // For table
      await expectApisListRequest([api]);
      fixture.detectChanges();
      expectGetApiHealth(api.id);
      expectGetApiHealthAverage(api.id);
      fixture.detectChanges();
    });

    it("should filter by 'has_health_check:true'", async () => {
      await loader.getHarness(MatButtonHarness.with({ text: 'Filter to APIs with Health Check enabled' })).then((button) => button.click());

      expectApisListRequest([api], 'has_health_check:true');
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

  async function expectApisListRequest(apis: Api[] = [], q?: string, order?: string, page = 1, size = 10) {
    await fixture.whenStable();

    const req = httpTestingController.expectOne(
      `${CONSTANTS_TESTING.env.baseURL}/apis/_search/_paged?page=${page}&size=${size}&q=${q ? q : '*'}${order ? `&order=${order}` : ''}`,
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

  function expectGetApiHealthAverage(apiId: string) {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/health/average`);
    });
    req.flush({
      timestamp: {
        from: 1679923284000,
        to: 1679923346000,
        interval: 2000,
      },
      values: [
        {
          buckets: [
            {
              name: 'by_available',
              data: [
                100.0, 0, 0, 100.0, 0, 100.0, 0, 0, 100.0, 0, 100.0, 0, 0, 100.0, 0, 100.0, 0, 0, 100.0, 0, 100.0, 0, 0, 100.0, 0, 100.0, 0,
                0, 100.0, 0, 0, 0,
              ],
            },
          ],
          field: 'available',
          name: 'by_available',
        },
      ],
    });
  }
});
