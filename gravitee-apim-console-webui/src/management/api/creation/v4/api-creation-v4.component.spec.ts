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

import { ApiCreationV4Component } from './api-creation-v4.component';
import { ApiCreationV4Step1Harness } from './steps/step-1/api-creation-v4-step-1.harness';
import { ApiCreationV4Step2Harness } from './steps/step-2/api-creation-v4-step-2.harness';
import { ApiCreationV4Module } from './api-creation-v4.module';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ConnectorListItem } from '../../../../entities/connector/connector-list-item';
import { fakeConnectorListItem } from '../../../../entities/connector/connector-list-item.fixture';

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
    }).compileComponents();

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
      expect(component.currentStep).toEqual(1);
      await fillStepOne('test API', '1', 'description');
      await step1Harness.clickValidate();
      expect(component.apiDetails).toEqual({ name: 'test API', version: '1', description: 'description' });
      expect(component.currentStep).toEqual(2);
      expectEntrypointsGetRequest([]);
    });

    it('should exit without confirmation when no modification', async () => {
      const ajsSpy = jest.spyOn(component.ajsState, 'go');

      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
      await step1Harness.clickExit();
      expect(ajsSpy).toHaveBeenCalled();
    });

    it('should cancel exit in confirmation', async () => {
      const ajsSpy = jest.spyOn(component.ajsState, 'go');

      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      await fillStepOne();
      await step1Harness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.cancel();
      expect(component.currentStep).toEqual(1);

      expect(ajsSpy).not.toHaveBeenCalled();
    });

    it('should not save data after exiting', async () => {
      const ajsSpy = jest.spyOn(component.ajsState, 'go');

      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      await fillStepOne();

      await step1Harness.clickExit();

      const dialogHarness = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(GioConfirmDialogHarness);

      expect(await dialogHarness).toBeTruthy();

      await dialogHarness.confirm();
      expect(component.apiDetails).toBeUndefined();

      expect(ajsSpy).toHaveBeenCalled();
    });
  });

  describe('step2', () => {
    it('should go back to step1 with API details restored when clicking on previous', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);
      expectEntrypointsGetRequest([]);
      await step2Harness.clickPrevious();
      expect(component.currentStep).toEqual(1);
      const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
      expect(step1Harness).toBeDefined();
      expect(await step1Harness.getName()).toEqual('API');
      expect(await step1Harness.getVersion()).toEqual('1.0');
      expect(await step1Harness.getDescription()).toEqual('Description');
    });
    it('should select entrypoints in the list', async () => {
      await fillStepOneAndValidate('API', '1.0', 'Description');
      const step2Harness = await harnessLoader.getHarness(ApiCreationV4Step2Harness);

      expectEntrypointsGetRequest([fakeConnectorListItem({ id: 'sse' }), fakeConnectorListItem({ id: 'webhook' })]);

      await step2Harness.markEntrypointSelectedById('sse');
      await step2Harness.markEntrypointSelectedById('webhook');

      await step2Harness.clickValidate();
      expect(component.selectedEntrypoints).toEqual(['sse', 'webhook']);
    });
  });

  function expectEntrypointsGetRequest(connectors: ConnectorListItem[]) {
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.env.baseURL}/v4/entrypoints`, method: 'GET' }).flush(connectors);
  }

  async function fillStepOne(name = 'API name', version = '1.0', description = 'description') {
    const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
    await step1Harness.setName(name);
    await step1Harness.setVersion(version);
    await step1Harness.setDescription(description);
  }

  async function fillStepOneAndValidate(name = 'API name', version = '1.0', description = 'description') {
    await fillStepOne(name, version, description);
    const step1Harness = await harnessLoader.getHarness(ApiCreationV4Step1Harness);
    await step1Harness.clickValidate();
  }
});
