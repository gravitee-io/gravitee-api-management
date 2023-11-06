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
import { ApiEntity, ApiEntityToJSON } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { Application } from '@gravitee/portal-webclient-sdk/src/lib/models/Application';
import { Subscription as PortalSubscription } from '@gravitee/portal-webclient-sdk/src/lib/models/Subscription';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { ApiLifecycleState } from '@gravitee/management-webclient-sdk/src/lib/models/ApiLifecycleState';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { PortalApplicationFaker } from '@gravitee/fixtures/portal/PortalApplicationFaker';
import { GetSubscriptionByIdIncludeEnum, SubscriptionApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/SubscriptionApi';
import { fetchGatewaySuccess } from '@gravitee/utils/apim-http';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { forManagementAsApiUser, forPortalAsAppUser } from '@gravitee/utils/configuration';
import { ApplicationApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import * as jwt from 'jsonwebtoken';
import { UpdateApiEntityFromJSON } from '@gravitee/management-webclient-sdk/src/lib/models/UpdateApiEntity';
import { teardownApisAndApplications } from '@gravitee/utils/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const JWT_SECURE_GIVEN_KEY = 'JWT_SECURE_GIVEN_KEY_eyJpc3MiOiJncmF2aXRl';
const OAUTH2_RESOURCE_NAME = 'OAuth2-resource';

const apisResource = new APIsApi(forManagementAsApiUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

describe('Subscribe to API Key & JWT plans and use them', () => {
  let createdApi: ApiEntity;
  let createdPortalApplication: Application;
  let createdApiKeyPlanPortalSubscription: PortalSubscription;
  let createdPortalApiKey;
  let createdJWTPlanPortalSubscription: PortalSubscription;

  beforeAll(async () => {
    // Create an API
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [
          // With a published API Key plan
          PlansFaker.plan({ security: PlanSecurityType.API_KEY, status: PlanStatus.PUBLISHED, order: 1 }),
          // With a published JWT plan
          PlansFaker.plan({
            security: PlanSecurityType.JWT,
            status: PlanStatus.PUBLISHED,
            order: 2,
            securityDefinition: JSON.stringify({
              signature: 'HMAC_HS256',
              publicKeyResolver: 'GIVEN_KEY',
              useSystemProxy: false,
              extractClaims: false,
              propagateAuthHeader: true,
              userClaim: 'sub',
              resolverParameter: JWT_SECURE_GIVEN_KEY,
            }),
          }),
        ],
      }),
    });

    // Publish the API
    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
      },
    });

    // Create an application from portal
    createdPortalApplication = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput(),
    });

    // Subscribe application to API Key plan
    createdApiKeyPlanPortalSubscription = await portalSubscriptionResource.createSubscription({
      subscriptionInput: {
        application: createdPortalApplication.id,
        plan: createdApi.plans.find((p) => p.security === PlanSecurityType.API_KEY).id,
      },
    });

    // Get portal subscription API Key
    createdPortalApiKey = (
      await portalSubscriptionResource.getSubscriptionById({
        subscriptionId: createdApiKeyPlanPortalSubscription.id,
        include: [GetSubscriptionByIdIncludeEnum.Keys],
      })
    ).keys[0];

    // Subscribe application to JWT plan
    createdJWTPlanPortalSubscription = await portalSubscriptionResource.createSubscription({
      subscriptionInput: {
        application: createdPortalApplication.id,
        plan: createdApi.plans.find((p) => p.security === PlanSecurityType.JWT).id,
      },
    });

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Gateway call with API Key in HTTP header', () => {
    describe('Gateway call with correct X-Gravitee-Api-Key header using portal subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: createdApi.context_path,
          headers: { 'X-Gravitee-Api-Key': createdPortalApiKey.key },
        });
      });
    });
  });

  describe('Gateway call with API Key in query parameter', () => {
    describe('Gateway call with correct api-key query param using portal subscription', () => {
      test('Should return 200 OK', async () => {
        await fetchGatewaySuccess({
          contextPath: `${createdApi.context_path}?${new URLSearchParams({ 'api-key': createdPortalApiKey.key })}`,
        });
      });
    });
  });

  describe('Gateway call with JWT token in HTTP header', () => {
    describe('Gateway call with correct `Authorization` header using portal subscription', () => {
      test('Should return 200 OK', async () => {
        const token = jwt.sign({ foo: 'bar', client_id: createdPortalApplication.settings.app.client_id }, JWT_SECURE_GIVEN_KEY, {
          algorithm: 'HS256',
        });

        await fetchGatewaySuccess({
          contextPath: createdApi.context_path,
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      });
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);

    if (createdPortalApplication) {
      await portalApplicationResource.deleteApplicationByApplicationId({
        applicationId: createdPortalApplication.id,
      });
    }
  });
});

// 📝 JWT and OAuth2 can be subscribed to same application
describe('Subscribe to OAuth plan and use it', () => {
  let createdApi: ApiEntity;
  let createdPortalApplication: Application;
  let createdJOAuth2PlanPortalSubscription: PortalSubscription;

  beforeAll(async () => {
    // Create an API
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [
          // With a published OAuth2 plan
          PlansFaker.plan({
            security: PlanSecurityType.OAUTH2,
            status: PlanStatus.PUBLISHED,
            securityDefinition: JSON.stringify({
              extractPayload: false,
              checkRequiredScopes: false,
              modeStrict: true,
              propagateAuthHeader: true,
              oauthResource: OAUTH2_RESOURCE_NAME,
            }),
          }),
        ],
      }),
    });

    // Publish the API
    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
      },
    });

    // Create an application from portal
    createdPortalApplication = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput({
        settings: {
          app: {
            client_id: 'clientId1',
          },
        },
      }),
    });

    // Add OAuth2 resource
    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        ...UpdateApiEntityFromJSON(ApiEntityToJSON(createdApi)),
        resources: [
          {
            name: OAUTH2_RESOURCE_NAME,
            enabled: true,
            type: 'oauth2',
            configuration: {
              // 📝 We pass the client_id in url to allow wiremock to add it introspectionEndpoint response. Next this allows the gateway to find the right application.
              authorizationServerUrl: `${process.env.WIREMOCK_BASE_URL}/${createdPortalApplication.settings.app.client_id}`,
              introspectionEndpoint: '/oauth/check_token',
              useSystemProxy: false,
              introspectionEndpointMethod: 'GET',
              scopeSeparator: ' ',
              userInfoEndpoint: '/userinfo',
              userInfoEndpointMethod: 'GET',
              useClientAuthorizationHeader: true,
              clientAuthorizationHeaderName: 'Authorization',
              clientAuthorizationHeaderScheme: 'Basic',
              tokenIsSuppliedByQueryParam: true,
              tokenQueryParamName: 'token',
              tokenIsSuppliedByHttpHeader: false,
              tokenIsSuppliedByFormUrlEncoded: false,
              tokenFormUrlEncodedName: 'token',
              userClaim: 'sub',
              clientId: 'Client Id',
              clientSecret: 'Client Secret',
            },
          },
        ],
      },
    });

    // Subscribe application to OAuth2 plan
    createdJOAuth2PlanPortalSubscription = await portalSubscriptionResource.createSubscription({
      subscriptionInput: {
        application: createdPortalApplication.id,
        plan: createdApi.plans[0].id,
      },
    });

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Gateway call with OAuth2 token in HTTP header', () => {
    describe('Gateway call with correct `Authorization` header using portal subscription', () => {
      test('Should return 200 OK', async () => {
        // Token always validated by wiremock with /oauth/check_token
        const token = 'valid_token_for_clientId1';

        await fetchGatewaySuccess({
          contextPath: createdApi.context_path,
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      });
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);

    if (createdPortalApplication) {
      await portalApplicationResource.deleteApplicationByApplicationId({
        applicationId: createdPortalApplication.id,
      });
    }
  });
});

describe('Subscribe to Keyless plan and use it', () => {
  let createdApi: ApiEntity;

  beforeAll(async () => {
    // Create an API
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [
          // With a published Keyless plan
          PlansFaker.plan({
            security: PlanSecurityType.KEY_LESS,
            status: PlanStatus.PUBLISHED,
          }),
        ],
      }),
    });

    // Publish the API
    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: createdApi.visibility,
      },
    });

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Gateway call with Keyless plan', () => {
    test('Should return 200 OK', async () => {
      await fetchGatewaySuccess({ contextPath: createdApi.context_path });
    });
  });

  afterAll(async () => {
    await teardownApisAndApplications(orgId, envId, [createdApi.id]);
  });
});
