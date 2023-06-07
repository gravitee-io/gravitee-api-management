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
import { sleep } from 'k6';
import { Connection, SCHEMA_TYPE_BYTES, SchemaRegistry, Writer } from 'k6/x/kafka';
import { ADMIN_USER, authorizationHeaderFor, k6Options } from '@env/environment';
import { LifecycleAction } from '@models/v3/ApiEntity';
import { failIf, generatePayloadInKB } from '@helpers/k6.helper';
import { ApisFixture } from '@fixtures/v3/apis.fixture';
import { HttpHelper } from '@helpers/http.helper';
import { GatewayTestData } from '@lib/test-api';
import { ApisV4Fixture } from '@fixtures/v4/apis.v4.fixture';
import { ApisV4Client } from '@clients/v4/ApisV4Client';
import { PlansV4Client } from '@clients/v4/PlansV4Client';
import { PlansV4Fixture } from '@fixtures/v4/plans.v4.fixture';
import { NewPlanEntityV4StatusEnum } from '@models/v4/NewPlanEntityV4';
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4 } from '@models/v4/PlanEntityV4';
import { NewApiEntityV4TypeEnum } from '@models/v4/NewApiEntityV4';
import { randomString } from '@helpers/random.helper';
import { LoadTestEndpointClient } from '@clients/LoadTestEndpointClient';

const schemaRegistry = new SchemaRegistry();
const connection = new Connection({
  // ConnectionConfig object
  address: k6Options.apim.kafkaBoostrapServer,
});

const kafkaTopic = k6Options.apim.websocket.topic;
const numPartitions = k6Options.apim.websocket.numPartitions;

const writer = new Writer({
  brokers: [k6Options.apim.kafkaBoostrapServer],
  topic: kafkaTopic,
  autoCreateTopic: true,
  compression: k6Options.apim.websocket.compression,
  requiredAcks: k6Options.apim.websocket.acks,
});

if (__VU == 0) {
  connection.createTopic({
    topic: kafkaTopic,
    numPartitions: numPartitions,
    configEntries: [
      {
        configName: 'compression.type',
        configValue: k6Options.apim.websocket.compression,
      },
    ],
  });
}

export const options = k6Options;
options['teardownTimeout'] = `${k6Options.apim.websocket.waitDurationInSec + 120}s`;
options['thresholds'] = {
  // Base thresholds to see if the writer or reader is working
  kafka_writer_error_count: ['count == 0'],
  kafka_reader_error_count: ['count == 0'],
};

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
            type: 'websocket',
            configuration: {
              publisher: {
                enabled: false,
              },
              subscriber: {
                enabled: true,
              },
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
              consumer: {
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
        subscribe: k6Options.apim.httpPost.withJsontoJson
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
        publish: [],
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

  // wait 2 times the wait interval to be sure that the sync takes place at least ones.
  sleep((k6Options.apim.gatewaySyncInterval * 2) / 1000);

  for (let i = 0; i < k6Options.apim.websocket.subscriptions; ++i) {
    LoadTestEndpointClient.startWebsocketSubscription(contextPath, `subscription_${i}`);
  }

  sleep(5);

  const message = generatePayloadInKB(k6Options.apim.websocket.messageSizeInKB);
  return {
    api: createdApi,
    plan: createdPlan,
    waitGateway: { contextPath: contextPath },
    // The data type of the value is a byte array
    msg: schemaRegistry.serialize({
      data: Array.from(JSON.stringify(message), (x) => x.charCodeAt(0)),
      schemaType: SCHEMA_TYPE_BYTES,
    }),
  };
}

export default (data: GatewayTestData) => {
  writer.produce({
    messages: [
      {
        // The data type of the key is a string
        key: schemaRegistry.serialize({
          data: Array.from('id-' + randomString(), (x) => x.charCodeAt(0)),
          schemaType: SCHEMA_TYPE_BYTES,
        }),
        value: data.msg,
      },
    ],
  });
};

export function teardown(data: GatewayTestData) {
  // wait a given time to let the consumers consume the topic
  sleep(k6Options.apim.websocket.waitDurationInSec);

  for (let i = 0; i < k6Options.apim.websocket.subscriptions; ++i) {
    LoadTestEndpointClient.stopWebsocketSubscription(`subscription_${i}`);
  }

  sleep(5);

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

  writer.close();
  connection.close();
}
