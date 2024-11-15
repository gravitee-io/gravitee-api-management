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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioConfirmDialogHarness, LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { set } from 'lodash';
import { Router } from '@angular/router';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { Step1ApiDetailsHarness } from './steps/step-1-api-details/step-1-api-details.harness';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step3EndpointListHarness } from './steps/step-3-endpoints/step-3-endpoints-1-list.harness';
import { Step4Security1PlansHarness } from './steps/step-4-security/step-4-security-1-plans.harness';
import { Step2Entrypoints2ConfigComponent } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.component';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step3Endpoints2ConfigHarness } from './steps/step-3-endpoints/step-3-endpoints-2-config.harness';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { Step2Entrypoints1ListComponent } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.component';
import { Step2Entrypoints0ArchitectureHarness } from './steps/step-2-entrypoints/step-2-entrypoints-0-architecture.harness';
import { Step5SummaryHarness } from './steps/step-5-summary/step-5-summary.harness';
import { ApiCreationV4SpecStepperHelper } from './api-creation-v4-spec-stepper-helper';
import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ConnectorPlugin } from '../../../entities/management-api-v2';
import { Constants } from '../../../entities/Constants';

describe('ApiCreationV4Component - Navigation', () => {
  const httpProxyEntrypoint: Partial<ConnectorPlugin> = {
    id: 'http-proxy',
    supportedApiType: 'PROXY',
    name: 'HTTP Proxy',
    supportedListenerType: 'HTTP',
  };
  const httpProxyEntrypoints: Partial<ConnectorPlugin>[] = [httpProxyEntrypoint];

  const tcpProxyEntrypoint: Partial<ConnectorPlugin> = {
    id: 'tcp-proxy',
    supportedApiType: 'PROXY',
    name: 'TCP Proxy',
    supportedListenerType: 'TCP',
  };
  const tcpProxyEntrypoints: Partial<ConnectorPlugin>[] = [tcpProxyEntrypoint];

  const httpPostEntrypoint: Partial<ConnectorPlugin> = {
    id: 'http-post',
    supportedApiType: 'MESSAGE',
    name: 'HTTP Post',
    supportedListenerType: 'HTTP',
  };
  const httpGetEntrypoint: Partial<ConnectorPlugin> = {
    id: 'http-get',
    supportedApiType: 'MESSAGE',
    name: 'HTTP Get',
    supportedListenerType: 'HTTP',
  };
  const websocketEntrypoint: Partial<ConnectorPlugin> = {
    id: 'websocket',
    supportedApiType: 'MESSAGE',
    name: 'Websocket',
    supportedListenerType: 'HTTP',
  };
  const sseEntrypoint: Partial<ConnectorPlugin> = {
    id: 'sse',
    supportedApiType: 'MESSAGE',
    name: 'TCP Proxy',
    supportedListenerType: 'HTTP',
  };
  const webhookEntrypoint: Partial<ConnectorPlugin> = {
    id: 'webhook',
    supportedApiType: 'MESSAGE',
    name: 'Webhook',
    supportedListenerType: 'SUBSCRIPTION',
  };
  const messageEntrypoints: Partial<ConnectorPlugin>[] = [httpPostEntrypoint, websocketEntrypoint, httpGetEntrypoint, sseEntrypoint];

  let fixture: ComponentFixture<ApiCreationV4Component>;
  let component: ApiCreationV4Component;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;

  let enabledReviewMode = false;
  let httpExpects: ApiCreationV4SpecHttpExpects;
  let stepperHelper: ApiCreationV4SpecStepperHelper;

  const init = () => {
    TestBed.configureTestingModule({
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

    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    httpExpects = new ApiCreationV4SpecHttpExpects(httpTestingController);
    stepperHelper = new ApiCreationV4SpecStepperHelper(harnessLoader, httpExpects, httpTestingController);
  };

  beforeEach(() => init());

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
    enabledReviewMode = false;
  });

  describe('Side menu', () => {
    it('should have 6 steps', (done) => {
      component.stepper.goToNextStep({ groupNumber: 1, component: Step1ApiDetailsComponent });
      component.stepper.validStep(() => ({ name: 'A1' }));
      component.stepper.goToNextStep({ groupNumber: 2, component: Step2Entrypoints1ListComponent });
      component.stepper.validStep(({ name }) => ({ name: `${name}>B1` }));
      component.stepper.goToNextStep({ groupNumber: 2, component: Step2Entrypoints2ConfigComponent });
      component.stepper.validStep(({ name }) => ({ name: `${name}>B2` }));

      component.menuSteps$.subscribe((menuSteps) => {
        expect(menuSteps.length).toEqual(5);
        expect(menuSteps[0].label).toEqual('API details');
        expect(menuSteps[1].label).toEqual('Entrypoints');
        // Expect to have the last valid step payload
        expect(menuSteps[1].payload).toEqual({ name: 'A1>B1>B2' });
        expect(menuSteps[2].label).toEqual('Endpoints');
        expect(menuSteps[3].label).toEqual('Security');
        expect(menuSteps[4].label).toEqual('Summary');
        done();
      });
    });
  });

  describe('Step 1 - API Details', () => {
    it('should save api details and move to next step', async () => {
      await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(component.currentStep.group.label).toEqual('API details');
      await stepperHelper.fillAndValidateStep1_ApiDetails('test API', '1', 'description');

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'test API', version: '1', description: 'description' });
      expect(component.currentStep.group.label).toEqual('Entrypoints');
    });

    it('should save api details and move to next step (description is optional)', async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', null);

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'API', version: '1.0', description: '' });
      expect(component.currentStep.group.label).toEqual('Entrypoints');
    });

    it('should exit without confirmation when no modification', async () => {
      const apiDetailsHarness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(apiDetailsHarness).toBeDefined();
      await apiDetailsHarness.clickExit();
      httpExpects.expectLicenseGetRequest({ tier: '', features: [], packs: [] });

      expect(routerNavigateSpy).toHaveBeenCalled();
    });

    it('should cancel exit in confirmation', async () => {
      const apiDetailsHarness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      httpExpects.expectLicenseGetRequest({ tier: '', features: [], packs: [] });

      await apiDetailsHarness.setName('Draft API');
      await apiDetailsHarness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.cancel();
      expect(component.currentStep.group.label).toEqual('API details');

      expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not save data after exiting', async () => {
      const apiDetailsHarness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      httpExpects.expectLicenseGetRequest({ tier: '', features: [], packs: [] });

      await apiDetailsHarness.setName('Draft API');

      await apiDetailsHarness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.confirm();
      expect(component.currentStep.payload).toEqual({});

      expect(routerNavigateSpy).toHaveBeenCalled();
    });
  });

  describe('Step 2 - Entrypoints', () => {
    it('should go back to step1 with API details restored when clicking on previous', async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      const architectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
      await architectureHarness.clickPrevious();
      expect(component.currentStep.group.label).toEqual('API details');

      const apiDetailsHarness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
      expect(apiDetailsHarness).toBeDefined();
      expect(await apiDetailsHarness.getName()).toEqual('API');
      expect(await apiDetailsHarness.getVersion()).toEqual('1.0');
      expect(await apiDetailsHarness.getDescription()).toEqual('Description');
    });

    it('should display only sync entrypoints in the list for PROXY architecture', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');

      const step2Entrypoints1ListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);

      httpExpects.expectEntrypointsGetRequest([httpProxyEntrypoint, tcpProxyEntrypoint, sseEntrypoint]);

      const entrypointsRadio = await step2Entrypoints1ListHarness.getSyncEntrypoints();
      expect(await entrypointsRadio.getValues()).toEqual(['http-proxy', 'tcp-proxy']);
      await entrypointsRadio.selectOptionById('http-proxy');
      await step2Entrypoints1ListHarness.clickValidate();

      httpExpects.expectEndpointGetRequest({ id: 'http-proxy', name: 'HTTP Proxy', deployed: true });

      expect(component.currentStep.payload.type).toEqual('PROXY');
      expect(component.currentStep.payload.selectedEntrypoints).toEqual([
        { icon: 'gio-literal:http-proxy', id: 'http-proxy', supportedListenerType: 'HTTP', name: 'HTTP Proxy', deployed: true },
      ]);
      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { icon: 'gio-literal:http-proxy', id: 'http-proxy', name: 'HTTP Proxy', deployed: true },
      ]);

      fixture.detectChanges();
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
      httpExpects.expectVerifyContextPath();
      httpExpects.expectApiGetPortalSettings();
    }));

    it('should display only async entrypoints in the list for MESSAGE architecture', async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');

      const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
      httpExpects.expectEntrypointsGetRequest([sseEntrypoint, webhookEntrypoint, httpProxyEntrypoint]);

      const list = await step2Harness.getAsyncEntrypoints();
      expect(await list.getListValues()).toEqual(['sse', 'webhook']);
    });

    describe('should reset all data with confirmation when changing architecture', () => {
      it('should not reset data if user cancels action and restore initial choice', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('PROXY', httpProxyEntrypoints);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(httpProxyEntrypoints);

        // Init Step 3 Config and go back to Step 2 config
        const endpointsConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([
          { id: 'http-proxy', name: 'HTTP Proxy', supportedListenerType: 'HTTP' },
        ]);
        await endpointsConfigHarness.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list and go back to Step 2 architecture 0
        httpExpects.expectEntrypointsGetRequest(httpProxyEntrypoints);
        let entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.clickPrevious();

        // Init Step 2 architecture
        const architectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);

        // Change architecture to async
        expect(await architectureHarness.getArchitecture().then((s) => s.getValue())).toEqual('PROXY');
        await architectureHarness.fillAndValidate('MESSAGE');

        // check confirmation dialog and cancel
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.cancel();
        expect(await architectureHarness.getArchitecture().then((s) => s.getValue())).toEqual('PROXY');
        await architectureHarness.clickValidate();

        // Validate step 2 list
        entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest(httpProxyEntrypoints);
        await entrypointsListHarness.clickValidate();
        httpExpects.expectEndpointGetRequest({ id: 'http-proxy', name: 'HTTP Proxy', deployed: true, supportedListenerType: 'HTTP' });
        fixture.detectChanges();

        // Init Step 2 config
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();

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
              selectedQos: 'AUTO',
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
      }));

      it('should reset all data if user confirms modification', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('PROXY', httpProxyEntrypoints);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(httpProxyEntrypoints);

        // Init Step 3 and go back to Step 2 config
        const endpointsConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        await endpointsConfigHarness.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list and go back to Step 2 architecture 0
        httpExpects.expectEntrypointsGetRequest(httpProxyEntrypoints);
        const entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.clickPrevious();
        const architectureHarness = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);

        // Change architecture to async
        expect(await architectureHarness.getArchitecture().then((s) => s.getValue())).toEqual('PROXY');
        await architectureHarness.fillAndValidate('MESSAGE');

        // check confirmation dialog and confirm
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.confirm();

        httpExpects.expectEntrypointsGetRequest([]);
        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'MESSAGE',
        });
      }));
    });

    describe('should reset all data with confirmation when changing PROXY entrypoint', () => {
      it('should not reset data if user cancels action and restore initial choice', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');
        let entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest([httpProxyEntrypoint, tcpProxyEntrypoint]);
        await entrypointsListHarness.fillSyncAndValidate('http-proxy');
        httpExpects.expectEndpointGetRequest({ id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' });
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(httpProxyEntrypoints);

        // Init Step 3 Config and go back to Step 2 config
        const endpointsConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        await endpointsConfigHarness.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list
        httpExpects.expectEntrypointsGetRequest([httpProxyEntrypoint, tcpProxyEntrypoint]);

        // Change entrypoint to TCP Proxy
        entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.fillSyncAndValidate('tcp-proxy');

        // check confirmation dialog and cancel
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.cancel();

        // Verify initial choice is selected again
        expect(await entrypointsListHarness.getSyncEntrypoints().then((s) => s.getValue())).toEqual('http-proxy');
        await entrypointsListHarness.clickValidate();
        httpExpects.expectEndpointGetRequest({ id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' });
        fixture.detectChanges();

        // Init Step 2 config
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();

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
              selectedQos: 'AUTO',
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
      }));

      it('should reset all data if user confirms modification', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');
        let entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest([httpProxyEntrypoint, tcpProxyEntrypoint]);
        await entrypointsListHarness.fillSyncAndValidate('http-proxy');
        httpExpects.expectEndpointGetRequest({ id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' });
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(httpProxyEntrypoints);

        // Init Step 3 and go back to Step 2 config
        const endpointsConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
        httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
        httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);
        await endpointsConfigHarness.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(httpProxyEntrypoints);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list
        httpExpects.expectEntrypointsGetRequest([httpProxyEntrypoint, tcpProxyEntrypoint]);

        // Change entrypoint to TCP Proxy
        entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.fillSyncAndValidate('tcp-proxy');

        // check confirmation dialog and confirm
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.confirm();
        httpExpects.expectEndpointGetRequest({
          id: 'tcp-proxy',
          supportedApiType: 'PROXY',
          name: 'TCP Proxy',
          supportedListenerType: 'TCP',
        });
        fixture.detectChanges();

        // Init Step 2 config
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(tcpProxyEntrypoints);

        httpExpects.expectSchemaGetRequest([{ id: 'tcp-proxy', name: 'tCP Proxy' }], 'endpoints');
        httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'tcp-proxy', name: 'tCP Proxy' }]);

        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'PROXY',
          hosts: [
            {
              host: 'host',
            },
          ],
          selectedEntrypoints: [
            {
              icon: 'gio-literal:tcp-proxy',
              id: 'tcp-proxy',
              name: 'TCP Proxy',
              supportedListenerType: 'TCP',
              deployed: true,
              configuration: {},
              selectedQos: 'AUTO',
            },
          ],
          selectedEndpoints: [
            {
              icon: 'gio-literal:tcp-proxy',
              id: 'tcp-proxy',
              name: 'TCP Proxy',
              deployed: true,
            },
          ],
        });
      }));
    });

    describe('should reset all data with confirmation when changing MESSAGE entrypoint', () => {
      it('should not reset data if user cancels action and restore initial choice', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
        let entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest(messageEntrypoints);
        await entrypointsListHarness.fillAsyncAndValidate(['http-post', 'http-get']);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([httpPostEntrypoint, httpGetEntrypoint]);

        // Init Step 3 Endpoints list and go back to Step 2 config
        httpExpects.expectEndpointsGetRequest([]);
        const endpointsLists = await harnessLoader.getHarness(Step3EndpointListHarness);
        await endpointsLists.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest([
          { id: 'http-post', name: 'HTTP Post' },
          { id: 'http-get', name: 'HTTP Get' },
        ]);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list
        httpExpects.expectEntrypointsGetRequest(messageEntrypoints);

        // Change entrypoint to sse
        entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.fillAsyncAndValidate(['sse']);

        // check confirmation dialog and cancel
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.cancel();

        // Verify initial choice is selected again
        expect(await entrypointsListHarness.getAsyncEntrypoints().then((s) => s.getListValues({ selected: true }))).toEqual([
          'http-get',
          'http-post',
        ]);
        await entrypointsListHarness.clickValidate();

        // Init Step 2 config
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest([
          { id: 'http-post', name: 'HTTP Post' },
          { id: 'http-get', name: 'HTTP Get' },
        ]);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();

        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'MESSAGE',
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          selectedEntrypoints: [
            {
              icon: 'gio-literal:http-get',
              id: 'http-get',
              name: 'HTTP Get',
              supportedListenerType: 'HTTP',
              selectedQos: 'AUTO',
              deployed: true,
              configuration: {
                messagesLimitCount: 500,
                messagesLimitDurationMs: 5000,
                headersInPayload: false,
                metadataInPayload: false,
              },
            },
            {
              icon: 'gio-literal:http-post',
              id: 'http-post',
              name: 'HTTP Post',
              supportedListenerType: 'HTTP',
              selectedQos: 'AUTO',
              deployed: true,
              configuration: {
                requestHeadersToMessage: false,
              },
            },
          ],
        });
      }));

      it('should reset all data if user confirms modification', fakeAsync(async () => {
        // Init Step 1 and 2
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
        let entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest(messageEntrypoints);
        await entrypointsListHarness.fillAsyncAndValidate(['http-post', 'http-get']);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([httpPostEntrypoint, httpGetEntrypoint]);

        // Init Step 3 Endpoints list and go back to Step 2 config
        httpExpects.expectEndpointsGetRequest([]);
        const endpointsLists = await harnessLoader.getHarness(Step3EndpointListHarness);
        await endpointsLists.clickPrevious();

        // Init Step 2 config and go back to Step 2 list 1
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest([
          { id: 'http-post', name: 'HTTP Post' },
          { id: 'http-get', name: 'HTTP Get' },
        ]);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();
        const entrypointsConfigHarness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
        await entrypointsConfigHarness.clickPrevious();

        // Init Step 2 entrypoints list
        httpExpects.expectEntrypointsGetRequest([
          { id: 'http-post', supportedApiType: 'MESSAGE', name: 'HTTP Post' },
          { id: 'websocket', supportedApiType: 'MESSAGE', name: 'Websocket' },
          { id: 'http-get', supportedApiType: 'MESSAGE', name: 'HTTP Get' },
          { id: 'sse', supportedApiType: 'MESSAGE', name: 'SSE' },
        ]);

        // Change entrypoint to sse
        entrypointsListHarness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        await entrypointsListHarness.getAsyncEntrypoints().then(async (form) => {
          await form.deselectOptionByValue('http-get');
          return form.deselectOptionByValue('http-post');
        });
        await entrypointsListHarness.fillAsyncAndValidate(['sse']);

        // check confirmation dialog and confirm
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        expect(await dialogHarness).toBeTruthy();
        await dialogHarness.confirm();

        // Init Step 2 config
        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest([{ id: 'sse', name: 'SSE' }]);
        httpExpects.expectApiGetPortalSettings();
        httpExpects.expectVerifyContextPath();

        expect(component.currentStep.payload).toEqual({
          name: 'API',
          description: 'Description',
          version: '1.0',
          type: 'MESSAGE',
          paths: [
            {
              path: '/api/my-api-3',
            },
          ],
          selectedEntrypoints: [
            {
              icon: 'gio-literal:sse',
              id: 'sse',
              name: 'SSE',
              supportedListenerType: 'HTTP',
              deployed: true,
            },
          ],
        });
      }));
    });
  });

  describe('step3', () => {
    it('should go back to step2 with API details restored when clicking on previous', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      httpExpects.expectEndpointsGetRequest([]);

      await step3Harness.clickPrevious();

      const step21Harness = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(step21Harness).toBeDefined();
      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest([
        { id: 'entrypoint-1', name: 'initial entrypoint' },
        { id: 'entrypoint-2', name: 'new entrypoint' },
      ]);
      httpExpects.expectApiGetPortalSettings();
      httpExpects.expectVerifyContextPath();
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
          selectedQos: 'AUTO',
          deployed: true,
        },
        {
          configuration: {},
          icon: 'gio-literal:entrypoint-2',
          id: 'entrypoint-2',
          name: 'new entrypoint',
          supportedListenerType: 'SUBSCRIPTION',
          selectedQos: 'AUTO',
          deployed: true,
        },
      ]);
    }));

    it('should display only async endpoints in the list', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);

      httpExpects.expectEndpointsGetRequest([
        { id: 'http-post', supportedApiType: 'PROXY', name: 'HTTP Post', supportedQos: ['AUTO', 'NONE'] },
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
      ]);

      const list = await step3Harness.getEndpoints();
      expect(await list.getListValues()).toEqual(['kafka', 'mock']);
    }));

    it('should select endpoints in the list', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3Harness).toBeTruthy();

      httpExpects.expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
      ]);

      await step3Harness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { icon: 'gio-literal:kafka', id: 'kafka', name: 'Kafka', deployed: true },
        { icon: 'gio-literal:mock', id: 'mock', name: 'Mock', deployed: true },
      ]);

      httpExpects.expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );
      httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);
    }));

    it('should configure endpoints in the list', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();

      const step3EndpointListHarness = await harnessLoader.getHarness(Step3EndpointListHarness);
      expect(step3EndpointListHarness).toBeTruthy();

      httpExpects.expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
        { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
      ]);

      await step3EndpointListHarness.fillAndValidate(['mock', 'kafka']);

      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { icon: 'gio-literal:kafka', id: 'kafka', name: 'Kafka', deployed: true },
        { icon: 'gio-literal:mock', id: 'mock', name: 'Mock', deployed: true },
      ]);

      httpExpects.expectSchemaGetRequest(
        [
          { id: 'kafka', name: 'Kafka' },
          { id: 'mock', name: 'Mock' },
        ],
        'endpoints',
      );
      httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([
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
    }));
  });

  describe('step3 with type=sync', () => {
    it('should skip list step and go to config', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('PROXY');
      await stepperHelper.fillAndValidateStep2_1_EntrypointsList('PROXY', [
        { id: 'http-proxy', supportedApiType: 'PROXY', name: 'HTTP Proxy' },
      ]);
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(httpProxyEntrypoints);

      // Step 3 endpoints config
      const step3Endpoints2ConfigHarness = await harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
      expect(step3Endpoints2ConfigHarness).toBeTruthy();
      httpExpects.expectSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }], 'endpoints');
      httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest([{ id: 'http-proxy', name: 'HTTP Proxy' }]);

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
    }));
  });

  describe('step4', () => {
    describe('with HTTP and SUBSCRIPTION entrypoint', () => {
      beforeEach(fakeAsync(async () => {
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
      }));

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
      beforeEach(fakeAsync(async () => {
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE', [
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
      }));

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
        it('should add API Key plan to payload', async () => {
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
                      type: 'CHANNEL',
                      channel: '/',
                      channelOperator: 'STARTS_WITH',
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

        it('should edit default keyless plan', fakeAsync(async () => {
          const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

          await step4Security1PlansHarness.editDefaultKeylessPlanNameAndAddRateLimit('Update name', httpTestingController);
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
                      type: 'CHANNEL',
                      channel: '/',
                      channelOperator: 'STARTS_WITH',
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
        }));
      });

      it('should be reinitialized if no plans saved in payload after going back to step 3', async () => {
        let step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);

        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(1);
        await step4Security1PlansHarness.clickRemovePlanButton();

        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(0);

        await step4Security1PlansHarness.clickPrevious();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();

        step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(1);
      });
    });

    describe('with SUBSCRIPTION entrypoint only', () => {
      beforeEach(fakeAsync(async () => {
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('MESSAGE');
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE', [
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
        ]);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
        ]);
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
      }));

      it('should add default push plan only', fakeAsync(async () => {
        tick(1000);
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
            mode: 'PUSH',
            validation: 'MANUAL',
          },
        ]);
      }));
    });
  });

  describe('step5', () => {
    const API_ID = 'my-api';
    const PLAN_ID = 'my-plan';
    let step5Harness: Step5SummaryHarness;

    describe('with HTTP and SUBSCRIPTION listener types', () => {
      beforeEach(fakeAsync(async () => {
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture();
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE');
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
        await stepperHelper.fillAndValidateStep4_1_SecurityPlansList();
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      }));

      it('should display payload info', async () => {
        const step1Summary = await step5Harness.getStepSummaryTextContent(1);
        expect(step1Summary).toContain('API name:' + 'API name');
        expect(step1Summary).toContain('Version:' + '1.0');
        expect(step1Summary).toContain('Description:' + ' description');

        const step2Summary = await step5Harness.getStepSummaryTextContent(2);
        expect(step2Summary).toContain('Path:' + '/api/my-api-3');
        expect(step2Summary).toContain('Type:' + 'HTTP' + 'SUBSCRIPTION');
        expect(step2Summary).toContain('EntrypointsPath:/api/my-api-3Type:HTTPSUBSCRIPTIONEntrypoints');

        const step3Summary = await step5Harness.getStepSummaryTextContent(3);
        expect(step3Summary).toContain('Endpoints' + 'Endpoints: ' + 'Kafka ' + ' Mock Change');
        expect(step3Summary).toContain('Kafka');
        expect(step3Summary).toContain('Mock');

        const step4Summary = await step5Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('Update name' + 'KEY_LESS');
      });

      it('should go back to step 1 after clicking Change button', async () => {
        await step5Harness.clickChangeButton(1);

        const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
        expect(await step1Harness.getName()).toEqual('API name');
        expect(await step1Harness.getVersion()).toEqual('1.0');
        expect(await step1Harness.getDescription()).toEqual('description');
      });

      it('should go back to step 2 after clicking Change button', fakeAsync(async () => {
        await step5Harness.clickChangeButton(2);

        const step2Harness0Architecture = await harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
        await step2Harness0Architecture.fillAndValidate('MESSAGE');

        const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
        httpExpects.expectEntrypointsGetRequest([
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
          { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);

        const list = await step2Harness.getAsyncEntrypoints();
        expect(await list.getListValues({ selected: true })).toEqual(['entrypoint-1', 'entrypoint-2']);
        await list.deselectOptionByValue('entrypoint-1');

        await step2Harness.clickValidate();
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();

        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig([{ id: 'entrypoint-2', name: 'new entrypoint' }], ['/my-api/v4']);

        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
        await stepperHelper.fillAndValidateStep4_1_SecurityPlansList();

        // Reinitialize step5Harness after last step validation
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
        const step2Summary = await step5Harness.getStepSummaryTextContent(2);

        expect(step2Summary).toContain('EntrypointsPath:/my-api/v4Type:HTTPEntrypoints: new entrypointChange');
      }));

      it('should go back to step 3 after clicking Change button', fakeAsync(async () => {
        await step5Harness.clickChangeButton(3);

        const step3Harness = await harnessLoader.getHarness(Step3EndpointListHarness);
        httpExpects.expectEndpointsGetRequest([
          { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
          { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
        ]);
        const list = await step3Harness.getEndpoints();

        expect(await list.getListValues({ selected: true })).toEqual(['kafka', 'mock']);
        await list.deselectOptionByValue('kafka');
        expect(await list.getListValues({ selected: true })).toEqual(['mock']);
        await step3Harness.clickValidate();
        const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);
        await dialogHarness.confirm();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig([{ id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' }]);

        await stepperHelper.fillAndValidateStep4_1_SecurityPlansList();

        // Reinitialize step5Harness after step2 validation
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
        const step2Summary = await step5Harness.getStepSummaryTextContent(3);

        expect(step2Summary).toContain('Endpoints' + 'Endpoints: Mock Change');
      }));

      it('should go back to step 4 after clicking Change button', async () => {
        let step4Summary = await step5Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('Update name' + 'KEY_LESS');

        await step5Harness.clickChangeButton(4);

        const step4Security1PlansHarness = await harnessLoader.getHarness(Step4Security1PlansHarness);
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(2);

        await step4Security1PlansHarness.clickRemovePlanButton();
        await step4Security1PlansHarness.clickRemovePlanButton();
        expect(await step4Security1PlansHarness.countNumberOfRows()).toEqual(0);

        await step4Security1PlansHarness.clickValidate();

        // Reinitialize step5Harness after step4 validation
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);

        step4Summary = await step5Harness.getStepSummaryTextContent(4);
        expect(step4Summary).toContain('No plans are selected.');
      });
    });

    describe('with HTTP entrypoint only', () => {
      beforeEach(fakeAsync(async () => {
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture();
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE', [
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
        await stepperHelper.fillAndValidateStep4_1_SecurityPlansList();
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      }));

      it('should go to confirmation page after clicking Deploy my API', async () => {
        await step5Harness.clickDeployMyApiButton();

        httpExpects.expectCallsForApiDeployment(API_ID, PLAN_ID);

        expect(routerNavigateSpy).toHaveBeenCalledWith(['.', 'my-api'], expect.anything());
      });
    });

    describe('with review mode enabled', () => {
      beforeEach(fakeAsync(async () => {
        enabledReviewMode = true;
        await stepperHelper.fillAndValidateStep1_ApiDetails();
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture();
        await stepperHelper.fillAndValidateStep2_1_EntrypointsList('MESSAGE', [
          { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
        ]);
        await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig();
        await stepperHelper.fillAndValidateStep3_1_EndpointsList();
        await stepperHelper.fillAndValidateStep3_2_EndpointsConfig();
        await stepperHelper.fillAndValidateStep4_1_SecurityPlansList();
        step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      }));

      it('should go to confirmation page after clicking Save API & Ask for review', async () => {
        await step5Harness.clickCreateAndAskForReviewMyApiButton();

        httpExpects.expectCallsForApiAndPlanCreation(API_ID, PLAN_ID);

        const publishPlansRequest = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/plans/${PLAN_ID}/_publish`,
          method: 'POST',
        });
        publishPlansRequest.flush({});

        const askRequest = httpTestingController.expectOne({
          url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/reviews/_ask`,
          method: 'POST',
        });
        askRequest.flush({});
      });
    });
  });
});
