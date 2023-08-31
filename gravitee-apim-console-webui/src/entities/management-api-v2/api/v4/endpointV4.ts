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

import { EndpointServices } from './endpointServices';

export interface EndpointV4 {
  /**
   * The name of the endpoint
   */
  name?: string;
  /**
   * The type of the endpoint
   */
  type: string;
  /**
   * The weight of the endpoint
   */
  weight?: number;
  /**
   * Is the configuration of the endpoint inherited from the endpoint group it belongs to.
   */
  inheritConfiguration?: boolean;
  configuration?: any;
  sharedConfigurationOverride?: any;
  services?: EndpointServices;
  secondary?: boolean;
}

const KAFKA_ENDPOINT: EndpointV4 = {
  name: 'Default Kafka Endpoint',
  type: 'kafka',
  inheritConfiguration: true,
};

const MOCK_ENDPOINT: EndpointV4 = {
  name: 'Default Mock Endpoint',
  type: 'mock',
  configuration: {},
};

const MQTT_5_ENDPOINT: EndpointV4 = {
  name: 'Default MQTT 5.X Endpoint',
  type: 'mqtt5',
  inheritConfiguration: true,
};

const RABBIT_MQ_ENDPOINT: EndpointV4 = {
  name: 'Default RabbitMQ Endpoint',
  type: 'rabbitmq',
  inheritConfiguration: true,
};

const SOLACE_ENDPOINT: EndpointV4 = {
  name: 'Default Solace Endpoint',
  type: 'solace',
  inheritConfiguration: true,
};

const HTTP_PROXY_ENDPOINT: EndpointV4 = {
  name: 'Default HTTP Proxy Endpoint',
  type: 'http-proxy',
  inheritConfiguration: true,
};

export const EndpointV4Default = {
  byType: (type: string): EndpointV4 => {
    switch (type) {
      case 'kafka': {
        return KAFKA_ENDPOINT;
      }
      case 'mqtt5': {
        return MQTT_5_ENDPOINT;
      }
      case 'rabbitmq': {
        return RABBIT_MQ_ENDPOINT;
      }
      case 'solace': {
        return SOLACE_ENDPOINT;
      }
      case 'mock': {
        return MOCK_ENDPOINT;
      }
      case 'http-proxy': {
        return HTTP_PROXY_ENDPOINT;
      }
      default: {
        return undefined;
      }
    }
  },
};
