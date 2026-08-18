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
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import { isStringMetric } from '../constants/alertConstants';
import type {
    AlertApiCondition,
    AlertApiNotification,
    AlertApiPeriod,
    AlertFormCondition,
    AlertFormNotification,
    AlertFormTimeframe,
    AlertHistoryPage,
    AlertOperator,
    AlertStringOperator,
    AlertTrigger,
} from '../types';

export function formConditionToApi(c: AlertFormCondition): AlertApiCondition {
    if (c.type === 'RATE') {
        const isStr = isStringMetric(c.property ?? '');
        const comparison: AlertApiCondition = isStr
            ? {
                  type: 'STRING',
                  property: c.property,
                  operator: (c.operator as AlertStringOperator) || 'EQUALS',
                  pattern: c.pattern,
              }
            : {
                  type: 'THRESHOLD',
                  property: c.property,
                  operator: (c.operator as AlertOperator) || 'GT',
                  threshold: c.threshold,
              };
        return {
            type: 'RATE',
            operator: c.rateOperator || 'GT',
            threshold: c.rateThreshold,
            comparison,
            duration: c.duration,
            timeUnit: c.timeUnit,
        };
    }
    if (c.type === 'AGGREGATION') {
        return {
            type: 'AGGREGATION',
            property: c.property,
            function: c.aggregationFunction,
            operator: (c.operator as AlertOperator) || 'GT',
            threshold: c.threshold,
            duration: c.duration,
            timeUnit: c.timeUnit,
        };
    }
    if (c.type === 'THRESHOLD_RANGE') {
        // Classic env payload always includes operator: BETWEEN on threshold-range conditions.
        return {
            type: 'THRESHOLD_RANGE',
            property: c.property,
            operator: 'BETWEEN',
            operatorLow: 'INCLUSIVE',
            thresholdLow: c.thresholdLow,
            operatorHigh: 'INCLUSIVE',
            thresholdHigh: c.thresholdHigh,
        };
    }
    if (c.type === 'STRING') {
        return {
            type: 'STRING',
            property: c.property,
            operator: (c.operator as AlertStringOperator) || 'EQUALS',
            pattern: c.pattern,
        };
    }
    if (c.type === 'THRESHOLD') {
        return {
            type: 'THRESHOLD',
            property: c.property,
            operator: (c.operator as AlertOperator) || 'GT',
            threshold: c.threshold,
        };
    }
    return {
        type: c.type,
        property: c.property,
        operator: c.operator,
        threshold: c.threshold,
        pattern: c.pattern,
        property2: c.property2,
        multiplier: c.multiplier,
        duration: c.duration,
        timeUnit: c.timeUnit,
    };
}

function apiOperatorToForm(operator: AlertApiCondition['operator']): AlertOperator | AlertStringOperator | undefined {
    if (!operator || operator === 'BETWEEN') return undefined;
    return operator;
}

export function apiConditionToForm(c: AlertApiCondition): AlertFormCondition {
    if (c.type === 'RATE') {
        const cmp = (c.comparison ?? {}) as AlertApiCondition;
        return {
            type: 'RATE',
            property: cmp.property,
            operator: apiOperatorToForm(cmp.operator),
            threshold: cmp.threshold,
            pattern: cmp.pattern,
            rateOperator: apiOperatorToForm(c.operator) as AlertOperator | undefined,
            rateThreshold: c.threshold,
            duration: c.duration,
            timeUnit: c.timeUnit,
        };
    }
    if (c.type === 'AGGREGATION') {
        return {
            type: 'AGGREGATION',
            property: c.property,
            aggregationFunction: c.function,
            operator: apiOperatorToForm(c.operator),
            threshold: c.threshold,
            duration: c.duration,
            timeUnit: c.timeUnit,
        };
    }
    if (c.type === 'THRESHOLD_RANGE') {
        return {
            type: 'THRESHOLD_RANGE',
            property: c.property,
            thresholdLow: c.thresholdLow,
            thresholdHigh: c.thresholdHigh,
        };
    }
    return {
        type: c.type,
        property: c.property,
        operator: apiOperatorToForm(c.operator),
        threshold: c.threshold,
        thresholdLow: c.thresholdLow,
        thresholdHigh: c.thresholdHigh,
        pattern: c.pattern,
        property2: c.property2,
        multiplier: c.multiplier,
        duration: c.duration,
        timeUnit: c.timeUnit,
    };
}

