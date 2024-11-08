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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiHealthCheckDashboardV4Component } from './api-health-check-dashboard-v4.component';
import { ApiHealthCheckDashboardV4Harness } from './api-health-check-dashboard-v4.harness';
import { ApiHealthCheckDashboardV4Module } from './api-health-check-dashboard-v4.module';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import {
  fakeApiHealthAvailability,
  fakeApiHealthAverageResponseTime,
  fakeApiHealthResponseTimeOvertime,
} from '../../../entities/management-api-v2/api/v4/healthCheck.fixture';
import { ApiAvailability, ApiAverageResponseTime } from '../../../entities/management-api-v2/api/v4/healthCheck';

describe('ApiHealthCheckDashboardV4Component', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiHealthCheckDashboardV4Component>;
  let componentHarness: ApiHealthCheckDashboardV4Harness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiHealthCheckDashboardV4Module, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: API_ID },
            },
          },
        },
      ],
    }).compileComponents();

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiHealthCheckDashboardV4Component);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiHealthCheckDashboardV4Harness);
    fixture.autoDetectChanges(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call backend for data at loading', async () => {
    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();
  });

  it('should call backend for data on refresh', async () => {
    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();

    const filters = await componentHarness.getFiltersHarness();

    await filters.refresh();

    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();
  });

  it('should call backend for data on filter date change', async () => {
    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();

    const filters = await componentHarness.getFiltersHarness();

    await filters.selectOption();

    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();
  });

  it('should display correct data in Availability widget', async () => {
    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();

    const availabilityWidget = await componentHarness.getAvailabilityWidgetHarness();

    expect(await availabilityWidget.getWidgetValue()).toEqual('98.76 %');
  });

  it('should display correct data in AverageResponseTime widget', async () => {
    expectGetApiHealthResponseStatusOvertime();
    expectGetApiAvailability();
    expectGetApiAverageResponseTime();

    const averageResponseTimeWidget = await componentHarness.getAverageResponseTimeWidgetHarness();

    expect(await averageResponseTimeWidget.getWidgetValue()).toEqual('100 ms');
  });

  function expectGetApiHealthResponseStatusOvertime(res = fakeApiHealthResponseTimeOvertime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/health/average-response-time-overtime`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }

  function expectGetApiAvailability(res: ApiAvailability = fakeApiHealthAvailability()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/health/availability`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }

  function expectGetApiAverageResponseTime(res: ApiAverageResponseTime = fakeApiHealthAverageResponseTime()) {
    const url = `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/health/average-response-time`;
    const req = httpTestingController.expectOne((req) => {
      return req.method === 'GET' && req.url.startsWith(url);
    });
    req.flush(res);
  }
});
