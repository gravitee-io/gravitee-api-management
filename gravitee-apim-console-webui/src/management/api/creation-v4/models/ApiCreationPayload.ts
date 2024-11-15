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
import { ApiType, CreatePlanV4, KafkaHost, KafkaPort, ListenerType, PathV4, Qos } from '../../../../entities/management-api-v2';
import { TcpHost } from '../../../../entities/management-api-v2/api/v4/tcpHost';

export type ApiCreationPayload = Partial<{
  // API details
  name?: string;
  version?: string;
  description?: string;

  // Entrypoints
  type?: ApiType;
  selectedNativeType?: 'KAFKA';
  paths?: PathV4[];
  hosts?: TcpHost[];
  host?: KafkaHost;
  port?: KafkaPort;
  selectedEntrypoints?: {
    id: string;
    name: string;
    icon: string;
    supportedListenerType: ListenerType;
    supportedQos?: Qos[];
    selectedQos?: Qos;
    configuration?: unknown;
    deployed: boolean;
  }[];

  // Endpoints
  selectedEndpoints?: {
    id: string;
    name: string;
    icon: string;
    configuration?: unknown;
    sharedConfiguration?: unknown;
    deployed: boolean;
  }[];

  // Security
  plans?: CreatePlanV4[];

  // Summary
  deploy: boolean;
  askForReview: boolean;
}>;
