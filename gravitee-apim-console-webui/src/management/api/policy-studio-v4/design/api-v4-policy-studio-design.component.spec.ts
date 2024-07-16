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
import { importProvidersFrom } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { GioPolicyStudioHarness } from '@gravitee/ui-policy-studio-angular/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { of } from 'rxjs';
import { GioFormJsonSchemaModule, GioLicenseTestingModule } from '@gravitee/ui-particles-angular';
import { GioPolicyStudioComponent } from '@gravitee/ui-policy-studio-angular';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ApiV4PolicyStudioDesignComponent } from './api-v4-policy-studio-design.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { ApiV4PolicyStudioModule } from '../api-v4-policy-studio.module';
import {
  Api,
  ApiPlansResponse,
  ApiV4,
  ConnectorPlugin,
  fakeApiV4,
  fakeConnectorPlugin,
  fakePlanV4,
  fakePoliciesPlugin,
  fakePolicyPlugin,
  FlowV4,
  PlanV4,
} from '../../../../entities/management-api-v2';

describe('ApiV4PolicyStudioDesignComponent', () => {
  const API_ID = 'api-id';

  let fixture: ComponentFixture<ApiV4PolicyStudioDesignComponent>;
  let component: ApiV4PolicyStudioDesignComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiV4PolicyStudioModule, MatIconTestingModule, GioLicenseTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: API_ID } } } },
        importProvidersFrom(GioFormJsonSchemaModule),
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiV4PolicyStudioDesignComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    (fixture.debugElement.query(By.directive(GioPolicyStudioComponent)).componentInstance as GioPolicyStudioComponent).enableSavingTimer =
      false;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('MESSAGE API type', () => {
    let api: ApiV4;
    let planA: PlanV4;
    let policyStudioHarness: GioPolicyStudioHarness;

    beforeEach(async () => {
      api = fakeApiV4({
        id: API_ID,
        name: 'my brand new API',
        type: 'MESSAGE',
        listeners: [
          {
            type: 'SUBSCRIPTION',
            entrypoints: [
              {
                type: 'webhook',
              },
            ],
          },
        ],
        endpointGroups: [
          {
            name: 'default-group',
            type: 'kafka',
            endpoints: [
              {
                name: 'default',
                type: 'kafka',
                weight: 1,
                inheritConfiguration: false,
                configuration: {
                  bootstrapServers: 'localhost:9092',
                },
              },
            ],
          },
        ],
        flows: [
          {
            name: 'my flow',
            enabled: true,
            selectors: [
              {
                channel: 'my-channel',
                channelOperator: 'EQUALS',
                type: 'CHANNEL',
              },
            ],
          },
        ],
      });

      planA = fakePlanV4({
        name: 'PlanA',
        flows: [
          {
            name: 'PlanA flow',
            selectors: [
              {
                type: 'CHANNEL',
                channel: 'my-channel',
                pathOperator: 'STARTS_WITH',
                condition: 'The condition',
                entrypoints: [],
                operations: [],
              },
            ],
            request: [
              {
                name: 'Mock',
                description: 'Saying hello to the world',
                enabled: true,
                policy: 'mock',
                configuration: { content: 'Hello world', status: '200' },
              },
            ],
            response: [],
            subscribe: [],
            publish: [],
            enabled: true,
          },
        ],
      });

      expectEntrypointsGetRequest([{ id: 'webhook', name: 'Webhook', supportedModes: ['SUBSCRIBE'] }]);
      expectEndpointsGetRequest([{ id: 'kafka', name: 'Kafka', supportedModes: ['PUBLISH', 'SUBSCRIBE'] }]);
      expectGetPolicies();

      expectListApiPlans(API_ID, [planA]);
      expectGetApi(api);

      policyStudioHarness = await loader.getHarness(GioPolicyStudioHarness);
    });

    it('should display simple policy studio', async () => {
      // Check that the component is correctly initialized
      expect(component.apiType).toEqual('MESSAGE');
      expect(component.commonFlows.length).toEqual(1);
      expect(component.endpointsInfo.length).toEqual(1);
      expect(component.entrypointsInfo.length).toEqual(1);
      expect(component.plans.length).toEqual(1);

      const policyStudioHarness = await loader.getHarness(GioPolicyStudioHarness);

      expect(await policyStudioHarness.getFlowsMenu()).toEqual([
        {
          name: planA.name,
          flows: [
            {
              infos: 'PUBSUBmy-channel',
              isSelected: true,
              name: 'PlanA flow',
              hasCondition: true,
            },
          ],
        },
        {
          name: 'Common flows',
          flows: [
            {
              infos: 'PUBSUBmy-channel',
              isSelected: false,
              name: 'my flow',
              hasCondition: true,
            },
          ],
        },
      ]);
    });

    it('should add api flow', async () => {
      const flowToAdd: FlowV4 = {
        name: 'New common flow',
        selectors: [
          {
            type: 'CHANNEL',
            channel: 'my-channel',
            pathOperator: 'STARTS_WITH',
            condition: 'The condition',
          },
        ],
      };

      await policyStudioHarness.addFlow('Common flows', flowToAdd);
      await policyStudioHarness.save();

      const updatedApi: ApiV4 = {
        ...api,
        flows: [
          {
            enabled: true,
            name: 'my flow',
            selectors: [
              {
                channel: 'my-channel',
                channelOperator: 'EQUALS',
                type: 'CHANNEL',
              },
            ],
          },
          {
            enabled: true,
            name: flowToAdd.name,
            selectors: [
              {
                channel: 'my-channel',
                channelOperator: 'EQUALS',
                entrypoints: [],
                operations: [],
                type: 'CHANNEL',
              },
            ],
          },
        ],
      };

      // Fetch fresh API before save
      expectGetApi(api);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        {
          enabled: true,
          name: 'my flow',
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              type: 'CHANNEL',
            },
          ],
        },
        {
          enabled: true,
          name: flowToAdd.name,
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              entrypoints: [],
              operations: [],
              type: 'CHANNEL',
            },
          ],
        },
      ]);
      req.flush(updatedApi);

      expectNewNgOnInit();
    });

    it('should add plan flow', async () => {
      const flowToAdd: FlowV4 = {
        name: 'New plan flow',
        selectors: [
          {
            type: 'CHANNEL',
            channel: 'my-channel',
            pathOperator: 'STARTS_WITH',
            condition: 'The condition',
          },
        ],
      };

      await policyStudioHarness.addFlow(planA.name, flowToAdd);
      await policyStudioHarness.save();

      // Fetch fresh Plan before save
      expectGetPlan(api.id, planA);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/plans/${planA.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        planA.flows[0],
        {
          enabled: true,
          name: flowToAdd.name,
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              entrypoints: [],
              operations: [],
              type: 'CHANNEL',
            },
          ],
        },
      ]);
      req.flush(planA);

      expectNewNgOnInit();
    });

    it('should save flow execution configuration', async () => {
      await policyStudioHarness.setFlowExecutionConfig({
        mode: 'BEST_MATCH',
        matchRequired: true,
      });
      await policyStudioHarness.save();

      // Fetch fresh API before save
      expectGetApi(api);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        {
          enabled: true,
          name: 'my flow',
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              type: 'CHANNEL',
            },
          ],
        },
      ]);
      expect(req.request.body.flowExecution).toEqual({
        mode: 'BEST_MATCH',
        matchRequired: true,
      });
      req.flush(api);

      expectNewNgOnInit();
    });

    it('should save add api flow and save flow execution configuration', async () => {
      const flowToAdd: FlowV4 = {
        name: 'New common flow',
        selectors: [
          {
            type: 'CHANNEL',
            channel: 'my-channel',
            pathOperator: 'STARTS_WITH',
            condition: 'The condition',
          },
        ],
      };

      await policyStudioHarness.addFlow('Common flows', flowToAdd);
      await policyStudioHarness.setFlowExecutionConfig({
        mode: 'BEST_MATCH',
        matchRequired: true,
      });
      await policyStudioHarness.save();

      // Fetch fresh API before save
      expectGetApi(api);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        {
          enabled: true,
          name: 'my flow',
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              type: 'CHANNEL',
            },
          ],
        },
        {
          enabled: true,
          name: flowToAdd.name,
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              entrypoints: [],
              operations: [],
              type: 'CHANNEL',
            },
          ],
        },
      ]);
      expect(req.request.body.flowExecution).toEqual({
        mode: 'BEST_MATCH',
        matchRequired: true,
      });
      req.flush(api);

      expectNewNgOnInit();
    });

    it('should add step policy into "my flow"', async () => {
      const policyToAdd = fakePolicyPlugin();

      // Override Fetcher function
      component.policySchemaFetcher = (_policy) => {
        return of({});
      };
      component.policyDocumentationFetcher = (_policy) => {
        return of('');
      };

      await policyStudioHarness.selectFlowInMenu('my flow');

      const requestPhase = await policyStudioHarness.getSelectedFlowPhase('REQUEST');

      await requestPhase.addStep(0, {
        policyName: policyToAdd.name,
        description: 'My policy step description',
      });
      expect(await requestPhase.getSteps()).toEqual([
        {
          name: 'Webhook',
          type: 'connector',
        },
        {
          name: policyToAdd.name,
          description: 'My policy step description',
          type: 'step',
          hasCondition: false,
        },
        {
          name: 'Kafka',
          type: 'connector',
        },
      ]);

      await policyStudioHarness.save();

      // Fetch fresh API before save
      expectGetApi(api);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        {
          enabled: true,
          name: 'my flow',
          request: [
            {
              description: 'My policy step description',
              enabled: true,
              name: 'Test policy',
              policy: 'test-policy',
              condition: undefined,
              configuration: undefined,
            },
          ],
          selectors: [
            {
              channel: 'my-channel',
              channelOperator: 'EQUALS',
              type: 'CHANNEL',
            },
          ],
        },
      ]);
    });

    it('should edit step into "PlanA"', async () => {
      // Override Fetcher function
      component.policySchemaFetcher = (_policy) => {
        return of({
          properties: {
            content: {
              type: 'string',
            },
            status: {
              type: 'string',
            },
          },
        });
      };
      component.policyDocumentationFetcher = (_policy) => {
        return of('');
      };

      await policyStudioHarness.selectFlowInMenu('PlanA');

      const requestPhase = await policyStudioHarness.getSelectedFlowPhase('REQUEST');
      await requestPhase.editStep(0, {
        description: 'New step description',
      });

      expect(await requestPhase.getSteps()).toEqual([
        {
          name: 'Webhook',
          type: 'connector',
        },
        {
          name: 'Mock',
          description: 'New step description',
          type: 'step',
          hasCondition: false,
        },
        {
          name: 'Kafka',
          type: 'connector',
        },
      ]);

      await policyStudioHarness.save();

      // Fetch fresh Plan before save
      expectGetPlan(api.id, planA);
      const req = httpTestingController.expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/plans/${planA.id}`,
        method: 'PUT',
      });
      expect(req.request.body.flows).toEqual([
        {
          ...planA.flows[0],
          request: [
            {
              ...planA.flows[0].request[0],
              description: 'New step description',
            },
          ],
        },
      ]);
    });
  });

  function expectGetApi(api: Api) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
        method: 'GET',
      })
      .flush(api);
  }

  function expectEntrypointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/entrypoints` }).flush(fullConnectors);
  }

  function expectEndpointsGetRequest(connectors: Partial<ConnectorPlugin>[]) {
    const fullConnectors = connectors.map((partial) => fakeConnectorPlugin(partial));
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/endpoints`,
        method: 'GET',
      })
      .flush(fullConnectors);
  }

  function expectListApiPlans(apiId: string, plans: PlanV4[]) {
    const fakeApiPlansResponse: ApiPlansResponse = {
      data: [...plans],
    };
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans?page=1&perPage=9999&statuses=PUBLISHED`,
        method: 'GET',
      })
      .flush(fakeApiPlansResponse);
  }

  function expectGetPlan(apiId: string, plan: PlanV4) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${apiId}/plans/${plan.id}`,
        method: 'GET',
      })
      .flush(plan);
  }

  function expectGetPolicies() {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies`,
        method: 'GET',
      })
      .flush([fakePolicyPlugin(), ...fakePoliciesPlugin()]);
  }

  function expectNewNgOnInit() {
    // 5 requests are made on init
    expect(httpTestingController.match(() => true).length).toEqual(5);
    // Not flush it to stop test here
  }
});
