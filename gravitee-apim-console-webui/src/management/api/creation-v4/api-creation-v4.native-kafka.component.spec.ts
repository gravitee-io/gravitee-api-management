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
import { ComponentFixture, discardPeriodicTasks, fakeAsync, flush, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { HttpTestingController } from '@angular/common/http/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { set } from 'lodash';

import { ApiCreationV4Component } from './api-creation-v4.component';
import { ApiCreationV4Module } from './api-creation-v4.module';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step5SummaryHarness } from './steps/step-5-summary/step-5-summary.harness';
import { ApiCreationV4SpecStepperHelper } from './api-creation-v4-spec-stepper-helper';
import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ConnectorPlugin } from '../../../entities/management-api-v2';
import { Constants } from '../../../entities/Constants';

describe('ApiCreationV4Component - Native Kafka', () => {
  const nativeKafkaEntrypoint: Partial<ConnectorPlugin>[] = [
    { id: 'native-kafka', supportedApiType: 'NATIVE', name: 'Native Kafka Entrypoint', supportedListenerType: 'KAFKA' },
  ];

  let fixture: ComponentFixture<ApiCreationV4Component>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

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
    it('should not continue when host and port not specified', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('KAFKA');

      httpExpects.expectRestrictedDomainsGetRequest([]);
      httpExpects.expectSchemaGetRequest(nativeKafkaEntrypoint);

      const entrypointsConfig = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
      expect(await entrypointsConfig.hasKafkaListenersForm()).toEqual(true);

      await entrypointsConfig.fillHost('my-lovely-host');
      httpExpects.expectVerifyHosts(['my-lovely-host'], 1);

      expect(await entrypointsConfig.hasValidationDisabled()).toBeTruthy();

      await entrypointsConfig.fillPort(1000);
      expect(await entrypointsConfig.hasValidationDisabled()).toBeFalsy();

      discardPeriodicTasks();
    }));

    it.each(['', 'path!', '//path'])(
      "should not validate when host equals:  ' %s '",
      fakeAsync(async (contextPath: string) => {
        await stepperHelper.fillAndValidateStep1_ApiDetails('API', '1.0', 'Description');
        await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('KAFKA');

        const entrypointsConfig = await harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);

        httpExpects.expectRestrictedDomainsGetRequest([]);
        httpExpects.expectSchemaGetRequest(nativeKafkaEntrypoint);

        await entrypointsConfig.fillPort(1000);
        await entrypointsConfig.fillHost(contextPath);
        httpExpects.expectVerifyContextPath();
        expect(await entrypointsConfig.hasValidationDisabled()).toBeTruthy();

        discardPeriodicTasks();
      }),
    );
  });
  describe('API Creation', () => {
    it('should create the API', fakeAsync(async () => {
      await stepperHelper.fillAndValidateStep1_ApiDetails('API name', '1.0', 'Description');
      await stepperHelper.fillAndValidateStep2_0_EntrypointsArchitecture('KAFKA');
      await stepperHelper.fillAndValidateStep2_2_EntrypointsConfig(nativeKafkaEntrypoint);

      discardPeriodicTasks();

      const step5Harness = await harnessLoader.getHarness(Step5SummaryHarness);
      const step1Summary = await step5Harness.getStepSummaryTextContent(1);
      expect(step1Summary).toContain('API name:' + 'API');
      expect(step1Summary).toContain('Version:' + '1.0');
      expect(step1Summary).toContain('Description:' + ' Description');

      const step2Summary = await step5Harness.getStepSummaryTextContent(2);
      expect(step2Summary).toContain('Host:' + 'kafka-host');
      expect(step2Summary).toContain('Type:' + 'KAFKA');
      expect(step2Summary).toContain('Entrypoints:' + ' Native Kafka Entrypoint');

      const step3Summary = await step5Harness.getStepSummaryTextContent(3);
      expect(step3Summary).toContain('Endpoints' + 'Endpoints: ' + 'Native Kafka Endpoint');

      const step4Summary = await step5Harness.getStepSummaryTextContent(4);
      expect(step4Summary).toContain('No plans are selected.');

      await step5Harness.clickCreateMyApiButton();
      httpExpects.expectCallsForApiCreation('api-id');

      flush();
    }));
  });
});
