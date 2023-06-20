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
import { check, sleep } from 'k6';
import { Connection } from 'k6/x/kafka';
import http from 'k6/http';
import { ADMIN_USER, authorizationHeaderFor, k6Options } from '@env/environment';
import { LifecycleAction } from '@models/v3/ApiEntity';
import { failIf, generatePayloadInKB } from '@helpers/k6.helper';
import { ApisFixture } from '@fixtures/v3/apis.fixture';
import { HttpHelper } from '@helpers/http.helper';
import { GatewayTestData } from '@lib/test-api';
import { GatewayClient, HttpMethod } from '@clients/GatewayClient';
import { ApisV4Fixture } from '@fixtures/v4/apis.v4.fixture';
import { ApisV4Client } from '@clients/v4/ApisV4Client';
import { PlansV4Client } from '@clients/v4/PlansV4Client';
import { PlansV4Fixture } from '@fixtures/v4/plans.v4.fixture';
import { NewPlanEntityV4StatusEnum } from '@models/v4/NewPlanEntityV4';
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4 } from '@models/v4/PlanEntityV4';
import { NewApiEntityV4TypeEnum } from '@models/v4/NewApiEntityV4';

/**
 * Creates an API with HTTP-POST entrypoint and Kafka Endpoint.
 * Expects a 202 response when posting message
 */
export const options = k6Options;

const connection = new Connection({
  // ConnectionConfig object
  address: k6Options.apim.kafkaBoostrapServer,
});

const kafkaTopic = k6Options.apim.httpPost.topic;
const numPartitions = k6Options.apim.httpPost.numPartitions;

if (__VU == 0) {
  connection.createTopic({ topic: kafkaTopic, numPartitions: numPartitions });
}

export function setup(): GatewayTestData {
  const contextPath = ApisFixture.randomPath();
  const api = ApisV4Fixture.newApi({
    listeners: [
      ApisV4Fixture.newHttpListener({
        paths: [
          {
            path: contextPath,
          },
        ],
        entrypoints: [
          {
            type: 'http-post',
            configuration: {
              requestHeadersToMessage: k6Options.apim.httpPost.requestHeadersToMessage,
            },
          },
        ],
      }),
    ],
    endpointGroups: [
      {
        name: 'default-group',
        type: 'kafka',
        endpoints: [
          {
            name: 'default',
            type: 'kafka',
            inheritConfiguration: false,
            configuration: {
              bootstrapServers: k6Options.apim.kafkaBoostrapServer,
            },
            sharedConfigurationOverride: {
              producer: {
                enabled: true,
                topics: [kafkaTopic],
              },
            },
          },
        ],
      },
    ],
    flows: [
      {
        name: 'Routing Flow',
        selectors: [],
        request: [],
        response: [],
        subscribe: [],
        publish: k6Options.apim.httpPost.withJsontoJson
          ? [
              {
                name: 'Json to Json',
                description: 'add an api properties in the message and change the Json Structure',
                enabled: true,
                policy: 'json-to-json',
                configuration: {
                  specification:
                    '[{ "operation": "default", "spec": { "static": "static-value" } },{"operation": "shift","spec": {"static": "StaticEntry","key_*": "Keys.&(0,1)"}}]',
                },
              },
            ]
          : [],
        enabled: true,
      },
    ],
    type: NewApiEntityV4TypeEnum.MESSAGE,
  });
  const apiCreationResponse = ApisV4Client.createApi(api, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(apiCreationResponse.status !== 201, 'Could not create API');
  const createdApi = HttpHelper.parseBody<ApiEntityV4>(apiCreationResponse);

  const planCreationResponse = PlansV4Client.createPlan(
    createdApi.id,
    PlansV4Fixture.newPlan({ status: NewPlanEntityV4StatusEnum.PUBLISHED }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    },
  );
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntityV4>(planCreationResponse);

  const changeLifecycleResponse = ApisV4Client.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  GatewayClient.waitForApiAvailability({ contextPath: contextPath, expectedStatusCode: 202, method: HttpMethod.POST, body: '' });

  const message = generatePayloadInKB(k6Options.apim.httpPost.messageSizeInKB);
  return { api: createdApi, plan: createdPlan, waitGateway: { contextPath: contextPath }, msg: message };
}

export default (data: GatewayTestData) => {
  const res = http.post(k6Options.apim.gatewayBaseUrl + data.waitGateway.contextPath, JSON.stringify(data.msg), {
    headers: {
      'Content-Type': 'application/json',
    },
  });
  check(res, {
    'status is 202': () => res.status === 202,
  });
};

export function teardown(data: GatewayTestData) {
  ApisV4Client.changeLifecycle(data.api.id, LifecycleAction.STOP, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  PlansV4Client.deletePlan(data.api.id, data.plan.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  ApisV4Client.deleteApi(data.api.id, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });

  // wait to let the time to undeploy the Api (and close kafka client)
  sleep((k6Options.apim.gatewaySyncInterval * 3) / 1000);
  if (__VU == 0) {
    connection.deleteTopic(kafkaTopic);
  }

  connection.close();
}
