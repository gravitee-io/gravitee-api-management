/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export type AlertDampeningMode = 'STRICT_COUNT' | 'RELAXED_COUNT' | 'RELAXED_TIME' | 'STRICT_TIME';
export type AlertTimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS';

export interface AlertDampening {
    mode: AlertDampeningMode;
    trueEvaluations?: number;
    totalEvaluations?: number;
    duration?: number;
    timeUnit?: AlertTimeUnit;
}

/** Condition shape returned by the management API (opaque to the list page). */
export type AlertApiCondition = Record<string, unknown>;

export interface AlertApiNotification {
    type: string;
    configuration?: Record<string, unknown>;
}

export interface AlertApiPeriod {
    days: number[];
    beginHour: number;
    endHour: number;
    zoneId?: string;
}

/** Platform / environment alert trigger entity from `/platform/alerts`. */
export interface AlertTrigger {
    id?: string;
    name: string;
    description?: string;
    severity: AlertSeverity;
    enabled: boolean;
    source: string;
    type: string;
    reference_type?: string;
    reference_id?: string;
    conditions?: AlertApiCondition[];
    filters?: AlertApiCondition[];
    notifications?: AlertApiNotification[];
    notificationPeriods?: AlertApiPeriod[];
    dampening?: AlertDampening;
    projections?: unknown[];
    template?: boolean;
    event_rules?: unknown[];
    counters?: Record<string, number>;
    last_alert_at?: string | null;
    last_alert_message?: string | null;
    created_at?: string;
    updated_at?: string;
}
