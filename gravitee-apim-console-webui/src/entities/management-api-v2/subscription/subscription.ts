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
import { SubscriptionConsumerConfiguration } from './subscriptionConsumerConfiguration';
import { SubscriptionConsumerStatus } from './subscriptionConsumerStatus';
import { SubscriptionStatus } from './subscriptionStatus';

import { BaseApi } from '../api';
import { BasePlan } from '../plan';
import { BaseApplication } from '../application';
import { BaseUser } from '../user';

export interface Subscription {
  /**
   * Subscription's uuid.
   */
  id?: string;
  api?: BaseApi;
  plan?: BasePlan;
  application?: BaseApplication;
  /**
   * Message given by the api consumer when subscribing to the api.
   */
  consumerMessage?: string;
  /**
   * Message given by the api publisher when accepting or rejecting the subscription.
   */
  publisherMessage?: string;
  /**
   * A list of metadata associated to this subscription.
   */
  metadata?: { [key: string]: string };
  /**
   * Number of days before the expiration of this subscription when the last pre-expiration notification was sent
   */
  daysToExpirationOnLastNotification?: number;
  consumerConfiguration?: SubscriptionConsumerConfiguration;
  /**
   * Details about the last failure encountered on this subscription.
   */
  failureCause?: string;
  status?: SubscriptionStatus;
  consumerStatus?: SubscriptionConsumerStatus;
  processedBy?: BaseUser;
  subscribedBy?: BaseUser;
  /**
   * The datetime when the subscription was processed.
   */
  processedAt?: Date;
  /**
   * The datetime when the subscription starts. No starting date means the subscription starts immediately.
   */
  startingAt?: Date;
  /**
   * The datetime when the subscription ends. No ending date means the subscription never ends.
   */
  endingAt?: Date;
  commentMessage?: string;
  commentRequired?: boolean;
  /**
   * The datetime when the subscription was created.
   */
  createdAt?: Date;
  /**
   * The last datetime when the subscription was updated.
   */
  updatedAt?: Date;
  /**
   * The datetime when the subscription was closed.
   */
  closedAt?: Date;
  /**
   * The datetime when the subscription was paused by the api publisher.
   */
  pausedAt?: Date;
  /**
   * The datetime when the subscription was paused by the api consumer.
   */
  consumerPausedAt?: Date;
}
