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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { ApiEntity, ApiEntityToJSON } from '@management-models/ApiEntity';
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
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';
import { teardownApisAndApplications } from '@lib/management';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const applicationsResource = new ApplicationsApi(forManagementAsApiUser());
const applicationSubscriptionsResource = new ApplicationSubscriptionsApi(forManagementAsApiUser());

describe('Create an API with OAuth2 plan and use it', () => {
  const OAUTH2_RESOURCE_NAME = 'OAuth2-resource';

  let createdApi: ApiEntity;
  let createdApplication: ApplicationEntity;
  let createdOAuth2Plan: PlanEntity;
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

    // Create OAuth2 Plan
    const securityDefinitionOAuth2Plan = {
      extractPayload: false,
      checkRequiredScopes: false,
      modeStrict: true,
      propagateAuthHeader: true,
      oauthResource: OAUTH2_RESOURCE_NAME,
    };
    createdOAuth2Plan = await apisResource.createApiPlan({
      orgId,
      envId,
      api: createdApi.id,
      newPlanEntity: PlansFaker.newPlan({
        security: PlanSecurityType.OAUTH2,
        status: PlanStatus.PUBLISHED,
        securityDefinition: JSON.stringify(securityDefinitionOAuth2Plan),
        flows: [
          // Add OAuth2 Plan flow
          {
            name: '',
            path_operator: {
              path: '',
              operator: PathOperatorOperatorEnum.STARTSWITH,
            },
            condition: '',
            consumers: [],
            methods: [],
            pre: [],
            post: [
              // Add policy to check the flow execution
              {
                name: 'Transform Headers',
                description: 'Add header to validate flow',
                enabled: true,
                policy: 'transform-headers',
                configuration: {
                  addHeaders: [{ name: 'X-Test-OAuth2-Flow', value: 'ok' }],
                  scope: 'RESPONSE',
                },
              },
            ],
            enabled: true,
          },
        ],
      }),
    });

    // Create an application
    createdApplication = await applicationsResource.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication({}),
    });

    // Update application to change settings only editable after creation
    createdApplication = await applicationsResource.updateApplication({
      envId,
      orgId,
      application: createdApplication.id,
      updateApplicationEntity: {
        ...UpdatePlanEntityFromJSON(ApplicationEntityToJSON(createdApplication)),
        settings: {
          app: {
            client_id: `MY_CLIENT_ID_${createdOAuth2Plan.id}`,
          },
        },
      },
    });

    // Subscribe application to OAuth2 plan
    createdSubscription = await applicationSubscriptionsResource.createSubscriptionWithApplication({
      envId,
      orgId,
      plan: createdOAuth2Plan.id,
      application: createdApplication.id,
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
              authorizationServerUrl: `${process.env.WIREMOCK_BASE_PATH}/${createdApplication.settings.app.client_id}`,
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

    // Start it
    await apisResource.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });
  });

  describe('Call on the OAuth2 plan', () => {
    test('Should return 200 Ok', async () => {
      // Token always validated by wiremock with /oauth/check_token
      const token = 'realFakeToken';

      const res = await fetchGatewaySuccess({
        contextPath: `${createdApi.context_path}`,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      expect(res.headers.get('X-Test-OAuth2-Flow')).toEqual('ok');
    });

    test('Should return 401 Unauthorized with empty token', async () => {
      const token = '';

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
