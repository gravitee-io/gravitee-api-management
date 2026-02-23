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
import { DefinitionVersion } from '../management-api-v2';

export type SubscriptionStatus = 'PENDING' | 'REJECTED' | 'ACCEPTED' | 'CLOSED' | 'PAUSED' | 'RESUMED';

export type SubscriptionOrigin = 'KUBERNETES' | 'MANAGEMENT';

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
  origin: SubscriptionOrigin;
  referenceType?: string;
  referenceId?: string;
}

export interface Subscription {
  id?: string;
  api?: SubscriptionApi;
  apiProduct?: SubscriptionApiProduct;
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
  origin: SubscriptionOrigin;
  configuration?: SubscriptionConsumerConfiguration;
  metadata?: { [key: string]: string };
  referenceType?: string;
  referenceId?: string;
}

/**
 * Consumer configuration associated to the subscription in case it is attached to a push plan.
 */
export interface SubscriptionConsumerConfiguration {
  /**
   * The id of the targeted entrypoint
   */
  entrypointId: string;
  /**
   * The channel to consume
   */
  channel?: string;
  /**
   * The configuration to use at subscription time to push to the target service.
   */
  entrypointConfiguration?: any;
}

export interface SubscriptionApi {
  id: string;
  name: string;
  version: string;
  definitionVersion: DefinitionVersion;
  owner: {
    id: string;
    displayName: string;
  };
}

/** Display info for API Product subscription (referenceType = API_PRODUCT). */
export interface SubscriptionApiProduct {
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
  security?: string;
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
  referenceType?: string;
  referenceId?: string;
}
