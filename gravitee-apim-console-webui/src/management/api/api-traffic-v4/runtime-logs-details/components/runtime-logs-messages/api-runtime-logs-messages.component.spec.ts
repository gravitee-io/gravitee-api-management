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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ApiRuntimeLogsMessagesComponent } from './api-runtime-logs-messages.component';
import { ApiRuntimeLogsMessagesModule } from './api-runtime-logs-messages.module';
import { ApiRuntimeLogsMessagesHarness } from './api-runtime-logs-messages.harness';

import {
  AggregatedMessageLog,
  ConnectionLogDetail,
  fakeAggregatedMessageLog,
  fakeConnectionLogDetail,
  fakeConnectorPlugin,
  fakeMessage,
  fakePagedResult,
} from '../../../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../../shared/testing';
import { IconService } from '../../../../../../services-ngx/icon.service';

describe('ApiRuntimeLogsMessagesComponent', () => {
  const API_ID = 'an-api-id';
  const REQUEST_ID = 'a-request-id';

  let fixture: ComponentFixture<ApiRuntimeLogsMessagesComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsMessagesHarness;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [ApiRuntimeLogsMessagesModule, GioHttpTestingModule, MatIconTestingModule, NoopAnimationsModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID, requestId: REQUEST_ID } } } }],
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiRuntimeLogsMessagesComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsMessagesHarness);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  };

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('GIVEN there are connection logs', () => {
    beforeEach(async () => {
      await initComponent();
      await componentHarness.clickOnConnectionLogsTab();
    });

    it('should init the component and fetch the connection log', async () => {
      expectApiWithConnectionLog(fakeConnectionLogDetail());
      fixture.detectChanges();

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
      // URI is unset on endpoint request
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
  });

  describe('GIVEN there are message logs', () => {
    let iconServiceSpy: jest.SpyInstance;

    beforeEach(async () => {
      await initComponent();
      iconServiceSpy = jest.spyOn(TestBed.inject(IconService), 'registerSvg').mockReturnValue('gio:mock');
      await componentHarness.clickOnMessagesTab();
    });

    it('should init the component and fetch the connectors icon', async () => {
      const messageLog = fakeAggregatedMessageLog({
        entrypoint: fakeMessage({ connectorId: 'http-get', payload: undefined }),
        endpoint: fakeMessage({ connectorId: 'mock', payload: undefined }),
      });
      expectApiWithMessageLogs(Array(5).fill(messageLog));
      expectEntrypointPlugin(messageLog);
      expectEndpointPlugin(messageLog);
      expect(iconServiceSpy).toHaveBeenCalledTimes(2);
      fixture.detectChanges();
      expect(await componentHarness.entrypointConnectorIcon()).toBeTruthy();
      expect(await componentHarness.endpointConnectorIcon()).toBeTruthy();
    });

    it('should be able to switch between message, headers and metadata tabs', async () => {
      const messageLog = fakeAggregatedMessageLog({
        entrypoint: fakeMessage({ connectorId: 'http-get' }),
        endpoint: fakeMessage({ connectorId: 'kafka' }),
      });
      expectApiWithMessageLogs([messageLog]);
      expectEntrypointPlugin(messageLog);
      expectEndpointPlugin(messageLog);

      expect(await componentHarness.getEntrypointTabBody()).toStrictEqual(messageLog.entrypoint.payload);
      expect(await componentHarness.getEndpointTabBody()).toStrictEqual(messageLog.endpoint.payload);

      await componentHarness.clickOnEntrypointTab('Headers');
      await componentHarness.clickOnEndpointTab('Headers');
      fixture.detectChanges();
      expect(JSON.parse(await componentHarness.getEntrypointTabBody())).toStrictEqual(messageLog.entrypoint.headers);
      expect(JSON.parse(await componentHarness.getEndpointTabBody())).toStrictEqual(messageLog.endpoint.headers);

      await componentHarness.clickOnEntrypointTab('Metadata');
      await componentHarness.clickOnEndpointTab('Metadata');
      fixture.detectChanges();
      expect(JSON.parse(await componentHarness.getEntrypointTabBody())).toStrictEqual(messageLog.entrypoint.metadata);
      expect(JSON.parse(await componentHarness.getEndpointTabBody())).toStrictEqual(messageLog.endpoint.metadata);

      await componentHarness.clickOnEntrypointTab('Payload');
      await componentHarness.clickOnEndpointTab('Payload');
      fixture.detectChanges();
      expect(await componentHarness.getEntrypointTabBody()).toStrictEqual(messageLog.entrypoint.payload);
      expect(await componentHarness.getEndpointTabBody()).toStrictEqual(messageLog.endpoint.payload);
    });

    it('should load more messages', async () => {
      expect.assertions(3);

      const messageLog = fakeAggregatedMessageLog({
        entrypoint: fakeMessage({ connectorId: 'http-get', payload: undefined }),
        endpoint: fakeMessage({ connectorId: 'kafka', payload: undefined }),
      });
      expectApiWithMessageLogs(Array(5).fill(messageLog));
      expectEndpointPlugin(messageLog);
      expect(await componentHarness.getMessages()).toHaveLength(5);

      await componentHarness.load5More();
      expectApiWithMessageLogs(Array(5).fill(messageLog), 10, 2);
      expect(await componentHarness.getMessages()).toHaveLength(10);

      try {
        await componentHarness.load5More();
      } catch (e) {
        expect(e.message).toMatch(/Failed to find element/);
      }
    });
  });

  function expectApiWithMessageLogs(data: AggregatedMessageLog[], totalCount = 10, page = 1) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}/messages?page=${page}&perPage=5`,
        method: 'GET',
      })
      .flush(
        fakePagedResult(data, {
          page: page,
          perPage: 5,
          pageCount: Math.ceil(totalCount / 5),
          pageItemsCount: data.length,
          totalCount,
        }),
      );
  }

  function expectApiWithConnectionLog(data: ConnectionLogDetail) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/logs/${REQUEST_ID}`,
        method: 'GET',
      })
      .flush(data);
  }

  function expectEndpointPlugin(messageLog: AggregatedMessageLog) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${messageLog.endpoint.connectorId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: messageLog.endpoint.connectorId, name: messageLog.endpoint.connectorId })]);
  }

  function expectEntrypointPlugin(messageLog: AggregatedMessageLog) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints/${messageLog.entrypoint.connectorId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: messageLog.entrypoint.connectorId, name: messageLog.entrypoint.connectorId })]);
  }
});
