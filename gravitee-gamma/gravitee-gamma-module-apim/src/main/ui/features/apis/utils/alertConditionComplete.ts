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
import { isInfoOnlyRule, isStringMetric } from '../constants/alertConstants';
import type { AlertConditionType, AlertDampening, AlertFormCondition, AlertRuleId } from '../types';

/** Default filter for the given metric — string metrics must not start as THRESHOLD. */
export function defaultFilterCondition(property: string): AlertFormCondition {
    if (isStringMetric(property)) {
        return { type: 'STRING', property, operator: 'EQUALS' };
    }
    return { type: 'THRESHOLD', property, operator: 'GT' };
}

/** Reset value fields when the condition type changes (FilterRow / SimpleConditionForm). */
export function conditionWithType(condition: AlertFormCondition, type: AlertConditionType, seededProperty2?: string): AlertFormCondition {
    return {
        ...condition,
        type,
        operator: type === 'STRING' ? 'EQUALS' : type === 'THRESHOLD' || type === 'COMPARE' ? 'GT' : undefined,
        threshold: undefined,
        thresholdLow: undefined,
        thresholdHigh: undefined,
        pattern: undefined,
        property2: type === 'COMPARE' ? (condition.property2 ?? seededProperty2) : undefined,
        multiplier: undefined,
    };
}

function hasPositiveNumber(value: number | undefined): value is number {
    return typeof value === 'number' && value >= 1;
}

export function isAlertConditionComplete(condition: AlertFormCondition): boolean {
    switch (condition.type) {
        case 'THRESHOLD':
            return hasPositiveNumber(condition.threshold) && !!condition.operator;
        case 'THRESHOLD_RANGE':
            return (
                hasPositiveNumber(condition.thresholdLow) &&
                hasPositiveNumber(condition.thresholdHigh) &&
                condition.thresholdLow <= condition.thresholdHigh
            );
        case 'STRING':
            return !!condition.operator && !!condition.pattern?.trim();
        case 'COMPARE':
            return !!condition.operator && hasPositiveNumber(condition.multiplier) && !!condition.property2;
        case 'AGGREGATION':
            return (
                !!condition.operator &&
                hasPositiveNumber(condition.threshold) &&
                hasPositiveNumber(condition.duration) &&
                !!condition.timeUnit
            );
        case 'RATE': {
            const comparisonType = condition.comparisonType ?? (isStringMetric(condition.property ?? '') ? 'STRING' : 'THRESHOLD');
            const comparisonFilled = isAlertConditionComplete({
                ...condition,
                type: comparisonType,
            });
            return (
                comparisonFilled &&
                hasPositiveNumber(condition.rateThreshold) &&
                !!condition.rateOperator &&
                hasPositiveNumber(condition.duration) &&
                !!condition.timeUnit
            );
        }
        case 'STRING_COMPARE':
            return !!condition.operator && !!condition.property && !!condition.property2;
        case 'MISSING_DATA':
            return hasPositiveNumber(condition.duration) && !!condition.timeUnit;
        default:
            return true;
    }
}

export function isAlertDampeningComplete(dampening: AlertDampening | undefined): boolean {
    if (!dampening) {
        return false;
    }
    switch (dampening.mode) {
        case 'STRICT_COUNT':
            return hasPositiveNumber(dampening.trueEvaluations);
        case 'RELAXED_COUNT':
            return (
                hasPositiveNumber(dampening.trueEvaluations) &&
                hasPositiveNumber(dampening.totalEvaluations) &&
                dampening.totalEvaluations >= dampening.trueEvaluations
            );
        case 'RELAXED_TIME':
            return hasPositiveNumber(dampening.trueEvaluations) && hasPositiveNumber(dampening.duration) && !!dampening.timeUnit;
        case 'STRICT_TIME':
            return hasPositiveNumber(dampening.duration) && !!dampening.timeUnit;
        default:
            return false;
    }
}

/** Same required-field set Classic uses to set `formAlert.$invalid` (Create disabled). */
export interface AlertFormReadinessInput {
    name: string;
    isUpdate: boolean;
    ruleId: AlertRuleId | undefined;
    conditions: AlertFormCondition[];
    filters: AlertFormCondition[];
    notifications: ReadonlyArray<{ type: string }>;
    notificationsComplete: boolean;
    dampening: AlertDampening | undefined;
}

export function collectAlertFormErrors(form: AlertFormReadinessInput): Record<string, string> {
    const errs: Record<string, string> = {};
    if (!form.name.trim()) {
        errs.name = 'Name is required.';
    } else if (form.name.length < 3) {
        errs.name = 'Name has to be at least 3 characters long.';
    } else if (form.name.length > 50) {
        errs.name = 'Name length must not exceed 50 characters.';
    }
    if (!form.isUpdate && !form.ruleId) {
        errs.rule = 'Rule is required.';
    }
    if (form.notifications.some(n => !n.type)) {
        errs.notifications = 'Channel is required for each notification.';
    } else if (!form.notificationsComplete) {
        errs.notifications = 'Fill in the required fields for each notification.';
    }
    if (form.ruleId && !isInfoOnlyRule(form.ruleId) && form.conditions.some(c => !isAlertConditionComplete(c))) {
        errs.conditions = 'Fill in the required condition fields.';
    }
    if (form.filters.some(c => !isAlertConditionComplete(c))) {
        errs.filters = 'Fill in the required filter fields.';
    }
    if (!isAlertDampeningComplete(form.dampening)) {
        errs.dampening = 'Fill in the required dampening fields.';
    }
    return errs;
}

export function isAlertFormReady(form: AlertFormReadinessInput): boolean {
    return Object.keys(collectAlertFormErrors(form)).length === 0;
}
