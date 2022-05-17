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

import { forManagementAsApiUser } from '@client-conf/*';
import { APIsApi } from '@management-apis/APIsApi';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { APISubscriptionsApi } from '@management-apis/APISubscriptionsApi';
import { Subscription } from '@management-models/Subscription';
import { ApplicationSubscriptionsApi } from '@management-apis/ApplicationSubscriptionsApi';
import { ApiKeyEntity } from '@management-models/ApiKeyEntity';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { fetchGateway } from '../../lib/gateway';
import { PlanStatus } from '@management-models/PlanStatus';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { SubscriptionStatus } from '@management-models/SubscriptionStatus';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// Management resources
const applicationApiAsApiUser = new ApplicationsApi(forManagementAsApiUser());
const apiSubscriptionsAsApiUser = new APISubscriptionsApi(forManagementAsApiUser());
const appSubscriptionsAsApiUser = new ApplicationSubscriptionsApi(forManagementAsApiUser());
const apisResourceAsPublisher = new APIsApi(forManagementAsApiUser());
const apiPlansResourceAsPublisher = new APIPlansApi(forManagementAsApiUser());

describe('Api plan keys tests', () => {
  let createdApi: ApiEntity;
  let createdApplication: ApplicationEntity;
  let createdSubscription: Subscription;
  let subscriptionKeys: ApiKeyEntity[];

  beforeAll(async () => {
    // create an API with a published api key plan
    createdApi = await apisResourceAsPublisher.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        plans: [PlansFaker.plan({ status: PlanStatus.PUBLISHED, security: PlanSecurityType.APIKEY })],
      }),
    });

    // start it
    await apisResourceAsPublisher.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.START,
    });

    // create application for subscription
    createdApplication = await applicationApiAsApiUser.createApplication({
      orgId,
      envId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });

    // create subscription
    createdSubscription = await apiSubscriptionsAsApiUser.createSubscriptionToApi({
      orgId,
      envId,
      api: createdApi.id,
      plan: createdApi.plans[0].id,
      application: createdApplication.id,
    });

    // get the subscription api keys
    subscriptionKeys = await appSubscriptionsAsApiUser.getApiKeysForApplicationSubscription({
      orgId,
      envId,
      application: createdApplication.id,
      subscription: createdSubscription.id,
    });
  });

  test('should not succeed to call API endpoint without API-Key', async () => {
    const response = await fetchGateway(createdApi.context_path);
    expect(response).toBeTruthy();
    expect(response.status).toStrictEqual(401);
    expect(response.statusText).toStrictEqual('Unauthorized');
  });

  test('should succeed to call API endpoint with API-Key in query parameter', async () => {
    const response = await fetchGateway(`${createdApi.context_path}?api-key=${subscriptionKeys[0].key}`);
    expect(response).toBeTruthy();
    expect(response.status).toStrictEqual(200);

    const body = await response.json();
    expect(body).toHaveProperty('date');
    expect(body).toHaveProperty('timestamp');
  });

  test('should succeed to call API endpoint with API-Key in header', async () => {
    const apiKeyHeader = {
      'x-gravitee-api-key': subscriptionKeys[0].key,
    };
    const response = await fetchGateway(`${createdApi.context_path}`, 'GET', apiKeyHeader);
    expect(response).toBeTruthy();
    expect(response.status).toStrictEqual(200);

    const body = await response.json();
    expect(body).toHaveProperty('date');
    expect(body).toHaveProperty('timestamp');
  });

  afterAll(async () => {
    await apiSubscriptionsAsApiUser.changeApiSubscriptionStatus({
      envId,
      orgId,
      api: createdApi.id,
      subscription: createdSubscription.id,
      status: SubscriptionStatus.CLOSED,
    });

    await applicationApiAsApiUser.deleteApplication({
      orgId,
      envId,
      application: createdApplication.id,
    });

    await apisResourceAsPublisher.doApiLifecycleAction({
      envId,
      orgId,
      api: createdApi.id,
      action: LifecycleAction.STOP,
    });

    await apiPlansResourceAsPublisher.closeApiPlan({
      envId,
      orgId,
      plan: createdApi.plans[0].id,
      api: createdApi.id,
    });

    await apisResourceAsPublisher.deleteApi({
      envId,
      orgId,
      api: createdApi.id,
    });
  });
});
