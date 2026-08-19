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
import { ApiType } from '../api/api';
import { PlanMode, PlanSecurityEnum, PlanUsageConfiguration } from '../plan/plan';

export interface ApiProductSubscriptionDetails {
  id: string;
  name?: string;
  version?: string;
  availability: ApiProductSubscriptionAvailability;
  plan?: ApiProductSubscriptionPlan;
  apis: ApiProductSubscriptionApi[];
}

export interface ApiProductSubscriptionPlan {
  id: string;
  name?: string;
  security?: PlanSecurityEnum;
  mode?: PlanMode;
  usageConfiguration?: PlanUsageConfiguration;
}

export interface ApiProductSubscriptionApi {
  id: string;
  name?: string;
  version?: string;
  type?: ApiType;
  availability: ApiProductSubscriptionApiAvailability;
  entrypoints: string[];
  documentation?: ApiProductSubscriptionApiDocumentation;
}

export interface ApiProductSubscriptionApiDocumentation {
  rootId: string;
  navigationItemId: string;
}

export type ApiProductSubscriptionAvailability = 'AVAILABLE' | 'UNAVAILABLE';
export type ApiProductSubscriptionApiAvailability = 'AVAILABLE' | 'UNPUBLISHED' | 'UNAVAILABLE';
