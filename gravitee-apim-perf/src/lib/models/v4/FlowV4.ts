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
export interface FlowV4 {
  /**
   *
   * @type {boolean}
   * @memberof FlowV4
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof FlowV4
   */
  name?: string;
  /**
   *
   * @type {Array<StepV4>}
   * @memberof FlowV4
   */
  publish?: Array<StepV4>;
  /**
   *
   * @type {Array<StepV4>}
   * @memberof FlowV4
   */
  request?: Array<StepV4>;
  /**
   *
   * @type {Array<StepV4>}
   * @memberof FlowV4
   */
  response?: Array<StepV4>;
  /**
   *
   * @type {Array<Selector>}
   * @memberof FlowV4
   */
  selectors?: Array<Selector>;
  /**
   *
   * @type {Array<StepV4>}
   * @memberof FlowV4
   */
  subscribe?: Array<StepV4>;
  /**
   *
   * @type {Array<string>}
   * @memberof FlowV4
   */
  tags?: Array<string>;
}

export interface StepV4 {
  /**
   *
   * @type {string}
   * @memberof StepV4
   */
  condition?: string;
  /**
   *
   * @type {any}
   * @memberof StepV4
   */
  configuration?: any;
  /**
   *
   * @type {string}
   * @memberof StepV4
   */
  description: string;
  /**
   *
   * @type {boolean}
   * @memberof StepV4
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof StepV4
   */
  messageCondition?: string;
  /**
   *
   * @type {string}
   * @memberof StepV4
   */
  name: string;
  /**
   *
   * @type {string}
   * @memberof StepV4
   */
  policy: string;
}

export interface Selector {
  /**
   *
   * @type {string}
   * @memberof Selector
   */
  type: SelectorTypeEnum;
}

export const enum SelectorTypeEnum {
  HTTP = 'HTTP',
  CHANNEL = 'CHANNEL',
  CONDITION = 'CONDITION',
}
