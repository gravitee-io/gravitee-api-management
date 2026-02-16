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
import { fakeV4AnalyticsResponseStatus, fakeV4AnalyticsResponseTime } from '../../../entities/analytics/analytics.fixture';
import { AnalyticsDefinitionVersion } from '../../../entities/analytics/analytics';

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

      req = expectCountApiRequest();
      expect(req.request.url).toContain('interval=120000');

      req = expectCountApplicationRequest();
      expect(req.request.url).toContain('interval=120000');

      expectTopApplicationsGetRequest();
      expectTopFailedAppsGetRequest();
      expectSearchApiEventsRequest();
      expectConsoleSettingsGetRequest();
      expectTopApisGetRequest();
      expectGetRequestStatsForV4();

      expectV4ResponseTimesGetRequest();
      expectV4ResponseStatusGetRequest();
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

      expectTopApplicationsGetRequest();
      expectTopFailedAppsGetRequest();
      expectResponseStatusRequest();
      expectApiStateRequest();
      expectCountApiRequest();
      expectCountApplicationRequest();
      expectSearchApiEventsRequest();
      expectConsoleSettingsGetRequest();
      expectTopApisGetRequest();
      expectGetRequestStatsForV4();
      expectV4ResponseTimesGetRequest();
      expectV4ResponseStatusGetRequest();
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
  });

  function expectRequests() {
    expectTopApplicationsGetRequest();
    expectTopFailedAppsGetRequest();
    expectApiLifecycleStateRequest();
    expectApiStateRequest();
    expectResponseStatusRequest();
    expectCountApiRequest();
    expectCountApplicationRequest();
    expectSearchApiEventsRequest();
    expectConsoleSettingsGetRequest();
    expectTopApisGetRequest();
    expectGetRequestStatsForV4();
    expectV4ResponseTimesGetRequest();
    expectV4ResponseStatusGetRequest();
  }

  function expectTopApplicationsGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/top-apps-by-request-count`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({
      data: [
        {
          id: 'tst',
          name: 'name-test',
          count: 100,
        },
      ],
    });
    expect(req.request.method).toEqual('GET');
  }

  function expectTopFailedAppsGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/top-failed-apis`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({
      data: [
        {
          id: '9f060ca4-326b-473a-860c-a4326be73a28',
          definitionVersion: AnalyticsDefinitionVersion.V2,
          name: 'Test api',
          failedCalls: 10,
          failedCallsRatio: 0.25,
        },
        {
          id: '28113601-5197-4815-9136-015197b81592',
          definitionVersion: AnalyticsDefinitionVersion.V4,
          name: 'brand new api',
          failedCalls: 20,
          failedCallsRatio: 0.2857142857142857,
        },
      ],
    });
    expect(req.request.method).toEqual('GET');
  }

  function expectApiLifecycleStateRequest(): TestRequest {
    const req = httpTestingController.expectOne(req => {
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
    const req = httpTestingController.expectOne(req => {
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
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=group_by&field=state`);
    });
    req.flush({
      values: {},
    });
    return req;
  }
  function expectCountApiRequest(): TestRequest {
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=count&field=api`);
    });
    req.flush({
      count: 0,
    });
    return req;
  }
  function expectCountApplicationRequest(): TestRequest {
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(`${CONSTANTS_TESTING.env.baseURL}/analytics?type=count&field=application`);
    });
    req.flush({
      count: 0,
    });
    return req;
  }
  function expectSearchApiEventsRequest(): TestRequest {
    const req = httpTestingController.expectOne(req => {
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

  // v4
  function expectV4ResponseTimesGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/response-time-over-time`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeV4AnalyticsResponseTime());
    expect(req.request.method).toEqual('GET');
  }

  function expectV4ResponseStatusGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/response-status-overtime`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(fakeV4AnalyticsResponseStatus());
    expect(req.request.method).toEqual('GET');
  }

  function expectConsoleSettingsGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/response-status-ranges`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({ requests: {} });
    expect(req.request.method).toEqual('GET');
  }
  function expectTopApisGetRequest() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/top-hits`;
    const req = httpTestingController.expectOne(req => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush({ data: [] });
    expect(req.request.method).toEqual('GET');
  }
  function expectGetRequestStatsForV4() {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/analytics/request-response-time`;
    const req = httpTestingController.expectOne(req => {
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
