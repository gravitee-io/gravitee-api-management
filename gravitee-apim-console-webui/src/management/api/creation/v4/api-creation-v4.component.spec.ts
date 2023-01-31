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
import { Step3EndpointHarness } from './steps/step-3-endpoints/step-3-endpoints.harness';
import { Step4SecurityHarness } from './steps/step-4-security/step-4-security.harness';
import { Step5DocumentationHarness } from './steps/step-5-documentation/step-5-documentation.harness';
import { Step2Entrypoints2ConfigComponent } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.component';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ConnectorListItem } from '../../../../entities/connector/connector-list-item';
import { fakeConnectorListItem } from '../../../../entities/connector/connector-list-item.fixture';
import { fakeApiEntity } from '../../../../entities/api-v4';

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
      component.stepper.validStepAndGoNext(() => ({ name: 'A1' }));
      component.stepper.addSecondaryStep({ component: Step2Entrypoints2ConfigComponent });
      component.stepper.addSecondaryStep({ component: Step2Entrypoints2ConfigComponent });
      component.stepper.validStepAndGoNext(({ name }) => ({ name: `${name}>B1` }));
      component.stepper.validStepAndGoNext(({ name }) => ({ name: `${name}>B2` }));

      component.menuSteps$.subscribe((steps) => {
        expect(steps.length).toEqual(6);
        expect(steps[0].label).toEqual('API Details');
        expect(steps[1].label).toEqual('Entrypoints');
        // Expect to have the last valid step payload
        expect(steps[1].payload).toEqual({ name: 'A1>B1>B2' });
        expect(steps[2].label).toEqual('Endpoints');
        expect(steps[3].label).toEqual('Security');
        expect(steps[4].label).toEqual('Documentation');
        expect(steps[5].label).toEqual('Summary');
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
      expect(component.currentStep.label).toEqual('API Details');
      await step1Harness.fillAndValidate('test API', '1', 'description');
      expectEntrypointsGetRequest([]);

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'test API', version: '1', description: 'description' });
      expect(component.currentStep.label).toEqual('Entrypoints');
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
      expect(component.currentStep.label).toEqual('API Details');

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
      expect(component.currentStep.label).toEqual('API Details');

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
        { id: 'sse', name: 'SSE' },
        { id: 'webhook', name: 'Webhook' },
      ]);
      expectEndpointsGetRequest([]);
    });
  });

  describe('step3', () => {
    it('should go back to step2 with API details restored when clicking on previous', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointHarness);
      expectEndpointsGetRequest([]);

      await step3Harness.clickPrevious();

      // TODO: Remove after entrypoints configuration integrated
      expectEndpointsGetRequest([]);

      // TODO: Integrate lines below when entrypoints configuration is integrated
      // expect(component.currentStep.position).toEqual(2);
      // const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
      // expect(step2Harness).toBeDefined();
      // const entrypointsList = await step2Harness.getEntrypointsForm().then((form) => form.getListOptions({ selected: true }));
      // expect(entrypointsList).toEqual([
      //   { id: 'entrypoint-1', name: 'initial entrypoint' },
      //   { id: 'entrypoint-2', name: 'new entrypoint' },
      // ]);
    });

    it('should display only async endpoints in the list', async () => {
      await fillAndValidateStep1ApiDetails('API', '1.0', 'Description');
      await fillAndValidateStep2Entrypoints();
      const step3Harness = await harnessLoader.getHarness(Step3EndpointHarness);

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
      await fillAndValidateStep2Entrypoints();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointHarness);
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
    });
  });

  describe('step6', () => {
    beforeEach(async () => {
      await fillAndValidateStep1ApiDetails();
      await fillAndValidateStep2Entrypoints();
      await fillAndValidateStep3Endpoints();
      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();
      fixture.detectChanges();
      fixture.detectChanges(); // TODO: remove this when fill step 2-1 exist
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
      expect(step2Summary).toContain('Entrypoints:' + 'initial entrypoint' + 'new entrypoint');

      const step3Summary = await step6Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Field' + 'Value');
      expect(step3Summary).toContain('Endpoints:' + 'Kafka' + 'Mock');
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
      fixture.autoDetectChanges(true); // TODO: remove this when fill step 2-1 exist

      await fillAndValidateStep3Endpoints();
      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();

      // Reinitialize step6Harness after last step validation
      step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(2);

      expect(step2Summary).toContain('Entrypoints:' + 'new entrypoint');
    });

    it('should go back to step 3 after clicking Change button', async () => {
      let step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      await step6Harness.clickChangeButton(3);
      fixture.detectChanges();

      const step3Harness = await harnessLoader.getHarness(Step3EndpointHarness);
      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      const list = await step3Harness.getEndpoints();

      expect(await list.getListValues({ selected: true })).toEqual(['kafka', 'mock']);
      await list.deselectOptionByValue('kafka');
      fixture.detectChanges();
      await step3Harness.clickValidate();

      await fillAndValidateStep4Security();
      await fillAndValidateStep5Documentation();

      // Reinitialize step6Harness after step2 validation
      step6Harness = await harnessLoader.getHarness(Step6SummaryHarness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(3);

      expect(step2Summary).toContain('Endpoints:' + 'Mock');
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

  function expectEndpointsGetRequest(connectors: Partial<ConnectorListItem>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorListItem(partial));

    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/endpoints`, method: 'GET' }).flush(fullConnectors);
  }

  async function fillAndValidateStep1ApiDetails(name = 'API name', version = '1.0', description = 'description') {
    const step1Harness = await harnessLoader.getHarness(Step1ApiDetailsHarness);
    await step1Harness.fillAndValidate(name, version, description);
  }

  async function fillAndValidateStep2Entrypoints(
    entrypoints: Partial<ConnectorListItem>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
    ],
  ) {
    const step2Harness = await harnessLoader.getHarness(Step2Entrypoints1ListHarness);
    expectEntrypointsGetRequest(entrypoints);

    await step2Harness.fillAndValidate(entrypoints.map((entrypoint) => entrypoint.id));
  }

  async function fillAndValidateStep3Endpoints(
    endpoints: Partial<ConnectorListItem>[] = [
      { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'async', name: 'Mock' },
    ],
  ) {
    const step3Harness = await harnessLoader.getHarness(Step3EndpointHarness);
    expectEndpointsGetRequest(endpoints);

    await step3Harness.fillAndValidate(endpoints.map((endpoint) => endpoint.id));
  }

  async function fillAndValidateStep4Security() {
    const step4 = await harnessLoader.getHarness(Step4SecurityHarness);
    await step4.fillAndValidate();
  }

  async function fillAndValidateStep5Documentation() {
    const step5 = await harnessLoader.getHarness(Step5DocumentationHarness);
    await step5.fillAndValidate();
  }
});
