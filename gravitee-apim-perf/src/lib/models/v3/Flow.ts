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
export interface Flow {
  /**
   *
   * @type {string}
   * @memberof Flow
   */
  condition?: string;
  /**
   *
   * @type {Array<Consumer>}
   * @memberof Flow
   */
  consumers?: Array<Consumer>;
  /**
   *
   * @type {boolean}
   * @memberof Flow
   */
  enabled?: boolean;
  /**
   *
   * @type {Array<string>}
   * @memberof Flow
   */
  methods?: Array<FlowMethodsEnum>;
  /**
   *
   * @type {string}
   * @memberof Flow
   */
  name?: string;
  /**
   *
   * @type {PathOperator}
   * @memberof Flow
   */
  path_operator?: PathOperator;
  /**
   *
   * @type {Array<Step>}
   * @memberof Flow
   */
  post?: Array<Step>;
  /**
   *
   * @type {Array<Step>}
   * @memberof Flow
   */
  pre?: Array<Step>;
}

export interface Consumer {
  /**
   *
   * @type {string}
   * @memberof Consumer
   */
  consumerId?: string;
  /**
   *
   * @type {string}
   * @memberof Consumer
   */
  consumerType?: ConsumerConsumerTypeEnum;
}

export interface PathOperator {
  /**
   *
   * @type {string}
   * @memberof PathOperator
   */
  operator?: PathOperatorOperatorEnum;
  /**
   *
   * @type {string}
   * @memberof PathOperator
   */
  path?: string;
}

export interface Step {
  /**
   *
   * @type {string}
   * @memberof Step
   */
  condition?: string;
  /**
   *
   * @type {any}
   * @memberof Step
   */
  configuration?: any;
  /**
   *
   * @type {string}
   * @memberof Step
   */
  description?: string;
  /**
   *
   * @type {boolean}
   * @memberof Step
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof Step
   */
  name?: string;
  /**
   *
   * @type {string}
   * @memberof Step
   */
  policy?: string;
}

export const enum PathOperatorOperatorEnum {
  STARTS_WITH = 'STARTS_WITH',
  EQUALS = 'EQUALS',
}

export const enum FlowMethodsEnum {
  CONNECT = 'CONNECT',
  DELETE = 'DELETE',
  GET = 'GET',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
  PATCH = 'PATCH',
  POST = 'POST',
  PUT = 'PUT',
  TRACE = 'TRACE',
  OTHER = 'OTHER',
}

export const enum ConsumerConsumerTypeEnum {
  TAG = 'TAG',
}
