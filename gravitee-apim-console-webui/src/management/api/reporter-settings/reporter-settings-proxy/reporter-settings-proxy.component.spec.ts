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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';

import { ReporterSettingsProxyHarness } from './reporter-settings-proxy.harness';
import { ReporterSettingsProxyComponent } from './reporter-settings-proxy.component';
import { ApiPayloadMaskingRulesComponent } from './api-payload-masking-rules/api-payload-masking-rules.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4, fakeProxyApiV4 } from '../../../../entities/management-api-v2';
import { ReporterSettingsComponent } from '../reporter-settings.component';

describe('ApiRuntimeLogsProxySettingsComponent', () => {
  const API_ID = 'api-id';
  let fixture: ComponentFixture<ReporterSettingsComponent>;
  let httpTestingController: HttpTestingController;
  let componentHarness: ReporterSettingsProxyHarness;

  const initComponent = async (api: ApiV4) => {
    fixture = TestBed.createComponent(ReporterSettingsComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ReporterSettingsProxyHarness);

    fixture.detectChanges();
    expectApiGetRequest(api);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ReporterSettingsComponent, MatIconTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } }],
    }).compileComponents();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('with an API', () => {
    const api = fakeProxyApiV4({ id: API_ID, analytics: { enabled: true, logging: { mode: { entrypoint: false, endpoint: false } } } });

    beforeEach(async () => {
      await initComponent(api);
    });

    it('should enable and disable all form fields according to analytics enabled', async () => {
      expect(await componentHarness.isEnabledChecked()).toStrictEqual(true);

      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
      await componentHarness.toggleEntrypoint();
      await checkLoggingFieldsEnabled();

      await componentHarness.toggleEnabled();

      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(true);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(true);
      await checkLoggingFieldsDisabled();

      await componentHarness.toggleEnabled();

      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isEntrypointChecked()).toStrictEqual(true);
      await checkLoggingFieldsEnabled();
    });

    it('should enable and disable form fields according to logging mode', async () => {
      expect(await componentHarness.isEntrypointChecked()).toStrictEqual(false);
      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isEndpointChecked()).toStrictEqual(false);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isRequestPhaseChecked()).toStrictEqual(false);
      expect(await componentHarness.isResponsePhaseChecked()).toStrictEqual(false);
      expect(await componentHarness.isHeadersChecked()).toStrictEqual(false);
      expect(await componentHarness.isPayloadChecked()).toStrictEqual(false);
      expect(await componentHarness.getCondition()).toStrictEqual('');
      await checkLoggingFieldsDisabled();

      await componentHarness.toggleEntrypoint();
      await checkLoggingFieldsEnabled();

      await componentHarness.toggleEntrypoint();
      await checkLoggingFieldsDisabled();

      await componentHarness.toggleEndpoint();
      await checkLoggingFieldsEnabled();
    });

    it('should save API proxy logging settings', async () => {
      await componentHarness.toggleEntrypoint();
      await componentHarness.toggleEndpoint();
      await componentHarness.toggleRequestPhase();
      await componentHarness.toggleResponsePhase();
      await componentHarness.toggleHeaders();
      await componentHarness.togglePayload();
      await componentHarness.setCondition('condition');

      await componentHarness.clickOnSaveButton();
      expectApiGetRequest(api);
      expectApiPutRequest({
        ...api,
        analytics: {
          ...api.analytics,
          logging: {
            mode: { entrypoint: true, endpoint: true },
            phase: { request: true, response: true },
            content: { headers: true, payload: true },
            condition: 'condition',
          },
          tracing: { enabled: false, verbose: false },
        },
      });
    });

    it('should init form with API logging values', async () => {
      const apiWithLogging = fakeProxyApiV4({
        id: API_ID,
        analytics: {
          enabled: true,
          logging: {
            mode: { entrypoint: true, endpoint: true },
            phase: { request: true, response: true },
            content: { headers: true, payload: true },
            condition: 'condition',
          },
        },
      });
      await initComponent(apiWithLogging);

      expect(await componentHarness.isEntrypointChecked()).toStrictEqual(true);
      expect(await componentHarness.isEndpointChecked()).toStrictEqual(true);
      expect(await componentHarness.isRequestPhaseChecked()).toStrictEqual(true);
      expect(await componentHarness.isResponsePhaseChecked()).toStrictEqual(true);
      expect(await componentHarness.isHeadersChecked()).toStrictEqual(true);
      expect(await componentHarness.isPayloadChecked()).toStrictEqual(true);
      expect(await componentHarness.getCondition()).toStrictEqual('condition');
    });

    it('should discard the changes', async () => {
      await componentHarness.toggleEntrypoint();
      await componentHarness.toggleEndpoint();
      await componentHarness.toggleRequestPhase();
      await componentHarness.toggleResponsePhase();
      await componentHarness.toggleHeaders();
      await componentHarness.togglePayload();
      await componentHarness.setCondition('condition');

      expect(await componentHarness.isEntrypointChecked()).toStrictEqual(true);
      expect(await componentHarness.isEndpointChecked()).toStrictEqual(true);
      expect(await componentHarness.isRequestPhaseChecked()).toStrictEqual(true);
      expect(await componentHarness.isResponsePhaseChecked()).toStrictEqual(true);
      expect(await componentHarness.isHeadersChecked()).toStrictEqual(true);
      expect(await componentHarness.isPayloadChecked()).toStrictEqual(true);
      expect(await componentHarness.getCondition()).toStrictEqual('condition');
      await checkLoggingFieldsEnabled();

      await componentHarness.clickOnResetButton();

      expect(await componentHarness.isEntrypointChecked()).toStrictEqual(false);
      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isEndpointChecked()).toStrictEqual(false);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(false);
      expect(await componentHarness.isRequestPhaseChecked()).toStrictEqual(false);
      expect(await componentHarness.isResponsePhaseChecked()).toStrictEqual(false);
      expect(await componentHarness.isHeadersChecked()).toStrictEqual(false);
      expect(await componentHarness.isPayloadChecked()).toStrictEqual(false);
      expect(await componentHarness.getCondition()).toStrictEqual('');
      await checkLoggingFieldsDisabled();
    });
  });

  describe('with an API published using the GKO', () => {
    const api = fakeProxyApiV4({
      id: API_ID,
      definitionContext: { origin: 'KUBERNETES' },
      analytics: { enabled: true, logging: { mode: { entrypoint: false, endpoint: false } } },
    });

    beforeEach(async () => {
      await initComponent(api);
    });

    it('should disable the fields when it is a kubernetes API', async () => {
      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(true);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(true);
      await checkLoggingFieldsDisabled();
    });
  });

  describe('with an API published using the GKO', () => {
    const api = fakeProxyApiV4({
      id: API_ID,
      definitionContext: { origin: 'KUBERNETES' },
      analytics: { enabled: true, logging: { mode: { entrypoint: false, endpoint: false } } },
    });

    beforeEach(async () => {
      await initComponent(api);
    });

    it('should disable the form fields when it is a kubernetes API', async () => {
      expect(await componentHarness.isEntrypointDisabled()).toStrictEqual(true);
      expect(await componentHarness.isEndpointDisabled()).toStrictEqual(true);
      await checkLoggingFieldsDisabled();
    });
  });

  function expectApiGetRequest(api: ApiV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
    fixture.detectChanges();
  }

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`, method: 'PUT' });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
    fixture.detectChanges();
  }

  async function checkLoggingFieldsDisabled() {
    expect(await componentHarness.isRequestPhaseDisabled()).toStrictEqual(true);
    expect(await componentHarness.isResponsePhaseDisabled()).toStrictEqual(true);
    expect(await componentHarness.isHeadersDisabled()).toStrictEqual(true);
    expect(await componentHarness.isPayloadDisabled()).toStrictEqual(true);
    expect(await componentHarness.isConditionDisabled()).toStrictEqual(true);
  }

  async function checkLoggingFieldsEnabled() {
    expect(await componentHarness.isRequestPhaseDisabled()).toStrictEqual(false);
    expect(await componentHarness.isResponsePhaseDisabled()).toStrictEqual(false);
    expect(await componentHarness.isHeadersDisabled()).toStrictEqual(false);
    expect(await componentHarness.isPayloadDisabled()).toStrictEqual(false);
    expect(await componentHarness.isConditionDisabled()).toStrictEqual(false);
  }

  describe('OpenTelemetry settings', () => {
    const apiWithTracingDisabled = fakeProxyApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        logging: {
          mode: { entrypoint: true, endpoint: true },
          phase: { request: true, response: true },
          content: { headers: true, payload: true },
          condition: 'condition',
        },
        tracing: {
          enabled: false,
          verbose: false,
        },
      },
    });

    const apiWithTracingEnabled = fakeProxyApiV4({
      id: API_ID,
      analytics: {
        enabled: true,
        tracing: {
          enabled: true,
          verbose: true,
        },
      },
    });

    const apiWithTracingEnabledAndVerboseFalse = fakeProxyApiV4({
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
      expect(await componentHarness.isTracingVerboseChecked()).toBe(true);
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

      await componentHarness.clickOnSaveButton();

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

    it('should include pending redaction rules when saving', async () => {
      const apiWithRules = fakeProxyApiV4({
        id: API_ID,
        analytics: {
          enabled: true,
          tracing: {
            enabled: true,
            verbose: true,
            redaction: {
              rules: [{ attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } }],
            },
          },
        },
      });
      await initComponent(apiWithRules);

      // Simulate the child component emitting new rules
      const proxyInstance = fixture.debugElement.query(By.directive(ReporterSettingsProxyComponent))
        .componentInstance as ReporterSettingsProxyComponent;
      proxyInstance.onRedactionRulesChange([
        { attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } },
        { attributeNamePattern: 'authorization', maskingStrategy: { type: 'FULL' } },
      ]);
      fixture.detectChanges();

      await componentHarness.clickOnSaveButton();

      expectApiGetRequest(apiWithRules);
      expectApiPutRequest({
        ...apiWithRules,
        analytics: {
          ...apiWithRules.analytics,
          logging: {
            condition: undefined,
            mode: { entrypoint: undefined, endpoint: undefined },
            phase: { request: undefined, response: undefined },
            content: { headers: undefined, payload: undefined },
          },
          tracing: {
            enabled: true,
            verbose: true,
            redaction: {
              defaultReplacement: undefined,
              rules: [
                { attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } },
                { attributeNamePattern: 'authorization', maskingStrategy: { type: 'FULL' } },
              ],
            },
          },
        },
      });
    });

    it('should discard changes in OpenTelemetry controls', async () => {
      await componentHarness.toggleTracingEnabled();
      await componentHarness.toggleTracingVerbose();

      expect(await componentHarness.isTracingEnabledChecked()).toStrictEqual(false);
      expect(await componentHarness.isTracingVerboseChecked()).toStrictEqual(true);

      await componentHarness.clickOnResetButton();

      expect(await componentHarness.isTracingEnabledChecked()).toStrictEqual(true);
      expect(await componentHarness.isTracingVerboseChecked()).toStrictEqual(true);
    });

    it('should preserve existing redaction block verbatim when tracing is disabled', async () => {
      const apiWithRulesAndTracingEnabled = fakeProxyApiV4({
        id: API_ID,
        analytics: {
          enabled: true,
          tracing: {
            enabled: true,
            verbose: false,
            redaction: {
              rules: [{ attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } }],
            },
          },
        },
      });
      await initComponent(apiWithRulesAndTracingEnabled);

      await componentHarness.toggleTracingEnabled();
      await componentHarness.clickOnSaveButton();

      expectApiGetRequest(apiWithRulesAndTracingEnabled);
      expectApiPutRequest({
        ...apiWithRulesAndTracingEnabled,
        analytics: {
          ...apiWithRulesAndTracingEnabled.analytics,
          logging: {
            condition: undefined,
            mode: { entrypoint: undefined, endpoint: undefined },
            phase: { request: undefined, response: undefined },
            content: { headers: undefined, payload: undefined },
          },
          tracing: {
            enabled: false,
            verbose: false,
            redaction: {
              rules: [{ attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } }],
            },
          },
        },
      });
    });

    it('should not include a redaction key when tracing is enabled but no rules exist and none are pending', async () => {
      const apiTracingEnabledNoRedaction = fakeProxyApiV4({
        id: API_ID,
        analytics: {
          enabled: true,
          tracing: {
            enabled: true,
            verbose: false,
          },
        },
      });
      await initComponent(apiTracingEnabledNoRedaction);

      await componentHarness.toggleTracingVerbose();
      await componentHarness.clickOnSaveButton();

      expectApiGetRequest(apiTracingEnabledNoRedaction);
      expectApiPutRequest({
        ...apiTracingEnabledNoRedaction,
        analytics: {
          ...apiTracingEnabledNoRedaction.analytics,
          logging: {
            condition: undefined,
            mode: { entrypoint: undefined, endpoint: undefined },
            phase: { request: undefined, response: undefined },
            content: { headers: undefined, payload: undefined },
          },
          tracing: {
            enabled: true,
            verbose: true,
          },
        },
      });
    });

    describe('Span Attribute Redaction section visibility', () => {
      it('should hide redaction section when verbose is false (even if tracing is enabled)', async () => {
        await initComponent(apiWithTracingEnabledAndVerboseFalse);
        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).toBeNull();
      });

      it('should show redaction section when both tracing and verbose are enabled', async () => {
        await initComponent(apiWithTracingEnabled);
        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).not.toBeNull();
      });

      it('should hide redaction section after toggling verbose off', async () => {
        await initComponent(apiWithTracingEnabled);
        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).not.toBeNull();

        await componentHarness.toggleTracingVerbose();
        fixture.detectChanges();

        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).toBeNull();
      });

      it('should hide redaction section after toggling tracing off (verbose value is still true)', async () => {
        await initComponent(apiWithTracingEnabled);
        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).not.toBeNull();

        await componentHarness.toggleTracingEnabled();
        fixture.detectChanges();

        expect(fixture.debugElement.query(By.css('api-redaction-rules'))).toBeNull();
      });
    });

    it('should clear pending redaction rules and increment reset counter on discard', async () => {
      const proxyInstance = fixture.debugElement.query(By.directive(ReporterSettingsProxyComponent))
        .componentInstance as ReporterSettingsProxyComponent;

      proxyInstance.onRedactionRulesChange([{ attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } }]);
      fixture.detectChanges();

      expect((proxyInstance as any).pendingRedactionRules()).toEqual([
        { attributeNamePattern: 'api-key', maskingStrategy: { type: 'FULL' } },
      ]);
      const counterBefore = (proxyInstance as any).resetTriggerCounter();

      await componentHarness.clickOnResetButton();

      expect((proxyInstance as any).pendingRedactionRules()).toBeNull();
      expect((proxyInstance as any).resetTriggerCounter()).toBe(counterBefore + 1);
    });

    it('should include pending payload masking rules when saving', async () => {
      const apiWithPayloadMasking = fakeProxyApiV4({
        id: API_ID,
        analytics: {
          enabled: true,
          tracing: {
            enabled: true,
            verbose: false,
            payloadMasking: {
              rules: [{ path: '$.password', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' }],
            },
          },
        },
      });
      await initComponent(apiWithPayloadMasking);

      const proxyInstance = fixture.debugElement.query(By.directive(ReporterSettingsProxyComponent))
        .componentInstance as ReporterSettingsProxyComponent;
      proxyInstance.onPayloadMaskingRulesChange([
        { path: '$.password', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' },
        { path: '$.token', maskingStrategy: { type: 'FULL' }, phase: 'REQUEST', format: 'AUTO' },
      ]);
      fixture.detectChanges();

      await componentHarness.clickOnSaveButton();

      expectApiGetRequest(apiWithPayloadMasking);
      expectApiPutRequest({
        ...apiWithPayloadMasking,
        analytics: {
          ...apiWithPayloadMasking.analytics,
          logging: {
            condition: undefined,
            mode: { entrypoint: undefined, endpoint: undefined },
            phase: { request: undefined, response: undefined },
            content: { headers: undefined, payload: undefined },
          },
          tracing: {
            enabled: true,
            verbose: false,
            payloadMasking: {
              defaultReplacement: undefined,
              rules: [
                { path: '$.password', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' },
                { path: '$.token', maskingStrategy: { type: 'FULL' }, phase: 'REQUEST', format: 'AUTO' },
              ],
            },
          },
        },
      });
    });

    it('should clear pending payload masking rules on discard', async () => {
      const proxyInstance = fixture.debugElement.query(By.directive(ReporterSettingsProxyComponent))
        .componentInstance as ReporterSettingsProxyComponent;

      proxyInstance.onPayloadMaskingRulesChange([{ path: '$.password', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' }]);
      fixture.detectChanges();

      expect((proxyInstance as any).pendingPayloadMaskingRules()).toHaveLength(1);

      await componentHarness.clickOnResetButton();

      expect((proxyInstance as any).pendingPayloadMaskingRules()).toBeNull();
    });

    describe('Payload Masking section visibility', () => {
      it('should show payload masking section when tracing is enabled (regardless of verbose)', async () => {
        await initComponent(apiWithTracingEnabledAndVerboseFalse);
        expect(fixture.debugElement.query(By.directive(ApiPayloadMaskingRulesComponent))).not.toBeNull();
      });

      it('should show payload masking section when tracing and verbose are both enabled', async () => {
        await initComponent(apiWithTracingEnabled);
        expect(fixture.debugElement.query(By.directive(ApiPayloadMaskingRulesComponent))).not.toBeNull();
      });

      it('should hide payload masking section when tracing is disabled', async () => {
        await initComponent(apiWithTracingDisabled);
        expect(fixture.debugElement.query(By.directive(ApiPayloadMaskingRulesComponent))).toBeNull();
      });

      it('should hide payload masking section after toggling tracing off', async () => {
        await initComponent(apiWithTracingEnabled);
        expect(fixture.debugElement.query(By.directive(ApiPayloadMaskingRulesComponent))).not.toBeNull();

        await componentHarness.toggleTracingEnabled();
        fixture.detectChanges();

        expect(fixture.debugElement.query(By.directive(ApiPayloadMaskingRulesComponent))).toBeNull();
      });
    });
  });
});
