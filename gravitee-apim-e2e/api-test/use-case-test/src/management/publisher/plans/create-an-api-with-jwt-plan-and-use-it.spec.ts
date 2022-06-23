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
import { afterAll, beforeAll, describe } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApplicationEntity, ApplicationEntityToJSON } from '@management-models/ApplicationEntity';
import { PlanEntity, PlanEntityToJSON } from '@management-models/PlanEntity';
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

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());
const apiPlansResource = new APIPlansApi(forManagementAsApiUser());

describe('Create an API with JWT plan and use it', () => {
  const MY_SECURE_GIVEN_KEY = 'MY_SECURE_GIVEN_KEY_eyJpc3MiOiJncmF2aXRl';

  let createdApi: ApiEntity;
  let createdApplication: ApplicationEntity;
  let createdJWTPlan: PlanEntity;
  let createdSubscription: Subscription;

  beforeAll(async () => {
    // Create new API
    createdApi = await apisResource.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi({
        gravitee: '2.0.0',
      }),
    });

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
    createdJWTPlan = await apisResource.createApiPlan({
      orgId,
      envId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({
        security: PlanSecurityType.JWT,
        status: PlanStatus.PUBLISHED,
        securityDefinition: JSON.stringify(securityDefinitionJWTPlan),
      }),
    });

    // Create an application
    createdApplication = await applicationsResource.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication({}),
    });

    createdApplication = await applicationsResource.updateApplication({
      envId,
      orgId,
      application: createdApplication.id,
      updateApplicationEntity: {
        ...UpdatePlanEntityFromJSON(ApplicationEntityToJSON(createdApplication)),
        settings: {
          app: {
            client_id: `MY_CLIENT_ID_${createdJWTPlan.id}`,
          },
        },
      },
    });

    // Subscribe application to JWT plan
    createdSubscription = await applicationSubscriptionsResource.createSubscriptionWithApplication({
      envId,
      orgId,
      plan: createdJWTPlan.id,
      application: createdApplication.id,
    });

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Call on the Jwt plan', () => {
    test('Should return 200 Ok with valid jwt token', async () => {
      const token = jwt.sign({ foo: 'bar', client_id: createdApplication.settings.app.client_id }, MY_SECURE_GIVEN_KEY, {
        algorithm: 'HS256',
      });

      await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
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
    if (createdApi) {
      // Stop API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.STOP,
      });

      // Close JWT plan
      await apiPlansResource.closeApiPlan({
        envId,
        orgId,
        plan: createdJWTPlan.id,
        api: createdApi.id,
      });

      // Delete API
      await apisResource.deleteApi({
        envId,
        orgId,
        api: createdApi.id,
      });
    }

    // Delete application
    if (createdApplication) {
      await applicationsResource.deleteApplication({
        envId,
        orgId,
        application: createdApplication.id,
      });
    }
  });
});
