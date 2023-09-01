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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiRuntimeLogsMessageSettingsModule } from './api-runtime-logs-message-settings.module';
import { ApiRuntimeLogsMessageSettingsComponent } from './api-runtime-logs-message-settings.component';
import { ApiRuntimeLogsMessageSettingsHarness } from './api-runtime-logs-message-settings.harness';

import { CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { User } from '../../../../../entities/user';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';

describe('ApiRuntimeLogsSettingsComponent', () => {
  const API_ID = 'apiId';
  const testApi: ApiV4 = {
    id: API_ID,
    definitionVersion: 'V4',
    type: 'MESSAGE',
    name: 'test',
    apiVersion: '1',
    analytics: {
      enabled: true,
      logging: {
        mode: { entrypoint: false, endpoint: false },
        phase: { request: false, response: false },
        content: { messagePayload: false, messageHeaders: false, messageMetadata: false, headers: false },
        condition: null,
        messageCondition: null,
      },
      sampling: { type: 'COUNT', value: '50' },
    },
  };
  let fixture: ComponentFixture<ApiRuntimeLogsMessageSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsMessageSettingsHarness;

  const initComponent = async (api: ApiV4 = testApi) => {
    const currentUser = new User();
    currentUser.userPermissions = ['api-definition-u'];

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiRuntimeLogsMessageSettingsModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID } },
        { provide: UIRouterState, useValue: { go: jest.fn() } },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    });

    fixture = TestBed.createComponent(ApiRuntimeLogsMessageSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsMessageSettingsHarness);

    fixture.componentInstance.api = api;
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('save tests', () => {
    describe('logging mode tests', () => {
      it('should enable logging mode on entrypoint and endpoint', async () => {
        await initComponent();
        expect(await componentHarness.isEntrypointChecked()).toBe(false);
        expect(await componentHarness.isEndpointChecked()).toBe(false);

        await componentHarness.toggleEntrypoint();

        await componentHarness.toggleEndpoint();
        await componentHarness.saveSettings();

        expectApiGetRequest(testApi);
        expectApiPutRequest({
          ...testApi,
          analytics: {
            ...testApi.analytics,
            logging: { ...testApi.analytics.logging, mode: { entrypoint: true, endpoint: true } },
          },
        });
      });

      it('should have logging mode enabled with entrypoint and endpoint', async () => {
        await initComponent(
          fakeApiV4({
            ...testApi,
            analytics: {
              ...testApi.analytics,
              logging: { ...testApi.analytics.logging, mode: { entrypoint: true, endpoint: true } },
            },
          }),
        );
        expect(await componentHarness.isEntrypointChecked()).toBe(true);
        expect(await componentHarness.isEndpointChecked()).toBe(true);

        await componentHarness.toggleEntrypoint();

        await componentHarness.toggleEndpoint();
        await componentHarness.saveSettings();

        expectApiGetRequest(testApi);
        expectApiPutRequest({
          ...testApi,
          analytics: {
            ...testApi.analytics,
            logging: { ...testApi.analytics.logging, mode: { entrypoint: false, endpoint: false } },
          },
        });
      });

      it('should enable/disable message checkboxes according to logging mode', async () => {
        await initComponent();
        expect(await componentHarness.isEntrypointChecked()).toBe(false);
        expect(await componentHarness.isEndpointChecked()).toBe(false);
        expect(await componentHarness.isMessageContentDisabled()).toBe(true);
        expect(await componentHarness.isMessageHeadersDisabled()).toBe(true);
        expect(await componentHarness.isMessageMetadataDisabled()).toBe(true);
        expect(await componentHarness.isHeadersDisabled()).toBe(true);

        await componentHarness.toggleEntrypoint();
        expect(await componentHarness.isEntrypointChecked()).toBe(true);
        expect(await componentHarness.isMessageContentDisabled()).toBe(false);
        expect(await componentHarness.isMessageHeadersDisabled()).toBe(false);
        expect(await componentHarness.isMessageMetadataDisabled()).toBe(false);
        expect(await componentHarness.isHeadersDisabled()).toBe(false);

        await componentHarness.toggleEntrypoint();
        await componentHarness.toggleEndpoint();
        expect(await componentHarness.isEntrypointChecked()).toBe(false);
        expect(await componentHarness.isEndpointChecked()).toBe(true);
        expect(await componentHarness.isMessageContentDisabled()).toBe(false);
        expect(await componentHarness.isMessageHeadersDisabled()).toBe(false);
        expect(await componentHarness.isMessageMetadataDisabled()).toBe(false);
        expect(await componentHarness.isHeadersDisabled()).toBe(false);

        await componentHarness.toggleEntrypoint();
        expect(await componentHarness.isEntrypointChecked()).toBe(true);
        expect(await componentHarness.isEndpointChecked()).toBe(true);
        expect(await componentHarness.isMessageContentDisabled()).toBe(false);
        expect(await componentHarness.isMessageContentChecked()).toBe(false);
        expect(await componentHarness.isMessageHeadersDisabled()).toBe(false);
        expect(await componentHarness.isMessageHeadersChecked()).toBe(false);
        expect(await componentHarness.isMessageMetadataDisabled()).toBe(false);
        expect(await componentHarness.isMessageMetadataChecked()).toBe(false);
        expect(await componentHarness.isHeadersDisabled()).toBe(false);
      });
    });

    it('should enable logging phase on request and response', async () => {
      await initComponent();
      expect(await componentHarness.isRequestPhaseChecked()).toBe(false);
      expect(await componentHarness.isResponsePhaseChecked()).toBe(false);

      await componentHarness.checkRequestPhase();
      await componentHarness.checkResponsePhase();
      await componentHarness.saveSettings();

      expectApiGetRequest(testApi);
      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          logging: {
            ...testApi.analytics.logging,
            phase: { request: true, response: true },
          },
        },
      });
    });

    it('should have logging phase enabled with request and response', async () => {
      await initComponent(
        fakeApiV4({
          ...testApi,
          analytics: {
            ...testApi.analytics,
            logging: {
              ...testApi.analytics.logging,
              phase: { request: true, response: true },
            },
          },
        }),
      );
      expect(await componentHarness.isRequestPhaseChecked()).toBe(true);
      expect(await componentHarness.isResponsePhaseChecked()).toBe(true);

      await componentHarness.uncheckRequestPhase();
      await componentHarness.uncheckResponsePhase();
      await componentHarness.saveSettings();

      expectApiGetRequest(testApi);
      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          logging: {
            ...testApi.analytics.logging,
            phase: { request: false, response: false },
          },
        },
      });
    });

    it('should enable logging content on payload, headers and metadata', async () => {
      await initComponent();
      expect(await componentHarness.isMessageContentChecked()).toBe(false);
      expect(await componentHarness.isMessageHeadersChecked()).toBe(false);
      expect(await componentHarness.isMessageMetadataChecked()).toBe(false);
      expect(await componentHarness.isHeadersChecked()).toBe(false);

      await componentHarness.toggleEndpoint();
      await componentHarness.toggleEntrypoint();
      await componentHarness.checkMessageContent();
      await componentHarness.checkMessageHeaders();
      await componentHarness.checkMessageMetadata();
      await componentHarness.checkHeaders();
      await componentHarness.saveSettings();

      expectApiGetRequest(testApi);
      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          logging: {
            ...testApi.analytics.logging,
            mode: { entrypoint: true, endpoint: true },
            content: { messagePayload: true, messageHeaders: true, messageMetadata: true, headers: true },
          },
        },
      });
    });

    it('should save settings with conditions on request and messages', async () => {
      await initComponent();
      expect(await componentHarness.getRequestCondition()).toEqual('');
      expect(await componentHarness.getMessageCondition()).toEqual('');

      await componentHarness.addMessageCondition('message condition');
      await componentHarness.addRequestCondition('request condition');
      await componentHarness.saveSettings();

      expectApiGetRequest(testApi);
      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          logging: {
            ...testApi.analytics.logging,
            condition: 'request condition',
            messageCondition: 'message condition',
          },
        },
      });
    });

    describe('sampling tests', () => {
      it('should save sampling settings', async () => {
        await initComponent();
        expect(await componentHarness.getSamplingType()).toStrictEqual('Count');
        expect(await componentHarness.getSamplingValue()).toStrictEqual('50');

        await componentHarness.choseSamplingType('Probabilistic');
        await componentHarness.addSamplingValue('0.5');
        await componentHarness.saveSettings();

        expectApiGetRequest(testApi);
        expectApiPutRequest({
          ...testApi,
          analytics: {
            ...testApi.analytics,
            sampling: { type: 'PROBABILITY', value: '0.5' },
          },
        });
      });

      it('should validate sampling value with COUNT type', async () => {
        await initComponent();
        expect(await componentHarness.getSamplingType()).toStrictEqual('Count');
        expect(await componentHarness.getSamplingValue()).toStrictEqual('50');

        await componentHarness.addSamplingValue(null);
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('5');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('42');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      });

      it('should validate sampling value with PROBABILITY type', async () => {
        await initComponent();
        await componentHarness.choseSamplingType('Probabilistic');

        await componentHarness.addSamplingValue(null);
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('-1');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('42');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('0.3');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      });

      it('should validate sampling value with TEMPORAL type', async () => {
        await initComponent();
        await componentHarness.choseSamplingType('Temporal');

        await componentHarness.addSamplingValue(null);
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('PT0.001S');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);

        await componentHarness.addSamplingValue('PT1S');
        expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      });
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
  }
});
