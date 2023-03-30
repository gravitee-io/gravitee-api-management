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

import { SubscriptionConfigurationEntityV4 } from '@models/v4/SubscriptionConfigurationEntityV4';

export interface NewSubscriptionEntitV4 {
  /**
   *
   * @type {string}
   * @memberof NewSubscriptionEntity
   */
  application?: string;
  /**
   *
   * @type {string}
   * @memberof NewSubscriptionEntity
   */
  configuration?: SubscriptionConfigurationEntityV4;
  /**
   *
   * @type {string}
   * @memberof NewSubscriptionEntity
   */
  filter?: string;
  /**
   *
   * @type {boolean}
   * @memberof NewSubscriptionEntity
   */
  generalConditionsAccepted?: boolean;
  /*
   *
   * @type {{ [key: string]: string; }}
   * @memberof NewSubscriptionEntity
   */
  metadata?: { [key: string]: string };
  /**
   *
   * @type {string}
   * @memberof NewSubscriptionEntity
   */
  plan?: string;
  /**
   *
   * @type {string}
   * @memberof NewSubscriptionEntity
   */
  request?: string;
}

export const enum NewApiEntityV4DefinitionVersionEnum {
  _1_0_0 = '1.0.0',
  _2_0_0 = '2.0.0',
  _4_0_0 = '4.0.0',
}

export const enum NewApiEntityV4FlowModeEnum {
  DEFAULT = 'DEFAULT',
  BEST_MATCH = 'BEST_MATCH',
}

export const enum NewApiEntityV4TypeEnum {
  PROXY = 'PROXY',
  MESSAGE = 'MESSAGE',
}