export function formNotifToApi(n: AlertFormNotification): AlertApiNotification {
    const isEmail = n.channel === 'email-notifier' || n.channel === 'default-email';
    return {
        type: n.channel,
        configuration: isEmail ? { to: n.target, subject: 'Alert notification' } : { url: n.target },
    };
}

export function apiNotifToForm(n: AlertApiNotification): AlertFormNotification {
    const cfg = (n.configuration ?? {}) as Record<string, unknown>;
    return {
        channel: n.type as AlertFormNotification['channel'],
        target: (cfg['to'] as string) || (cfg['url'] as string) || '',
    };
}

function formTimeframeToApi(tf: AlertFormTimeframe): AlertApiPeriod {
    return {
        days: tf.days,
        beginHour: tf.startHour,
        endHour: tf.endHour,
        zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
    };
}

function apiTimeframeToForm(np: AlertApiPeriod): AlertFormTimeframe {
    return { days: np.days, startHour: np.beginHour, endHour: np.endHour };
}

export interface AlertFormData {
    name: string;
    description: string;
    severity: AlertTrigger['severity'];
    enabled: boolean;
    source: string;
    type: string;
    conditions: AlertFormCondition[];
    filters: AlertFormCondition[];
    notifications: AlertFormNotification[];
    timeframes: AlertFormTimeframe[];
    dampening: AlertTrigger['dampening'];
}

export function alertTriggerToFormData(alert: AlertTrigger): AlertFormData {
    return {
        name: alert.name,
        description: alert.description ?? '',
        severity: alert.severity,
        enabled: alert.enabled,
        source: alert.source,
        type: alert.type,
        conditions: (alert.conditions ?? []).map(apiConditionToForm),
        filters: (alert.filters ?? []).map(apiConditionToForm),
        notifications: (alert.notifications ?? []).map(apiNotifToForm),
        timeframes: (alert.notificationPeriods ?? []).map(apiTimeframeToForm),
        dampening: alert.dampening ?? { mode: 'STRICT_COUNT', trueEvaluations: 1 },
    };
}

export function formDataToAlertTrigger(data: AlertFormData): Omit<AlertTrigger, 'id'> {
    return {
        name: data.name,
        description: data.description || undefined,
        severity: data.severity,
        enabled: data.enabled,
        source: data.source,
        type: data.type,
        conditions: data.conditions.map(formConditionToApi),
        filters: data.filters.map(formConditionToApi),
        notifications: data.notifications.map(formNotifToApi),
        notificationPeriods: data.timeframes.map(formTimeframeToApi),
        dampening: data.dampening,
    };
}

export async function listPlatformAlerts(environmentId: string): Promise<AlertTrigger[]> {
    return apimFetchJsonV1Env<AlertTrigger[]>(environmentId, '/platform/alerts?event_counts=true');
}

export async function createPlatformAlert(environmentId: string, data: AlertFormData): Promise<AlertTrigger> {
    const payload = {
        ...formDataToAlertTrigger(data),
        reference_type: 'ENVIRONMENT',
        reference_id: environmentId,
        template: false,
    };
    return apimFetchJsonV1Env<AlertTrigger>(environmentId, '/platform/alerts', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function updatePlatformAlert(environmentId: string, alert: AlertTrigger): Promise<AlertTrigger> {
    if (!alert.id) {
        throw new Error('Cannot update a platform alert without an id');
    }
    return apimFetchJsonV1Env<AlertTrigger>(environmentId, `/platform/alerts/${encodeURIComponent(alert.id)}`, {
        method: 'PUT',
        body: JSON.stringify(alert),
    });
}

export async function updatePlatformAlertFromForm(environmentId: string, alertId: string, data: AlertFormData): Promise<AlertTrigger> {
    const payload = { ...formDataToAlertTrigger(data), id: alertId };
    return apimFetchJsonV1Env<AlertTrigger>(environmentId, `/platform/alerts/${encodeURIComponent(alertId)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
}

export async function deletePlatformAlert(environmentId: string, alertId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `/platform/alerts/${encodeURIComponent(alertId)}`, {
        method: 'DELETE',
    });
}

export async function listPlatformAlertEvents(environmentId: string, alertId: string, page = 1, size = 10): Promise<AlertHistoryPage> {
    return apimFetchJsonV1Env<AlertHistoryPage>(
        environmentId,
        `/platform/alerts/${encodeURIComponent(alertId)}/events?page=${page}&size=${size}`,
    );
}
