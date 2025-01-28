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
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';

import { HomeOverviewComponent } from './home-overview.component';
import { HomeOverviewHarness } from './home-overview.harness';

import { HomeModule } from '../home.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';

describe('HomeOverviewComponent', () => {
  let fixture: ComponentFixture<HomeOverviewComponent>;
  let componentHarness: HomeOverviewHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [HomeModule, OwlNativeDateTimeModule, NoopAnimationsModule, MatIconTestingModule, GioTestingModule],
      providers: [{ provide: GioTestingPermissionProvider, useValue: ['environment-platform-r'] }],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeOverviewComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, HomeOverviewHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('with permissions', () => {
    beforeEach(async () => {
      await init();
    });

    it('should load request stats when changing date range', async () => {
      expectRequests();

      const timeRangeHarness = await componentHarness.getDashboardFiltersBarHarness();
      const input = await timeRangeHarness.matSelectLocator();
      await input.clickOptions({ text: 'Last hour' });
      let req = expectApiLifecycleStateRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectApiStateRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectResponseStatusRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectRequestStatsRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectTopApiRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectCountApiRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectCountApplicationRequest();
      expect(req.request.url).toContain('interval=120000');

      expectSearchApiEventsRequest();
      expectConsoleSettingsGetRequest();
      expectTopApisGetRequest();
      expectGetRequestStatsForV4();

      expectResponseStatusGetRequest();
      expectResponseTimesLogsGetRequest();
    });

    it('should select custom date range', async () => {
      expectRequests();

      const dashboardFiltersBarHarness = await componentHarness.getDashboardFiltersBarHarness();
      const matSelect = await dashboardFiltersBarHarness.matSelectLocator();
      await matSelect.clickOptions({ text: 'Custom' });

      const applyButtonHarness = await dashboardFiltersBarHarness.applyButtonLocator();
      expect(await applyButtonHarness.isDisabled()).toEqual(true);

      const fromDate = '2023-10-09 15:21:00';
      const toDate = '2023-10-24 15:21:00';
      const fromDateInMilliSeconds = new Date(fromDate).getTime();
      const toDateInMilliseconds = new Date(toDate).getTime();

      await dashboardFiltersBarHarness.setFromDate(fromDate);
      const from = await dashboardFiltersBarHarness.fromInputLocator();
      expect(await from.getValue()).toEqual(fromDate);

      await dashboardFiltersBarHarness.setToDate(toDate);
      const to = await dashboardFiltersBarHarness.toInputLocator();
      expect(await to.getValue()).toEqual(toDate);

      expect(await applyButtonHarness.isDisabled()).toEqual(false);

      await dashboardFiltersBarHarness.applyClick();

      const req = expectApiLifecycleStateRequest();
      expect(req.request.url).toContain(`from=${fromDateInMilliSeconds}&to=${toDateInMilliseconds}`);

      expectResponseStatusRequest();
      expectApiStateRequest();
      expectRequestStatsRequest();
      expectTopApiRequest();
      expectCountApiRequest();
      expectCountApplicationRequest();
      expectSearchApiEventsRequest();
      expectConsoleSettingsGetRequest();
      expectTopApisGetRequest();
      expectGetRequestStatsForV4();
      expectResponseStatusGetRequest();
      expectResponseTimesLogsGetRequest();
    });

    it('should show request stats', async () => {
      expectRequests();
      const stats = await componentHarness.getGioRequestStatsHarness();
      expect(await stats.getAverage()).toEqual('8.43 ms');
    });

    it('should show v4 APIs request stats', async () => {
      expectRequests();

      const stats = await componentHarness.getDashboardV4ApiRequestStatsHarness();
      expect(await stats.getRequestsPerSecond()).toEqual('< 0.1 ');
      expect(await stats.getTotalRequests()).toEqual('17 ');
      expect(await stats.getMinResponseTime()).toEqual('25.12 ms ');
      expect(await stats.getMaxResponseTime()).toEqual('20,123.13 ms ');
      expect(await stats.getAverageResponseTime()).toEqual('234.76 ms ');
    });

    // TODO in APIM-7863: uncomment test.
    // it('should show requests that do not match a context path', async () => {
    //   expectRequests();
    //   const v2ApiCallsWithNoContextPathHarness = await componentHarness.getV2ApiCallsWithNoContextPathHarness();
    //   const tableRowsNumber = await v2ApiCallsWithNoContextPathHarness.rowsNumber();
    //   expect(tableRowsNumber).toEqual(1);
    // });
  });

  function expectRequests() {
    expectApiLifecycleStateRequest();
    expectApiStateRequest();
    expectResponseStatusRequest();
    expectRequestStatsRequest();
    expectTopApiRequest();
    expectCountApiRequest();
    expectCountApplicationRequest();
    expectSearchApiEventsRequest();
    expectConsoleSettingsGetRequest();
    expectTopApisGetRequest();
    expectGetRequestStatsForV4();
    expectResponseStatusGetRequest();
    expectResponseTimesLogsGetRequest();
  }

  function expectRequestStatsRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=stats&field=response-time`);
    });
    req.flush({
      min: 0.02336,
      max: 23009.29032,
      avg: 8.4323,
      rps: 1.2012334,
      rpm: 72.074004,
      rph: 4324.44024,
      count: 332981092,
      sum: 4567115654.2,
    });
    return req;
  }
  function expectApiLifecycleStateRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=lifecycle_state`);
    });
    req.flush({
      values: {
        CREATED: 0,
      },
    });
    return req;
  }
  function expectResponseStatusRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=status`);
    });
    req.flush({
      values: {
        '100.0-200.0': 0,
      },
      metadata: {},
    });
    return req;
  }
  function expectApiStateRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=state`);
    });
    req.flush({
      values: {},
    });
    return req;
  }
  function expectTopApiRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=api`);
    });
    req.flush({
      values: {},
    });
    return req;
  }
  function expectCountApiRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=count&field=api`);
    });
    req.flush({
      count: 0,
    });
    return req;
  }
  function expectCountApplicationRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=count&field=application`);
    });
    req.flush({
      count: 0,
    });
    return req;
  }
  function expectSearchApiEventsRequest(): TestRequest {
    const req = httpTestingController.expectOne((req) => {
      return (
        req.method === 'GET' &&
        req.url.startsWith(
          `${CONSTANTS_TESTING.env.baseURL}/platform/events?type=START_API,STOP_API,PUBLISH_API,UNPUBLISH_API&query=&api_ids=`,
        )
      );
    });
    req.flush({
      content: [],
    });
    return req;
  }

  // TODO in APIM-7863.
  // function expectLogsGetRequest() {
  //   const url = `${CONSTANTS_TESTING.env.baseURL}/platform/logs?`;
  //   const req = httpTestingController.expectOne((req) => {
  //     return req.method === 'GET' && req.url.startsWith(url);
  //   });
  //   req.flush(fakePlatformLogsResponse());
  //   expect(req.request.method).toEqual('GET');
  // }

  function expectResponseStatusGetRequest() {
    const url = `${CONSTANTS_TESTING.env.baseURL}/analytics?type=date_histo&aggs=field:status`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({
      timestamp: {
        from: 100,
        to: 100000,
        interval: 10,
      },
      values: [],
    });
    expect(req.request.method).toEqual('GET');
  }
  function expectResponseTimesLogsGetRequest() {
    const url = `${CONSTANTS_TESTING.env.baseURL}/analytics?type=date_histo&aggs=avg:response-time%3Bavg:api-response-time`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({
      timestamp: {
        from: 100,
        to: 100000,
        interval: 10,
      },
      values: [],
    });
    expect(req.request.method).toEqual('GET');
  }

  // v4
  function expectConsoleSettingsGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/response-status-ranges`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({ requests: {} });
    expect(req.request.method).toEqual('GET');
  }
  function expectTopApisGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/top-hits`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({ data: [] });
    expect(req.request.method).toEqual('GET');
  }
  function expectGetRequestStatsForV4() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/request-response-time`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({
      requestsPerSecond: 0.001,
      requestsTotal: 17,
      responseMinTime: 25.12,
      responseMaxTime: 20123.13,
      responseAvgTime: 234.76,
    });
    expect(req.request.method).toEqual('GET');
  }
});
