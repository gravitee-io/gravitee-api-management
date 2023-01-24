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
import { ApiCreationV4Step1Harness } from './steps/step-1/api-creation-v4-step-1.harness';
import { ApiCreationV4Step2Harness } from './steps/step-2/api-creation-v4-step-2.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { ApiCreationV4Step6Harness } from './steps/step-6/api-creation-v4-step-6.harness';
import { ApiCreationV4Step3Harness } from './steps/step-3/api-creation-v4-step-3.harness';

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

  describe('step1', () => {
    it('should start with step 1', async () => {
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
    });

    it('should save api details and move to next step', async () => {
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
      expect(component.currentStep.label).toEqual('API Metadata');
      await step1Harness.fillStep('test API', '1', 'description');
      await step1Harness.clickValidate();
      expectEntrypointsGetRequest([]);

      fixture.detectChanges();

      component.stepper.compileStepPayload(component.currentStep);
      expect(component.currentStep.payload).toEqual({ name: 'test API', version: '1', description: 'description' });
      expect(component.currentStep.label).toEqual('Entrypoints');
    });

    it('should exit without confirmation when no modification', async () => {
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
      await step1Harness.clickExit();

      fixture.detectChanges();
      expect(fakeAjsState.go).toHaveBeenCalled();
    });

    it('should cancel exit in confirmation', async () => {
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      await step1Harness.fillStep();
      await step1Harness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.cancel();
      expect(component.currentStep.label).toEqual('API Metadata');

      fixture.detectChanges();
      expect(fakeAjsState.go).not.toHaveBeenCalled();
    });

    it('should not save data after exiting', async () => {
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      await step1Harness.fillStep();

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
      await fillStepOneAndValidate('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
      expectEntrypointsGetRequest([]);

      await step2Harness.clickPrevious();
      expect(component.currentStep.label).toEqual('API Metadata');

      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
      expect(await step1Harness.getName()).toEqual('API');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('Description');
    });

    it('should display only async entrypoints in the list', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);

      expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'async', name: 'SSE' },
        { id: 'webhook', supportedApiType: 'async', name: 'Webhook' },
        { id: 'http', supportedApiType: 'sync', name: 'HTTP' },
      ]);

      expect(await step2Harness.getEntrypointsList()).toEqual(['sse', 'webhook']);
    });

    it('should select entrypoints in the list', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);

      expectEntrypointsGetRequest([
        { id: 'sse', supportedApiType: 'async', name: 'SSE' },
        { id: 'webhook', supportedApiType: 'async', name: 'Webhook' },
      ]);

      await step2Harness.fillStep(['sse', 'webhook']);

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
      await fillStepOneAndValidate('API', '1.0', 'Description');
      await fillStepTwoAndValidate();

      const step3Harness = await harnessLoader.getHarness(ApiCreationV4Step3Harness);
      expectEndpointsGetRequest([]);

      await step3Harness.clickPrevious();

      // TODO: Remove after entrypoints configuration integrated
      expectEndpointsGetRequest([]);

      // TODO: Integrate lines below when entrypoints configuration is integrated
      // expect(component.currentStep.position).toEqual(2);
      // const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
      // expect(step2Harness).toBeDefined();
      // expect(await step2Harness.getEntrypointsList({ selected: true })).toEqual([
      //   { id: 'entrypoint-1', name: 'initial entrypoint' },
      //   { id: 'entrypoint-2', name: 'new entrypoint' },
      // ]);
    });

    it('should display only async endpoints in the list', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      await fillStepTwoAndValidate();
      const step3Harness = await harnessLoader.getHarness(ApiCreationV4Step3Harness);

      expectEndpointsGetRequest([
        { id: 'http-post', supportedApiType: 'sync', name: 'HTTP Post' },
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      expect(await step3Harness.getEndpointsList()).toEqual(['kafka', 'mock']);
    });

    it('should select endpoints in the list', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      await fillStepTwoAndValidate();

      const step3Harness = await harnessLoader.getHarness(ApiCreationV4Step3Harness);

      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      await step3Harness.fillStep(['mock', 'kafka']);

      await step3Harness.clickValidate();
      expect(component.currentStep.payload.selectedEndpoints).toEqual([
        { id: 'kafka', name: 'Kafka' },
        { id: 'mock', name: 'Mock' },
      ]);
    });
  });

  describe('step6', () => {
    beforeEach(async () => {
      await fillAllSteps();
      fixture.detectChanges();
      fixture.detectChanges(); // TODO: remove this when fill step 2-1 exist
    });

    it('should display payload info', async () => {
      const step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);

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
      const step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
      await step6Harness.clickChangeButton(1);

      fixture.detectChanges();
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(await step1Harness.getName()).toEqual('API name');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('description');
    });

    it('should go back to step 2 after clicking Change button', async () => {
      let step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
      await step6Harness.clickChangeButton(2);
      fixture.detectChanges();

      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
      expectEntrypointsGetRequest([
        { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
        { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
      ]);

      expect(await step2Harness.getEntrypointsList({ selected: true })).toEqual(['entrypoint-1', 'entrypoint-2']);
      await step2Harness.deselectEntrypointById('entrypoint-1');
      fixture.detectChanges();

      await step2Harness.clickValidate();
      fixture.detectChanges();
      fixture.autoDetectChanges(true); // TODO: remove this when fill step 2-1 exist

      await fillStepThreeAndValidate();

      // Reinitialize step6Harness after last step validation
      step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(2);

      expect(step2Summary).toContain('Entrypoints:' + 'new entrypoint');
    });

    it('should go back to step 3 after clicking Change button', async () => {
      let step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
      await step6Harness.clickChangeButton(3);
      fixture.detectChanges();

      const step3Harness = await harnessLoader.getHarness(ApiCreationV4Step3Harness);
      expectEndpointsGetRequest([
        { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
        { id: 'mock', supportedApiType: 'async', name: 'Mock' },
      ]);

      expect(await step3Harness.getEndpointsList({ selected: true })).toEqual(['kafka', 'mock']);
      await step3Harness.deselectEndpointById('kafka');
      fixture.detectChanges();

      await step3Harness.clickValidate();
      fixture.detectChanges();

      // Reinitialize step6Harness after step2 validation
      step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
      const step2Summary = await step6Harness.getStepSummaryTextContent(3);

      expect(step2Summary).toContain('Endpoints:' + 'Mock');
    });

    it('should go to confirmation page after clicking Create my API', async () => {
      const step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
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
      const step6Harness = await harnessLoader.getHarness(ApiCreationV4Step6Harness);
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

  async function fillStepOneAndValidate(name = 'API name', version = '1.0', description = 'description') {
    const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
    await step1Harness.fillStep(name, version, description);
    await step1Harness.clickValidate();
  }

  async function fillStepTwoAndValidate(
    entrypoints: Partial<ConnectorListItem>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'async' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'async' },
    ],
  ) {
    const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
    expectEntrypointsGetRequest(entrypoints);
    await step2Harness.fillStep(entrypoints.map((entrypoint) => entrypoint.id));
    await step2Harness.clickValidate();
  }

  async function fillStepThreeAndValidate(
    endpoints: Partial<ConnectorListItem>[] = [
      { id: 'kafka', supportedApiType: 'async', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'async', name: 'Mock' },
    ],
  ) {
    const step3Harness = await harnessLoader.getHarness(ApiCreationV4Step3Harness);
    expectEndpointsGetRequest(endpoints);
    await step3Harness.fillStep(endpoints.map((endpoint) => endpoint.id));
    await step3Harness.clickValidate();
  }

  async function fillAllSteps() {
    await fillStepOneAndValidate();
    await fillStepTwoAndValidate();
    await fillStepThreeAndValidate();
  }
});
