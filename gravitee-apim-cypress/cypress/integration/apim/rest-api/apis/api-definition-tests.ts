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

import { ApiImportFakers } from '@fakers/api-imports';
import {
  deleteApi,
  importCreateApi,
  patchApiDefinition,
  getApiDefinition,
  startApi,
  deployApi,
  stopApi,
} from '@commands/management/api-management-commands';
import { ADMIN_USER } from '@fakers/users/users';
import { ApiMetadataFormat, ApiPageType } from '@model/apis';
import { deletePage } from '@commands/management/api-pages-management-commands';
import faker from 'faker';
import { ApiFakers } from '@fakers/apis';
import { deletePlan, publishPlan } from '@commands/management/api-plan-management-commands';
import { PlanFakers } from '@fakers/plans';

context('API - Patch', () => {
  let apiId;
  let crossId;
  let pageIds;
  let publishedPlanId;
  let stagingPlanId;
  let addedPlanId;
  const fakePlan1 = ApiImportFakers.plan({ name: 'test plan 1', description: 'this is a test plan', order: 1 });
  const fakePlan2 = ApiImportFakers.plan({ name: 'test plan 2', description: 'this is a test plan', order: 2 });
  const fakeApi = ApiImportFakers.api({
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
      ApiImportFakers.page({
        type: ApiPageType.FOLDER,
        content: null,
      }),
      ApiImportFakers.page({
        type: ApiPageType.FOLDER,
        parentId: '29b97194-8786-48cb-8162-d3989ce5ad48',
        content: null,
      }),
      ApiImportFakers.page({
        parentId: '7ef6a60d-3c29-459d-b05b-3d74ade03fa6',
      }),
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

  before(() => {
    importCreateApi(ADMIN_USER, fakeApi)
      .ok()
      .should((response) => {
        apiId = response.body.id;
        publishedPlanId = response.body.plans[0].id;
        stagingPlanId = response.body.plans[1].id;
        expect(apiId).to.not.be.null;
        expect(apiId).to.not.be.empty;
      })
      .then(() => {
        publishPlan(ADMIN_USER, apiId, publishedPlanId)
          .should((response) => {
            expect(response.status).to.equal(200);
            expect(response.body.status).to.equal('PUBLISHED');
          })
          .then(() => {
            startApi(ADMIN_USER, apiId)
              .should((response) => {
                expect(response.status).to.equal(204);
              })
              .then(() => {
                deployApi(ADMIN_USER, apiId).ok();
              });
          });
      });
  });
  it('should get API definition', () => {
    getApiDefinition(ADMIN_USER, apiId)
      .ok()
      .should((response) => {
        crossId = response.body.crossId;
        expect(crossId).to.not.be.null;
        expect(crossId).to.not.be.empty;
        const pages = response.body.pages;
        expect(pages).to.not.be.null;
        expect(pages).to.not.be.empty;
        pageIds = pages.filter((p) => p.type != 'SYSTEM_FOLDER').map((p) => p.id);
      });
  });

  it('should replace simple properties', () => {
    const name = faker.commerce.productName();
    const version = ApiFakers.version();
    const description = faker.commerce.productDescription();
    const visibility = 'PUBLIC';
    const flowMode = 'BEST_MATCH';
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.name',
        value: name,
      },
      {
        jsonPath: '$.version',
        value: version,
      },
      {
        jsonPath: '$.description',
        value: description,
      },
      {
        jsonPath: '$.visibility',
        value: visibility,
      },
      {
        jsonPath: '$.flow_mode',
        value: flowMode,
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.name).to.eq(name);
        expect(response.body.version).to.eq(version);
        expect(response.body.description).to.eq(description);
        expect(response.body.visibility).to.eq(visibility);
        expect(response.body.flow_mode).to.eq(flowMode);
      });
  });

  it('should set properties', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.properties',
        value: [
          { key: 'a', value: '0' },
          { key: 'b', value: '1' },
        ],
        operation: 'REPLACE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.properties.length).to.eq(2);
        expect(response.body.properties[0].key).to.eq('a');
        expect(response.body.properties[0].value).to.eq('0');
        expect(response.body.properties[1].key).to.eq('b');
        expect(response.body.properties[1].value).to.eq('1');
      });
  });

  it('should stop operation if test failed', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.properties',
        value: 'null',
        operation: 'TEST',
      },
      {
        jsonPath: '$.properties',
        value: { key: 'a', value: '0' },
        operation: 'ADD',
      },
    ]).noContent();
  });

  it('should remove property', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.properties[?(@.key == 'a')]",
        operation: 'REMOVE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.properties.length).to.eq(1);
        expect(response.body.properties[0].key).to.eq('b');
        expect(response.body.properties[0].value).to.eq('1');
      });
  });

  it('should add endpoint', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints",
        value: { name: 'new-endpoint', target: 'http://localhost:8080' },
        operation: 'ADD',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.proxy.groups[0].endpoints.length).to.eq(2);
      });
  });

  it('should switch endpoint backup', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'default')].backup",
        value: true,
      },
      {
        jsonPath: "$.proxy.groups[?(@.name == 'default-group')].endpoints[?(@.name == 'new-endpoint')].backup",
        value: false,
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.proxy.groups[0].endpoints[0].backup).to.eq(true);
        expect(response.body.proxy.groups[0].endpoints[1].backup).to.eq(false);
      });
  });

  it('should update proxy group configuration', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
    ])
      .ok()
      .should((response) => {
        expect(response.body.proxy.groups[0].http.connectTimeout).to.eq(1500);
        expect(response.body.proxy.groups[0].load_balancing.type).to.eq('RANDOM');
        expect(response.body.proxy.groups[0].endpoints[0].weight).to.eq(10);
      });
  });

  it('should add resource', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
        operation: 'ADD',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.resources.length).to.eq(2);
      });
  });

  it('should not add resource with wrong configuration', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
        operation: 'ADD',
      },
    ])
      .badRequest()
      .should((response) => {
        expect(response.body.message).to.eq('#/timeToIdleSeconds: -10 is not greater or equal to 0');
      });
  });

  it('should update resource configuration', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
    ])
      .ok()
      .should((response) => {
        expect(response.body.resources.length).to.eq(2);
        const cacheResource = response.body.resources.find((r) => r.name === 'cache_name_2');
        expect(cacheResource.enabled).to.eq(false);
        expect(cacheResource.configuration.timeToIdleSeconds).to.eq(100);
        expect(cacheResource.configuration.timeToLiveSeconds).to.eq(200);
        expect(cacheResource.configuration.maxEntriesLocalHeap).to.eq(10);
      });
  });

  it('should not update resource with wrong configuration', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
    ])
      .badRequest()
      .should((response) => {
        expect(response.body.message).to.eq('#/timeToLiveSeconds: -5 is not greater or equal to 0');
      });
  });

  it('should add flows', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
    ])
      .ok()
      .should((response) => {
        expect(response.body.flows.length).to.eq(1);
        expect(response.body.flows[0].methods.length).to.eq(3);
      });
  });

  it('should update flows', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].methods",
        value: ['GET'],
        operation: 'REPLACE',
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
        operation: 'REPLACE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.flows.length).to.eq(1);
        expect(response.body.flows[0].methods.length).to.eq(1);
        expect(response.body.flows[0].pre.length).to.eq(1);
        expect(response.body.flows[0].pre[0].policy).to.eq('mock');
        expect(response.body.flows[0].pre[0].configuration.status).to.eq('200');
        expect(response.body.flows[0].pre[0].configuration.content).to.eq('{#request.attributes.api}');
      });
  });

  it('should update policy', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].pre[?(@.name == 'Mock policy')].configuration",
        value: {
          status: '500',
          content: '{#request.attributes.application}',
        },
        operation: 'REPLACE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.flows.length).to.eq(1);
        expect(response.body.flows[0].pre.length).to.eq(1);
        expect(response.body.flows[0].pre[0].policy).to.eq('mock');
        expect(response.body.flows[0].pre[0].configuration.status).to.eq('500');
        expect(response.body.flows[0].pre[0].configuration.content).to.eq('{#request.attributes.application}');
      });
  });

  it('should remove policy', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.flows[?(@.path-operator.path == '/')].pre[?(@.name == 'Mock policy')]",
        operation: 'REMOVE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.flows.length).to.eq(1);
        expect(response.body.flows[0].pre.length).to.eq(0);
      });
  });

  it('should not replace id', () => {
    const fakeId = 'foobar';
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.id',
        value: fakeId,
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.id).not.to.eq(fakeId);
        expect(response.body.id).to.eq(apiId);
      });
  });

  it('should not replace crossId', () => {
    const fakeId = 'foobar';
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.crossId',
        value: fakeId,
      },
    ]).notFound();
  });

  it('should not remove members', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.members',
        operation: 'REMOVE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.members).not.to.eq([]);
      });
  });

  it('should not replace members', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.members',
        value: [],
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.members).not.to.eq([]);
      });
  });

  it('should not update members', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: "$.members[?(@.sourceId == 'admin')].sourceId",
        value: 'foobar',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.members[0].sourceId).to.eq('admin');
      });
  });

  it('should not add member', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.members',
        value: {
          source: 'memory',
          sourceId: 'admin',
          roles: ['89f58752-bde4-4bc9-b587-52bde48bc968'],
        },
        operation: 'ADD',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.members.length).to.eq(1);
      });
  });

  it('should not remove pages', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.pages',
        value: [],
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.pages).not.to.eq([]);
      });
  });

  it('should add plan', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans`,
        operation: 'ADD',
        value: PlanFakers.plan(),
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.plans.length).to.eq(3);
        addedPlanId = response.body.plans.find((p) => ![publishedPlanId, stagingPlanId].includes(p.id)).id;
        expect(addedPlanId).not.to.be.undefined;
      });
  });

  it('should add flow to plan', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
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
    ])
      .ok()
      .should((response) => {
        expect(response.body.plans.length).to.eq(3);
        const plan = response.body.plans.find((p) => p.id === addedPlanId);
        expect(plan.flows.length).to.eq(1);
        expect(plan.flows[0].post.length).to.eq(1);
      });
  });

  it('should update policy of plan', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans[?(@.id == '${addedPlanId}')].flows[0].post[0].configuration.content`,
        value: 'foobar',
      },
    ])
      .ok()
      .should((response) => {
        const plan = response.body.plans.find((p) => p.id === addedPlanId);
        expect(plan.flows.length).to.eq(1);
        expect(plan.flows[0].post.length).to.eq(1);
        expect(plan.flows[0].post[0].configuration.content).to.eq('foobar');
      });
  });

  it('should remove policy of plan', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans[?(@.id == '${addedPlanId}')].flows[0].post[0]`,
        operation: 'REMOVE',
      },
    ])
      .ok()
      .should((response) => {
        const plan = response.body.plans.find((p) => p.id === addedPlanId);
        expect(plan.flows.length).to.eq(1);
        expect(plan.flows[0].post.length).to.eq(0);
      });
  });

  it('should not published plan', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans[?(@.id == '${stagingPlanId}')].status`,
        value: 'PUBLISHED',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.plans.find((p) => p.id === stagingPlanId).status).to.eq('STAGING');
      });
  });

  it('should remove plan without subscription', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans[?(@.id == '${stagingPlanId}')]`,
        operation: 'REMOVE',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.plans.length).to.eq(2);
      });
  });

  it('should remove all plans without subscriptions', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: `$.plans`,
        value: [],
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.plans.length).to.eq(0);
      });
  });

  it('should not update primary owner', () => {
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.primaryOwner.id',
        value: 'fake-id',
      },
      {
        jsonPath: '$.primaryOwner.displayName',
        value: 'foobar',
      },
    ])
      .ok()
      .should((response) => {
        expect(response.body.primaryOwner.id).not.to.eq('fake-id');
        expect(response.body.primaryOwner.displayName).not.to.eq('foobar');
      });
  });

  it('should not downgrade definition version', () => {
    const definitionVersion = '1.0.0';
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.gravitee',
        value: definitionVersion,
      },
    ])
      .badRequest()
      .should((response) => {
        expect(response.body.message).to.eq('You are not allowed to downgrade definition version');
      });
  });

  it('should not inject script', () => {
    const script = '<script src=”http://localhost:8080”></script>';
    patchApiDefinition(ADMIN_USER, apiId, [
      {
        jsonPath: '$.script',
        value: script,
      },
    ])
      .badRequest()
      .should((response) => {
        expect(response.body.message).to.eq('The json patch does not follow security policy : [Tag not allowed: script]');
      });
  });

  after(() => {
    stopApi(ADMIN_USER, apiId).should((response) => {
      expect(response.status).to.equal(204);
    });
    pageIds.reverse().forEach((pageId) => {
      deletePage(ADMIN_USER, apiId, pageId).noContent();
    });
    deleteApi(ADMIN_USER, apiId, false).noContent();
  });
});
