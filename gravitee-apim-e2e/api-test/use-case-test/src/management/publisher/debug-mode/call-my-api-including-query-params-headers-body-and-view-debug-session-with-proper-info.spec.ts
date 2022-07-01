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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { OrganizationEntityToJSON } from '@management-models/OrganizationEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { OrganizationApi } from '@management-apis/OrganizationApi';
import { ApiEntity, ApiEntityToJSON } from '@management-models/ApiEntity';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { teardownApisAndApplications } from '@lib/management';
import { DebugApiEntity, DebugApiEntityFromJSON } from '@management-models/DebugApiEntity';
import { succeed } from '@lib/jest-utils';
import { delayWhen, from, Observable, switchMap, tap, timer, map, retryWhen } from 'rxjs';
import { EventEntity } from '@management-models/EventEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const organizationApi = new OrganizationApi(forManagementAsAdminUser());

describe('Call my API (incl. query params, Headers and body) and view debug session with proper info', () => {
  let apiEntity: ApiEntity;

  beforeAll(async () => {
    // Create Global Flow
    const organization = await organizationApi.get({ orgId });
    await organizationApi.update({
      orgId,
      updateOrganizationEntity: {
        ...OrganizationEntityToJSON(organization),
        flows: [
          {
            name: `[Testing flow] Platform`,
            path_operator: {
              path: '/',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            consumers: [],
            methods: [],
            pre: [],
            post: [fakePolicyToAddHeader({ name: 'X-Test-Platform-Policy', scope: 'RESPONSE' })],
            enabled: true,
          },
        ],
      },
    });

    // Create new API
    apiEntity = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
        // With flow on root path
        flows: [
          {
            name: `[Testing flow] API`,
            path_operator: {
              path: '/',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            consumers: [],
            methods: [],
            pre: [
              fakePolicyToAddHeader({ name: 'X-Test-Api-Request-Policy', scope: 'REQUEST' }),
              fakePolicyToAddHeader({
                name: 'X-Test-Api-Request-Conditioned-Policy',
                scope: 'REQUEST',
                condition: '{#request.headers["Use-Conditioned-Flow"] != null && #request.headers["Use-Conditioned-Flow"][0] == "true"}',
              }),
              fakePolicyToAddHeader({ name: 'X-Test-Api-Request-Disabled-Policy', scope: 'REQUEST', enabled: false }),
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
              fakePolicyToAddHeader({ name: 'X-Test-Api-Response-Disabled-Policy', scope: 'RESPONSE', enabled: false }),
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
            enabled: true,
          },
        ],
      }),
    });

    // Create first KeylessPlan
    await apisResource.createApiPlan({
      orgId,
      envId,
      api: apiEntity.id,
      newPlanEntity: PlansFaker.newPlan({
        security: PlanSecurityType.KEYLESS,
        status: PlanStatus.PUBLISHED,
      }),
    });

    // Fetch api description (with keyless plan)
    apiEntity = await apisResource.getApi({ orgId, envId, api: apiEntity.id });
  });

  describe('Get debug event on `GET /`', () => {
    let debugResult;

    beforeEach((done) => {
      createAndWaitForDebugResult$({
        ...DebugApiEntityFromJSON(ApiEntityToJSON(apiEntity)),
        entrypoints: undefined,
        request: {
          body: '',
          headers: {},
          method: 'GET',
          path: '/',
        },
      }).subscribe((value) => {
        debugResult = JSON.parse(value.payload);
        done();
      });
    });

    test('Should contain response', () => {
      // Expect final Response
      expect(debugResult.response.statusCode).toEqual(200);
      expect(debugResult.response.body).toContain('Hello');
      expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Platform-Policy');
      expect(Object.keys(debugResult.response.headers)).toContain('X-Test-Api-Response-Policy');
      expect(Object.keys(debugResult.response.headers)).not.toContain('X-Test-Api-Response-Conditioned-Policy');
    });

    test('Should contain debug steps', () => {
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

  describe('Get debug event on `GET /?name=TheFox`', () => {
    let debugResult;

    beforeEach((done) => {
      createAndWaitForDebugResult$({
        ...DebugApiEntityFromJSON(ApiEntityToJSON(apiEntity)),
        entrypoints: undefined,
        request: {
          body: '',
          headers: {},
          method: 'GET',
          path: '/?name=TheFox',
        },
      }).subscribe((value) => {
        debugResult = JSON.parse(value.payload);
        done();
      });
    });

    test('Should send query params to backend', () => {
      expect(debugResult.response.body).toEqual('<root><message>Hello, TheFox!</message></root>');
    });
  });

  describe('Get debug event on `GET /` with headers', () => {
    let debugResult;

    beforeEach((done) => {
      createAndWaitForDebugResult$({
        ...DebugApiEntityFromJSON(ApiEntityToJSON(apiEntity)),
        entrypoints: undefined,
        request: {
          body: '',
          headers: {
            'string-header': ['string-value'],
            'array-header': ['1', '2', '3'],
          },
          method: 'GET',
          path: '/',
        },
      }).subscribe((value) => {
        debugResult = JSON.parse(value.payload);
        done();
      });
    });

    test('Should contain preprocessorStep', () => {
      expect(debugResult.preprocessorStep.headers['array-header']).toEqual(['1', '2', '3']);
      expect(debugResult.preprocessorStep.headers['string-header']).toEqual(['string-value']);
    });
  });

  describe('Get debug event on `GET /` with body', () => {
    let debugResult;

    beforeEach((done) => {
      createAndWaitForDebugResult$({
        ...DebugApiEntityFromJSON(ApiEntityToJSON(apiEntity)),
        entrypoints: undefined,
        request: {
          body: 'The body',
          headers: {},
          method: 'GET',
          path: '/',
        },
      }).subscribe((value) => {
        debugResult = JSON.parse(value.payload);
        done();
      });
    });

    test('Should contain body in the policy-assign-content', () => {
      // Expect final Response
      expect(debugResult.debugSteps[5].policyId).toEqual('policy-assign-content');
      expect(debugResult.debugSteps[5].status).toEqual('COMPLETED');
      expect(debugResult.debugSteps[5].result.body).toContain('The body');
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [apiEntity.id]);
  });

  const createAndWaitForDebugResult$ = (debugApiEntity: DebugApiEntity): Observable<EventEntity> => {
    return from(
      succeed(
        apisResource.debugAPIRaw({
          orgId,
          envId,
          api: apiEntity.id,
          debugApiEntity,
        }),
      ),
    ).pipe(
      // Create debug request event
      switchMap((debugApiResponse) => from(apisResource.getEvent({ orgId, envId, api: apiEntity.id, eventId: debugApiResponse.id }))),
      // Throw error if debug request is not successful
      map((value) => {
        if (value.properties['api_debug_status'] !== 'SUCCESS') {
          // error will be picked up by retryWhen
          throw value;
        }
        return value;
      }),
      // Retry pipe operators if error is thrown
      retryWhen((errors) =>
        errors.pipe(
          // log error message
          tap((value) => console.error(`Fail to get SUCCESS event`, value)),
          // restart in 0.5 seconds
          delayWhen((value) => timer(500)),
        ),
      ),
    );
  };
});

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
