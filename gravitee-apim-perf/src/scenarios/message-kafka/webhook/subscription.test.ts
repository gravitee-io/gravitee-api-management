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
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4, PlanModeV4 } from '@models/v4/PlanEntityV4';
import { NewApiEntityV4TypeEnum } from '@models/v4/NewApiEntityV4';
import { ApplicationsV4Client } from '@clients/v4/ApplicationsV4Client';
import { SubscriptionEntityV4 } from '@models/v4/SubscriptionEntityV4';
import { ApplicationEntityV4 } from '@models/v4/ApplicationEntityV4';
import { randomString } from '@helpers/random.helper';

/**
 * Creates an API with Webhook entrypoint consuming messages from Kafka Endpoint.
 */
const schemaRegistry = new SchemaRegistry();
const connection = new Connection({
  // ConnectionConfig object
  address: k6Options.apim.kafkaBoostrapServer,
});

const kafkaTopic = k6Options.apim.webhook.topic;
const numPartitions = k6Options.apim.webhook.numPartitions;
const numberOfSubscriptions = k6Options.apim.webhook.subscriptions;

const writer = new Writer({
  brokers: [k6Options.apim.kafkaBoostrapServer],
  topic: kafkaTopic,
  autoCreateTopic: true,
  compression: k6Options.apim.webhook.compression,
  requiredAcks: k6Options.apim.webhook.acks,
});

if (__VU == 0) {
  connection.createTopic({
    topic: kafkaTopic,
    numPartitions: numPartitions,
    configEntries: [
      {
        configName: 'compression.type',
        configValue: k6Options.apim.webhook.compression,
      },
    ],
  });
}

export const options = k6Options;
options['teardownTimeout'] = `${k6Options.apim.webhook.waitDurationInSec + 120}s`;
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
        type: 'SUBSCRIPTION',
        entrypoints: [
          {
            type: 'webhook',
            qos: k6Options.apim.webhook.qos,
            configuration: {
              http: {
                maxConcurrentConnections: k6Options.apim.webhook.webhookMaxConnection,
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
        selectors: [
          {
            type: 'CHANNEL',
            operation: ['SUBSCRIBE'],
            channel: '/',
            'channel-operator': 'STARTS_WITH',
          },
        ],
        request: [],
        response: [],
        subscribe: k6Options.apim.webhook.withJsontoJson
          ? [
              {
                name: 'Json to Json',
                description: 'Add sourceTimestamp to help webhook to generate globalMessageLatency metric',
                enabled: true,
                policy: 'json-to-json',
                configuration: {
                  specification: '[{ "operation": "default", "spec": { "sourceTimestamp": "{#message.metadata[\'sourceTimestamp\']}" }}]',
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
    PlansV4Fixture.newPlan({
      mode: PlanModeV4.PUSH,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    },
  );
  failIf(planCreationResponse.status !== 201, 'Could not create plan');
  const createdPlan = HttpHelper.parseBody<PlanEntityV4>(planCreationResponse);

  const publishPlanResponse = PlansV4Client.publishPlan(createdApi.id, createdPlan.id, {
    headers: {
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(publishPlanResponse.status !== 200, 'Could not publish plan');

  const changeLifecycleResponse = ApisV4Client.changeLifecycle(createdApi.id, LifecycleAction.START, {
    headers: {
      'Content-Type': 'application/json',
      ...authorizationHeaderFor(ADMIN_USER),
    },
  });
  failIf(changeLifecycleResponse.status !== 204, 'Could not change lifecycle');

  const applications = [];
  const subscriptions = [];
  for (let i = 0; i < numberOfSubscriptions; ++i) {
    const appCreationResponse = ApplicationsV4Client.createApplication({
      headers: {
        'Content-Type': 'application/json',
        ...authorizationHeaderFor(ADMIN_USER),
      },
    });
    const createdApp = HttpHelper.parseBody<ApplicationEntityV4>(appCreationResponse);
    applications.push(createdApp);

    const subscriptionCreationResponse = ApisV4Client.createSubscription(
      createdApi.id,
      {
        applicationId: createdApp.id,
        planId: createdPlan.id,
        consumerConfiguration: {
          entrypointId: 'webhook',
          entrypointConfiguration: {
            callbackUrl: `${k6Options.apim.webhook.callbackBaseUrl}/subscription_${i}`,
          },
        },
        metadata: {
          consumerGroup: `subscription_${i}`,
        },
      },
      {
        headers: {
          'Content-Type': 'application/json',
          ...authorizationHeaderFor(ADMIN_USER),
        },
      },
    );
    const createdSubscription = HttpHelper.parseBody<SubscriptionEntityV4>(subscriptionCreationResponse);
    subscriptions.push(createdSubscription);
  }

  // wait 1 sec per subscription + 10 seconds that the webhook subscription are in place
  sleep(numberOfSubscriptions + 10);

  const message = generatePayloadInKB(k6Options.apim.webhook.messageSizeInKB);
  return {
    api: createdApi,
    plan: createdPlan,
    waitGateway: { contextPath: contextPath },
    // The data type of the value is a byte array
    msg: schemaRegistry.serialize({
      data: Array.from(JSON.stringify(message), (x) => x.charCodeAt(0)),
      schemaType: SCHEMA_TYPE_BYTES,
    }),
    applications: applications,
    subscriptions: subscriptions,
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
  sleep(k6Options.apim.webhook.waitDurationInSec);

  if (data.subscriptions) {
    data.subscriptions.forEach((sub) => {
      ApisV4Client.stopSubscription(data.api.id, sub.id, {
        headers: {
          'Content-Type': 'application/json',
          ...authorizationHeaderFor(ADMIN_USER),
        },
      });
    });
  }

  if (data.applications) {
    data.applications.forEach((app) => {
      ApplicationsV4Client.deleteApplication(app.id, {
        headers: {
          'Content-Type': 'application/json',
          ...authorizationHeaderFor(ADMIN_USER),
        },
      });
    });
  }

  ApisV4Client.changeLifecycle(data.api.id, LifecycleAction.STOP, {
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
