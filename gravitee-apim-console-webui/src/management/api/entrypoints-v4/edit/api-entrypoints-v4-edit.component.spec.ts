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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { By } from '@angular/platform-browser';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { HarnessLoader } from '@angular/cdk/testing';

import { ApiEntrypointsV4EditComponent } from './api-entrypoints-v4-edit.component';

import {
  Api,
  ApiV4,
  ConnectorPlugin,
  Entrypoint,
  fakeApiV4,
  getEntrypointConnectorSchema,
  Listener,
  UpdateApiV4,
} from '../../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ApiEntrypointsV4Module } from '../api-entrypoints-v4.module';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeSubscriptionListener } from '../../../../entities/management-api-v2/api/v4/listener.fixture';
import { GioFormQosHarness } from '../../component/gio-form-qos/gio-form-qos.harness';

describe('ApiEntrypointsV4EditComponent', () => {
  const API_ID = 'apiId';
  const HTTP_GET_ENTRYPOINT: Entrypoint = {
    type: 'http-get',
    configuration: {
      messagesLimitCount: 111,
      headersInPayload: false,
      metadataInPayload: false,
      messagesLimitDurationMs: 5000,
    },
    qos: 'AUTO',
  };
  const WEBSOCKET_ENTRYPOINT: Entrypoint = {
    type: 'websocket',
    configuration: { publisher: { enabled: true }, subscriber: { enabled: false } },
    qos: 'AUTO',
  };
  const HTTP_LISTENER: Listener = {
    type: 'HTTP',
    entrypoints: [HTTP_GET_ENTRYPOINT, WEBSOCKET_ENTRYPOINT],
  };
  const SUBSCRIPTION_LISTENER = fakeSubscriptionListener();

  const API = fakeApiV4({
    id: API_ID,
    listeners: [HTTP_LISTENER, SUBSCRIPTION_LISTENER],
  });
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiEntrypointsV4EditComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const createComponent = (api: ApiV4) => {
    fixture = TestBed.createComponent(ApiEntrypointsV4EditComponent);
    fixture.detectChanges();

    expectGetEntrypoints();
    expectGetApi(api);
    expectGetEntrypointSchema('http-get');
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiEntrypointsV4Module, MatIconTestingModule, MatAutocompleteModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, entrypointId: 'http-get' } },
        { provide: UIRouterState, useValue: fakeUiRouter },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);

    createComponent(API);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show API configuration', async () => {
    const formElement = fixture.debugElement.query(By.css('gio-form-json-schema'));
    expect(formElement).not.toBeNull();

    const input = await loader.getHarness(MatInputHarness.with({ selector: '[id*="messagesLimitCount"]' }));
    await input.setValue('111');
  });

  it('should save configuration changes', async () => {
    const button = await loader.getHarness(MatButtonHarness.with({ text: 'Save changes' }));
    const formElement = fixture.debugElement.query(By.css('gio-form-json-schema'));
    expect(formElement).not.toBeNull();
    expect(button.isDisabled()).toBeTruthy();

    const input = await loader.getHarness(MatInputHarness.with({ selector: '[id*="messagesLimitCount"]' }));
    await input.setValue('1234');

    const qosSelect = await loader.getHarness(GioFormQosHarness);
    expect(await qosSelect.getSelectedQos()).toEqual('AUTO');
    await qosSelect.selectOption('AT_MOST_ONCE');

    expect(await button.isDisabled()).toBeFalsy();
    await button.click();
    fixture.detectChanges();

    // GET
    expectGetApi(API);
    // UPDATE
    const saveReq = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'PUT' });
    const expectedUpdateApi: UpdateApiV4 = {
      ...API,
      listeners: [
        SUBSCRIPTION_LISTENER,
        {
          type: 'HTTP',
          entrypoints: [
            WEBSOCKET_ENTRYPOINT,
            {
              type: 'http-get',
              configuration: {
                messagesLimitCount: 1234,
                headersInPayload: false,
                metadataInPayload: false,
                messagesLimitDurationMs: 5000,
              },
              qos: 'AT_MOST_ONCE',
            },
          ],
        },
      ],
    };
    expect(saveReq.request.body).toEqual(expectedUpdateApi);
    saveReq.flush(API);
  });

  const expectGetApi = (api: Api) => {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}`, method: 'GET' }).flush(api);
    fixture.detectChanges();
  };

  const expectGetEntrypoints = () => {
    const entrypoints: Partial<ConnectorPlugin>[] = [
      { id: 'http-get', supportedApiType: 'MESSAGE', supportedQos: ['AUTO', 'AT_MOST_ONCE', 'AT_LEAST_ONCE'], name: 'HTTP GET' },
      { id: 'http-post', supportedApiType: 'MESSAGE', supportedQos: ['NONE', 'AUTO'], name: 'HTTP POST' },
      {
        id: 'sse',
        supportedApiType: 'MESSAGE',
        supportedQos: ['NONE', 'AUTO', 'AT_MOST_ONCE', 'AT_LEAST_ONCE'],
        name: 'Server-Sent Events',
      },
      { id: 'webhook', supportedApiType: 'MESSAGE', supportedQos: ['NONE', 'AUTO', 'AT_MOST_ONCE', 'AT_LEAST_ONCE'], name: 'Webhook' },
    ];

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints`, method: 'GET' }).flush(entrypoints);
  };

  const expectGetEntrypointSchema = (entrypointType: string) => {
    const entrypointSchema = getEntrypointConnectorSchema(entrypointType);

    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints/${entrypointType}/schema`, method: 'GET' })
      .flush(entrypointSchema);
  };
});
