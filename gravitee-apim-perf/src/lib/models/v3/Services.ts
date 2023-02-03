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
import { HealthCheckStep } from '@lib/models/v3/EndpointGroup';

export interface Services {
  /**
   *
   * @type {EndpointDiscoveryService}
   * @memberof Services
   */
  discovery?: EndpointDiscoveryService;
  /**
   *
   * @type {DynamicPropertyService}
   * @memberof Services
   */
  dynamic_property?: DynamicPropertyService;
  /**
   *
   * @type {HealthCheckService}
   * @memberof Services
   */
  health_check?: HealthCheckService;
}

export interface EndpointDiscoveryService {
  /**
   *
   * @type {any}
   * @memberof EndpointDiscoveryService
   */
  configuration?: any;
  /**
   *
   * @type {boolean}
   * @memberof EndpointDiscoveryService
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof EndpointDiscoveryService
   */
  provider?: string;
}

export interface DynamicPropertyService {
  /**
   *
   * @type {any}
   * @memberof DynamicPropertyService
   */
  configuration?: any;
  /**
   *
   * @type {boolean}
   * @memberof DynamicPropertyService
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof DynamicPropertyService
   */
  provider?: DynamicPropertyServiceProviderEnum;
  /**
   *
   * @type {string}
   * @memberof DynamicPropertyService
   */
  schedule?: string;
}

export interface HealthCheckService {
  /**
   *
   * @type {boolean}
   * @memberof HealthCheckService
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof HealthCheckService
   */
  schedule?: string;
  /**
   *
   * @type {Array<HealthCheckStep>}
   * @memberof HealthCheckService
   */
  steps?: Array<HealthCheckStep>;
}

export const enum DynamicPropertyServiceProviderEnum {
  HTTP = 'HTTP',
}
