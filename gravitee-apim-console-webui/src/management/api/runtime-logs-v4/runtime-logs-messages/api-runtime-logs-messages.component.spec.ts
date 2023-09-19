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
  const FAKE_MESSAGE_LOG = fakeMessageLog();
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
    it('should init the component and fetch the connector icon', async () => {
      await initComponent();
      const iconServiceSpy = jest.spyOn(TestBed.inject(IconService), 'registerSvg').mockReturnValue('gio:mock');

      expectApiWithMessageLogs(2);
      expectEndpointPlugin();
      expect(iconServiceSpy).toHaveBeenCalledTimes(1);

      fixture.detectChanges();
      expect(await componentHarness.connectorIcon()).toBeTruthy();
    });
  });

  function expectApiWithMessageLogs(items: number, totalCount = 10, page = 1) {
    const data: MessageLog[] = [];
    for (let i = 0; i < items; i++) {
      data.push(FAKE_MESSAGE_LOG);
    }

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
          pageItemsCount: items,
          totalCount,
        }),
      );
  }

  function expectEndpointPlugin() {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${FAKE_MESSAGE_LOG.connectorId}`, method: 'GET' })
      .flush([fakeConnectorPlugin({ id: FAKE_MESSAGE_LOG.connectorId, name: FAKE_MESSAGE_LOG.connectorId })]);
  }
});
