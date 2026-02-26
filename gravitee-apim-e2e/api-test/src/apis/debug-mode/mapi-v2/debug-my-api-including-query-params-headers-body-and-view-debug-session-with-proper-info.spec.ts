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
import { forManagementAsAdminUser, forManagementAsApiUser, forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { OrganizationApi } from '@gravitee/management-webclient-sdk/src/lib/apis/OrganizationApi';
import { APIsApi as APIsApiV1, ImportApiDefinitionRequest } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { afterAll, beforeAll, expect } from '@jest/globals';
import {
  OrganizationEntity,
  OrganizationEntityToJSON,
  PathOperatorOperatorEnum,
  PlanStatus,
} from '../../../../../lib/management-webclient-sdk/src/lib/models';
import { created, describeIfV3, describeIfV4EmulationEngine, describeIfClientGatewayCompatible, succeed } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { MAPIV2PlansFaker } from '@gravitee/fixtures/management/MAPIV2PlansFaker';
import {
  APIEventsApi,
  APIsApi,
  CreateApiWithImportDefinitionRequest,
  DebugHttpRequest,
  PlanSecurityType,
} from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import { teardownApisAndApplications, teardownV4ApisAndApplications } from '@gravitee/utils/management';
import { from, map, retry, Subscription, switchMap } from 'rxjs';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const organizationApi = new OrganizationApi(forManagementAsAdminUser());

const apisResource = new APIsApiV1(forManagementAsApiUser());
const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());
const v2ApiEventsResourceAsApiPublisher = new APIEventsApi(forManagementV2AsApiUser());

