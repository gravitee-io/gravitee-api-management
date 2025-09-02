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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiRuntimeLogsProxyComponent } from './api-runtime-logs-proxy.component';
import { ApiProxyRequestMetricOverviewHarness } from './components/api-proxy-request-metric-overview/api-proxy-request-metric-overview.harness';
import { ApiProxyRequestLogOverviewHarness } from './components/api-proxy-request-log-overview/api-proxy-request-log-overview.harness';

import {
  ConnectionLogDetail,
  fakeConnectionLogDetail,
  fakeConnectionLogDetailRequest,
  fakeConnectionLogDetailResponse,
} from '../../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiMetricsDetailResponse } from '../../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse';
import { fakeApiMetricResponse } from '../../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse.fixture';

describe('ApiRuntimeLogsProxyComponent', () => {
  const API_ID = 'an-api-id';
  const REQUEST_ID = 'a-request-id';

  let fixture: ComponentFixture<ApiRuntimeLogsProxyComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, ApiRuntimeLogsProxyComponent, GioTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, requestId: REQUEST_ID } } } }],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsProxyComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    loader = TestbedHarnessEnvironment.loader(fixture);
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

  it('should display the component with all metrics and logs detail', async () => {
    await initComponent();

    expectApiMetric(fakeApiMetricResponse({ apiId: API_ID, requestId: REQUEST_ID }));
    expectApiWithConnectionLog(
      fakeConnectionLogDetail({
        apiId: API_ID,
        requestId: REQUEST_ID,
        entrypointRequest: fakeConnectionLogDetailRequest({ body: 'entrypointRequestBody' }),
        endpointRequest: fakeConnectionLogDetailRequest({ body: 'endpointRequestBody', uri: '' }),
        entrypointResponse: fakeConnectionLogDetailResponse({ body: 'entrypointResponseBody' }),
        endpointResponse: fakeConnectionLogDetailResponse({ body: 'endpointResponseBody', headers: {} }),
      }),
    );

    const metricsHarness = await loader.getHarness(ApiProxyRequestMetricOverviewHarness);
    expect(await metricsHarness.getAllKeyValues()).toEqual([
      { key: 'Date', value: 'Aug 1, 2025, 3:29:20 PM' },
      { key: 'Host', value: 'localhost:8082' },
      { key: 'Method', value: 'GET' },
      { key: 'URI', value: '/v4/echo' },
      { key: 'Request ID', value: 'a-request-id' },
      { key: 'Transaction ID', value: '39107cc9-b8bf-4f16-907c-c9b8bf8f16fb' },
      { key: 'Remote IP', value: '0:0:0:0:0:0:0:1' },
      { key: 'Status', value: '202' },
      { key: 'Global response time', value: '276' },
      { key: 'API response time', value: '150' },
      { key: 'Latency', value: '3' },
      { key: 'Content-length', value: '276' },
      { key: 'Application', value: 'Unknown' },
      { key: 'Plan', value: 'Default Keyless (UNSECURED)' },
      { key: 'Endpoint', value: 'https://example.endpoint.com' },
      { key: 'Gateway Host', value: 'Mac.lan' },
      { key: 'Gateway IP', value: '192.168.1.139' },
    ]);

    const logHarness = await loader.getHarness(ApiProxyRequestLogOverviewHarness);
    expect(await logHarness.getAllKeyValues()).toEqual([
      { key: 'Method', value: 'GET' },
      { key: 'URI', value: '/api-uri' },
      { key: 'X-Header', value: 'first-header' },
      { key: 'X-Header-Multiple', value: 'first-header,second-header' },
      { key: 'Method', value: 'GET' },
      { key: 'URI', value: null },
      { key: 'X-Header', value: 'first-header' },
      { key: 'X-Header-Multiple', value: 'first-header,second-header' },
      { key: 'Status', value: '200' },
      { key: 'X-Header', value: 'first-header' },
      { key: 'X-Header-Multiple', value: 'first-header,second-header' },
      { key: 'Status', value: '200' },
    ]);

    expect(await logHarness.getBodies()).toEqual([
      'entrypointRequestBody',
      'endpointRequestBody',
      'entrypointResponseBody',
      'endpointResponseBody',
    ]);
  });

  it('should display nothing when connection log is not found', async () => {
    await initComponent();

    expectApiMetric(fakeApiMetricResponse({ apiId: API_ID, requestId: REQUEST_ID }));
    expectApiConnectionLogNotFound();

    const logHarness = await loader.getHarness(ApiProxyRequestLogOverviewHarness);
    expect(await logHarness.getAllKeyValues()).toEqual([]);
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
