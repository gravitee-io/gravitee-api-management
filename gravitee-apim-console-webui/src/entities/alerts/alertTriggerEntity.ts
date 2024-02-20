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
export type AlertReferenceType = 'API' | 'APPLICATION' | 'ENVIRONMENT';

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface AlertEventRuleEntity {
  event: string;
}

export interface AlertTriggerEntity {
  description: string;
  reference_type: AlertReferenceType;
  referenceId: string;
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
}