describeIfClientGatewayCompatible('Debug my API (incl. query params, Headers and body) and view debug session with proper info', () => {
  beforeAll(async () => {
    // Create Global Flow
    const organization = await organizationApi.get({ orgId });
    await createGlobalFlow(organization, [
      fakePolicyToAddHeader({
        name: 'X-Test-Platform-Policy',
        scope: 'RESPONSE',
      }),
    ]);
  });

  describe('V4 API Proxy', () => {
    let api: any;
    beforeAll(async () => {
      api = await importV4Api({
        envId,
        exportApiV4: MAPIV2ApisFaker.apiImportV4({
          plans: [MAPIV2PlansFaker.planV4({ security: { type: PlanSecurityType.KEY_LESS }, validation: 'AUTO' })],
          api: MAPIV2ApisFaker.apiV4Proxy({
            endpointGroups: [
              {
                name: 'Default HTTP proxy group',
                type: 'http-proxy',
                loadBalancer: {
                  type: 'ROUND_ROBIN',
                },
                endpoints: [
                  {
                    name: 'Default HTTP proxy',
                    type: 'http-proxy',
                    configuration: { target: `${process.env.WIREMOCK_BASE_URL}/hello` },
                  },
                ],
              },
            ],
            flows: [
              MAPIV2ApisFaker.newFlow({
                name: '[Testing flow] API',
                enabled: true,
                selectors: [
                  {
                    type: 'HTTP',
                    path: '/',
                    pathOperator: 'STARTS_WITH',
                  },
                ],
                request: [
                  fakePolicyToAddHeader({ name: 'X-Test-Api-Request-Policy', scope: 'REQUEST' }),
                  fakePolicyToAddHeader({
                    name: 'X-Test-Api-Request-Conditioned-Policy',
                    scope: 'REQUEST',
                    condition:
                      '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                  }),
                  fakePolicyToAddHeader({
                    name: 'X-Test-Api-Request-Disabled-Policy',
                    scope: 'REQUEST',
                    enabled: false,
                  }),
                  {
                    name: 'Assign content',
                    description: 'Validate request content change',
                    enabled: true,
                    policy: 'policy-assign-content',
                    condition: '{#request.method != "GET"}',
                    configuration: {
                      scope: 'REQUEST',
                      body: 'Validate content change\n${request.getContent()}',
                    },
                  },
                ],
                response: [
                  fakePolicyToAddHeader({ name: 'X-Test-Api-Response-Policy', scope: 'RESPONSE' }),
                  fakePolicyToAddHeader({
                    name: 'X-Test-Api-Response-Conditioned-Policy',
                    scope: 'RESPONSE',
                    condition:
                      '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                  }),
                  fakePolicyToAddHeader({
                    name: 'X-Test-Api-Response-Disabled-Policy',
                    scope: 'RESPONSE',
                    enabled: false,
                  }),
                  {
                    name: 'JSON to XML',
                    description: 'Validate response content change',
                    enabled: true,
                    policy: 'json-xml',
                    configuration: {
                      rootElement: 'root',
                      scope: 'RESPONSE',
                    },
                  },
                ],
              }),
            ],
          }),
        }),
      });
    });

    afterAll(async () => {
      await teardownV4ApisAndApplications(orgId, envId, [api.id]);
    });

    describe('Send debug event on `GET /`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            console.info('Debug event received:', debugEvent);
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => {
            console.error('Error during debug event:', err);
            done(err);
          },
        });
      });

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain response', () => {
        // Expect final Response
        expect(debugResult.response.statusCode).toEqual(200);
        expect(debugResult.response.body).toContain('Hello');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Platform-Policy');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.response.headers)).not.toContain('X-Test-Api-Response-Conditioned-Policy');
      });

      it('should contain debug steps', () => {
        expect(debugResult.debugSteps.length).toEqual(8);

        // Step 1 key-less
        expect(debugResult.debugSteps[0].policyId).toEqual('keyless');
        expect(debugResult.debugSteps[0].stage).toEqual('security');

        // Step 2 Request transform-headers
        expect(debugResult.debugSteps[1].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[1].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[1].result.headers)).toContain('X-Test-Api-Request-Policy');

        // Step 3 Request Conditioned transform-headers
        expect(debugResult.debugSteps[2].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[2].status).toEqual('SKIPPED');

        // Step 4 Request assign-content
        expect(debugResult.debugSteps[3].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[3].scope).toEqual('ON_REQUEST');
        expect(debugResult.debugSteps[3].status).toEqual('SKIPPED');

        // Step 5 Response transform-headers
        expect(debugResult.debugSteps[4].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[4].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[4].result.headers)).toContain('X-Test-Api-Response-Policy');

        // Step 6 Response Conditioned transform-headers
        expect(debugResult.debugSteps[5].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[5].status).toEqual('SKIPPED');

        // Step 7 Response json-xml
        expect(debugResult.debugSteps[6].policyId).toEqual('json-xml');
        expect(debugResult.debugSteps[6].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[6].scope).toEqual('ON_RESPONSE');
        expect(debugResult.debugSteps[6].result.body).toContain('<root><message>Hello, World!</message></root>');

        // Step 7 Platform Response transform-headers
        expect(debugResult.debugSteps[7].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[7].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[7].stage).toEqual('organization');
        expect(Object.keys(debugResult.debugSteps[7].result.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.debugSteps[7].result.headers)).toContain('X-Test-Platform-Policy');
      });
    });

    describe('Send debug event on `GET /?name=TheFox`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/?name=TheFox',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should send query params to backend', () => {
        expect(debugResult.response.body).toEqual('<root><message>Hello, TheFox!</message></root>');
      });
    });

    describe('Send debug event on `GET /` with headers', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {
            'string-header': ['string-value'],
            'array-header': ['1', '2', '3'],
          },
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain preprocessorStep', () => {
        expect(debugResult.preprocessorStep.headers['array-header']).toEqual(['1', '2', '3']);
        expect(debugResult.preprocessorStep.headers['string-header']).toEqual(['string-value']);
      });
    });

    describe('Send debug event on `POST /` with body', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'POST',
          path: '/',
          headers: {},
          body: 'The body',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('Should contain body in the policy-assign-content', () => {
        // Expect final Response
        expect(debugResult.debugSteps[3].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[3].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[3].result.body).toContain('The body');
      });
    });
  });

  describeIfV3('V2 API (compatibility mode)', () => {
    let api: any;
    beforeAll(async () => {
      api = await importV2Api({
        definitionVersion: '2.0.0',
        envId,
        orgId,
        body: ApisFaker.apiImport({
          execution_mode: 'v3',
          plans: [
            PlansFaker.plan({
              security: PlanSecurityType.KEY_LESS,
              status: PlanStatus.PUBLISHED,
            }),
          ],
          flows: [
            {
              name: '[Testing flow] API',
              enabled: true,
              path_operator: {
                path: '/',
                operator: PathOperatorOperatorEnum.STARTS_WITH,
              },
              pre: [
                fakePolicyToAddHeader({ name: 'X-Test-Api-Request-Policy', scope: 'REQUEST' }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Request-Conditioned-Policy',
                  scope: 'REQUEST',
                  condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Request-Disabled-Policy',
                  scope: 'REQUEST',
                  enabled: false,
                }),
                {
                  name: 'Assign content',
                  description: 'Validate request content change',
                  enabled: true,
                  policy: 'policy-assign-content',
                  configuration: {
                    scope: 'REQUEST',
                    body: 'Validate content change\n${request.getContent()}',
                  },
                },
              ],
              post: [
                fakePolicyToAddHeader({ name: 'X-Test-Api-Response-Policy', scope: 'RESPONSE' }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Response-Conditioned-Policy',
                  scope: 'RESPONSE',
                  condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Response-Disabled-Policy',
                  scope: 'RESPONSE',
                  enabled: false,
                }),
                {
                  name: 'JSON to XML',
                  description: 'Validate response content change',
                  enabled: true,
                  policy: 'json-xml',
                  configuration: {
                    rootElement: 'root',
                    scope: 'RESPONSE',
                  },
                },
              ],
            },
          ],
        }),
      });
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [api.id]);
    });

    describe('Send debug event on `GET /`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            console.info('Debug event received:', debugEvent);
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => {
            console.error('Error during debug event:', err);
            done(err);
          },
        });
      });

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain response', () => {
        // Expect final Response
        expect(debugResult.response.statusCode).toEqual(200);
        expect(debugResult.response.body).toContain('Hello');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Platform-Policy');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.response.headers)).not.toContain('X-Test-Api-Response-Conditioned-Policy');
      });

      it('should contain debug steps', () => {
        expect(debugResult.debugSteps.length).toEqual(13);

        // Step 1 key-less
        expect(debugResult.debugSteps[0].policyId).toEqual('key-less');

        // Step 4 Request transform-headers
        expect(debugResult.debugSteps[3].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[3].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[3].result.headers)).toContain('X-Test-Api-Request-Policy');

        // Step 5 Request Conditioned transform-headers
        expect(debugResult.debugSteps[4].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[4].status).toEqual('SKIPPED');

        // Step 6 Request assign-content
        expect(debugResult.debugSteps[5].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[5].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[5].scope).toEqual('ON_REQUEST_CONTENT');
        expect(debugResult.debugSteps[5].result.body).toContain('Validate content change');

        // Step 9 Response Conditioned transform-headers
        expect(debugResult.debugSteps[8].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[8].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[8].result.headers)).toContain('X-Test-Api-Response-Policy');

        // Step 10 Response Conditioned transform-headers
        expect(debugResult.debugSteps[9].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[9].status).toEqual('SKIPPED');

        // Step 12 Platform Response transform-headers
        expect(debugResult.debugSteps[11].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[11].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[11].stage).toEqual('PLATFORM');
        expect(Object.keys(debugResult.debugSteps[11].result.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.debugSteps[11].result.headers)).toContain('X-Test-Platform-Policy');

        // Step 13 Response json-xml
        expect(debugResult.debugSteps[12].policyId).toEqual('json-xml');
        expect(debugResult.debugSteps[12].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[12].scope).toEqual('ON_RESPONSE_CONTENT');
        expect(debugResult.debugSteps[12].result.body).toContain('<root><message>Hello, World!</message></root>');
      });
    });

    describe('Send debug event on `GET /?name=TheFox`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/?name=TheFox',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should send query params to backend', () => {
        expect(debugResult.response.body).toEqual('<root><message>Hello, TheFox!</message></root>');
      });
    });

    describe('Send debug event on `GET /` with headers', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {
            'string-header': ['string-value'],
            'array-header': ['1', '2', '3'],
          },
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain preprocessorStep', () => {
        expect(debugResult.preprocessorStep.headers['array-header']).toEqual(['1', '2', '3']);
        expect(debugResult.preprocessorStep.headers['string-header']).toEqual(['string-value']);
      });
    });

    describe('Send debug event on `POST /` with body', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'POST',
          path: '/',
          headers: {},
          body: 'The body',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('Should contain body in the policy-assign-content', () => {
        // Expect final Response
        expect(debugResult.debugSteps[5].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[5].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[5].result.body).toContain('The body');
      });
    });
  });

  describeIfV4EmulationEngine('V2 API (emulation mode)', () => {
    let api: any;
    beforeAll(async () => {
      api = await importV2Api({
        definitionVersion: '2.0.0',
        envId,
        orgId,
        body: ApisFaker.apiImport({
          execution_mode: 'v4-emulation-engine',
          plans: [
            PlansFaker.plan({
              security: PlanSecurityType.KEY_LESS,
              status: PlanStatus.PUBLISHED,
            }),
          ],
          flows: [
            {
              name: '[Testing flow] API',
              enabled: true,
              path_operator: {
                path: '/',
                operator: PathOperatorOperatorEnum.STARTS_WITH,
              },
              pre: [
                fakePolicyToAddHeader({ name: 'X-Test-Api-Request-Policy', scope: 'REQUEST' }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Request-Conditioned-Policy',
                  scope: 'REQUEST',
                  condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Request-Disabled-Policy',
                  scope: 'REQUEST',
                  enabled: false,
                }),
                {
                  name: 'Assign content',
                  description: 'Validate request content change',
                  enabled: true,
                  policy: 'policy-assign-content',
                  configuration: {
                    scope: 'REQUEST',
                    body: 'Validate content change\n${request.getContent()}',
                  },
                },
              ],
              post: [
                fakePolicyToAddHeader({ name: 'X-Test-Api-Response-Policy', scope: 'RESPONSE' }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Response-Conditioned-Policy',
                  scope: 'RESPONSE',
                  condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
                }),
                fakePolicyToAddHeader({
                  name: 'X-Test-Api-Response-Disabled-Policy',
                  scope: 'RESPONSE',
                  enabled: false,
                }),
                {
                  name: 'JSON to XML',
                  description: 'Validate response content change',
                  enabled: true,
                  policy: 'json-xml',
                  configuration: {
                    rootElement: 'root',
                    scope: 'RESPONSE',
                  },
                },
              ],
            },
          ],
        }),
      });
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [api.id]);
    });

    describe('Send debug event on `GET /`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            console.info('Debug event received:', debugEvent);
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => {
            console.error('Error during debug event:', err);
            done(err);
          },
        });
      });

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain response', () => {
        // Expect final Response
        expect(debugResult.response.statusCode).toEqual(200);
        expect(debugResult.response.body).toContain('Hello');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Platform-Policy');
        expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.response.headers)).not.toContain('X-Test-Api-Response-Conditioned-Policy');
      });

      it('should contain debug steps', () => {
        expect(debugResult.debugSteps.length).toEqual(8);

        // Step 1 key-less
        expect(debugResult.debugSteps[0].policyId).toEqual('keyless');
        expect(debugResult.debugSteps[0].stage).toEqual('security');

        // Step 2 Request transform-headers
        expect(debugResult.debugSteps[1].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[1].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[1].result.headers)).toContain('X-Test-Api-Request-Policy');

        // Step 3 Request Conditioned transform-headers
        expect(debugResult.debugSteps[2].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[2].status).toEqual('SKIPPED');

        // Step 4 Request assign-content
        expect(debugResult.debugSteps[3].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[3].scope).toEqual('ON_REQUEST');
        expect(debugResult.debugSteps[3].status).toEqual('COMPLETED');

        // Step 5 Response transform-headers
        expect(debugResult.debugSteps[4].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[4].status).toEqual('COMPLETED');
        expect(Object.keys(debugResult.debugSteps[4].result.headers)).toContain('X-Test-Api-Response-Policy');

        // Step 6 Response Conditioned transform-headers
        expect(debugResult.debugSteps[5].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[5].status).toEqual('SKIPPED');

        // Step 7 Response json-xml
        expect(debugResult.debugSteps[6].policyId).toEqual('json-xml');
        expect(debugResult.debugSteps[6].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[6].scope).toEqual('ON_RESPONSE');
        expect(debugResult.debugSteps[6].result.body).toContain('<root><message>Hello, World!</message></root>');

        // Step 7 Platform Response transform-headers
        expect(debugResult.debugSteps[7].policyId).toEqual('transform-headers');
        expect(debugResult.debugSteps[7].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[7].stage).toEqual('organization');
        expect(Object.keys(debugResult.debugSteps[7].result.headers)).toContain('X-Test-Api-Response-Policy');
        expect(Object.keys(debugResult.debugSteps[7].result.headers)).toContain('X-Test-Platform-Policy');
      });
    });

    describe('Send debug event on `GET /?name=TheFox`', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/?name=TheFox',
          headers: {},
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should send query params to backend', () => {
        expect(debugResult.response.body).toEqual('<root><message>Hello, TheFox!</message></root>');
      });
    });

    describe('Send debug event on `GET /` with headers', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'GET',
          path: '/',
          headers: {
            'string-header': ['string-value'],
            'array-header': ['1', '2', '3'],
          },
          body: '',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('should contain preprocessorStep', () => {
        expect(debugResult.preprocessorStep.headers['array-header']).toEqual(['1', '2', '3']);
        expect(debugResult.preprocessorStep.headers['string-header']).toEqual(['string-value']);
      });
    });

    describe('Send debug event on `POST /` with body', () => {
      let debugResult: any;
      let debugEventSubscription: Subscription;

      beforeAll((done) => {
        debugEventSubscription = createAndWaitForDebugResult$(envId, api.id, {
          method: 'POST',
          path: '/',
          headers: {},
          body: 'The body',
        }).subscribe({
          next: (debugEvent) => {
            debugResult = JSON.parse(debugEvent.payload);
            done();
          },
          error: (err) => done(err),
        });
      }, 90000);

      afterAll(() => debugEventSubscription.unsubscribe());

      it('Should contain body in the policy-assign-content', () => {
        // Expect final Response
        expect(debugResult.debugSteps[3].policyId).toEqual('policy-assign-content');
        expect(debugResult.debugSteps[3].status).toEqual('COMPLETED');
        expect(debugResult.debugSteps[3].result.body).toContain('The body');
      });
    });
  });
});

