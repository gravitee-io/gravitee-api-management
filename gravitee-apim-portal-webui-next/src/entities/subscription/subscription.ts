/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { SubscriptionConsumerConfiguration } from './subscription-consumer-configuration';

export interface Subscription {
  id: string;
  api: string;
  application: string;
  closed_at?: string;
  created_at?: string;
  updated_at?: string;
  plan: string;
  reason?: string;
  request?: string;
  status: SubscriptionStatusEnum;
  consumerStatus: SubscriptionConsumerStatusEnum;
  failureCause?: string;
  subscribed_by?: string;
  keys?: SubscriptionDataKeys[];
  consumerConfiguration?: SubscriptionConsumerConfiguration;
}

export type SubscriptionStatusEnum = 'PENDING' | 'ACCEPTED' | 'CLOSED' | 'REJECTED' | 'PAUSED';

export enum SubscriptionConsumerStatusEnum {
  STARTED = 'STARTED',
  STOPPED = 'STOPPED',
  FAILURE = 'FAILURE',
}

export const SubscriptionStatusEnum = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  CLOSED: 'CLOSED',
  REJECTED: 'REJECTED',
  PAUSED: 'PAUSED',
} as const;

export interface SubscriptionDataKeys {
  key?: string;
  id?: string;
  hash?: string;
  application: {
    id: string;
    name: string;
  };
}

export interface CreateSubscription {
  api_key_mode?: 'SHARED' | 'EXCLUSIVE' | 'UNSPECIFIED';
  application: string;
  general_conditions_accepted?: boolean;
  general_conditions_content_revision?: {
    pageId?: string;
    revision?: number;
  };
  plan: string;
  request?: string;
  configuration?: SubscriptionConsumerConfiguration;
  metadata?: Record<string, string>;
}

export interface UpdateSubscription extends Omit<Subscription, 'consumerConfiguration'> {
  configuration?: SubscriptionConsumerConfiguration;
}
