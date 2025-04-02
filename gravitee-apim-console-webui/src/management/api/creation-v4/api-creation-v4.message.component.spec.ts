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
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { set } from 'lodash';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step5SummaryHarness } from './steps/step-5-summary/step-5-summary.harness';
import { ApiCreationV4SpecStepperHelper } from './api-creation-v4-spec-stepper-helper';
import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeRestrictedDomains } from '../../../entities/restricted-domain/restrictedDomain.fixture';
import { Constants } from '../../../entities/Constants';

describe('ApiCreationV4Component - Message', () => {
  let fixture: ComponentFixture<ApiCreationV4Component>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let component: ApiCreationV4Component;

  let enabledReviewMode = false;
  let httpExpects: ApiCreationV4SpecHttpExpects;
  let stepperHelper: ApiCreationV4SpecStepperHelper;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiCreationV4Component],
      providers: [
        {
          provide: Constants,
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', {
              apikey: {
                enabled: true,
              },
              jwt: {
                enabled: true,
              },
              keyless: {
                enabled: true,
              },
              oauth2: {
                enabled: true,
              },
              customApiKey: {
                enabled: true,
              },
              sharedApiKey: {
                enabled: true,
              },
              push: {
                enabled: true,
              },
            });

            set(constants, 'env.settings.apiReview', {
              get enabled() {
                return enabledReviewMode;
              },
            });
            return constants;
          },
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
      ],
      imports: [NoopAnimationsModule, ApiCreationV4Module, GioTestingModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiCreationV4Component);
    httpTestingController = TestBed.inject(HttpTestingController);

    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
    httpExpects = new ApiCreationV4SpecHttpExpects(httpTestingController);
    stepperHelper = new ApiCreationV4SpecStepperHelper(harnessLoader, httpExpects, httpTestingController);
  };

  beforeEach(async () => await init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
    enabledReviewMode = false;
  });

  describe('Entrypoint validation', () => {
    it('should not display context-path form for non http supportedListenerType', async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'SUBSCRIPTION' },
      ]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(await step21Harness.hasListenersForm()).toEqual(false);
    });

    it('should not validate without path', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(await step21Harness.hasListenersForm()).toEqual(true);
      expect(await step21Harness.hasValidationDisabled()).toEqual(true);
      httpExpects.expectVerifyContextPath();
    }));

    it('should not validate with bad path', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      const step22Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step22Harness.fillPaths('bad-path');
      expect(await step22Harness.hasValidationDisabled()).toEqual(true);
      httpExpects.expectVerifyContextPath();
    }));

    it('should configure paths', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'HTTP' },
        { id: 'webhook', supportedApiType: 'MESSAGE', name: 'Webhook', supportedListenerType: 'SUBSCRIPTION' },
      ]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
        { icon: 'gio-literal:webhook', id: 'webhook', name: 'Webhook', supportedListenerType: 'SUBSCRIPTION', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([
        { id: 'sse', name: 'SSE' },
        { id: 'webhook', name: 'Webhook' },
      ]);
      httpExpects.expectApiGetPortalConfiguration();
      tick(500);

      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.fillPaths('/api/my-api-3');
      httpExpects.expectVerifyContextPath();
      expect(await step21Harness.hasValidationDisabled()).toBeFalsy();
      await step21Harness.clickValidate();

      expect(component.currentStep.payload.paths).toEqual([
        {
          path: '/api/my-api-3',
        },
      ]);

      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        {
          configuration: {
            headersAsComment: false,
            heartbeatIntervalInMs: 5000,
            metadataAsComment: false,
          },
          id: 'sse',
          name: 'SSE',
          icon: 'gio-literal:sse',
          supportedListenerType: 'HTTP',
          selectedQos: 'AUTO',
          deployed: true,
        },
        {
          configuration: {
            http: {
              connectTimeout: 3000,
              idleTimeout: 60000,
              maxConcurrentConnections: 5,
              readTimeout: 10000,
            },
            proxy: {
              enabled: false,
              useSystemProxy: false,
            },
          },
          id: 'webhook',
          name: 'Webhook',
          icon: 'gio-literal:webhook',
          supportedListenerType: 'SUBSCRIPTION',
          selectedQos: 'AUTO',
          deployed: true,
        },
      ]);

      httpExpects.expectEndpointsGetRequest([]);

      flush();
    }));

    it('should not validate with empty host', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.clickListenerType();
      httpExpects.expectApiGetPortalConfiguration();

      await step21Harness.fillVirtualHosts({ host: '', path: '/api/my-api-3' });
      httpExpects.expectVerifyContextPath();
      expect(await step21Harness.hasValidationDisabled()).toEqual(true);
      await step21Harness.clickValidate();
    }));

    it('should configure virtual host', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'HTTP' }]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.clickListenerType();
      httpExpects.expectApiGetPortalConfiguration();

      await step21Harness.fillVirtualHosts({ host: 'hostname', path: '/api/my-api-3' });
      httpExpects.expectVerifyContextPath();
      await step21Harness.clickValidate();

      expect(component.currentStep.payload.paths).toEqual([
        {
          host: 'hostname',
          overrideAccess: false,
          path: '/api/my-api-3',
        },
      ]);

      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        {
          configuration: {
            headersAsComment: false,
            heartbeatIntervalInMs: 5000,
            metadataAsComment: false,
          },
          id: 'sse',
          name: 'SSE',
          icon: 'gio-literal:sse',
          supportedListenerType: 'HTTP',
          selectedQos: 'AUTO',
          deployed: true,
        },
      ]);

      httpExpects.expectEndpointsGetRequest([]);

      flush();
    }));

    it('should allow to disable virtual host when domain restrictions are set', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

      await step2Harness.getAsyncEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest(fakeRestrictedDomains(['domain.com', 'domain.net']));
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      httpExpects.expectVerifyContextPath();

      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(await step21Harness.canSwitchListenerMode()).toEqual(true);
    }));
  });

  describe('API Creation', () => {
    it('should create the API', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails();
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture();
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
      await stepperHelper.fillAndValidateStep3_1_EndpointsList();
      await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
      await stepperHelper.editAndValidateStep4_1_SecurityPlansList();

      const step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      const step1Summary = await step5Harness.getStepSummaryTextContent(1);
      expect(step1Summary).toContain('API name:' + 'API');
      expect(step1Summary).toContain('Version:' + '1.0');
      expect(step1Summary).toContain('Description:' + ' description');

      const step2Summary = await step5Harness.getStepSummaryTextContent(2);
      expect(step2Summary).toContain('Path:' + '/api/my-api-3');
      expect(step2Summary).toContain('Type:' + 'HTTP' + 'SUBSCRIPTION');
      expect(step2Summary).toContain('Entrypoints:' + ' initial entrypoint' + ' new entrypoint');

      const step3Summary = await step5Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Endpoints' + 'Endpoints: ' + 'Kafka ' + ' Mock');

      const step4Summary = await step5Harness.getStepSummaryTextContent(4);
      expect(step4Summary).toContain('Update name' + 'KEY_LESS');
    }));
  });
});
