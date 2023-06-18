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
import { set } from 'lodash';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { Step1ApiDetailsHarness } from './steps/step-1-api-details/step-1-api-details.harness';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step6SummaryHarness } from './steps/step-6-summary/step-6-summary.harness';
import { Step3EndpointListHarness } from './steps/step-3-endpoints/step-3-endpoints-1-list.harness';
import { Step4Security1PlansHarness } from './steps/step-4-security/step-4-security-1-plans.harness';
import { Step5DocumentationHarness } from './steps/step-5-documentation/step-5-documentation.harness';
import { Step2Entrypoints2ConfigComponent } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.component';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step3Endpoints2ConfigHarness } from './steps/step-3-endpoints/step-3-endpoints-2-config.harness';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { Step2Entrypoints1ListComponent } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.component';
import { Step2Entrypoints0ArchitectureHarness } from './steps/step-2-entrypoints/step-2-entrypoints-0-architecture.harness';
import { ApiCreationStepperMenuHarness } from './components/api-creation-stepper-menu/api-creation-stepper-menu.harness';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { PortalSettings } from '../../../../entities/portal/portalSettings';
import { Environment } from '../../../../entities/environment/environment';
import { fakeEnvironment } from '../../../../entities/environment/environment.fixture';
import {
  fakePlanV4,
  ApiType,
  ConnectorPlugin,
  fakeApiV4,
  getEntrypointConnectorSchema,
  fakeConnectorPlugin,
} from '../../../../entities/management-api-v2';

