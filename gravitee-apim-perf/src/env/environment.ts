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
import { b64encode } from 'k6/encoding';
import { Configuration } from '@env/configuration';
import { EntrypointV4QosEnum } from '@models/v4/EntrypointV4';

const { K6_OPTIONS } = __ENV;

const k6DefaultOptions: Configuration = {
  apim: {
    managementBaseUrl: 'http://localhost:8083/management',
    portalBaseUrl: 'http://localhost:8083/portal/environments',
    gatewayBaseUrl: 'http://localhost:8082',
    skipTlsVerify: 'false',
    adminUserName: 'admin',
    adminPassword: 'admin',
    apiUserName: 'api1',
    apiPassword: 'api1',
    appUserName: 'application1',
    appPassword: 'application1',
    simpleUserName: 'user',
    simplePassword: 'password',
    apiEndpointUrl: 'http://localhost:8080/echo',
    apiExecutionMode: 'v3',
    organization: 'DEFAULT',
    environment: 'DEFAULT',
    gatewaySyncInterval: 1000,
    kafkaBoostrapServer: 'localhost:9092',
    httpPost: {
      requestHeadersToMessage: false,
      messageSizeInKB: 1,
      topic: 'msg-http-post',
      numPartitions: 1,
      withJsontoJson: false,
      compression: 'snappy',
    },
    webhook: {
      messageSizeInKB: 1,
      topic: 'msg-webhook',
      numPartitions: 1,
      subscriptions: 1,
      callbackBaseUrl: 'http://localhost:8888/hook',
      compression: 'snappy',
      withJsontoJson: true,
      webhookMaxConnection: 5,
      acks: 1,
      waitDurationInSec: 60,
      qos: EntrypointV4QosEnum.AUTO,
    },
    websocket: {
      messageSizeInKB: 1,
      topic: 'msg-websocket',
      numPartitions: 1,
      subscriptions: 1,
      compression: 'snappy',
      withJsontoJson: true,
      websocketServiceBaseUrl: 'http://localhost:8888/websocket/',
      waitDurationInSec: 60,
      qos: EntrypointV4QosEnum.AUTO,
      acks: 1,
    },
    kafkaInjector: {
      messageSizeInKB: 1,
      topic: 'msg-websocket',
      compression: 'snappy',
      acks: 1,
    },
  },
  k6: {
    prometheusRemoteUrl: 'http://localhost:9090/api/v1/write',
    outputMode: 'xk6-prometheus-rw',
  },
  setupTimeout: '3600s',
  discardResponseBodies: false,
  insecureSkipTLSVerify: false,
  scenarios: {
    default: {
      executor: 'ramping-arrival-rate',

      // Our test with at a rate of 10 iterations started per second.
      startRate: 10,

      // It should start `startRate` iterations per minute
      timeUnit: '1s',

      // It should preallocate 2 VUs before starting the test.
      preAllocatedVUs: 2,

      // It is allowed to spin up to 50 maximum VUs in order to sustain the defined constant arrival rate.
      maxVUs: 50,

      stages: [
        // It should start 300 iterations per second for the first 5 minutes.
        { target: 300, duration: '5m' },

        // It should stay at 300 iterations per second during 2 minutes.
        { target: 300, duration: '2m' },

        // It should linearly ramp-up to 800 iterations per second over the following 5 minutes.
        { target: 800, duration: '5m' },

        // It should linearly ramp-up to 1000 iterations per second for the following 2 minutes.
        { target: 1000, duration: '2m' },

        // It should stay to 1000 iterations per second over the following 5 minutes.
        { target: 1000, duration: '5m' },

        // It should linearly ramp-down to 50 iterations per second over the last 3 minutes.
        { target: 50, duration: '3m' },
      ],
    },
  },
};

const givenOptions = K6_OPTIONS ? JSON.parse(K6_OPTIONS) : {};
// Merge data from config.json with k6DefaultOptions
// ⚠️ Object.assign() does not do a deep copy, only the immediate members, meaning complex objects will be overridden.
export const k6Options: Configuration = Object.assign({}, k6DefaultOptions, givenOptions);

export const ADMIN_USER = {
  username: k6Options.apim.adminUserName,
  password: k6Options.apim.adminPassword,
};

export function authorizationHeaderFor(user: { username: string; password: string }): { [name: string]: string } {
  const basicCredentials = b64encode(`${user.username}:${user.password}`);
  return { Authorization: `Basic ${basicCredentials}` };
}
