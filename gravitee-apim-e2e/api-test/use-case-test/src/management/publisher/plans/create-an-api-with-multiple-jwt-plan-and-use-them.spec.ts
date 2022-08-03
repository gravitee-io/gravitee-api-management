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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApplicationEntity, ApplicationEntityToJSON } from '@management-models/ApplicationEntity';
import { PlanEntity } from '@management-models/PlanEntity';
import { Subscription } from '@management-models/Subscription';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { UpdatePlanEntityFromJSON } from '@management-models/UpdatePlanEntity';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGatewaySuccess, fetchGatewayUnauthorized } from '@lib/gateway';
import * as jwt from 'jsonwebtoken';
import { teardownApisAndApplications } from '@lib/management';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { describeIfJupiter } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());

// FIXME : restore this test for gateway V3 when V3 behavior is fixed
describeIfJupiter('Create an API with multiple JWT plans and use then', () => {
  const MY_SECURE_GIVEN_KEY = 'MY_SECURE_GIVEN_KEY_eyJpc3MiOiJncmF2aXRl';

  let createdApi: ApiEntity;
  const createdApplications: ApplicationEntity[] = [];
  const createdJWTPlans: PlanEntity[] = [];
  const createdSubscriptions: Subscription[] = [];

  beforeAll(async () => {
    // Create new API
    createdApi = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
        flows: [
          {
            name: '',
            path_operator: {
              path: '/',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [
              {
                name: 'Mock',
                description: 'echo the plan used',
                enabled: true,
                policy: 'mock',
                configuration: { content: '{"plan":"{#context.attributes[\'plan\']}"}', status: '200' },
              },
            ],
            post: [],
            enabled: true,
          },
        ],
      }),
    });

    // create 3 JWT plans, with 1 application subscribed to each
    for (let i = 0; i < 3; i++) {
      // Create JWT Plan with GIVEN_KEY resolver
      const securityDefinitionJWTPlan = {
        signature: 'HMAC_HS256',
        publicKeyResolver: 'GIVEN_KEY',
        useSystemProxy: false,
        extractClaims: false,
        propagateAuthHeader: true,
        userClaim: 'sub',
        resolverParameter: MY_SECURE_GIVEN_KEY,
      };
      createdJWTPlans.push(
        await apisResource.createApiPlan({
          orgId,
          envId,
          api: createdApi.id,
          newPlanEntity: PlansFaker.newPlan({
            security: PlanSecurityType.JWT,
            status: PlanStatus.PUBLISHED,
            securityDefinition: JSON.stringify(securityDefinitionJWTPlan),
          }),
        }),
      );

      // Create an application
      createdApplications.push(
        await applicationsResource.createApplication({
          envId,
          orgId,
          newApplicationEntity: ApplicationsFaker.newApplication({}),
        }),
      );
      createdApplications[i] = await applicationsResource.updateApplication({
        envId,
        orgId,
        application: createdApplications[i].id,
        updateApplicationEntity: {
          ...UpdatePlanEntityFromJSON(ApplicationEntityToJSON(createdApplications[i])),
          settings: {
            app: {
              client_id: `MY_CLIENT_ID_${createdJWTPlans[i].id}`,
            },
          },
        },
      });

      // Subscribe application to JWT plan
      createdSubscriptions.push(
        await applicationSubscriptionsResource.createSubscriptionWithApplication({
          envId,
          orgId,
          plan: createdJWTPlans[i].id,
          application: createdApplications[i].id,
        }),
      );
    }

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Call on the Jwt plan', () => {
    test(`Should use the right plan, and return 200 OK when calling API with valid JWT token`, async () => {
      // NOTE: can't implement this test with a test.each as test.each data are collected before they are fed in beforeAll
      for (let i = 0; i < createdApplications.length; i++) {
        const token = jwt.sign(
          {
            foo: 'bar',
            client_id: createdApplications[i].settings.app.client_id,
          },
          MY_SECURE_GIVEN_KEY,
          {
            algorithm: 'HS256',
          },
        );

        await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}`,
          headers: {
            Authorization: `Bearer ${token}`,
          },
          // check in the mock response that the right plan has been executed
          async expectedResponseValidator(res) {
            const responseBody = await res.json();
            expect(responseBody.plan).toEqual(createdJWTPlans[i].id);
            return true;
          },
        });
      }
    });

    test('Should return 401 Unauthorized with invalid jwt token', async () => {
      const token = "I'm not a valid token (°_0)";

      await fetchGatewayUnauthorized({
        contextPath: `${createdApi.context_path}`,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(
      orgId,
      envId,
      [createdApi.id],
      createdApplications.map((app) => app.id),
    );
  });
});
