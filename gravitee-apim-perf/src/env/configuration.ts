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
import { Options } from 'k6/options';
import { EntrypointV4QosEnum } from '@models/v4/EntrypointV4';

export interface Configuration extends Options {
  apim: APIManagementConfiguration;
  k6: K6Configuration;
}

interface APIManagementConfiguration {
  managementBaseUrl: string;
  portalBaseUrl: string;
  gatewayBaseUrl: string;
  skipTlsVerify: string;
  adminUserName: string;
  adminPassword: string;
  apiUserName: string;
  apiPassword: string;
  appUserName: string;
  appPassword: string;
  simpleUserName: string;
  simplePassword: string;
  apiEndpointUrl: string;
  kafkaBoostrapServer: string;
  apiExecutionMode: string;
  organization: string;
  environment: string;
  gatewaySyncInterval: number;
  httpPost: HttpPost;
  webhook: Webhook;
  websocket: Websocket;
  kafkaInjector: KafkaInjector;
}

interface HttpPost {
  requestHeadersToMessage: boolean;
  messageSizeInKB: number;
  topic: string;
  withJsontoJson: boolean;
  numPartitions: number;
  compression: string;
}

interface Webhook {
  messageSizeInKB: number;
  topic: string;
  numPartitions: number;
  subscriptions: number;
  callbackBaseUrl: string;
  compression: string;
  acks: number;
  withJsontoJson: boolean;
  waitDurationInSec: number;
  webhookMaxConnection: number;
  qos: EntrypointV4QosEnum;
}
interface Websocket {
  messageSizeInKB: number;
  topic: string;
  numPartitions: number;
  subscriptions: number;
  compression: string;
  withJsontoJson: boolean;
  waitDurationInSec: number;
  websocketServiceBaseUrl: string;
  qos: EntrypointV4QosEnum;
  acks: number;
}
interface KafkaInjector {
  messageSizeInKB: number;
  topic: string;
  compression: string;
  acks: number;
}

interface K6Configuration {
  prometheusRemoteUrl: string;
  outputMode: string;
}
