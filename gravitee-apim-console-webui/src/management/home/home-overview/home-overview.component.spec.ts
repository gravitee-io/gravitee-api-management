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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { HomeOverviewComponent } from './home-overview.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { HomeModule } from '../home.module';
import { GioQuickTimeRangeHarness } from '../components/gio-quick-time-range/gio-quick-time-range.harness';
import { GioRequestStatsHarness } from '../components/gio-request-stats/gio-request-stats.harness';
import { GioTestingPermissionProvider } from '../../../shared/components/gio-permission/gio-permission.service';
import { DashboardV4ApiRequestStatsHarness } from '../components/dashboard-v4-api-request-stats/dashboard-v4-api-request-stats.harness';

describe('HomeOverviewComponent', () => {
  let fixture: ComponentFixture<HomeOverviewComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('with environment-platform-read permission', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, HomeModule, MatIconTestingModule],
        providers: [{ provide: GioTestingPermissionProvider, useValue: ['environment-platform-r'] }],
      });
      fixture = TestBed.createComponent(HomeOverviewComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);

      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    it('should show request stats', async () => {
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

      const stats = await loader.getHarness(GioRequestStatsHarness);
      expect(await stats.getAverage()).toEqual('8.43 ms');
    });

    it('should show v4 APIs request stats', async () => {
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

      const stats = await loader.getHarness(DashboardV4ApiRequestStatsHarness);
      expect(await stats.getRequestsPerSecond()).toEqual('< 0.1 ');
      expect(await stats.getTotalRequests()).toEqual('17 ');
      expect(await stats.getMinResponseTime()).toEqual('25.12 ms ');
      expect(await stats.getMaxResponseTime()).toEqual('20,123.13 ms ');
      expect(await stats.getAverageResponseTime()).toEqual('234.76 ms ');
    });

    it('should load request stats when changing date range', async () => {
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

      const timeRangeHarness = await loader.getHarness(GioQuickTimeRangeHarness);
      await timeRangeHarness.selectTimeRangeByText('Last hour');
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
    });
  });

  describe('without environment-platform-read permission', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [NoopAnimationsModule, GioTestingModule, HomeModule, MatIconTestingModule],
        providers: [{ provide: GioTestingPermissionProvider, useValue: [] }],
      });
      fixture = TestBed.createComponent(HomeOverviewComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);

      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    it('should not load api events', async () => {
      expectApiLifecycleStateRequest();
      expectApiStateRequest();
      expectResponseStatusRequest();
      expectRequestStatsRequest();
      expectTopApiRequest();
      expectCountApiRequest();
      expectCountApplicationRequest();
      expectConsoleSettingsGetRequest();
      expectTopApisGetRequest();
      expectGetRequestStatsForV4();

      const stats = await loader.getHarness(GioRequestStatsHarness);
      expect(await stats.getAverage()).toEqual('8.43 ms');
    });
  });

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
