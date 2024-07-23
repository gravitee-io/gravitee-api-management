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
export interface Subscription {
  id: string;
  api: string;
  application: string;
  closed_at?: string;
  created_at?: string;
  plan: string;
  reason?: string;
  request?: string;
  status: SubscriptionStatusEnum;
  subscribed_by?: string;
  keys?: SubscriptionDataKeys[];
}

export type SubscriptionStatusEnum = 'PENDING' | 'ACCEPTED' | 'CLOSED' | 'REJECTED' | 'PAUSED';

export const SubscriptionStatusEnum = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  CLOSED: 'CLOSED',
  REJECTED: 'REJECTED',
  PAUSED: 'PAUSED',
};

export interface SubscriptionDataKeys {
  key?: string;
  id?: string;
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
}
