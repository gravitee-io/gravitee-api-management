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
import { ComponentFixture, discardPeriodicTasks, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { set } from 'lodash';

import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';
import { ApiCreationV4SpecStepperHelper } from './api-creation-v4-spec-stepper-helper';
import { ApiCreationV4Component } from './api-creation-v4.component';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step5SummaryHarness } from './steps/step-5-summary/step-5-summary.harness';
import { Step3EndpointListHarness } from './steps/step-3-endpoints/step-3-endpoints-1-list.harness';

import { Constants } from '../../../entities/Constants';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { AGENT_TO_AGENT } from '../../../entities/management-api-v2/api/v4/agentToAgent';

describe('ApiCreationV4Component - Message - Agent Proxy', () => {
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

  const agentToAgent: Partial<ConnectorPlugin> = {
    id: AGENT_TO_AGENT.id,
    supportedApiType: 'MESSAGE',
    name: AGENT_TO_AGENT.name,
    supportedListenerType: 'HTTP',
  };

  beforeEach(async () => await init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
    enabledReviewMode = false;
  });

  describe('Entrypoint and endpoint validation', () => {
    /* We save the A2A or the Agent proxy as MESSAGE only but if Protocol Mediation is selected,
    agent-to-agent entry/end point is not visible and vice versa. */

    it('should not show the agent-to-agent entrypoint when Protocol Mediation is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'HTTP' },
        { id: AGENT_TO_AGENT.id, supportedApiType: 'MESSAGE', name: AGENT_TO_AGENT.name, supportedListenerType: 'HTTP' },
      ]);

      const asyncList = await step2Harness.getAsyncEntrypoints();
      const values = await asyncList.getListValues();

      // agent-to-agent has been removed
      expect(values).not.toContain(AGENT_TO_AGENT.id);
      expect(values).toContain('sse');
    }));

    it('should select the agent-to-agent entrypoint automatically when Agent Proxy is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      // Select AI architecture
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('AI');
      // Select A2A and validate the expected entry/end point get request
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [agentToAgent]);
      fixture.detectChanges();
      // agent-to-agent entrypoint gets selected automatically
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        {
          icon: 'gio-literal:agent-to-agent',
          id: AGENT_TO_AGENT.id,
          name: AGENT_TO_AGENT.name,
          supportedListenerType: 'HTTP',
          deployed: true,
          selectedQos: 'NONE',
        },
      ]);
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([agentToAgent]);
      httpExpects.expectApiGetPortalConfiguration();
      flush();
      discardPeriodicTasks();
    }));

    it('should not show the agent-to-agent endpoint when Protocol Mediation is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'HTTP' }]);

      await step2Harness.getAsyncEntrypoints().then(form => form.selectOptionsByIds(['sse']));
      await step2Harness.clickValidate();
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      httpExpects.expectApiGetPortalConfiguration();
      tick(500);
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.fillPaths('/api/my-api-3');
      httpExpects.expectVerifyContextPath();
      expect(await step21Harness.hasValidationDisabled()).toBeFalsy();
      await step21Harness.clickValidate();
      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);

      httpExpects.expectEndpointsGetRequest([
        { id: AGENT_TO_AGENT.id, supportedApiType: 'MESSAGE', name: AGENT_TO_AGENT.name, supportedListenerType: 'HTTP' },
      ]);

      const asyncList = await step3Harness.getEndpoints();
      const values = await asyncList.getListValues();

      // agent-to-agent has been removed
      expect(values).not.toContain(AGENT_TO_AGENT.id);
    }));

    it('should select the agent-to-agent endpoint automatically when Agent Proxy is selected', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API name', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('AI');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [agentToAgent]);
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([agentToAgent]);
      await stepperHelper.fillAndValidateStep3_2_EndpointsConfig([agentToAgent]);
      // agent-to-agent endpoint gets selected automatically
      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        {
          icon: 'gio-literal:agent-to-agent',
          id: AGENT_TO_AGENT.id,
          name: AGENT_TO_AGENT.name,
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
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('AI', [agentToAgent]);
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([agentToAgent]);
      await stepperHelper.fillAndValidateStep3_2_EndpointsConfig([agentToAgent]);
      await stepperHelper.validateStep4_1_SecurityPlansList();

      discardPeriodicTasks();

      const step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      const step1Summary = await step5Harness.getStepSummaryTextContent(1);
      expect(step1Summary).toContain('API name:' + 'API');
      expect(step1Summary).toContain('Version:' + '1.0');
      expect(step1Summary).toContain('Description:' + ' Description');

      const step2Summary = await step5Harness.getStepSummaryTextContent(2);
      expect(step2Summary).toContain('Type:' + 'HTTP');
      expect(step2Summary).toContain('Entrypoints:' + ' Agent to agent');

      const step3Summary = await step5Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Endpoints' + 'Endpoints: ' + AGENT_TO_AGENT.name);

      const step4Summary = await step5Harness.getStepSummaryTextContent(4);
      expect(step4Summary).toContain('Default Keyless (UNSECURED)' + 'KEY_LESS');

      await step5Harness.clickCreateMyApiButton();
      httpExpects.expectCallsForApiAndPlanCreation('api-id', 'plan-id');
      flush();
    }));
  });
});
