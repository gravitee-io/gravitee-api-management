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
import { UIRouterModule } from '@uirouter/angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiRuntimeLogsMessagesComponent } from './api-runtime-logs-messages.component';
import { ApiRuntimeLogsMessgesModule } from './api-runtime-logs-messges.module';
import { ApiRuntimeLogsMessagesHarness } from './api-runtime-logs-messages.harness';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioUiRouterTestingModule } from '../../../../shared/testing/gio-uirouter-testing-module';
import { fakeConnectorPlugin, fakePagedResult, MessageLog } from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakeMessageLog } from '../../../../entities/management-api-v2/log/messageLog.fixture';
import { IconService } from '../../../../services-ngx/icon.service';

describe('ApiRuntimeLogsMessagesComponent', () => {
  const API_ID = 'an-api-id';
  const REQUEST_ID = 'a-request-id';
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiRuntimeLogsMessagesComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsMessagesHarness;

  const initComponent = async () => {
    TestBed.configureTestingModule({
      imports: [
        ApiRuntimeLogsMessgesModule,
        UIRouterModule.forRoot({
          useHash: true,
        }),
        GioUiRouterTestingModule,
        GioHttpTestingModule,
        MatIconTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, requestId: REQUEST_ID } },
      ],
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

  describe('GIVEN there are message logs', () => {
    let iconServiceSpy: jest.SpyInstance;

    beforeEach(async () => {
      await initComponent();
      iconServiceSpy = jest.spyOn(TestBed.inject(IconService), 'registerSvg').mockReturnValue('gio:mock');
    });

    it('should init the component and fetch the endpoint connector icon', async () => {
      const messageLog = fakeMessageLog({ connectorId: 'mock', connectorType: 'ENDPOINT' });
      expectApiWithMessageLogs([messageLog, messageLog, messageLog, messageLog, messageLog]);
      expectEndpointPlugin(messageLog);
      expect(iconServiceSpy).toHaveBeenCalledTimes(1);
      fixture.detectChanges();
      expect(await componentHarness.connectorIcon()).toBeTruthy();
    });

    it('should init the component and fetch the entrypoint connector icon', async () => {
      const messageLog = fakeMessageLog({ connectorId: 'mock', connectorType: 'ENTRYPOINT' });
      expectApiWithMessageLogs([messageLog, messageLog, messageLog, messageLog, messageLog]);
      expectEntrypointPlugin(messageLog);
      expect(iconServiceSpy).toHaveBeenCalledTimes(1);
      fixture.detectChanges();
      expect(await componentHarness.connectorIcon()).toBeTruthy();
    });

    it('should be able to switch between message, headers and metadata tabs', async () => {
      const messageLog = fakeMessageLog({ connectorId: 'mock', connectorType: 'ENDPOINT' });
      expectApiWithMessageLogs([messageLog]);
      expectEndpointPlugin(messageLog);

      expect(messageLog.message.payload).toStrictEqual(await componentHarness.getTabBody());

      await componentHarness.clickOnTab('Headers');
      fixture.detectChanges();
      expect(messageLog.message.headers).toStrictEqual(JSON.parse(await componentHarness.getTabBody()));

      await componentHarness.clickOnTab('Metadata');
      fixture.detectChanges();
      expect(messageLog.message.metadata).toStrictEqual(JSON.parse(await componentHarness.getTabBody()));

      await componentHarness.clickOnTab('Message');
      fixture.detectChanges();
      expect(messageLog.message.payload).toStrictEqual(await componentHarness.getTabBody());
    });

    it('should load more messages', async () => {
      expect.assertions(3);

      const messageLog = fakeMessageLog({ connectorId: 'mock', connectorType: 'ENDPOINT' });
      expectApiWithMessageLogs([messageLog, messageLog, messageLog, messageLog, messageLog]);
      expectEndpointPlugin(messageLog);
      expect(await componentHarness.getMessages()).toHaveLength(5);

      await componentHarness.load5More();
      expectApiWithMessageLogs([messageLog, messageLog, messageLog, messageLog, messageLog], 10, 2);
      expect(await componentHarness.getMessages()).toHaveLength(10);

      try {
        await componentHarness.load5More();
      } catch (e) {
        expect(e.message).toMatch(/Failed to find element/);
      }
    });
  });

  function expectApiWithMessageLogs(data: MessageLog[], totalCount = 10, page = 1) {
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

  function expectEndpointPlugin(messageLog: MessageLog) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${messageLog.connectorId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: messageLog.connectorId, name: messageLog.connectorId })]);
  }

  function expectEntrypointPlugin(messageLog: MessageLog) {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints/${messageLog.connectorId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: messageLog.connectorId, name: messageLog.connectorId })]);
  }
});
