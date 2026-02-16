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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormsModule } from '@angular/forms';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatStepHarness } from '@angular/material/stepper/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { fakeKafkaMessageEndpoint } from '@gravitee/ui-policy-studio-angular/testing';
import { cloneDeep } from 'lodash';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';

import { ApiEndpointGroupCreateComponent } from './api-endpoint-group-create.component';
import { ApiEndpointGroupCreateHarness } from './api-endpoint-group-create.harness';

import { ApiEndpointGroupModule } from '../api-endpoint-group.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import {
  ApiV4,
  ConnectorPlugin,
  EndpointGroupV4,
  EndpointV4Default,
  fakeApiV4,
  fakeNativeKafkaApiV4,
} from '../../../../../entities/management-api-v2';
import { fakeEndpointGroupV4 } from '../../../../../entities/management-api-v2/api/v4/endpointGroupV4.fixture';
import { GioLicenseBannerModule } from '../../../../../shared/components/gio-license-banner/gio-license-banner.module';

const API_ID = 'api-id';
const ENDPOINT_GROUP_NAME = 'My Endpoint Group';
const fakeKafkaSharedSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    topic: {
      title: 'Kafka topic',
      type: 'string',
      description: 'A kafka topic',
    },
  },
  additionalProperties: false,
  required: ['topic'],
};

const fakeRabbitMqSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    server: {
      title: 'Server',
      type: 'string',
      description: 'Server',
    },
  },
  additionalProperties: false,
  required: ['server'],
};

const fakeRabbitMqSharedSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    rabbitFood: {
      title: 'Rabbit food',
      type: 'string',
      description: 'Some rabbit food',
    },
  },
  additionalProperties: false,
  required: ['rabbitFood'],
};

const fakeKafkaSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    bootstrapServers: {
      title: 'bootstrapServers',
      type: 'string',
      description: 'bootstrap servers',
    },
  },
  additionalProperties: false,
  required: ['bootstrapServers'],
};

const fakeHttpProxySchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    target: {
      title: 'Target',
      type: 'string',
      description: 'Target',
    },
  },
  additionalProperties: false,
  required: ['target'],
};
const fakeLlmProxySchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    target: {
      title: 'Target',
      type: 'string',
      description: 'Target',
    },
  },
  additionalProperties: false,
  required: ['target'],
};
const fakeHttpProxySharedSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    proxyParam: {
      title: 'Proxy Param',
      type: 'string',
      description: 'Some param for your proxy',
    },
  },
  additionalProperties: false,
  required: ['proxyParam'],
};
const fakeLlmProxySharedSchema = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  properties: {
    llmProxyParam: {
      title: 'Llm Proxy Param',
      type: 'string',
      description: 'Some param for your Llm proxy',
    },
  },
  additionalProperties: false,
  required: ['llmProxyParam'],
};
const ENDPOINT_LIST = [
  {
    id: 'mock',
    name: 'Mock',
    description: 'Use a Mock backend to emulate the behaviour of a typical HTTP server and test processes',
    icon: 'mock-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
  },
  {
    id: 'rabbitmq',
    name: 'RabbitMQ',
    description: 'RabbitMQ Endpoint',
    icon: 'rabbitmq-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    supportedQos: ['NONE', 'AUTO'],
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Publish and subscribe to messages from one or more Kafka topics',
    icon: 'kafka-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
  },
];
const ENTRYPOINT_LIST: ConnectorPlugin[] = [
  {
    id: 'webhook',
    name: 'Webhook',
    description: 'Webhook entrypoint',
    icon: 'webhook-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    availableFeatures: ['DLQ'],
  },
  {
    id: 'http-get',
    name: 'HTTP Get',
    description: 'HTTP Get entrypoint',
    icon: 'http-get-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    availableFeatures: [],
  },
];
describe('ApiEndpointGroupCreateComponent', () => {
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<ApiEndpointGroupCreateComponent>;
  let harness: ApiEndpointGroupCreateHarness;
  let api: ApiV4;
  let routerNavigationSpy: jest.SpyInstance;

  const initComponent = async (testApi: ApiV4, queryParams?: unknown) => {
    const routerParams: unknown = { apiId: API_ID };

    api = testApi;

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiEndpointGroupModule, MatIconTestingModule, FormsModule, GioLicenseBannerModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: routerParams, queryParams } },
        },
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
      ],
    }).overrideProvider(InteractivityChecker, {
      useValue: {
        isFocusable: () => true,
      },
    });

    await TestBed.compileComponents();
    fixture = TestBed.createComponent(ApiEndpointGroupCreateComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupCreateHarness);

    const router = TestBed.inject(Router);
    routerNavigationSpy = jest.spyOn(router, 'navigate');

    expectApiGet();
    fixture.detectChanges();

    if (api.type === 'MESSAGE') {
      expectEndpointListGet();
    }
  };

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('V4 API - Message', () => {
    describe('Stepper', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }] }));
      });

      it('should go back to endpoint groups page on exit', async () => {
        await harness.goBackToEndpointGroups();
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], { relativeTo: expect.anything() });
      });

      it('should not go to General step if endpoint type not selected', async () => {
        expect(await harness.getSelectedEndpointGroupId()).toBeNull();

        expect(await harness.canGoToGeneralStep()).toEqual(false);

        await harness.selectEndpointGroup('mock');
        expect(await harness.getSelectedEndpointGroupId()).toEqual('mock');

        expect(await harness.canGoToGeneralStep()).toEqual(true);
      });

      it('should not go to Configuration step if invalid General information', async () => {
        await fillOutAndValidateEndpointSelection();
        expect(await harness.canGoToConfigurationStep()).toEqual(false);

        expect(await harness.getNameValue()).toEqual('');
        expect(await harness.getLoadBalancerValue()).toEqual('');

        await harness.setNameValue('A new name');
        expect(await harness.canGoToConfigurationStep()).toEqual(false);

        await harness.setLoadBalancerValue('ROUND_ROBIN');
        expect(await harness.getNameValue()).toEqual('A new name');
        expect(await harness.getLoadBalancerValue()).toEqual('ROUND_ROBIN');
        expect(await harness.canGoToConfigurationStep()).toEqual(true);

        await harness.setNameValue('default-group'); // Name already exists in fakeApiV4
        expect(await harness.canGoToConfigurationStep()).toEqual(false);
      });

      it('should not save endpoint group if Configuration is invalid', async () => {
        await fillOutAndValidateEndpointSelection();
        await fillOutAndValidateGeneralInformation();

        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        expect(await harness.isConfigurationFormShown()).toEqual(true);

        expect(await harness.getConfigurationInputValue('topic')).toEqual('');

        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.getConfigurationInputValue('topic')).toEqual('my-kafka-topic');

        expect(await harness.isConfigurationStepValid()).toEqual(true);
        expect(await harness.canCreateEndpointGroup()).toEqual(false);
      });
    });

    describe('When creating a Kafka endpoint group', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }] }));
        await fillOutAndValidateEndpointSelection();
        await fillOutAndValidateGeneralInformation();
        await harness.setConfigurationInputValue('bootstrapServers', 'a new server');
        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.isConfigurationStepValid()).toEqual(true);
      });

      it('should be possible', async () => {
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('kafka', ENDPOINT_GROUP_NAME), configuration: { bootstrapServers: 'a new server' } },
          ],
          sharedConfiguration: {
            topic: 'my-kafka-topic',
          },
        });
      });

      it('should show error in configuration after changing endpoint type to RabbitMQ', async () => {
        await chooseEndpointTypeAndGoToConfiguration('rabbitmq', fakeRabbitMqSchema, fakeRabbitMqSharedSchema);
        await harness.setConfigurationInputValue('server', 'lettuce-server:8888');
        await harness.setConfigurationInputValue('rabbitFood', 'lettuce');

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'rabbitmq',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('rabbitmq', ENDPOINT_GROUP_NAME), configuration: { server: 'lettuce-server:8888' } },
          ],
          sharedConfiguration: {
            rabbitFood: 'lettuce',
          },
        });
      });

      it('should not show error in configuration after changing endpoint type to Mock', async () => {
        await chooseEndpointTypeAndGoToConfiguration('mock');
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'mock',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byTypeAndGroupName('mock', ENDPOINT_GROUP_NAME)],
          sharedConfiguration: {},
        });
      });
    });

    describe('When creating a Mock endpoint group', () => {
      beforeEach(async () => {
        await initComponent(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }] }));
        await fillOutAndValidateEndpointSelection('mock');
        await fillOutAndValidateGeneralInformation();

        expect(await harness.isConfigurationFormShown()).toEqual(false);
        expect(await harness.isEndpointGroupMockBannerShown()).toEqual(true);
      });
      it('can create a mock endpoint group', async () => {
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'mock',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [EndpointV4Default.byTypeAndGroupName('mock', ENDPOINT_GROUP_NAME)],
          sharedConfiguration: {},
        });
      });

      it('should invalidate configuration if user switches to Kafka endpoint type', async () => {
        await chooseEndpointTypeAndGoToConfiguration();

        await harness.setConfigurationInputValue('bootstrapServers', 'bootstrap');
        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        expect(await harness.isConfigurationStepValid()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('kafka', ENDPOINT_GROUP_NAME), configuration: { bootstrapServers: 'bootstrap' } },
          ],
          sharedConfiguration: {
            topic: 'my-kafka-topic',
          },
        });
      });
    });

    describe('Endpoint group name validation', () => {
      const EXISTING_ENDPOINT_GROUP = fakeEndpointGroupV4({
        type: 'kafka',
        name: 'Existing endpoint group ',
        endpoints: [fakeKafkaMessageEndpoint({ name: 'Existing endpoint ' })],
      });
      beforeEach(async () => {
        await initComponent(
          fakeApiV4({
            id: API_ID,
            endpointGroups: [EXISTING_ENDPOINT_GROUP],
            listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }],
          }),
        );
        await fillOutAndValidateEndpointSelection('kafka');
      });

      afterEach(async () => {
        await harness.setNameValue('A unique name');
        expect(await harness.canGoToConfigurationStep()).toEqual(true);
      });

      it('cannot have the same name as another group', async () => {
        expect(await harness.isGeneralStepSelected()).toEqual(true);
        await harness.setNameValue(EXISTING_ENDPOINT_GROUP.name);
        await harness.setLoadBalancerValue('ROUND_ROBIN');
        expect(await harness.canGoToConfigurationStep()).toEqual(false);
      });

      it('cannot have the same name as another endpoint', async () => {
        expect(await harness.isGeneralStepSelected()).toEqual(true);
        await harness.setNameValue('Existing endpoint');
        await harness.setLoadBalancerValue('ROUND_ROBIN');
        expect(await harness.canGoToConfigurationStep()).toEqual(false);
      });
    });

    describe('Creating a DLQ Kafka endpoint group', () => {
      async function beforeTest(dlqQueryParam: string, endpointGroupName: string) {
        await initComponent(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }] }), {
          dlq: dlqQueryParam,
        });
        expectEntrypointListGet();
        await fillOutAndValidateEndpointSelection('kafka');
        await fillOutAndValidateGeneralInformation(endpointGroupName);
        await harness.setConfigurationInputValue('topic', 'my-kafka-topic');
        await harness.setConfigurationInputValue('bootstrapServers', 'bootstrap');
        expect(await harness.isConfigurationStepValid()).toEqual(true);
      }

      it('should create DLQ endpoint group and assign it as DLQ', async () => {
        await beforeTest('webhook', 'dlq group');
        await createDlqEndpointGroup(
          {
            name: 'dlq group',
            type: 'kafka',
            loadBalancer: { type: 'ROUND_ROBIN' },

            endpoints: [
              { ...EndpointV4Default.byTypeAndGroupName('kafka', 'dlq group'), configuration: { bootstrapServers: 'bootstrap' } },
            ],
            sharedConfiguration: {
              topic: 'my-kafka-topic',
            },
          },
          'webhook',
        );
      });

      it('should show error and create regular endpoint group if incompatible DLQ query param', async () => {
        await beforeTest('http-get', ENDPOINT_GROUP_NAME);
        const snackBar = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
        expect(await snackBar.getMessage()).toEqual(
          "Cannot create DLQ endpoint group for 'http-get' entrypoint, creating regular endpoint group instead.",
        );
        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'kafka',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('kafka', ENDPOINT_GROUP_NAME), configuration: { bootstrapServers: 'bootstrap' } },
          ],
          sharedConfiguration: {
            topic: 'my-kafka-topic',
          },
        });
      });
    });
  });

  describe('When creating a RabbitMQ endpoint group', () => {
    it('cannot create an endpoint group for AT_LEAST_ONCE QoS', async () => {
      await initComponent(
        fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AT_LEAST_ONCE' }] }] }),
      );
      expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(true);
      expect(await harness.getEndpointGroupTypes()).toEqual(['kafka', 'mock']);
    });

    it('can create an endpoint group for AUTO QoS', async () => {
      await initComponent(fakeApiV4({ id: API_ID, listeners: [{ type: 'HTTP', entrypoints: [{ type: 'http-get', qos: 'AUTO' }] }] }));
      expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(true);
      expect(await harness.getEndpointGroupTypes()).toEqual(['kafka', 'mock', 'rabbitmq']);
    });
  });

  describe('V4 API - Proxy', () => {
    beforeEach(async () => {
      await initComponent(fakeApiV4({ id: API_ID, type: 'PROXY' }));
      expectConfigurationSchemaGet('http-proxy', fakeHttpProxySchema);
      expectSharedConfigurationSchemaGet('http-proxy', fakeHttpProxySharedSchema);
    });

    describe('Stepper', () => {
      it('should go back to endpoint groups page on exit', async () => {
        expect(await isStepActive(harness.getGeneralStep())).toEqual(true);
        await harness.goBackToEndpointGroups();
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], { relativeTo: expect.anything() });
      });
    });

    describe('When creating a http-proxy endpoint group', () => {
      it('should be possible', async () => {
        await fillOutAndValidateGeneralInformation();
        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        await harness.setConfigurationInputValue('proxyParam', 'my-proxy-param');
        await harness.setConfigurationInputValue('target', 'http://target.gio');
        expect(await harness.isConfigurationStepValid()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'http-proxy',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('http-proxy', ENDPOINT_GROUP_NAME), configuration: { target: 'http://target.gio' } },
          ],
          sharedConfiguration: {
            proxyParam: 'my-proxy-param',
          },
        });
      });
    });
  });
  describe('V4 API - LLM Proxy', () => {
    beforeEach(async () => {
      await initComponent(fakeApiV4({ id: API_ID, type: 'LLM_PROXY' }));
      expectConfigurationSchemaGet('llm-proxy', fakeLlmProxySchema);
      expectSharedConfigurationSchemaGet('llm-proxy', fakeLlmProxySharedSchema);
    });

    describe('Stepper', () => {
      it('should go back to endpoint groups page on exit', async () => {
        expect(await isStepActive(harness.getGeneralStep())).toEqual(true);
        await harness.goBackToEndpointGroups();
        expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], { relativeTo: expect.anything() });
      });
    });

    describe('When creating a llm-proxy endpoint group', () => {
      it('should be possible', async () => {
        await fillOutAndValidateGeneralInformation();
        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        await harness.setConfigurationInputValue('llmProxyParam', 'my-llm-proxy-param');
        await harness.setConfigurationInputValue('target', 'http://target.gio');
        expect(await harness.isConfigurationStepValid()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'llm-proxy',
          loadBalancer: { type: 'ROUND_ROBIN' },
          endpoints: [
            { ...EndpointV4Default.byTypeAndGroupName('llm-proxy', ENDPOINT_GROUP_NAME), configuration: { target: 'http://target.gio' } },
          ],
          sharedConfiguration: {
            llmProxyParam: 'my-llm-proxy-param',
          },
        });
      });
    });
  });

  describe('V4 API - Native Kafka', () => {
    beforeEach(async () => {
      await initComponent(fakeNativeKafkaApiV4({ id: API_ID }));

      expectConfigurationSchemaGet('native-kafka', fakeKafkaSchema);
      expectSharedConfigurationSchemaGet('native-kafka', {
        $schema: 'http://json-schema.org/draft-07/schema#',
        type: 'object',
        properties: {},
      });
    });

    describe('When creating a native-kafka endpoint group', () => {
      it('should be possible', async () => {
        // Fill General information
        expect(await harness.isGeneralStepSelected()).toEqual(true);
        await harness.setNameValue(ENDPOINT_GROUP_NAME);
        await harness.validateGeneralInformation();
        expect(await harness.isGeneralStepSelected()).toEqual(false);
        expect(await isStepActive(harness.getConfigurationStep())).toEqual(true);

        expect(await harness.canCreateEndpointGroup()).toEqual(false);
        await harness.setConfigurationInputValue('bootstrapServers', 'bootstrap');
        expect(await harness.isConfigurationStepValid()).toEqual(true);
        expect(await harness.canCreateEndpointGroup()).toEqual(true);

        await createEndpointGroup({
          name: ENDPOINT_GROUP_NAME,
          type: 'native-kafka',
          loadBalancer: { type: null },
          endpoints: [
            {
              ...EndpointV4Default.byTypeAndGroupName('native-kafka', ENDPOINT_GROUP_NAME),
              configuration: {
                bootstrapServers: 'bootstrap',
              },
            },
          ],
          sharedConfiguration: {},
        });
      });
    });
  });

  /**
   * Expect requests
   */
  function expectApiGet(): void {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectConfigurationSchemaGet(endpointId = 'kafka', schema: any = fakeKafkaSchema): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${endpointId}/schema`, method: 'GET' })
      .flush(schema);
  }

  function expectSharedConfigurationSchemaGet(endpointId = 'kafka', schema: any = fakeKafkaSharedSchema): void {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints/${endpointId}/shared-configuration-schema`,
        method: 'GET',
      })
      .flush(schema);
  }

  function expectApiPut(updatedApi: ApiV4): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
      method: 'PUT',
    });
    expect(req.request.body.endpointGroups).toEqual(updatedApi.endpointGroups);
    expect(req.request.body.listeners).toStrictEqual(updatedApi.listeners);
    req.flush(updatedApi);
  }

  function expectEndpointListGet(): void {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`,
        method: 'GET',
      })
      .flush(ENDPOINT_LIST);
  }

  function expectEntrypointListGet(): void {
    httpTestingController
      .expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints`, method: 'GET' })
      .flush(ENTRYPOINT_LIST);
  }

  /**
   * Helpers
   */
  async function isStepActive(step: Promise<MatStepHarness>): Promise<boolean> {
    return step.then(foundStep => foundStep.isSelected());
  }

  async function fillOutAndValidateEndpointSelection(type = 'kafka'): Promise<void> {
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(true);
    await harness.selectEndpointGroup(type);
    if (type !== 'mock') {
      expectConfigurationSchemaGet(type);
      expectSharedConfigurationSchemaGet(type);
    }
    await harness.validateEndpointGroupSelection();
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(false);
    expect(await harness.isGeneralStepSelected()).toEqual(true);
  }

  async function fillOutAndValidateGeneralInformation(endpointGroupName = ENDPOINT_GROUP_NAME): Promise<void> {
    expect(await harness.isGeneralStepSelected()).toEqual(true);
    await harness.setNameValue(endpointGroupName);
    await harness.setLoadBalancerValue('ROUND_ROBIN');
    await harness.validateGeneralInformation();
    expect(await harness.isGeneralStepSelected()).toEqual(false);
    expect(await isStepActive(harness.getConfigurationStep())).toEqual(true);
  }

  async function chooseEndpointTypeAndGoToConfiguration(
    type = 'kafka',
    schema: any = fakeKafkaSchema,
    sharedSchema: any = fakeKafkaSharedSchema,
  ): Promise<void> {
    // Choose endpoint type
    await harness.getEndpointGroupTypeStep().then(step => step.select());
    await harness.selectEndpointGroup(type);

    if (type !== 'mock') {
      expectConfigurationSchemaGet(type, schema);
      expectSharedConfigurationSchemaGet(type, sharedSchema);
    }

    // Go to configuration page
    expect(await harness.canGoToGeneralStep()).toEqual(true);
    await harness.validateEndpointGroupSelection();
    expect(await harness.isEndpointGroupTypeStepSelected()).toEqual(false);

    expect(await harness.isGeneralStepSelected()).toEqual(true);
    await harness.validateGeneralInformation();
    expect(await isStepActive(harness.getConfigurationStep())).toEqual(true);

    if (type === 'mock') {
      expect(await harness.isEndpointGroupMockBannerShown()).toEqual(true);
      expect(await harness.isConfigurationFormShown()).toEqual(false);
      expect(await harness.canCreateEndpointGroup()).toEqual(true);
    } else {
      expect(await harness.isInheritedConfigurationBannerShown()).toEqual(true);
      expect(await harness.isConfigurationFormShown()).toEqual(true);
      expect(await harness.canCreateEndpointGroup()).toEqual(false);
    }
  }

  async function createDlqEndpointGroup(endpointGroup: EndpointGroupV4, dlqEntrypoint: string): Promise<void> {
    await harness.createDlqEndpointGroup();
    // need to clone deep to be able to modify listeners
    const updatedApi = cloneDeep(api);
    updatedApi.endpointGroups = [...api.endpointGroups, endpointGroup];
    updatedApi.listeners
      .flatMap(listener => listener.entrypoints)
      .filter(entrypoint => entrypoint.type === dlqEntrypoint)
      .forEach(entrypoint => (entrypoint.dlq = { endpoint: endpointGroup.name }));
    expectApiGet();
    expectApiPut(updatedApi);
    expect(routerNavigationSpy).toHaveBeenCalledWith(['../../entrypoints/', dlqEntrypoint], { relativeTo: expect.anything() });
  }
  async function createEndpointGroup(endpointGroup: EndpointGroupV4): Promise<void> {
    await harness.createEndpointGroup();
    const updatedApi = { ...api };
    updatedApi.endpointGroups = [...api.endpointGroups, endpointGroup];

    expectApiGet();
    expectApiPut(updatedApi);
    expect(routerNavigationSpy).toHaveBeenCalledWith(['../'], { relativeTo: expect.anything() });
  }
});
