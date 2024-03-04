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
import { AlertTriggerEntity, NewAlertTriggerEntity } from './alertTriggerEntity';

import { Scope } from '../alert';

export function fakeAlertTriggerEntity(attributes?: Partial<AlertTriggerEntity>): AlertTriggerEntity {
  const base: AlertTriggerEntity = {
    id: 'id',
    name: 'my alert',
    description: 'description',
    reference_type: Scope.API,
    reference_id: 'api-id',
    created_at: new Date(),
    updated_at: new Date(),
    type: 'alert-type',
    last_alert_at: new Date(),
    last_alert_message: 'last alert message',
    counters: undefined,
    template: false,
    event_rules: [],
    parent_id: 'parent-id',
    environment_id: 'env-id',
    severity: 'INFO',
  };

  return {
    ...base,
    ...attributes,
  };
}

export function fakeNewAlertTriggerEntity(attributes?: Partial<NewAlertTriggerEntity>): NewAlertTriggerEntity {
  const base: NewAlertTriggerEntity = {
    severity: 'INFO',
    source: 'REQUEST',
    name: 'new alert',
    description: 'description',
    conditions: [{ type: 'STRING', property: 'Tenant', operator: 'EQUALS', pattern: 'value', ignoreCase: true }],
    dampening: {
      mode: 'STRICT_COUNT',
      duration: 42,
      timeUnit: 'DAYS',
      totalEvaluations: 42,
      trueEvaluations: 42,
    },
    enabled: true,
    filters: [{ type: 'THRESHOLD', property: 'Status code', operator: 'GT', threshold: 200 }],
  };

  return {
    ...base,
    ...attributes,
  };
}
