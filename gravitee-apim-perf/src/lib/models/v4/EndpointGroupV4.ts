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
export interface EndpointGroupV4 {
  /**
   *
   * @type {Array<EndpointV4>}
   * @memberof EndpointGroupV4
   */
  endpoints?: Array<EndpointV4>;
  /**
   *
   * @type {LoadBalancerV4}
   * @memberof EndpointGroupV4
   */
  loadBalancer?: LoadBalancerV4;
  /**
   *
   * @type {string}
   * @memberof EndpointGroupV4
   */
  name: string;
  /**
   *
   * @type {EndpointGroupServicesV4}
   * @memberof EndpointGroupV4
   */
  services?: EndpointGroupServicesV4;
  /**
   *
   * @type {any}
   * @memberof EndpointGroupV4
   */
  sharedConfiguration?: any;
  /**
   *
   * @type {string}
   * @memberof EndpointGroupV4
   */
  type: string;
}

export interface EndpointV4 {
  /**
   *
   * @type {any}
   * @memberof EndpointV4
   */
  configuration?: any;
  /**
   *
   * @type {boolean}
   * @memberof EndpointV4
   */
  inheritConfiguration?: boolean;
  /**
   *
   * @type {string}
   * @memberof EndpointV4
   */
  name: string;
  /**
   *
   * @type {boolean}
   * @memberof EndpointV4
   */
  secondary?: boolean;
  /**
   *
   * @type {EndpointServicesV4}
   * @memberof EndpointV4
   */
  services?: EndpointServicesV4;
  /**
   *
   * @type {string}
   * @memberof EndpointV4
   */
  type: string;
  /**
   *
   * @type {number}
   * @memberof EndpointV4
   */
  weight?: number;
}

export interface EndpointServicesV4 {
  /**
   *
   * @type {ServiceV4}
   * @memberof EndpointServicesV4
   */
  healthCheck?: ServiceV4;
}

export interface ServiceV4 {
  /**
   *
   * @type {any}
   * @memberof ServiceV4
   */
  configuration?: any;
  /**
   *
   * @type {boolean}
   * @memberof ServiceV4
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof ServiceV4
   */
  type: string;
}

export interface LoadBalancerV4 {
  /**
   *
   * @type {string}
   * @memberof LoadBalancerV4
   */
  type?: LoadBalancerV4TypeEnum;
}

export const enum LoadBalancerV4TypeEnum {
  ROUND_ROBIN = 'ROUND_ROBIN',
  RANDOM = 'RANDOM',
  WEIGHTED_ROUND_ROBIN = 'WEIGHTED_ROUND_ROBIN',
  WEIGHTED_RANDOM = 'WEIGHTED_RANDOM',
}

export interface EndpointGroupServicesV4 {
  /**
   *
   * @type {ServiceV4}
   * @memberof EndpointGroupServicesV4
   */
  discovery?: ServiceV4;
  /**
   *
   * @type {ServiceV4}
   * @memberof EndpointGroupServicesV4
   */
  healthCheck?: ServiceV4;
}
