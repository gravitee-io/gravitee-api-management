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

import { ApiRuntimeLogsProxyComponent } from './api-runtime-logs-proxy.component';

import { ConnectionLogDetail, fakeConnectionLogDetail } from '../../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiMetricsDetailResponse } from '../../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse';
import { fakeApiMetricResponse } from '../../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse.fixture';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

describe('ApiRuntimeLogsProxyComponent', () => {
  const API_ID = 'an-api-id';
  const REQUEST_ID = 'a-request-id';

  let fixture: ComponentFixture<ApiRuntimeLogsProxyComponent>;
  let httpTestingController: HttpTestingController;
  let rootLoader: HarnessLoader;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiRuntimeLogsProxyComponent, GioTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, requestId: REQUEST_ID } } } }],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsProxyComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should init the component, fetch the metrics', async () => {
    await initComponent();

    expectApiWithConnectionLog(
      fakeConnectionLogDetail({
        apiId: API_ID,
        requestId: REQUEST_ID,
      }),
    );

    expectApiMetric(
      fakeApiMetricResponse({
        apiId: API_ID,
        requestId: REQUEST_ID,
      }),
    );

    expect(fixture.nativeElement.querySelector('api-proxy-request-log-overview')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('api-proxy-request-metric-overview')).toBeTruthy();
  });

  function expectApiMetric(data: ApiMetricsDetailResponse) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush(data);
    fixture.detectChanges();
  }

  function expectApiWithConnectionLog(data: ConnectionLogDetail) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush(data);
    fixture.detectChanges();
  }

  function expectApiConnectionLogNotFound() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush({ message: 'Not found' }, { status: 404, statusText: 'Not found' });
    fixture.detectChanges();
  }
});
