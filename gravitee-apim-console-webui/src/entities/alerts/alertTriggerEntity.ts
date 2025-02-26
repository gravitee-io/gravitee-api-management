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

import { Dampening } from './dampening';
import { AlertCondition } from './conditions';
import { NotificationPeriod } from './notificationPeriod';
import { AlertNotification } from './notification';

import { Scope } from '../alert';

export const ALERT_SEVERITIES = ['INFO', 'WARNING', 'CRITICAL'] as const;
export type AlertSeverity = (typeof ALERT_SEVERITIES)[number];

export interface AlertEventRuleEntity {
  event: string;
}

export interface AlertTriggerEntity {
  id: string;
  name: string;
  description: string;
  reference_type: Scope;
  reference_id: string;
  created_at: Date;
  updated_at: Date;
  type: string;
  last_alert_at: Date;
  last_alert_message: string;
  counters: Record<string, number>;
  template: boolean;
  event_rules: AlertEventRuleEntity[];
  parent_id: string;
  environment_id: string;
  severity: AlertSeverity;
  enabled: boolean;
  conditions?: AlertCondition[];
}

export interface NewAlertTriggerEntity extends Trigger {
  reference_type?: string;
  reference_id?: string;
  type?: string;
  template?: boolean;
  eventRules?: AlertEventRuleEntity[];
}

interface Trigger {
  id?: string;
  severity?: AlertSeverity;
  source?: string;
  name?: string;
  description?: string;
  conditions?: AlertCondition[];
  notifications?: AlertNotification[];
  dampening?: Dampening;
  metadata?: Record<string, Record<string, string>>;
  enabled?: boolean;
  filters?: AlertCondition[];
  notificationPeriods?: NotificationPeriod[];
}