async function createGlobalFlow(organization: OrganizationEntity, postPolicies: any[]) {
  return organizationApi.update({
    orgId: organization.id,
    updateOrganizationEntity: {
      ...OrganizationEntityToJSON(organization),
      flows: [
        {
          name: `[Testing flow] Platform`,
          path_operator: {
            path: '/',
            operator: PathOperatorOperatorEnum.STARTS_WITH,
          },
          consumers: [],
          methods: [],
          pre: [],
          post: postPolicies,
          enabled: true,
        },
      ],
    },
  });
}

async function importV4Api(input: CreateApiWithImportDefinitionRequest) {
  return created(v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw(input));
}

async function importV2Api(input: ImportApiDefinitionRequest) {
  return succeed(apisResource.importApiDefinitionRaw(input));
}

// Create Transform Headers policy to add header
const fakePolicyToAddHeader = ({
  name,
  scope,
  condition,
  enabled,
}: {
  name: string;
  scope: 'RESPONSE' | 'REQUEST';
  condition?: string;
  enabled?: boolean;
}) => {
  return {
    name: 'Transform Headers',
    description: `Add "${name}" header`,
    enabled: enabled ?? true,
    policy: 'transform-headers',
    condition,
    configuration: {
      addHeaders: [{ name: `${name}`, value: 'ok' }],
      scope,
    },
  };
};

const createAndWaitForDebugResult$ = (envId: string, apiId: string, debugHttpRequest: DebugHttpRequest) => {
  return from(
    succeed(
      v2ApisResourceAsApiPublisher.debugApiRaw({
        envId,
        apiId,
        debugHttpRequest,
      }),
      202,
    ),
  ).pipe(
    // Get debug event by ID
    switchMap((debugApiResponse) => {
      return from(v2ApiEventsResourceAsApiPublisher.getApiEventById({ envId, apiId, eventId: debugApiResponse.id }));
    }),
    // Throw error if debug request is not successful
    map((value) => {
      const debugStatus = value.properties['API_DEBUG_STATUS'] ?? value.properties['api_debug_status'];
      if (debugStatus !== 'SUCCESS') {
        // error will be picked up by retryWhen
        throw value;
      }
      return value;
    }),
    // Retry pipe operators if error is thrown
    retry({ delay: 1000, count: 60 }),
  );
};
