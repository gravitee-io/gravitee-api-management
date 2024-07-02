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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { Plan } from './plan';
import { PlansResponse } from './plans-response';
export function fakePlan(modifier?: Partial<Plan> | ((baseSubscription: Plan) => Plan)): Plan {
  const base: Plan = {
    id: '5ac5ca94-160f-4acd-85ca-94160fcacd7d',
    security: 'API_KEY',
    name: 'My lovely plan',
    mode: 'STANDARD',
    validation: 'MANUAL',
    order: 0,
    description: 'A nice plan description',
    usage_configuration: {
      quota: { limit: 1, period_time: 1, period_time_unit: 'MONTHS' },
      rate_limit: { limit: 5, period_time_unit: 'MINUTES', period_time: 5 },
    },
    general_conditions: 'general-conditions-page',
    comment_required: false,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakePlansResponse(modifier?: Partial<PlansResponse> | ((baseSubscription: PlansResponse) => PlansResponse)): PlansResponse {
  const base: PlansResponse = {
    data: [fakePlan()],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
