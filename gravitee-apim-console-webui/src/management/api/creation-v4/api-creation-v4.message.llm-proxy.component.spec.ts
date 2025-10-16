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
import { ConnectorPlugin } from 'src/entities/management-api-v2';

import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, discardPeriodicTasks, fakeAsync, flush, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { set } from 'lodash';

import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';
import { ApiCreationV4SpecStepperHelper } from './api-creation-v4-spec-stepper-helper';
import { ApiCreationV4Component } from './api-creation-v4.component';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step5SummaryHarness } from './steps/step-5-summary/step-5-summary.harness';

import { Constants } from '../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';

describe('ApiCreationV4Component - PROXY - LLM Proxy', () => {
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

  const llmConnector: Partial<ConnectorPlugin> = {
    id: 'llm-proxy',
    supportedApiType: 'LLM_PROXY',
    name: 'LLM Proxy',
    supportedListenerType: 'HTTP',
  };

  beforeEach(async () => await init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
    enabledReviewMode = false;
  });

  describe('Entrypoint and endpoint validation', () => {
    it('should select the LLM entrypoint automatically when AI is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      // Select AI architecture
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('AI');
      // Select LLM and validate the expected entry/end point get request
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [llmConnector]);
      fixture.detectChanges();
      // LLM entrypoint gets selected automatically
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        {
          icon: 'gio-literal:llm-proxy',
          id: llmConnector.id,
          name: llmConnector.name,
          supportedListenerType: 'HTTP',
          deployed: true,
        },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([llmConnector]);
      httpExpects.expectApiGetPortalConfiguration();
      flush();
      discardPeriodicTasks();
    }));

    it('should select the llm endpoint automatically when LLM Proxy is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API name', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('AI');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [llmConnector]);
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([llmConnector]);
      await stepperHelper.fillAndValidateStep3_2_EndpointsConfig([llmConnector]);
      // llm-proxy' endpoint gets selected automatically
      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        {
          icon: 'gio-literal:llm-proxy',
          id: llmConnector.id,
          name: llmConnector.name,
          configuration: {},
          sharedConfiguration: {},
          deployed: true,
        },
      ]);
      flush();
      discardPeriodicTasks();
    }));
  });

  describe('API Creation', () => {
    it('should create the API', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API name', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('AI');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [llmConnector]);
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([llmConnector]);
      await stepperHelper.fillAndValidateStep3_2_EndpointsConfig([llmConnector]);
      await stepperHelper.validateStep4_1_SecurityPlansList();

      discardPeriodicTasks();

      const step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      const step1Summary = await step5Harness.getStepSummaryTextContent(1);
      expect(step1Summary).toContain('API name:' + 'API');
      expect(step1Summary).toContain('Version:' + '1.0');
      expect(step1Summary).toContain('Description:' + ' Description');

      const step2Summary = await step5Harness.getStepSummaryTextContent(2);
      expect(step2Summary).toContain('Type:' + 'HTTP');
      expect(step2Summary).toContain('Entrypoints:' + ' LLM Proxy');

      const step3Summary = await step5Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Endpoints' + 'Endpoints: ' + llmConnector.name);

      const step4Summary = await step5Harness.getStepSummaryTextContent(4);
      expect(step4Summary).toContain('Default Keyless (UNSECURED)' + 'KEY_LESS');

      await step5Harness.clickCreateMyApiButton();
      httpExpects.expectCallsForApiAndPlanCreation('api-id', 'plan-id');
      flush();
    }));
  });
});
