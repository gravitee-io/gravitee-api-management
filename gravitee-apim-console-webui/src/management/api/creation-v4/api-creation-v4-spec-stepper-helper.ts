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

import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { License } from '@gravitee/ui-particles-angular';

import { Step1ApiDetailsHarness } from './steps/step-1-api-details/step-1-api-details.harness';
import { Step2Entrypoints0ArchitectureHarness } from './steps/step-2-entrypoints/step-2-entrypoints-0-architecture.harness';
import { Step2Entrypoints1ListHarness } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.harness';
import { ApiCreationV4SpecHttpExpects } from './api-creation-v4-spec-http-expects';
import { Step2Entrypoints2ConfigHarness } from './steps/step-2-entrypoints/step-2-entrypoints-2-config.harness';
import { Step3Endpoints2ConfigHarness } from './steps/step-3-endpoints/step-3-endpoints-2-config.harness';
import { Step4Security1PlansHarness } from './steps/step-4-security/step-4-security-1-plans.harness';
import { Step3EndpointListHarness } from './steps/step-3-endpoints/step-3-endpoints-1-list.harness';

import { ConnectorPlugin } from '../../../entities/management-api-v2';

export class ApiCreationV4SpecStepperHelper {
  private ossLicense: License = { tier: 'oss', features: [], packs: [] };

  constructor(
    private harnessLoader: HarnessLoader,
    private httpExpects: ApiCreationV4SpecHttpExpects,
    private httpTestingController: HttpTestingController,
  ) {}

  async fillAndValidateStep1_ApiDetails(
    name = 'API name',
    version = '1.0',
    description = 'description',
    license: License = this.ossLicense,
  ) {
    const apiDetails = await this.harnessLoader.getHarness(Step1ApiDetailsHarness);
    this.httpExpects.expectLicenseGetRequest(license);

    await apiDetails.fillAndValidate(name, version, description);
  }

  async fillAndValidateStep2_0_EntrypointsArchitecture(type: 'PROXY' | 'MESSAGE' | 'KAFKA' = 'MESSAGE') {
    const architecture = await this.harnessLoader.getHarness(Step2Entrypoints0ArchitectureHarness);
    if (type === 'MESSAGE' || type === 'KAFKA') {
      expect(await architecture.isLicenseBannerShown()).toEqual(true);
    }
    await architecture.fillAndValidate(type);
  }

  async fillAndValidateStep2_1_EntrypointsList(
    architecture: 'PROXY' | 'MESSAGE',
    entrypoints: Partial<ConnectorPlugin>[] = [
      { id: 'entrypoint-1', name: 'initial entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'HTTP' },
      { id: 'entrypoint-2', name: 'new entrypoint', supportedApiType: 'MESSAGE', supportedListenerType: 'SUBSCRIPTION' },
    ],
  ) {
    const entrypointsList = await this.harnessLoader.getHarness(Step2Entrypoints1ListHarness);
    this.httpExpects.expectEntrypointsGetRequest(entrypoints);

    if (architecture === 'MESSAGE') {
      await entrypointsList.fillAsyncAndValidate(entrypoints.map((entrypoint) => entrypoint.id));
    } else {
      if (entrypoints.length !== 1) {
        throw new Error('Only one entrypoint is supported for PROXY api');
      }
      await entrypointsList.fillSyncAndValidate(entrypoints[0].id);
      this.httpExpects.expectEndpointGetRequest(entrypoints[0]);
    }
  }

  async fillAndValidateStep2_2_EntrypointsConfig(
    entrypoints: Partial<ConnectorPlugin>[] = [
      {
        id: 'entrypoint-1',
        name: 'initial entrypoint',
        supportedApiType: 'MESSAGE',
        supportedListenerType: 'HTTP',
        supportedQos: ['AUTO'],
      },
      {
        id: 'entrypoint-2',
        name: 'new entrypoint',
        supportedApiType: 'MESSAGE',
        supportedListenerType: 'SUBSCRIPTION',
        supportedQos: ['AUTO'],
      },
    ],
    paths: string[] = ['/api/my-api-3'],
    hosts: string[] = ['host'],
  ) {
    const entrypointsConfig = await this.harnessLoader.getHarness(Step2Entrypoints2ConfigHarness);
    this.httpExpects.expectRestrictedDomainsGetRequest([]);
    this.httpExpects.expectSchemaGetRequest(entrypoints);

    if (entrypoints.some((entrypoint) => entrypoint.supportedListenerType === 'TCP')) {
      await entrypointsConfig.fillHosts(...hosts);
      this.httpExpects.expectVerifyHosts(hosts);
    } else if (entrypoints.some((entrypoint) => entrypoint.supportedListenerType !== 'SUBSCRIPTION')) {
      this.httpExpects.expectApiGetPortalSettings();
      await entrypointsConfig.fillPaths(...paths);
      this.httpExpects.expectVerifyContextPath();
    }

    expect(await entrypointsConfig.hasValidationDisabled()).toBeFalsy();
    await entrypointsConfig.clickValidate();
  }

  async fillAndValidateStep3_1_EndpointsList(
    endpoints: Partial<ConnectorPlugin>[] = [
      { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
      { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock', supportedQos: ['AUTO', 'NONE', 'AT_LEAST_ONCE', 'AT_MOST_ONCE'] },
    ],
  ) {
    const endpointsList = await this.harnessLoader.getHarness(Step3EndpointListHarness);
    this.httpExpects.expectEndpointsGetRequest(endpoints);

    await endpointsList.fillAndValidate(endpoints.map((endpoint) => endpoint.id));
  }

  async fillAndValidateStep3_2_EndpointsConfig(
    endpoints: Partial<ConnectorPlugin>[] = [
      { id: 'kafka', supportedApiType: 'MESSAGE', name: 'Kafka' },
      { id: 'mock', supportedApiType: 'MESSAGE', name: 'Mock' },
    ],
  ) {
    const endpointsConfig = await this.harnessLoader.getHarness(Step3Endpoints2ConfigHarness);
    this.httpExpects.expectSchemaGetRequest(endpoints, 'endpoints');
    this.httpExpects.expectEndpointsSharedConfigurationSchemaGetRequest(endpoints);

    await endpointsConfig.clickValidate();
  }

  async fillAndValidateStep4_1_SecurityPlansList() {
    const plansList = await this.harnessLoader.getHarness(Step4Security1PlansHarness);

    await plansList.editDefaultKeylessPlanNameAndAddRateLimit('Update name', this.httpTestingController);
    await plansList.clickValidate();
  }

  async validateStep4_1_SecurityPlansList() {
    const plansList = await this.harnessLoader.getHarness(Step4Security1PlansHarness);
    await plansList.clickValidate();
  }
}