describe('ApiCreationV4Component', () => {
  const httpProxyEntrypoint: Partial<ConnectorPlugin>[] = [{ id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' }];

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
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        {
          provide: 'Constants',
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
            return constants;
          },
        },
      ],
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
      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
      expectEntrypointsGetRequest([]);

      await step2Harness.clickPrevious();
      expect(component.currentStep.group.label).toEqual('API details');

      const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(step1Harness).toBeDefined();
      expect(await step1Harness.getName()).toEqual('API');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('Description');
    });

    describe('step2 - sync architecture', () => {
      it('should go directly to entrypoint settings', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        const step20ArchitectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        expectEntrypointsGetRequest([{ id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' }]);

        await step20ArchitectureHarness.fillAndValidate('PROXY');
        // For sync api type, http-proxy endpoint is automatically added
        expectEndpointGetRequest({ id: 'http-proxy', name: 'HTTP Proxy' });

        expect(component.currentStep.payload.type).toEqual('PROXY');
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:http-proxy', id: 'http-proxy', supportedListenerType: 'HTTP', name: 'HTTP Proxy', deployed: true },
        ]);
        expect(component.currentStep.payload.selectedEndpoints).toEqual([
          { icon: 'gio-literal:http-proxy', id: 'http-proxy', name: 'HTTP Proxy', deployed: true },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        expectApiGetPortalSettings();

        expect(await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness)).toBeDefined();
      });
    });

    describe('step2 - async architecture', () => {
      it('should display only async entrypoints in the list', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');

        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        expectEntrypointsGetRequest([
          { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' },
          { id: 'webhook', supportedApiType: 'MESSAGE', name: 'Webhook' },
          { id: 'http', supportedApiType: 'PROXY', name: 'HTTP' },
        ]);

        const list = await step2Harness.getEntrypoints();
        expect(await list.getListValues()).toEqual(['sse', 'webhook']);
      });

      it('should select entrypoints in the list', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([
          { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' },
          { id: 'webhook', supportedApiType: 'MESSAGE', name: 'Webhook' },
        ]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
          { icon: 'gio-literal:webhook', id: 'webhook', name: 'Webhook', supportedListenerType: 'HTTP', deployed: true },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([
          { id: 'sse', name: 'SSE' },
          { id: 'webhook', name: 'Webhook' },
        ]);
        expectApiGetPortalSettings();
      });

      it('should not display context-path form for non http supportedListenerType', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'SUBSCRIPTION' }]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

        await step2Harness.clickValidate();
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
        const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        expect(await step21Harness.hasListenersForm()).toEqual(false);
      });

      it('should not validate without path', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
        expectApiGetPortalSettings();
        const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        expect(await step21Harness.hasListenersForm()).toEqual(true);
        expect(await step21Harness.hasValidationDisabled()).toEqual(true);
      });

      it('should not validate with bad path', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
        expectApiGetPortalSettings();
        const step22Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await step22Harness.fillPaths('bad-path');
        expect(await step22Harness.hasValidationDisabled()).toEqual(true);
      });

      it('should configure paths', async () => {
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([
          { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE', supportedListenerType: 'HTTP' },
          { id: 'webhook', supportedApiType: 'MESSAGE', name: 'Webhook', supportedListenerType: 'SUBSCRIPTION' },
        ]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
          { icon: 'gio-literal:webhook', id: 'webhook', name: 'Webhook', supportedListenerType: 'SUBSCRIPTION', deployed: true },
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
            deployed: true,
          },
        ]);

        expectEndpointsGetRequest([]);
      });

      it('should not validate with empty host', async () => {
        await fillAndValidateStep1ApiDetails('API', '3.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
        ]);
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
        const step2Harness0Architecture = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        expectEntrypointsGetRequest([]);
        await step2Harness0Architecture.fillAndValidate('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([{ id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' }]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
        expectApiGetPortalSettings();
        const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await step21Harness.clickListenerType();
        expectApiGetPortalSettings();

        await step21Harness.fillVirtualHostsAndValidate({ host: 'hostname', path: '/api/my-api-3' });
        expectVerifyContextPathGetRequest();

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
            deployed: true,
          },
        ]);

        expectEndpointsGetRequest([]);
      });

      it('should configure entrypoints in the list', async () => {
        await fillAndValidateStep1ApiDetails('API', '2.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

        expectEntrypointsGetRequest([
          { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' },
          { id: 'webhook', supportedApiType: 'MESSAGE', name: 'Webhook' },
        ]);

        await step2Harness.getEntrypoints().then((form) => form.selectOptionsByIds(['sse', 'webhook']));

        await step2Harness.clickValidate();
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          { icon: 'gio-literal:sse', id: 'sse', name: 'SSE', supportedListenerType: 'HTTP', deployed: true },
          { icon: 'gio-literal:webhook', id: 'webhook', name: 'Webhook', supportedListenerType: 'HTTP', deployed: true },
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
            icon: 'gio-literal:sse',
            id: 'sse',
            name: 'SSE',
            supportedListenerType: 'HTTP',
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
            icon: 'gio-literal:webhook',
            id: 'webhook',
            name: 'Webhook',
            supportedListenerType: 'HTTP',
            deployed: true,
          },
        ]);

        expectEndpointsGetRequest([]);
      });

      it('should allow to go back and ask confirmation if remove entrypoint', async () => {
        // Configure Step 2
        await fillAndValidateStep1ApiDetails('API', '2.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        const entrypoints: Partial<ConnectorPlugin>[] = [
          { id: 'http-get', supportedApiType: 'MESSAGE', name: 'HTTP GET' },
          { id: 'http-post', supportedApiType: 'MESSAGE', name: 'HTTP POST' },
        ];
        await fillAndValidateStep2Entrypoints1List(entrypoints);
        const entrypointsConfig: Partial<ConnectorPlugin>[] = [
          { id: 'http-get', name: 'HTTP GET', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ];
        const paths: string[] = ['/api/my-api-3'];
        await fillAndValidateStep2Entrypoints2Config(entrypointsConfig, paths);
        expectEndpointsGetRequest([]);

        // Go back to Step 2.0
        const stepper = await harnessLoader.getHarness(ApiCreationStepperMenuHarness);
        await stepper.clickOnStepById('2');
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');

        const step21Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        expectEntrypointsGetRequest(entrypoints);

        // Change selected entrypoints : add one and remove one
        await step21Harness.getEntrypoints().then(async (form) => {
          await form.deselectOptionByValue('http-get');
          return form.selectOptionsByIds(['http-post']);
        });
        await step21Harness.clickValidate();
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        expectSchemaGetRequest([{ id: 'http-post' }]);

        const step22Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        expect(step22Harness).toBeDefined();
        expect(component.currentStep.payload.paths).toEqual([{ path: '/api/my-api-3' }]);
        expect(component.currentStep.payload.selectedEntrypoints).toEqual([
          {
            id: 'http-post',
            name: 'HTTP POST',
            supportedListenerType: 'HTTP',
            icon: 'gio-literal:http-post',
            deployed: true,
            configuration: {},
          },
        ]);
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest(entrypoints);
        expectApiGetPortalSettings();
        expectVerifyContextPathGetRequest();
      });
    });

    describe('step2 - changing architecture should reset all data with confirmation', () => {
      it('should not reset data if user cancels action', async () => {
        // Init Step 1 and 2
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('PROXY');
        await fillAndValidateStep2Entrypoints2Config(httpProxyEntrypoint);

        // Init Step 3 Config and go back to Step 2 config
        const step3endpoint2config = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        await step3endpoint2config.clickPrevious();

        // Init Step 2 config and go back to Step 2 architecture
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest(httpProxyEntrypoint);
        expectApiGetPortalSettings();
        expectVerifyContextPathGetRequest();
        await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness).then((harness) => harness.clickPrevious());

        // Init Step 2 architecture
        const step20ArchitectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        expectEntrypointsGetRequest(httpProxyEntrypoint);

        // Change architecture to async
        expect(await step20ArchitectureHarness.getArchitecture().then((s) => s.getListValues({ selected: true }))).toEqual(['PROXY']);
        await step20ArchitectureHarness.fillAndValidate('MESSAGE');

        // check confirmation dialog and cancel
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.cancel();
        expect(await step20ArchitectureHarness.getArchitecture().then((s) => s.getListValues({ selected: true }))).toEqual(['PROXY']);

        await step20ArchitectureHarness.clickValidate();
        expectEndpointGetRequest({ id: 'http-proxy', name: 'HTTP Proxy' });

        // Init Step 2 config
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest(httpProxyEntrypoint);
        expectApiGetPortalSettings();
        expectVerifyContextPathGetRequest();

        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'PROXY',
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          selectedEntrypoints: [
            {
              icon: 'gio-literal:http-proxy',
              id: 'http-proxy',
              name: 'HTTP Proxy',
              supportedListenerType: 'HTTP',
              deployed: true,
              configuration: {},
            },
          ],
          selectedEndpoints: [
            {
              icon: 'gio-literal:http-proxy',
              id: 'http-proxy',
              name: 'HTTP Proxy',
              deployed: true,
            },
          ],
        });
      });

      it('should reset all data if user confirms modification', async () => {
        // Init Step 1 and 2
        await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
        await fillAndValidateStep2Entrypoints0Architecture('PROXY');
        await fillAndValidateStep2Entrypoints2Config(httpProxyEntrypoint);

        // Init Step 3 and go back to Step 2 config
        const step3endpoint2config = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        await step3endpoint2config.clickPrevious();

        // Init Step 2 config and go back to Step 2 architecture
        exceptEnvironmentGetRequest(fakeEnvironment());
        expectSchemaGetRequest(httpProxyEntrypoint);
        expectApiGetPortalSettings();
        expectVerifyContextPathGetRequest();
        await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness).then((harness) => harness.clickPrevious());

        // Init Step 2 architecture
        const step20ArchitectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        expectEntrypointsGetRequest(httpProxyEntrypoint);

        // Change architecture to async
        expect(await step20ArchitectureHarness.getArchitecture().then((s) => s.getListValues({ selected: true }))).toEqual(['PROXY']);
        await step20ArchitectureHarness.fillAndValidate('MESSAGE');

        // check confirmation dialog and confirm
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.confirm();

        expectEntrypointsGetRequest([]);
        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'MESSAGE',
        });
      });
    });
  });

  describe('step3', () => {
    it('should go back to step2 with API details restored when clicking on previous', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
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
      expect(component.currentStep.payload.paths).toEqual([
        {
          path: '/api/my-api-3',
        },
      ]);

      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        {
          configuration: {},
          icon: 'gio-literal:entrypoint-1',
          id: 'entrypoint-1',
          name: 'initial entrypoint',
          supportedListenerType: 'HTTP',
          deployed: true,
        },
        {
          configuration: {},
          icon: 'gio-literal:entrypoint-2',
          id: 'entrypoint-2',
          name: 'new entrypoint',
          supportedListenerType: 'SUBSCRIPTION',
          deployed: true,
        },
      ]);
    });

    it('should display only async endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();
      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);

      expectEndpointsGetRequest([
        { id: 'http-post', supportedApiType: 'PROXY', name: 'HTTP Post' },
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
      ]);

      const list = await step3Harness.getEndpoints();
      expect(await list.getListValues()).toEqual(['kafka', 'mock']);
    });

    it('should select endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3Harness).toBeTruthy();

      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
      ]);

      fixture.detectChanges();

      await step3Harness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { icon: 'gio-literal:kafka', id: 'kafka', name: 'Kafka', deployed: true },
        { icon: 'gio-literal:mock', id: 'mock', name: 'Mock', deployed: true },
      ]);

      expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );
      expectEndpointsSharedConfigurationSchemaGetRequest([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);
    });

    it('should configure endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
      await fillAndValidateStep2Entrypoints1List();
      await fillAndValidateStep2Entrypoints2Config();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3Harness).toBeTruthy();

      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
      ]);

      fixture.detectChanges();

      await step3Harness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { icon: 'gio-literal:kafka', id: 'kafka', name: 'Kafka', deployed: true },
        { icon: 'gio-literal:mock', id: 'mock', name: 'Mock', deployed: true },
      ]);

      expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );
      expectEndpointsSharedConfigurationSchemaGetRequest([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);

      const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
      await step3Endpoints2ConfigHarness.clickValidate();

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        {
          configuration: {},
          sharedConfiguration: {},
          id: 'kafka',
          name: 'Kafka',
          icon: 'gio-literal:kafka',
          deployed: true,
        },
        {
          configuration: {},
          sharedConfiguration: {},
          id: 'mock',
          name: 'Mock',
          icon: 'gio-literal:mock',
          deployed: true,
        },
      ]);
    });
  });

  describe('step3 with type=sync', () => {
    it('should skip list step and go to config', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints0Architecture('PROXY');
      await fillAndValidateStep2Entrypoints2Config([{ id: 'http-proxy', name: 'HTTP Proxy', supportedApiType: 'PROXY' }]);

      // Step 3 endpoints config
      const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
      expect(step3Endpoints2ConfigHarness).toBeTruthy();
      expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
      expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);

      await step3Endpoints2ConfigHarness.clickValidate();

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        {
          configuration: {},
          sharedConfiguration: {},
          id: 'http-proxy',
          name: 'HTTP Proxy',
          icon: 'gio-literal:http-proxy',
          deployed: true,
        },
      ]);
    });
  });

  describe('step4', () => {
    describe('with HTTP and SUBSCRIPTION entrypoint', () => {
      beforeEach(async () => {
        await fillAndValidateStep1ApiDetails();
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        await fillAndValidateStep2Entrypoints1List();
        await fillAndValidateStep2Entrypoints2Config();
        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        fixture.detectChanges();
      });
      describe('step4 - plans list', () => {
        it('should add default keyless and push plans to payload', async () => {
          const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
          expect(await step4Security1PlansHarness.getPlanNames()).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)', 'Push plan']);

          const keylessPlan = await step4Security1PlansHarness.getColumnTextByRowIndex(0);
          expect(keylessPlan.name).toEqual('Default Keyless (UNSECURED)');
          expect(keylessPlan.security).toEqual('KEY_LESS');

          const pushPlan = await step4Security1PlansHarness.getColumnTextByRowIndex(1);
          expect(pushPlan.name).toEqual('Default PUSH plan');
          expect(pushPlan.mode).toEqual('PUSH');
          expect(pushPlan.security).toEqual('');

          await step4Security1PlansHarness.clickValidate();
          expect(component.currentStep.payload.plans).toEqual([
            {
              definitionVersion: 'V4',
              name: 'Default Keyless (UNSECURED)',
              description: 'Default unsecured plan',
              mode: 'STANDARD',
              security: {
                type: 'KEY_LESS',
                configuration: {},
              },
              validation: 'MANUAL',
            },
            {
              definitionVersion: 'V4',
              name: 'Default PUSH plan',
              description: 'Default push plan',
              security: {
                type: 'PUSH',
                configuration: {},
              },
              mode: 'PUSH',
              validation: 'MANUAL',
            },
          ]);
        });

        it('should add no plans to payload after deleting default plan', async () => {
          const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

          expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(2);

          // remove keyless plan
          await step4Security1PlansHarness.clickRemovePlanButton();

          // remove push plan
          await step4Security1PlansHarness.clickRemovePlanButton();

          expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(0);

          await step4Security1PlansHarness.clickValidate();
          expect(component.currentStep.payload.plans).toEqual([]);
        });
      });
    });

    describe('with HTTP entrypoint only', () => {
      beforeEach(async () => {
        await fillAndValidateStep1ApiDetails();
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        await fillAndValidateStep2Entrypoints1List([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await fillAndValidateStep2Entrypoints2Config([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        fixture.detectChanges();
      });

      it('should add default keyless plan only', async () => {
        const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.getPlanNames()).toEqual(['OAuth2', 'JWT', 'API Key', 'Keyless (public)']);

        const keylessPlan = await step4Security1PlansHarness.getColumnTextByRowIndex(0);
        expect(keylessPlan.name).toEqual('Default Keyless (UNSECURED)');
        expect(keylessPlan.security).toEqual('KEY_LESS');

        await step4Security1PlansHarness.clickValidate();
        expect(component.currentStep.payload.plans).toEqual([
          {
            definitionVersion: 'V4',
            name: 'Default Keyless (UNSECURED)',
            description: 'Default unsecured plan',
            mode: 'STANDARD',
            security: {
              type: 'KEY_LESS',
              configuration: {},
            },
            validation: 'MANUAL',
          },
        ]);
      });

      describe('step4 - add API_KEY plan', () => {
        it('should add api key plan to payload', async () => {
          const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

          const keylessPlan = await step4Security1PlansHarness.getColumnTextByRowIndex(0);
          expect(keylessPlan.name).toEqual('Default Keyless (UNSECURED)');
          expect(keylessPlan.security).toEqual('KEY_LESS');

          await step4Security1PlansHarness.addApiKeyPlan('Secure by ApiKey', httpTestingController);
          const planLine2 = await step4Security1PlansHarness.getColumnTextByRowIndex(1);
          expect(planLine2.name).toEqual('Secure by ApiKey');
          expect(planLine2.security).toEqual('API_KEY');

          await step4Security1PlansHarness.clickValidate();

          expect(component.currentStep.payload.plans).toEqual([
            {
              definitionVersion: 'V4',
              name: 'Default Keyless (UNSECURED)',
              description: 'Default unsecured plan',
              mode: 'STANDARD',
              security: {
                type: 'KEY_LESS',
                configuration: {},
              },
              validation: 'MANUAL',
            },
            {
              characteristics: [],
              commentMessage: '',
              commentRequired: false,
              description: '',
              excludedGroups: [],
              flows: [
                {
                  selectors: [
                    {
                      type: 'HTTP',
                      path: '/',
                      pathOperator: 'STARTS_WITH',
                    },
                  ],
                  enabled: true,
                  request: [],
                },
              ],
              generalConditions: '',
              name: 'Secure by ApiKey',
              mode: 'STANDARD',
              security: {
                configuration: {},
                type: 'API_KEY',
              },
              definitionVersion: 'V4',
              selectionRule: null,
              tags: [],
              validation: 'MANUAL',
            },
          ]);
        });

        it('should edit default keyless plan', async () => {
          const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

          await step4Security1PlansHarness.editDefaultKeylessPlanName('Update name', httpTestingController);
          await step4Security1PlansHarness.addRateLimitToPlan(httpTestingController);
          await step4Security1PlansHarness.clickValidate();

          expect(component.currentStep.payload.plans).toEqual([
            {
              name: 'Update name',
              description: 'Default unsecured plan',
              mode: 'STANDARD',
              security: {
                type: 'KEY_LESS',
                configuration: {},
              },
              definitionVersion: 'V4',
              validation: 'MANUAL',
              commentRequired: false,
              characteristics: undefined,
              commentMessage: undefined,
              excludedGroups: undefined,
              generalConditions: undefined,
              selectionRule: undefined,
              tags: undefined,
              flows: [
                {
                  selectors: [
                    {
                      type: 'HTTP',
                      path: '/',
                      pathOperator: 'STARTS_WITH',
                    },
                  ],
                  enabled: true,
                  request: [
                    {
                      enabled: true,
                      name: 'Rate Limiting',
                      configuration: {},
                      policy: 'rate-limit',
                    },
                  ],
                },
              ],
            },
          ]);
        });
      });

      it('should be reinitialized if no plans saved in payload after going back to step 3', async () => {
        let step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(1);
        await step4Security1PlansHarness.clickRemovePlanButton();

        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(0);

        await step4Security1PlansHarness.clickPrevious();
        await fillAndValidateStep3Endpoints2Config();

        step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(1);
      });
    });

    describe('with SUBSCRIPTION entrypoint only', () => {
      beforeEach(async () => {
        await fillAndValidateStep1ApiDetails();
        await fillAndValidateStep2Entrypoints0Architecture('MESSAGE');
        await fillAndValidateStep2Entrypoints1List([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
        ]);
        await fillAndValidateStep2Entrypoints2Config([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
        ]);
        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        fixture.detectChanges();
      });

      it('should add default push plan only', async () => {
        const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.getPlanNames()).toEqual(['Push plan']);

        const pushPlan = await step4Security1PlansHarness.getColumnTextByRowIndex(0);
        expect(pushPlan.name).toEqual('Default PUSH plan');
        expect(pushPlan.mode).toEqual('PUSH');
        expect(pushPlan.security).toEqual('');

        await step4Security1PlansHarness.clickValidate();
        expect(component.currentStep.payload.plans).toEqual([
          {
            definitionVersion: 'V4',
            name: 'Default PUSH plan',
            description: 'Default push plan',
            security: {
              type: 'PUSH',
              configuration: {},
            },
            mode: 'PUSH',
            validation: 'MANUAL',
          },
        ]);
      });
    });
  });

  describe('step6', () => {
    const API_ID = 'my-api';
    const PLAN_ID = 'my-plan';

    describe('with HTTP and SUBSCRIPTION entrypoint', () => {
      beforeEach(async () => {
        await fillAndValidateStep1ApiDetails();
        await fillAndValidateStep2Entrypoints0Architecture();
        await fillAndValidateStep2Entrypoints1List();
        await fillAndValidateStep2Entrypoints2Config();
        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        await fillAndValidateStep4Security1PlansList();
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
        expect(step2Summary).toContain('Path:' + '/api/my-api-3');
        expect(step2Summary).toContain('Type:' + 'HTTP' + 'SUBSCRIPTION');
        expect(step2Summary).toContain('EntrypointsPath:/api/my-api-3Type:HTTPSUBSCRIPTIONEntrypoints');

        const step3Summary = await step6Harness.getStepSummaryTextContent(3);
        expect(step3Summary).toContain('Field' + 'Value');
        expect(step3Summary).toContain('Kafka');
        expect(step3Summary).toContain('Mock');

        const step4Summary = await step6Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('Update name' + 'KEY_LESS');
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
        expectEntrypointsGetRequest([]);
        fixture.detectChanges();

        const step2Harness0Architecture = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        expect(await step2Harness0Architecture.getArchitecture().then((s) => s.getListValues({ selected: true }))).toEqual(['MESSAGE']);
        await step2Harness0Architecture.fillAndValidate('MESSAGE');

        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        expectEntrypointsGetRequest([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE' },
          { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'MESSAGE' },
        ]);

        const list = await step2Harness.getEntrypoints();
        expect(await list.getListValues({ selected: true })).toEqual(['entrypoint-1', 'entrypoint-2']);
        await list.deselectOptionByValue('entrypoint-1');
        fixture.detectChanges();

        await step2Harness.clickValidate();
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();
        fixture.detectChanges();

        await fillAndValidateStep2Entrypoints2Config([{ id: 'entrypoint-2', name: 'new entrypoint' }], ['/my-api/v4']);

        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        await fillAndValidateStep4Security1PlansList();
        await fillAndValidateStep5Documentation();

        // Reinitialize step6Harness after last step validation
        step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
        const step2Summary = await step6Harness.getStepSummaryTextContent(2);

        expect(step2Summary).toContain('EntrypointsPath:/my-api/v4Type:HTTPEntrypoints: new entrypointChange');
      });

      it('should go back to step 3 after clicking Change button', async () => {
        let step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
        await step6Harness.clickChangeButton(3);
        fixture.detectChanges();

        const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
        expectEndpointsGetRequest([
          { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
          { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
        ]);

        const list = await step3Harness.getEndpoints();

        expect(await list.getListValues({ selected: true })).toEqual(['kafka', 'mock']);
        await list.deselectOptionByValue('kafka');
        expect(await list.getListValues({ selected: true })).toEqual(['mock']);
        fixture.detectChanges();
        await step3Harness.clickValidate();
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        await fillAndValidateStep3Endpoints2Config([{ id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' }]);

        await fillAndValidateStep4Security1PlansList();
        await fillAndValidateStep5Documentation();

        // Reinitialize step6Harness after step2 validation
        step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
        const step2Summary = await step6Harness.getStepSummaryTextContent(3);

        expect(step2Summary).toContain('EndpointsFieldValueEndpoints: Mock Change');
      });

      it('should go back to step 4 after clicking Change button', async () => {
        let step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);

        let step4Summary = await step6Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('Update name' + 'KEY_LESS');

        await step6Harness.clickChangeButton(4);

        fixture.detectChanges();

        const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(2);

        await step4Security1PlansHarness.clickRemovePlanButton();
        await step4Security1PlansHarness.clickRemovePlanButton();
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(0);

        await step4Security1PlansHarness.clickValidate();
        await fillAndValidateStep5Documentation();

        // Reinitialize step6Harness after step4 validation
        step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);

        step4Summary = await step6Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('No plans are selected.');
      });
    });

    describe('with HTTP entrypoint only', () => {
      beforeEach(async () => {
        await fillAndValidateStep1ApiDetails();
        await fillAndValidateStep2Entrypoints0Architecture();
        await fillAndValidateStep2Entrypoints1List([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await fillAndValidateStep2Entrypoints2Config();
        await fillAndValidateStep3Endpoints1List();
        await fillAndValidateStep3Endpoints2Config();
        await fillAndValidateStep4Security1PlansList();
        await fillAndValidateStep5Documentation();
        fixture.detectChanges();
      });

      it('should go to confirmation page after clicking Create my API', async () => {
        const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
        await step6Harness.clickCreateMyApiButton();

        expectCallsForApiCreation(API_ID, PLAN_ID);

        expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.create-v4-confirmation', { apiId: API_ID });
      });

      it('should go to confirmation page after clicking Deploy my API', async () => {
        const step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
        await step6Harness.clickDeployMyApiButton();

        expectCallsForApiDeployment(API_ID, PLAN_ID);

        expect(fakeAjsState.go).toHaveBeenCalledWith('management.apis.create-v4-confirmation', { apiId: API_ID });
      });
    });
  });

  function expectEntrypointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/entrypoints` }).flush(fullConnectors);
  }

  function expectSchemaGetRequest(connectors: Partial<ConnectorPlugin>[], connectorType: 'entrypoints' | 'endpoints' = 'entrypoints') {
    connectors.forEach((connector) => {
      httpTestingController
        .match({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/${connectorType}/${connector.id}/schema`, method: 'GET' })
        .map((req) => {
          if (!req.cancelled) req.flush(getEntrypointConnectorSchema(connector.id));
        });
    });
  }
  function expectEndpointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints`, method: 'GET' }).flush(fullConnectors);
  }

  function expectEndpointsSharedConfigurationSchemaGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    connectors.forEach((connector) => {
      httpTestingController
        .match({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${connector.id}/shared-configuration-schema`, method: 'GET' })
        .map((req) => {
          if (!req.cancelled) req.flush(getEntrypointConnectorSchema(connector.id));
        });
    });
  }

  function expectEndpointGetRequest(connector: Partial<ConnectorPlugin>) {
    const fullConnector = fakeConnectorPlugin(connector);

    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.v2BaseURL}/plugins/endpoints/${fullConnector.id}`, method: 'GET' })
      .flush(fullConnector);
    fixture.detectChanges();
  }

  async function fillAndValidateStep1ApiDetails(name = 'API name', version = '1.0', description = 'description') {
    const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
    await step1Harness.fillAndValidate(name, version, description);
  }

  async function fillAndValidateStep2Entrypoints1List(
    entrypoints: Partial<ConnectorPlugin>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
    ],
  ) {
    const step21EntrypointsHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
    expectEntrypointsGetRequest(entrypoints);

    await step21EntrypointsHarness.fillAndValidate(entrypoints.map((entrypoint) => entrypoint.id));
  }

  async function fillAndValidateStep2Entrypoints0Architecture(type: ApiType = 'MESSAGE') {
    const step20ArchitectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
    expectEntrypointsGetRequest(httpProxyEntrypoint);
    await step20ArchitectureHarness.fillAndValidate(type);
    if (type === 'PROXY') {
      // For sync api type, we need to select the http proxy endpoint
      expectEndpointGetRequest({ id: 'http-proxy', name: 'HTTP Proxy' });
    }
    fixture.detectChanges();
  }

  async function fillAndValidateStep3Endpoints1List(
    endpoints: Partial<ConnectorPlugin>[] = [
      { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
    ],
  ) {
    const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
    expectEndpointsGetRequest(endpoints);

    await step3Harness.fillAndValidate(endpoints.map((endpoint) => endpoint.id));
  }

  async function fillAndValidateStep2Entrypoints2Config(
    entrypoints: Partial<ConnectorPlugin>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
    ],
    paths: string[] = ['/api/my-api-3'],
  ) {
    const step22EntrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
    exceptEnvironmentGetRequest(fakeEnvironment());
    expectSchemaGetRequest(entrypoints);

    if (entrypoints.some((entrypoint) => entrypoint.supportedListenerType !== 'SUBSCRIPTION')) {
      expectApiGetPortalSettings();
      await step22EntrypointsConfigHarness.fillPaths(...paths);
      expect(await step22EntrypointsConfigHarness.hasValidationDisabled()).toBeFalsy();
    }

    await step22EntrypointsConfigHarness.clickValidate();
    expectVerifyContextPathGetRequest();
  }

  async function fillAndValidateStep3Endpoints2Config(
    endpoints: Partial<ConnectorPlugin>[] = [
      { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
    ],
  ) {
    const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
    expectSchemaGetRequest(endpoints, 'endpoints');
    expectEndpointsSharedConfigurationSchemaGetRequest(endpoints);

    await step3Endpoints2ConfigHarness.clickValidate();
  }

  async function fillAndValidateStep4Security1PlansList() {
    const step4 = await harnessLoader.getHarness(Step4Security1PlansHarness);

    await step4.editDefaultKeylessPlanName('Update name', httpTestingController);
    await step4.addRateLimitToPlan(httpTestingController);
    await step4.clickValidate();
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

  function expectCallsForApiCreation(apiId: string, planId: string) {
    const createApiRequest = httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis`, method: 'POST' });

    // TODO: complete with all the expected fields
    expect(createApiRequest.request.body).toEqual(
      expect.objectContaining({
        definitionVersion: 'V4',
        name: 'API name',
      }),
    );
    createApiRequest.flush(fakeApiV4({ id: apiId }));

    const createPlansRequest = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans`,
      method: 'POST',
    });
    expect(createPlansRequest.request.body).toEqual(
      expect.objectContaining({
        definitionVersion: 'V4',
        name: 'Update name',
      }),
    );
    createPlansRequest.flush(fakePlanV4({ apiId: apiId, id: planId }));
  }

  function expectCallsForApiDeployment(apiId: string, planId: string) {
    expectCallsForApiCreation(apiId, planId);

    const publishPlansRequest = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${planId}/_publish`,
      method: 'POST',
    });
    publishPlansRequest.flush({});

    const startApiRequest = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/_start`,
      method: 'POST',
    });
    startApiRequest.flush(fakeApiV4({ id: apiId }));
  }
});
