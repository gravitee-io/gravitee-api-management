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
import { ActivatedRoute } from '@angular/router';

import { ApiRuntimeLogsMessageSettingsModule } from './api-runtime-logs-message-settings.module';
import { ApiRuntimeLogsMessageSettingsComponent } from './api-runtime-logs-message-settings.component';
import { ApiRuntimeLogsMessageSettingsHarness } from './api-runtime-logs-message-settings.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { ConsoleSettings } from '../../../../../entities/consoleSettings';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../../entities/Constants';

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
      tracing: {
        enabled: true,
        verbose: false,
      },
      sampling: { type: 'COUNT', value: '50' },
    },
    originContext: {
      origin: 'MANAGEMENT',
    },
  };
  const testSettings: ConsoleSettings = {
    metadata: {
      readonly: [],
    },
    logging: {
      messageSampling: {
        probabilistic: {
          limit: 0.52,
          default: 0.33322,
        },
        count: {
          limit: 40,
          default: 666,
        },
        temporal: {
          limit: 'PT10S',
          default: 'PT10S',
        },
      },
    },
  };
  let fixture: ComponentFixture<ApiRuntimeLogsMessageSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApiRuntimeLogsMessageSettingsHarness;

  const initComponent = async (api: ApiV4 = testApi, settings: ConsoleSettings = testSettings) => {
    fixture = TestBed.createComponent(ApiRuntimeLogsMessageSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiRuntimeLogsMessageSettingsHarness);

    fixture.componentInstance.api = api;
    expectConsoleSettingsGetRequest(settings);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiRuntimeLogsMessageSettingsModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        { provide: GioTestingPermissionProvider, useValue: ['api-definition-u'] },
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should enable and disable all form fields according to analytics enabled', async () => {
    await initComponent();

    expect(await componentHarness.isEnabledChecked()).toStrictEqual(true);

    expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
    expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
    await componentHarness.toggleEntrypoint();
    expect(await componentHarness.isMessageContentDisabled()).toEqual(false);
    expect(await componentHarness.isMessageHeadersDisabled()).toEqual(false);
    expect(await componentHarness.isMessageMetadataDisabled()).toEqual(false);
    expect(await componentHarness.isHeadersDisabled()).toEqual(false);
    await componentHarness.checkMessageContent();

    await componentHarness.toggleEnabled();

    expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(true);
    expect(await componentHarness.isEndpointDisabled()).toStrictEqual(true);
    expect(await componentHarness.isMessageContentDisabled()).toEqual(true);
    expect(await componentHarness.isMessageHeadersDisabled()).toEqual(true);
    expect(await componentHarness.isMessageMetadataDisabled()).toEqual(true);
    expect(await componentHarness.isHeadersDisabled()).toEqual(true);

    await componentHarness.toggleEnabled();

    expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
    expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
    expect(await componentHarness.isMessageContentDisabled()).toEqual(false);
    expect(await componentHarness.isMessageHeadersDisabled()).toEqual(false);
    expect(await componentHarness.isMessageMetadataDisabled()).toEqual(false);
    expect(await componentHarness.isHeadersDisabled()).toEqual(false);

    expect(await componentHarness.isEntrypointChecked()).toStrictEqual(true);
    expect(await componentHarness.isMessageContentChecked()).toEqual(true);
  });

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
          tracing: { enabled: true, verbose: false },
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
    await componentHarness.toggleEntrypoint();

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
          mode: { entrypoint: true, endpoint: false },
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
            mode: { entrypoint: true, endpoint: true },
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
          mode: { entrypoint: true, endpoint: true },
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
      expect(await componentHarness.getSamplingValue()).toStrictEqual(testApi.analytics.sampling.value);

      await componentHarness.addSamplingValue(null);
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('required')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value is required.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('5');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('min')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value should be greater than 40.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('42');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      expect(await componentHarness.samplingValueHasErrors()).toEqual(false);
      expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
    });

    it('should validate sampling value with PROBABILITY type', async () => {
      await initComponent();
      await componentHarness.choseSamplingType('Probabilistic');
      expect(await componentHarness.getSamplingType()).toStrictEqual('Probabilistic');

      await componentHarness.addSamplingValue(null);
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('required')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value is required.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('-1');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('min')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value should be greater than 0.01.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('42');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('max')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value should be lower than 0.52.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('0.3');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      expect(await componentHarness.samplingValueHasErrors()).toEqual(false);
      expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
    });

    it('should validate sampling value with TEMPORAL type', async () => {
      await initComponent();
      await componentHarness.choseSamplingType('Temporal');
      expect(await componentHarness.getSamplingType()).toStrictEqual('Temporal');

      await componentHarness.addSamplingValue(null);
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('required')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value is required.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('PT1S');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('minTemporal')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual(['The sampling value should be greater than PT10S.']);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('PT');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(true);
      expect(fixture.componentInstance.form.get('samplingValue').hasError('invalidISO8601Duration')).toBeTruthy();
      expect(await componentHarness.getSamplingValueErrors()).toEqual([
        'The sampling value should use ISO-8601 duration format, e.g. PT10S.',
      ]);
      expect(await componentHarness.isSaveButtonInvalid()).toBeTruthy();

      await componentHarness.addSamplingValue('PT11S');
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      expect(fixture.componentInstance.form.get('samplingValue').invalid).toBe(false);
      expect(await componentHarness.samplingValueHasErrors()).toEqual(false);
      expect(await componentHarness.isSaveButtonInvalid()).toBeFalsy();
    });
  });

  it('should reset the form', async () => {
    await initComponent();
    expect(await componentHarness.isEntrypointChecked()).toBe(false);
    expect(await componentHarness.isEndpointChecked()).toBe(false);

    await componentHarness.toggleEntrypoint();
    await componentHarness.toggleEndpoint();
    expect(await componentHarness.isEntrypointChecked()).toBe(true);
    expect(await componentHarness.isEndpointChecked()).toBe(true);

    await componentHarness.resetSettings();

    expect(await componentHarness.isEntrypointChecked()).toBe(false);
    expect(await componentHarness.isEndpointChecked()).toBe(false);
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

  function expectConsoleSettingsGetRequest(consoleSettingsResponse: ConsoleSettings) {
    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
    req.flush(consoleSettingsResponse);
    expect(req.request.method).toEqual('GET');
  }

  describe('ApiRuntimeLogsMessageSettingsComponent - OpenTelemetry settings', () => {
    const apiWithTracingDisabled = fakeApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        logging: {
          mode: { entrypoint: true, endpoint: true },
          phase: { request: true, response: true },
          content: { messagePayload: true, messageHeaders: true, messageMetadata: true, headers: true },
          condition: 'request condition',
          messageCondition: 'message condition',
        },
        tracing: {
          enabled: false,
          verbose: false,
        },
        sampling: { type: 'COUNT', value: '50' },
      },
    });

    const apiWithTracingEnabled = fakeApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        tracing: {
          enabled: true,
          verbose: true,
        },
      },
    });

    const apiWithTracingEnabledAndVerboseFalse = fakeApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        tracing: {
          enabled: true,
          verbose: false,
        },
      },
    });

    beforeEach(async () => {
      await initComponent(apiWithTracingEnabled);
    });

    it('should reflect the initial state of OpenTelemetry controls', async () => {
      expect(await componentHarness.isTracingEnabledChecked()).toStrictEqual(true);
      expect(await componentHarness.isTracingVerboseChecked()).toStrictEqual(true);
      expect(await componentHarness.isTracingVerboseDisabled()).toStrictEqual(false);
    });

    it('should disable tracingVerbose when tracingEnabled is toggled off from enabled state where tracingVerbose was enabled', async () => {
      expect(await componentHarness.isTracingEnabledChecked()).toBe(true);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(true);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(false);

      await componentHarness.toggleTracingEnabled();

      expect(await componentHarness.isTracingEnabledChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(true);
    });

    it('should disable tracingVerbose when tracingEnabled is toggled off from enabled state where tracingVerbose was disabled', async () => {
      await initComponent(apiWithTracingEnabledAndVerboseFalse);

      expect(await componentHarness.isTracingEnabledChecked()).toBe(true);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(false);

      await componentHarness.toggleTracingEnabled();

      expect(await componentHarness.isTracingEnabledChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(true);
    });

    it('should enable tracingVerbose when tracingEnabled is toggled on from disabled state, with tracingVerbose set to false', async () => {
      await initComponent(apiWithTracingDisabled);

      expect(await componentHarness.isTracingEnabledChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(true);

      await componentHarness.toggleTracingEnabled();

      expect(await componentHarness.isTracingEnabledChecked()).toBe(true);
      expect(await componentHarness.isTracingVerboseChecked()).toBe(false);
      expect(await componentHarness.isTracingVerboseDisabled()).toBe(false);
    });

    it('should save API proxy OpenTelemetry settings', async () => {
      await initComponent(apiWithTracingDisabled);

      await componentHarness.toggleTracingEnabled();
      await componentHarness.toggleTracingVerbose();

      await componentHarness.saveSettings();

      expectApiGetRequest(apiWithTracingDisabled);
      expectApiPutRequest({
        ...apiWithTracingDisabled,
        analytics: {
          ...apiWithTracingDisabled.analytics,
          tracing: {
            enabled: true,
            verbose: true,
          },
        },
      });
    });

    it('should discard changes in OpenTelemetry controls', async () => {
      await componentHarness.toggleTracingEnabled();
      await componentHarness.toggleTracingVerbose();

      expect(await componentHarness.isTracingEnabledChecked()).toStrictEqual(false);
      expect(await componentHarness.isTracingVerboseChecked()).toStrictEqual(false);

      await componentHarness.resetSettings();

      expect(await componentHarness.isTracingEnabledChecked()).toStrictEqual(true);
      expect(await componentHarness.isTracingVerboseChecked()).toStrictEqual(true);
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
});
