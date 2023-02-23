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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { Step1ApiDetailsHarness } from './steps/step-1-api-details/step-1-api-details.harness';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step6SummaryHarness } from './steps/step-6-summary/step-6-summary.harness';
import { Step3EndpointListHarness } from './steps/step-3-endpoints/step-3-endpoints-1-list.harness';
import { Step4SecurityHarness } from './steps/step-4-security/step-4-security.harness';
import { Step5DocumentationHarness } from './steps/step-5-documentation/step-5-documentation.harness';
import { Step2Entrypoints2ConfigComponent } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.component';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step3Endpoints2ConfigHarness } from './steps/step-3-endpoints/step-3-endpoints-2-config.harness';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { Step2Entrypoints1ListComponent } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.component';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ConnectorListItem } from '../../../../entities/connector/connector-list-item';
import { fakeConnectorListItem, getEntrypointConnectorSchema } from '../../../../entities/connector/connector-list-item.fixture';
import { fakeApiEntity } from '../../../../entities/api-v4';
import { PortalSettings } from '../../../../entities/portal/portalSettings';
import { Environment } from '../../../../entities/environment/environment';
import { fakeEnvironment } from '../../../../entities/environment/environment.fixture';

describe('ApiCreationV4Component', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };

  let fixture: ComponentFixture<ApiCreationV4Component>;
  let component: ApiCreationV4Component;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiCreationV4Component],
      providers: [{ provide: UIRouterState, useValue: fakeAjsState }],
      imports: [NoopAnimationsModule, ApiCreationV4Module, GioHttpTestingModule, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiCreationV4Component);
    httpTestingController = TestBed.inject(HttpTestingController);
    component = fixture.componentInstance;
    fixture.detectChanges();
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(async () => await init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('menu', () => {
    it('should have 6 steps', (done) => {
      component.stepper.goToNextStep({ groupNumber: 1, component: Step1ApiDetailsComponent });
      component.stepper.validStep(() => ({ name: 'A1' }));
      component.stepper.goToNextStep({ groupNumber: 2, component: Step2Entrypoints1ListComponent });
      component.stepper.validStep(({ name }) => ({ name: `${name}>B1` }));
      component.stepper.goToNextStep({ groupNumber: 2, component: Step2Entrypoints2ConfigComponent });
      component.stepper.validStep(({ name }) => ({ name: `${name}>B2` }));

      component.menuSteps$.subscribe((menuSteps) => {
        expect(menuSteps.length).toEqual(6);
        expect(menuSteps[0].label).toEqual('API details');
        expect(menuSteps[1].label).toEqual('Entrypoints');
        // Expect to have the last valid step payload
        expect(menuSteps[1].payload).toEqual({ name: 'A1>B1>B2' });
        expect(menuSteps[2].label).toEqual('Endpoints');
        expect(menuSteps[3].label).toEqual('Security');
        expect(menuSteps[4].label).toEqual('Documentation');
        expect(menuSteps[5].label).toEqual('Summary');
        done();
      });
    });
  });

  describe('step1', () => {
    it('should start with step 1', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
    });

    it('should save api details and move to next step', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
      expect(component.currentStep.group.label).toEqual('API details');
      await step1Harness.fillAndValidate('test API', '1', 'description');
      expectEntrypointsGetRequest([]);

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'test API', version: '1', description: 'description' });
      expect(component.currentStep.group.label).toEqual('Entrypoints');
    });

    it('should save api details and move to next step (description is optional)', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
      expect(component.currentStep.group.label).toEqual('API details');
      await step1Harness.setName('API');
      await step1Harness.setVersion('1.0');
      await step1Harness.clickValidate();
      expectEntrypointsGetRequest([]);

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'API', version: '1.0', description: '' });
      expect(component.currentStep.group.label).toEqual('Entrypoints');
    });

    it('should exit without confirmation when no modification', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
      await step1Harness.clickExit();

      fixture.detectChanges();
      expect(fakeAjsState.go).toHaveBeenCalled();
    });

    it('should cancel exit in confirmation', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      await step1Harness.setName('Draft API');
      await step1Harness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.cancel();
      expect(component.currentStep.group.label).toEqual('API details');

      fixture.detectChanges();
      expect(fakeAjsState.go).not.toHaveBeenCalled();
    });

    it('should not save data after exiting', async () => {
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      await step1Harness.setName('Draft API');

      await step1Harness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.confirm();
      expect(component.currentStep.payload).toEqual({});

      fixture.detectChanges();
      expect(fakeAjsState.go).toHaveBeenCalled();
    });
  });

  describe('step2', () => {
    it('should go back to step1 with API details restored when clicking on previous', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
      expectEntrypointsGetRequest([]);

      await step2Harness.clickPrevious();
      expect(component.currentStep.group.label).toEqual('API details');

      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
      expect(await step1Harness.getName()).toEqual('API');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('Description');
    });

    it('should display only async entrypoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'async', name: 'SSE' },
        { id: 'webhook', supportedApiType: 'async', name: 'Webhook' },
        { id: 'http', supportedApiType: 'sync', name: 'HTTP' },
      ]);

      const list = await step2Harness.getEntrypoints();
      expect(await list.getListValues()).toEqual(['sse', 'webhook']);
    });

    it('should select entrypoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'async', name: 'SSE' },
        { id: 'webhook', supportedApiType: 'async', name: 'Webhook' },
      ]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { id: 'sse', name: 'SSE', supportedListenerType: 'http' },
        { id: 'webhook', name: 'Webhook', supportedListenerType: 'http' },
      ]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([
        { id: 'sse', name: 'SSE' },
        { id: 'webhook', name: 'Webhook' },
      ]);
      expectApiGetPortalSettings();
    });

    it('should not validate with bad path', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'async', name: 'SSE' }]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([{ id: 'sse', name: 'SSE', supportedListenerType: 'http' }]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      expectApiGetPortalSettings();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.fillPathsAndValidate('bad-path');
      expect(await step21Harness.hasValidationDisabled()).toEqual(true);
    });

    it('should configure paths', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'async', name: 'SSE' }]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([{ id: 'sse', name: 'SSE', supportedListenerType: 'http' }]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      expectApiGetPortalSettings();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.fillPathsAndValidate('/api/my-api-3');
      expectVerifyContextPathGetRequest();

      expect(component.currentStep.payload.listeners).toEqual([
        {
          entrypoints: [
            {
              configuration: {
                headersAsComment: false,
                heartbeatIntervalInMs: 5000,
                metadataAsComment: false,
              },
              type: 'sse',
            },
          ],
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          type: 'http',
        },
      ]);

      expectEndpointsGetRequest([]);
    });

    it('should not validate with empty host', async () => {
      await fillAndValidateStep1ApiDetails('API', '3.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'async', name: 'SSE' }]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([{ id: 'sse', name: 'SSE', supportedListenerType: 'http' }]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      expectApiGetPortalSettings();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.clickListenerType();
      expectApiGetPortalSettings();

      await step21Harness.fillVirtualHostsAndValidate({ host: '', path: '/api/my-api-3' });
      expect(await step21Harness.hasValidationDisabled()).toEqual(true);
    });

    it('should configure virtual host', async () => {
      await fillAndValidateStep1ApiDetails('API', '3.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'async', name: 'SSE' }]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([{ id: 'sse', name: 'SSE', supportedListenerType: 'http' }]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
      expectApiGetPortalSettings();
      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.clickListenerType();
      expectApiGetPortalSettings();

      await step21Harness.fillVirtualHostsAndValidate({ host: 'hostname', path: '/api/my-api-3' });
      expectVerifyContextPathGetRequest();

      expect(component.currentStep.payload.listeners).toEqual([
        {
          entrypoints: [
            {
              configuration: {
                headersAsComment: false,
                heartbeatIntervalInMs: 5000,
                metadataAsComment: false,
              },
              type: 'sse',
            },
          ],
          paths: [
            {
              host: 'hostname',
              overrideAccess: false,
              path: '/api/my-api-3',
            },
          ],
          type: 'http',
        },
      ]);

      expectEndpointsGetRequest([]);
    });

    it('should configure entrypoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '2.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'async', name: 'SSE' },
        { id: 'webhook', supportedApiType: 'async', name: 'Webhook' },
      ]);

      await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

      await step2Harness.clickValidate();
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { id: 'sse', name: 'SSE', supportedListenerType: 'http' },
        { id: 'webhook', name: 'Webhook', supportedListenerType: 'http' },
      ]);
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([
        { id: 'sse', name: 'SSE' },
        { id: 'webhook', name: 'Webhook' },
      ]);
      expectApiGetPortalSettings();

      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      await step21Harness.fillPathsAndValidate('/api/my-api-3');
      expectVerifyContextPathGetRequest();
      await step21Harness.clickValidate();

      expect(component.currentStep.payload.listeners).toEqual([
        {
          entrypoints: [
            {
              configuration: {
                headersAsComment: false,
                heartbeatIntervalInMs: 5000,
                metadataAsComment: false,
              },
              type: 'sse',
            },
            {
              configuration: {},
              type: 'webhook',
            },
          ],
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          type: 'http',
        },
      ]);

      expectEndpointsGetRequest([]);
    });
  });

  describe('step3', () => {
    it('should go back to step2 with API details restored when clicking on previous', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expectEndpointsGetRequest([]);

      await step3Harness.clickPrevious();

      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(step21Harness).toBeDefined();
      exceptEnvironmentGetRequest(fakeEnvironment());
      expectSchemaGetRequest([
        { id: 'entrypoint-1', name: 'initial entrypoint' },
        { id: 'entrypoint-2', name: 'new entrypoint' },
      ]);
      expectApiGetPortalSettings();
      expectVerifyContextPathGetRequest();
      expect(component.currentStep.payload.listeners).toEqual([
        {
          entrypoints: [
            {
              configuration: {
                headersInPayload: false,
                messagesLimitCount: 500,
                messagesLimitDurationMs: 5000,
                metadataInPayload: false,
              },
              type: 'entrypoint-1',
            },
            {
              configuration: {
                headersInPayload: false,
                messagesLimitCount: 500,
                messagesLimitDurationMs: 5000,
                metadataInPayload: false,
              },
              type: 'entrypoint-2',
            },
          ],
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          type: 'http',
        },
      ]);
    });

    it('should display only async endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();
      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);

      expectEndpointsGetRequest([
        { id: 'http-post', supportedApiType: 'sync', name: 'HTTP Post' },
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      const list = await step3Harness.getEndpoints();
      expect(await list.getListValues()).toEqual(['kafka', 'mock']);
    });

    it('should select endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3Harness).toBeTruthy();

      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      fixture.detectChanges();

      await step3Harness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);

      expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );
    });

    it('should configure endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3Harness).toBeTruthy();

      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      fixture.detectChanges();

      await step3Harness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);

      expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );

      const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
      await step3Endpoints2ConfigHarness.clickValidate();

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        {
          configuration: {
            headersInPayload: false,
            messagesLimitCount: 500,
            messagesLimitDurationMs: 5000,
            metadataInPayload: false,
          },
          id: 'kafka',
          name: 'Kafka',
        },
        {
          configuration: {
            headersInPayload: false,
            messagesLimitCount: 500,
            messagesLimitDurationMs: 5000,
            metadataInPayload: false,
          },
          id: 'mock',
          name: 'Mock',
        },
      ]);
    });
  });

  describe('step6', () => {
    beforeEach(async () => {
      await fillAndValidateStep1ApiDetails();
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();
      await fillAndValidateStep3Endpoints1List();
      await fillAndValidateStep3Endpoints2Config();
      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();
      fixture.detectChanges();
    });

    it('should display payload info', async () => {
      const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);

      const step1Summary = await step6Harness.getStepSummaryTextContent(1);
      expect(step1Summary).toContain('API name:' + 'API name');
      expect(step1Summary).toContain('Version:' + '1.0');
      expect(step1Summary).toContain('Description:' + ' description');

      const step2Summary = await step6Harness.getStepSummaryTextContent(2);
      expect(step2Summary).toContain('Path:' + '/my-new-api');
      expect(step2Summary).toContain('Type:' + 'Subscription');
      expect(step2Summary).toContain('EntrypointsPath:/my-new-apiType:SubscriptionEntrypoints');

      const step3Summary = await step6Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Field' + 'Value');
      expect(step3Summary).toContain('Kafka');
      expect(step3Summary).toContain('Mock');
    });

    it('should go back to step 1 after clicking Change button', async () => {
      const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickChangeButton(1);

      fixture.detectChanges();
      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(await step1Harness.getName()).toEqual('API name');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('description');
    });

    it('should go back to step 2 after clicking Change button', async () => {
      let step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickChangeButton(2);
      fixture.detectChanges();

      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
      expectEntrypointsGetRequest([
        { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
        { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
      ]);

      const list = await step2Harness.getEntrypoints();
      expect(await list.getListValues({ selected: true })).toEqual(['entrypoint-1', 'entrypoint-2']);
      await list.deselectOptionByValue('entrypoint-1');
      fixture.detectChanges();

      await step2Harness.clickValidate();
      fixture.detectChanges();
      await fillAndValidateStep2Entrypoints2Config([{ id: 'entrypoint-2', name: 'new entrypoint' }], ['/my-api/v4']);

      await fillAndValidateStep3Endpoints1List();
      await fillAndValidateStep3Endpoints2Config();
      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();

      // Reinitialize step6Harness after last step validation
      step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(2);

      expect(step2Summary).toContain('EntrypointsPath:/my-new-apiType:SubscriptionEntrypoints: new entrypoint\nChange');
    });

    it('should go back to step 3 after clicking Change button', async () => {
      let step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickChangeButton(3);
      fixture.detectChanges();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      const list = await step3Harness.getEndpoints();

      expect(await list.getListValues({ selected: true })).toEqual(['kafka', 'mock']);
      await list.deselectOptionByValue('kafka');
      expect(await list.getListValues({ selected: true })).toEqual(['mock']);
      fixture.detectChanges();
      await step3Harness.clickValidate();

      await fillAndValidateStep3Endpoints2Config([{ id: 'mock', supportedApiType: 'async', name: 'Mock' }]);

      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();

      // Reinitialize step6Harness after step2 validation
      step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(3);

      expect(step2Summary).toContain('EndpointsFieldValueEndpoints: Mock\nChange');
    });

    it('should go to confirmation page after clicking Create my API', async () => {
      const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickCreateMyApiButton();

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis`, method: 'POST' });

      // Todo: complete with all the expected fields
      expect(req.request.body).toEqual(
        expect.objectContaining({
          name: 'API name',
        }),
      );
      req.flush(fakeApiEntity({ id: 'api-id' }));

      expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.create-v4-confirmation', { apiId: 'api-id' });
    });

    it('should go to confirmation page after clicking Deploy my API', async () => {
      const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickDeployMyApiButton();

      const req = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/apis`, method: 'POST' });

      // Todo: complete with all the expected fields
      expect(req.request.body).toEqual(
        expect.objectContaining({
          name: 'API name',
        }),
      );
      req.flush(fakeApiEntity({ id: 'api-id' }));

      expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.create-v4-confirmation', { apiId: 'api-id' });
    });
  });

  function expectEntrypointsGetRequest(connectors: Partial<ConnectorListItem>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorListItem(partial));

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/entrypoints`, method: 'GET' }).flush(fullConnectors);
  }

  function expectSchemaGetRequest(connectors: Partial<ConnectorListItem>[], connectorType: 'entrypoints' | 'endpoints' = 'entrypoints') {
    connectors.forEach((connector) => {
      httpTestingController
        .expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/${connectorType}/${connector.id}/schema`, method: 'GET' })
        .flush(getEntrypointConnectorSchema(connector.id));
    });
  }

  function expectEndpointsGetRequest(connectors: Partial<ConnectorListItem>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorListItem(partial));

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/endpoints`, method: 'GET' }).flush(fullConnectors);
  }

  async function fillAndValidateStep1ApiDetails(name = 'API name', version = '1.0', description = 'description') {
    const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
    await step1Harness.fillAndValidate(name, version, description);
  }

  async function fillAndValidateStep2Entrypoints1List(
    entrypoints: Partial<ConnectorListItem>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
    ],
  ) {
    const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
    expectEntrypointsGetRequest(entrypoints);

    await step2Harness.fillAndValidate(entrypoints.map((entrypoint) => entrypoint.id));
  }

  async function fillAndValidateStep2Entrypoints2Config(
    entrypoints: Partial<ConnectorListItem>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
    ],
    paths: string[] = ['/api/my-api-3'],
  ) {
    const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
    exceptEnvironmentGetRequest(fakeEnvironment());
    expectSchemaGetRequest(entrypoints);
    expectApiGetPortalSettings();

    await step21Harness.fillPathsAndValidate(...paths);
    expectVerifyContextPathGetRequest();
  }

  async function fillAndValidateStep3Endpoints1List(
    endpoints: Partial<ConnectorListItem>[] = [
      { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'async', name: 'Mock' },
    ],
  ) {
    const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
    expectEndpointsGetRequest(endpoints);

    await step3Harness.fillAndValidate(endpoints.map((endpoint) => endpoint.id));
  }

  async function fillAndValidateStep3Endpoints2Config(
    endpoints: Partial<ConnectorListItem>[] = [
      { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'async', name: 'Mock' },
    ],
  ) {
    const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
    expectSchemaGetRequest(endpoints, 'endpoints');

    await step3Endpoints2ConfigHarness.clickValidate();
  }

  async function fillAndValidateStep4Security() {
    const step4 = await harnessLoader.getHarness(Step4SecurityHarness);
    await step4.fillAndValidate();
  }

  async function fillAndValidateStep5Documentation() {
    const step5 = await harnessLoader.getHarness(Step5DocumentationHarness);
    await step5.fillAndValidate();
  }

  function expectApiGetPortalSettings() {
    const settings: PortalSettings = {
      portal: {
        entrypoint: 'entrypoint',
      },
    };
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/settings`, method: 'GET' }).flush(settings);
    fixture.detectChanges();
  }

  function expectVerifyContextPathGetRequest() {
    httpTestingController.match({ url: `${CONSTANTS_TESTING.env.baseURL}/apis/verify`, method: 'POST' });
  }

  function exceptEnvironmentGetRequest(environment: Environment) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}`, method: 'GET' }).flush(environment);
    fixture.detectChanges();
  }
});
