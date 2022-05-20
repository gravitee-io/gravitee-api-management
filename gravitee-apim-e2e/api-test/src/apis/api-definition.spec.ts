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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { fail, succeed } from '@lib/jest-utils';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { APIDefinitionApi } from '@management-apis/APIDefinitionApi';
import { APIsApi } from '@management-apis/APIsApi';
import { JsonPatchOperationEnum } from '@management-models/JsonPatch';
import { ApiMetadataFormat, ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { APIPagesApi } from '@management-apis/APIPagesApi';
import { PagesFaker } from '@management-fakers/PagesFaker';
import { forManagement } from '@client-conf/*';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const configuration = forManagement();
const apisApi = new APIsApi(configuration);
const apiPlansApi = new APIPlansApi(configuration);
const apiDefinitionApi = new APIDefinitionApi(configuration);

const fakePlan1 = PlansFaker.plan({ name: 'test plan 1', description: 'this is a test plan', order: 1 });
const fakePlan2 = PlansFaker.plan({ name: 'test plan 2', description: 'this is a test plan', order: 2 });

let api: string;
let pageIds: Array<string>;
let publishedPlanId: string;
let stagingPlanId: string;
let addedPlanId: string;

describe('API definition', () => {
  beforeAll(async () => {
    const apiImport = ApisFaker.apiImport({
      plans: [fakePlan1, fakePlan2],
      resources: [
        {
          name: 'cache_name',
          type: 'cache',
          enabled: true,
          configuration: {
            name: 'my-cache',
            timeToIdleSeconds: 1,
            timeToLiveSeconds: 2,
            maxEntriesLocalHeap: 1000,
          },
        },
      ],
      pages: [
        PagesFaker.folder(),
        PagesFaker.folder({ parentId: '29b97194-8786-48cb-8162-d3989ce5ad48' }),
        PagesFaker.page({ parentId: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6' }),
      ],
      metadata: [
        {
          key: 'team',
          name: 'team',
          format: ApiMetadataFormat.STRING,
          value: 'Ops',
        },
      ],
    });

    const apiEntity = await apisApi.importApiDefinition({
      orgId,
      envId,
      definitionVersion: '2.0.0',
      body: apiImport,
    });
    api = apiEntity.id;
    publishedPlanId = apiEntity.plans[0].id;
    stagingPlanId = apiEntity.plans[1].id;

    const plan = await apiPlansApi.publishApiPlan({ orgId, envId, api, plan: publishedPlanId });
    expect(plan.status).toEqual('PUBLISHED');

    const apiStartedResponse = await apisApi.doApiLifecycleActionRaw({
      orgId,
      envId,
      api,
      action: LifecycleAction.START,
    });
    expect(apiStartedResponse.raw.status).toEqual(204);

    const apiDeployedResponse = await apisApi.deployApiRaw({ orgId, envId, api: api });

    expect(apiDeployedResponse.raw.status).toEqual(200);
    const response = await apiDefinitionApi.getApiDefinition({
      orgId,
      envId,
      api,
    });

    // @ts-ignore
    const { pages, crossId } = JSON.parse(response);
    expect(crossId).not.toBeNull();
    expect(pages).toBeDefined();
    pageIds = pages.filter((p) => p.type != 'SYSTEM_FOLDER').map((p) => p.id);
    expect(pages.length).toEqual(2);

    return apiEntity;
  });

  test('should replace simple properties', async () => {
    const nameExpected = faker.commerce.productName();
    const versionExpected = ApisFaker.version();
    const descriptionExpected = faker.commerce.productDescription();
    const visibilityExpected = 'PUBLIC';
    const flowModeExpected = 'BEST_MATCH';

    const jsonPatch = [
      {
        jsonPath: '$.name',
        value: nameExpected,
      },
      {
        jsonPath: '$.version',
        value: versionExpected,
      },
      {
        jsonPath: '$.description',
        value: descriptionExpected,
      },
      {
        jsonPath: '$.visibility',
        value: visibilityExpected,
      },
      {
        jsonPath: '$.flow_mode',
        value: flowModeExpected,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { name, version, description, visibility, flow_mode } = JSON.parse(apiDefinitionUpdated);
    expect(name).toEqual(nameExpected);
    expect(version).toEqual(versionExpected);
    expect(description).toEqual(descriptionExpected);
    expect(visibility).toEqual(visibilityExpected);
    expect(flow_mode).toEqual(flowModeExpected);
  });

  test('should set properties', async () => {
    const propertiesExpected = [
      { key: 'a', value: '0' },
      { key: 'b', value: '1' },
    ];
    const jsonPatch = [
      {
        jsonPath: '$.properties',
        value: propertiesExpected,
        operation: JsonPatchOperationEnum.REPLACE,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { properties } = JSON.parse(apiDefinitionUpdated);
    expect(properties.length).toEqual(propertiesExpected.length);
    expect(properties[0].key).toEqual(propertiesExpected[0].key);
    expect(properties[0].value).toEqual(propertiesExpected[0].value);
    expect(properties[1].key).toEqual(propertiesExpected[1].key);
    expect(properties[1].value).toEqual(propertiesExpected[1].value);
  });

  test('should stop operation if test failed', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.properties',
        value: 'null',
        operation: JsonPatchOperationEnum.TEST,
      },
      {
        jsonPath: '$.properties',
        value: { key: 'a', value: '0' },
        operation: JsonPatchOperationEnum.ADD,
      },
    ];

    await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }), 204);
  });

  test('should remove property', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.properties[?(@.key == 'a')]",
        operation: JsonPatchOperationEnum.REMOVE,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { properties } = JSON.parse(apiDefinitionUpdated);
    expect(properties.length).toEqual(1);
    expect(properties[0].key).toEqual('b');
    expect(properties[0].value).toEqual('1');
  });

  test('should add endpoint', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints",
        value: { name: 'new-endpoint', target: 'http://localhost:8080' },
        operation: JsonPatchOperationEnum.ADD,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { proxy } = JSON.parse(apiDefinitionUpdated);
    expect(proxy.groups[0].endpoints.length).toEqual(2);
  });

  test('should switch endpoint backup', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
        value: true,
      },
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup",
        value: false,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { proxy } = JSON.parse(apiDefinitionUpdated);
    expect(proxy.groups[0].endpoints.length).toEqual(2);
    expect(proxy.groups[0].endpoints[0].backup).toEqual(true);
    expect(proxy.groups[0].endpoints[1].backup).toEqual(false);
  });

  test('should update proxy group configuration', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].http.connectTimeout",
        value: 1500,
      },
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].load_balancing.type",
        value: 'RANDOM',
      },
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].weight",
        value: 10,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { proxy } = JSON.parse(apiDefinitionUpdated);
    expect(proxy.groups[0].http.connectTimeout).toEqual(1500);
    expect(proxy.groups[0].load_balancing.type).toEqual('RANDOM');
    expect(proxy.groups[0].endpoints[0].weight).toEqual(10);
  });

  test('should add resource', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.resources',
        value: {
          name: 'cache_name_2',
          type: 'cache',
          enabled: true,
          configuration: {
            name: 'my-cache',
            timeToIdleSeconds: 1,
            timeToLiveSeconds: 2,
            maxEntriesLocalHeap: 1000,
          },
        },
        operation: JsonPatchOperationEnum.ADD,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { resources } = JSON.parse(apiDefinitionUpdated);
    expect(resources.length).toEqual(2);
  });

  test('should not add resource with wrong configuration', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.resources',
        value: {
          name: 'cache_name_3',
          type: 'cache',
          enabled: true,
          configuration: {
            timeToIdleSeconds: -10,
          },
        },
        operation: JsonPatchOperationEnum.ADD,
      },
    ];

    await fail(
      apiDefinitionApi.patch({
        orgId,
        envId,
        api,
        jsonPatch,
      }),
      400,
      '#/timeToIdleSeconds: -10 is not greater or equal to 0',
    );
  });

  test('should update resource configuration', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.resources[?(@.name == 'cache_name_2')].enabled",
        value: false,
      },
      {
        jsonPath: "$.resources[?(@.name == 'cache_name_2')].configuration",
        value: {
          name: 'my-cache',
          timeToIdleSeconds: 100,
          timeToLiveSeconds: 200,
          maxEntriesLocalHeap: 10,
        },
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { resources } = JSON.parse(apiDefinitionUpdated);
    expect(resources.length).toEqual(2);
    const cacheResource = resources.find((r) => r.name === 'cache_name_2');
    expect(cacheResource.enabled).toEqual(false);
    expect(cacheResource.configuration.timeToIdleSeconds).toEqual(100);
    expect(cacheResource.configuration.timeToLiveSeconds).toEqual(200);
    expect(cacheResource.configuration.maxEntriesLocalHeap).toEqual(10);
  });

  test('should not update resource with wrong configuration', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.resources[?(@.name == 'cache_name_2')].enabled",
        value: true,
      },
      {
        jsonPath: "$.resources[?(@.name == 'cache_name_2')].configuration",
        value: {
          name: 'my-cache',
          timeToIdleSeconds: 100,
          timeToLiveSeconds: -5,
          maxEntriesLocalHeap: 10,
        },
      },
    ];

    await fail(
      apiDefinitionApi.patch({
        orgId,
        envId,
        api,
        jsonPatch,
      }),
      400,
      '#/timeToLiveSeconds: -5 is not greater or equal to 0',
    );
  });

  test('should add flows', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.flows',
        value: [
          {
            name: 'ALL',
            methods: ['GET', 'POST', 'PUT'],
            'path-operator': {
              path: '/',
            },
          },
        ],
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { flows } = JSON.parse(apiDefinitionUpdated);
    expect(flows.length).toEqual(1);
    expect(flows[0].methods.length).toEqual(3);
  });

  test('should update flows', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].methods",
        value: ['GET'],
      },
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].pre",
        value: [
          {
            policy: 'mock',
            name: 'Mock policy',
            configuration: {
              status: '200',
              content: '{#request.attributes.api}',
            },
          },
        ],
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { flows } = JSON.parse(apiDefinitionUpdated);
    expect(flows.length).toEqual(1);
    expect(flows[0].methods.length).toEqual(1);
    expect(flows[0].pre.length).toEqual(1);
    expect(flows[0].pre[0].policy).toEqual('mock');
    expect(flows[0].pre[0].configuration.status).toEqual('200');
    expect(flows[0].pre[0].configuration.content).toEqual('{#request.attributes.api}');
  });

  test('should update policy', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].pre[?(@.name == 'Mock policy')].configuration",
        value: {
          status: '500',
          content: '{#request.attributes.application}',
        },
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { flows } = JSON.parse(apiDefinitionUpdated);

    expect(flows.length).toEqual(1);
    expect(flows[0].pre.length).toEqual(1);
    expect(flows[0].pre[0].policy).toEqual('mock');
    expect(flows[0].pre[0].configuration.status).toEqual('500');
    expect(flows[0].pre[0].configuration.content).toEqual('{#request.attributes.application}');
  });

  test('should remove policy', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].pre[?(@.name == 'Mock policy')]",
        operation: JsonPatchOperationEnum.REMOVE,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { flows } = JSON.parse(apiDefinitionUpdated);

    expect(flows.length).toEqual(1);
    expect(flows[0].pre.length).toEqual(0);
  });

  test('should not replace id', async () => {
    const fakeId = 'foobar';
    const jsonPatch = [
      {
        jsonPath: '$.id',
        value: fakeId,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { id } = JSON.parse(apiDefinitionUpdated);

    expect(id).not.toEqual(fakeId);
    expect(id).toEqual(api);
  });

  test('should not replace crossId', async () => {
    const fakeId = 'foobar';
    const jsonPatch = [
      {
        jsonPath: '$.crossId',
        value: fakeId,
      },
    ];

    await fail(apiDefinitionApi.patch({ orgId, envId, api, jsonPatch }), 404);
  });

  test('should not remove members', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.members',
        operation: JsonPatchOperationEnum.REMOVE,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { members } = JSON.parse(apiDefinitionUpdated);
    expect(members).not.toEqual([]);
  });

  test('should not replace members', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.members',
        value: [],
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { members } = JSON.parse(apiDefinitionUpdated);

    expect(members).not.toEqual([]);
  });

  test('should not update members', async () => {
    const jsonPatch = [
      {
        jsonPath: "$.members[?(@.sourceId == 'admin')].sourceId",
        value: 'foobar',
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { members } = JSON.parse(apiDefinitionUpdated);

    expect(members[0].sourceId).toEqual('admin');
  });

  test('should not add member', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.members',
        value: {
          source: 'memory',
          sourceId: 'admin',
          roles: ['89f58752-bde4-4bc9-b587-52bde48bc968'],
        },
        operation: JsonPatchOperationEnum.ADD,
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { members } = JSON.parse(apiDefinitionUpdated);

    expect(members.length).toEqual(1);
  });

  test('should not remove pages', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.pages',
        value: [],
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { pages } = JSON.parse(apiDefinitionUpdated);

    expect(pages).not.toEqual([]);
  });

  test('should add plan', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans`,
        operation: JsonPatchOperationEnum.ADD,
        value: PlansFaker.plan(),
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { plans } = JSON.parse(apiDefinitionUpdated);

    expect(plans.length).toEqual(3);
    addedPlanId = plans.find((p) => ![publishedPlanId, stagingPlanId].includes(p.id)).id;
    expect(addedPlanId).toBeDefined();
  });

  test('should add flow to plan', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans[?(@.id == '${addedPlanId}')].flows`,
        value: [
          {
            name: 'Flow plan',
            'path-operator': {
              path: '/',
            },
            post: [
              {
                policy: 'mock',
                name: 'Mock policy',
                configuration: {
                  status: '200',
                  content: '{#request.attributes.api}',
                },
              },
            ],
          },
        ],
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { plans } = JSON.parse(apiDefinitionUpdated);
    expect(plans.length).toEqual(3);
    const plan = plans.find((p) => p.id === addedPlanId);
    expect(plan.flows.length).toEqual(1);
    expect(plan.flows[0].post.length).toEqual(1);
  });

  test('should update policy of plan', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans[?(@.id == '${addedPlanId}')].flows[0].post[0].configuration.content`,
        value: 'foobar',
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { plans } = JSON.parse(apiDefinitionUpdated);
    const plan = plans.find((p) => p.id === addedPlanId);
    expect(plan.flows.length).toEqual(1);
    expect(plan.flows[0].post.length).toEqual(1);
    expect(plan.flows[0].post[0].configuration.content).toEqual('foobar');
  });

  test('should remove policy of plan', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans[?(@.id == '${addedPlanId}')].flows[0].post[0]`,
        operation: JsonPatchOperationEnum.REMOVE,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));

    const { plans } = JSON.parse(apiDefinitionUpdated);
    const plan = plans.find((p) => p.id === addedPlanId);
    expect(plan.flows.length).toEqual(1);
    expect(plan.flows[0].post.length).toEqual(0);
  });

  test('should not published plan', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans[?(@.id == '${stagingPlanId}')].status`,
        value: 'PUBLISHED',
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { plans } = JSON.parse(apiDefinitionUpdated);

    expect(plans.find((p) => p.id === stagingPlanId).status).toEqual('STAGING');
  });

  test('should remove plan without subscription', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans[?(@.id == '${stagingPlanId}')]`,
        operation: JsonPatchOperationEnum.REMOVE,
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { plans } = JSON.parse(apiDefinitionUpdated);

    expect(plans.length).toEqual(2);
  });

  test('should remove all plans without subscriptions', async () => {
    const jsonPatch = [
      {
        jsonPath: `$.plans`,
        value: [],
      },
    ];
    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { plans } = JSON.parse(apiDefinitionUpdated);

    expect(plans.length).toEqual(0);
  });

  test('should not update primary owner', async () => {
    const jsonPatch = [
      {
        jsonPath: '$.primaryOwner.id',
        value: 'fake-id',
      },
      {
        jsonPath: '$.primaryOwner.displayName',
        value: 'foobar',
      },
    ];

    const apiDefinitionUpdated = await succeed(apiDefinitionApi.patchRaw({ orgId, envId, api, jsonPatch }));
    const { primaryOwner } = JSON.parse(apiDefinitionUpdated);

    expect(primaryOwner.id).not.toEqual('fake-id');
    expect(primaryOwner.displayName).not.toEqual('foobar');
  });

  test('should not downgrade definition version', async () => {
    const definitionVersion = '1.0.0';
    const jsonPatch = [
      {
        jsonPath: '$.gravitee',
        value: definitionVersion,
      },
    ];

    await fail(
      apiDefinitionApi.patch({
        orgId,
        envId,
        api,
        jsonPatch,
      }),
      400,
      'You are not allowed to downgrade definition version',
    );
  });

  test('should not inject script', async () => {
    const script = '<script src=”http://localhost:8080”></script>';
    const jsonPatch = [
      {
        jsonPath: '$.script',
        value: script,
      },
    ];

    await fail(
      apiDefinitionApi.patch({ orgId, envId, api, jsonPatch }),
      400,
      'The json patch does not follow security policy : [Tag not allowed: script]',
    );
  });

  afterAll(async () => {
    const apiStoppedResponse = await apisApi.doApiLifecycleActionRaw({
      orgId,
      envId,
      api,
      action: LifecycleAction.STOP,
    });
    expect(apiStoppedResponse.raw.status).toEqual(204);
    const apiPagesApi = new APIPagesApi(configuration);

    await Promise.all(pageIds.reverse().map((page) => apiPagesApi.deleteApiPage({ orgId, envId, api, page })));

    return await apisApi.deleteApi({ orgId, envId, api: api });
  });
});
