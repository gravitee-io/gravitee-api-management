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
  data: SubscriptionData[];
  links: SubscriptionLinks;
  metadata: SubscriptionMetadata;
}

export interface SubscriptionData {
  id?: string;
  api?: string;
  application?: string;
  closed_at?: string;
  created_at?: string;
  plan?: string;
  reason?: string;
  request?: string;
  status?: string;
  subscribed_by?: string;
  keys: SubscriptionDataKeys[];
}

export interface SubscriptionLinks {
  self?: string;
}

export interface SubscriptionDataKeys {
  key?: string;
  id?: string;
  application: {
    id: string;
    name: string;
  };
}

export interface SubscriptionMetadata {
  [key: string]: SubscriptionMetadataEntrypoints;
}

export interface SubscriptionMetadataEntrypoints {
  entrypoints?: SubscriptionMetadataEntrypointsTarget[];
  name?: string;
}

export interface SubscriptionMetadataEntrypointsTarget {
  target?: string;
}

export const SubscriptionStatusEnum = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  CLOSED: 'CLOSED',
  REJECTED: 'REJECTED',
  PAUSED: 'PAUSED',
};
