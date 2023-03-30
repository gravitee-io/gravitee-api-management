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

/**
 *
 * @export
 * @interface SubscriptionEntityV4
 */
export interface SubscriptionEntityV4 {
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  api?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  application?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  client_id?: string;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  closed_at?: Date;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  configuration?: SubscriptionConfigurationEntityV4;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  consumerPausedAt?: Date;
  /**
   *
   * @type {SubscriptionConsumerStatus}
   * @memberof SubscriptionEntity
   */
  consumerStatus?: SubscriptionConsumerStatus;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  created_at?: Date;
  /**
   *
   * @type {number}
   * @memberof SubscriptionEntity
   */
  daysToExpirationOnLastNotification?: number;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  ending_at?: Date;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  failureCause?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  id?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof SubscriptionEntity
   */
  keys?: Array<string>;
  /**
   *
   * @type {{ [key: string]: string; }}
   * @memberof SubscriptionEntity
   */
  metadata?: { [key: string]: string };
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  paused_at?: Date;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  plan?: string;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  processed_at?: Date;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  processed_by?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  reason?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  request?: string;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  security?: string;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  starting_at?: Date;
  /**
   *
   * @type {SubscriptionStatus}
   * @memberof SubscriptionEntity
   */
  status?: SubscriptionStatus;
  /**
   *
   * @type {string}
   * @memberof SubscriptionEntity
   */
  subscribed_by?: string;
  /**
   *
   * @type {Date}
   * @memberof SubscriptionEntity
   */
  updated_at?: Date;
}

export const SubscriptionStatus = {
  PENDING: 'PENDING',
  REJECTED: 'REJECTED',
  ACCEPTED: 'ACCEPTED',
  CLOSED: 'CLOSED',
  PAUSED: 'PAUSED',
  RESUMED: 'RESUMED',
} as const;
export type SubscriptionStatus = (typeof SubscriptionStatus)[keyof typeof SubscriptionStatus];

export function SubscriptionStatusFromJSON(json: any): SubscriptionStatus {
  return SubscriptionStatusFromJSONTyped(json, false);
}

export function SubscriptionStatusFromJSONTyped(json: any, ignoreDiscriminator: boolean): SubscriptionStatus {
  return json as SubscriptionStatus;
}

export function SubscriptionStatusToJSON(value?: SubscriptionStatus | null): any {
  return value as any;
}

export const SubscriptionConsumerStatus = {
  STARTED: 'STARTED',
  STOPPED: 'STOPPED',
  FAILURE: 'FAILURE',
} as const;
export type SubscriptionConsumerStatus = (typeof SubscriptionConsumerStatus)[keyof typeof SubscriptionConsumerStatus];

export function SubscriptionConsumerStatusFromJSON(json: any): SubscriptionConsumerStatus {
  return SubscriptionConsumerStatusFromJSONTyped(json, false);
}

export function SubscriptionConsumerStatusFromJSONTyped(json: any, ignoreDiscriminator: boolean): SubscriptionConsumerStatus {
  return json as SubscriptionConsumerStatus;
}

export function SubscriptionConsumerStatusToJSON(value?: SubscriptionConsumerStatus | null): any {
  return value as any;
}
