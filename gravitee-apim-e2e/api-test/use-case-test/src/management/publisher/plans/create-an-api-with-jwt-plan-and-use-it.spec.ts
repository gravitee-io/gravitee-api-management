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
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { ApplicationsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationSubscriptionsApi';
import { APIPlansApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIPlansApi';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { ApplicationEntity, ApplicationEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/ApplicationEntity';
import { PlanEntity, PlanEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/PlanEntity';
import { Subscription } from '@gravitee/management-webclient-sdk/src/lib/models/Subscription';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { UpdatePlanEntityFromJSON } from '@gravitee/management-webclient-sdk/src/lib/models/UpdatePlanEntity';
import { ApplicationsFaker } from '@gravitee/fixtures/management/ApplicationsFaker';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { fetchGatewaySuccess, fetchGatewayUnauthorized } from '@gravitee/utils/gateway';
import * as jwt from 'jsonwebtoken';
import { teardownApisAndApplications } from '@gravitee/utils/management';

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
    await teardownApisAndApplications(orgId, envId, [createdApi.id], [createdApplication.id]);
  });
});
