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
import { UIRouterModule } from '@uirouter/angular';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiRuntimeLogsProxyComponent } from './api-runtime-logs-proxy.component';
import { ApiRuntimeLogsProxyHarness } from './api-runtime-logs-proxy.harness';
import { ApiRuntimeLogsProxyModule } from './api-runtime-logs-proxy.module';

import { GioUiRouterTestingModule } from '../../../../../shared/testing/gio-uirouter-testing-module';
import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ConnectionLogDetail, fakeConnectionLogDetail } from '../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';

describe('ApiRuntimeLogsProxyComponent', () => {
  const API_ID = 'an-api-id';
  const REQUEST_ID = 'a-request-id';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiRuntimeLogsProxyComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsProxyHarness;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [
        ApiRuntimeLogsProxyModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
        GioUiRouterTestingModule,
        GioHttpTestingModule,
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, requestId: REQUEST_ID } },
      ],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsProxyComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsProxyHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should init the component and fetch the connection log', async () => {
    await initComponent();

    expectApiWithConnectionLog(
      fakeConnectionLogDetail({
        apiId: API_ID,
        requestId: REQUEST_ID,
      }),
    );

    // Entrypoint request
    const logsDetailHarness = await componentHarness.logsDetailHarness();
    const entryPointRequestPanel = await logsDetailHarness.entrypointRequestPanelSelector();
    expect(await logsDetailHarness.getConnectionLogRequestUri(entryPointRequestPanel)).toEqual('/api-uri');
    expect(await logsDetailHarness.getConnectionLogRequestMethod(entryPointRequestPanel)).toEqual('get');
    expect(await logsDetailHarness.getConnectionLogHeaders(entryPointRequestPanel)).toMatchObject({
      'X-Header': 'first-header',
      'X-Header-Multiple': 'first-header,second-header',
    });

    // Endpoint request
    const endpointRequestPanel = await logsDetailHarness.endpointRequestPanelSelector();
    expect(await logsDetailHarness.getConnectionLogRequestUri(endpointRequestPanel)).toBeUndefined();
    expect(await logsDetailHarness.getConnectionLogRequestMethod(endpointRequestPanel)).toEqual('get');
    expect(await logsDetailHarness.getConnectionLogHeaders(endpointRequestPanel)).toMatchObject({
      'X-Header': 'first-header',
      'X-Header-Multiple': 'first-header,second-header',
    });

    // Endpoint response
    const endpointResponsePanel = await logsDetailHarness.endpointResponsePanelSelector();
    expect(await logsDetailHarness.getConnectionLogResponseStatus(endpointResponsePanel)).toEqual('200');
    expect(await logsDetailHarness.getConnectionLogHeaders(endpointResponsePanel)).toMatchObject({});

    // Endpoint request
    const entrypointResponsePanel = await logsDetailHarness.entrypointResponsePanelSelector();
    expect(await logsDetailHarness.getConnectionLogResponseStatus(entrypointResponsePanel)).toEqual('200');
    expect(await logsDetailHarness.getConnectionLogHeaders(entrypointResponsePanel)).toMatchObject({
      'X-Header': 'first-header',
      'X-Header-Multiple': 'first-header,second-header',
    });
  });

  it('should navigate to settings page', async () => {
    await initComponent();
    expectApiWithoutConnectionLog();

    const emptyStateHarness = await componentHarness.emptyStateHarness();
    await emptyStateHarness.clickOpenSettingsButton();

    expect(fakeUiRouter.go).toHaveBeenCalledWith('management.apis.runtimeLogs-settings');
  });

  function expectApiWithConnectionLog(data: ConnectionLogDetail) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush(data);
    fixture.detectChanges();
  }

  function expectApiWithoutConnectionLog() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush(null);
    fixture.detectChanges();
  }
});
