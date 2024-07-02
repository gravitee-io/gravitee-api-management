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
import { ApiKeyMode } from '../application/Application';
import { User } from '../user/user';

export type SubscriptionStatus = 'PENDING' | 'REJECTED' | 'ACCEPTED' | 'CLOSED' | 'PAUSED' | 'RESUMED';

export interface SubscriptionPage {
  id?: string;
  api?: string;
  plan?: string;
  application?: string;
  status?: SubscriptionStatus;
  processed_at?: Date;
  processed_by?: string;
  subscribed_by?: User;
  request?: string;
  reason?: string;
  starting_at?: Date;
  ending_at?: Date;
  created_at?: Date;
  updated_at?: Date;
  closed_at?: Date;
  paused_at?: Date;
  client_id?: string;
  security?: string;
}

export interface Subscription {
  id?: string;
  api?: SubscriptionApi;
  plan?: SubscriptionPlan;
  application?: SubscriptionApplication;
  status?: SubscriptionStatus;
  processed_at?: Date;
  processed_by?: string;
  subscribed_by?: User;
  request?: string;
  reason?: string;
  starting_at?: Date;
  ending_at?: Date;
  created_at?: Date;
  updated_at?: Date;
  closed_at?: Date;
  paused_at?: Date;
  client_id?: string;
  security?: string;
}

export interface SubscriptionApi {
  id: string;
  name: string;
  version: string;
  owner: {
    id: string;
    displayName: string;
  };
}

export interface SubscriptionPlan {
  id: string;
  name: string;
  security: string;
}

export interface SubscriptionApplication {
  id: string;
  name: string;
  type: string;
  description: string;
  domain: string;
  apiKeyMode: ApiKeyMode;
  owner: {
    id: string;
    displayName: string;
  };
}

export interface ApplicationSubscription {
  id?: string;
  api?: string;
  plan?: string;
  application?: string;
  status?: SubscriptionStatus;
  security?: string;
  consumerStatus?: string;
  processed_at?: Date;
  processed_by?: string;
  subscribed_by?: User;
  starting_at?: Date;
  created_at?: Date;
  updated_at?: Date;
  ending_at?: Date;
}
