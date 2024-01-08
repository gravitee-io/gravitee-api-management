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

import { ApiEntity } from '@lib/models/v3/ApiEntity';
import { PlanEntity } from '@lib/models/v3/PlanEntity';
import { ApiEntityV4 } from '@models/v4/ApiEntityV4';
import { PlanEntityV4 } from '@models/v4/PlanEntityV4';
import { ApplicationEntityV4 } from './models/v4/ApplicationEntityV4';
import { SubscriptionEntityV4 } from './models/v4/SubscriptionEntityV4';
import { SubscriptionEntity } from '@models/v3/SubscriptionEntity';
import { ApiKeyEntity } from '@models/v3/ApiKeyEntity';

export interface GatewayTestData {
  api?: ApiEntity | ApiEntityV4;
  plan?: PlanEntity | PlanEntityV4;
  waitGateway?: { gatewayTcpUrl?: string; contextPath: string };
  msg?: any;
  applications?: Array<ApplicationEntityV4>;
  subscriptions?: Array<SubscriptionEntityV4>;
  subscription?: SubscriptionEntity | SubscriptionEntityV4;
  keys?: Array<ApiKeyEntity>;
}
