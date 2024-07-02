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
import { Subscription } from './subscription';
import { PlanMode, PlanSecurityEnum } from '../plan/plan';

export interface SubscriptionsResponse {
  data: Subscription[];
  links: SubscriptionLinks;
  metadata: SubscriptionMetadata;
}

export interface SubscriptionLinks {
  self?: string;
}

export interface SubscriptionMetadata {
  [key: string]: SubscriptionMetadataContent;
}

export interface SubscriptionMetadataContent {
  entrypoints?: SubscriptionMetadataEntrypointsTarget[];
  name?: string;
  planMode?: PlanMode;
  securityType?: PlanSecurityEnum;
}

export interface SubscriptionMetadataEntrypointsTarget {
  target?: string;
}
