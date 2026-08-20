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
import { isStringMetric } from '../constants/alertConstants';
import type { AlertConditionType, AlertDampening, AlertFormCondition } from '../types';

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
            return !!condition.operator && hasPositiveNumber(condition.threshold) && hasPositiveNumber(condition.duration);
        case 'RATE': {
            const comparisonFilled = isStringMetric(condition.property ?? '')
                ? !!condition.pattern?.trim()
                : hasPositiveNumber(condition.threshold);
            return (
                comparisonFilled &&
                !!condition.operator &&
                hasPositiveNumber(condition.rateThreshold) &&
                !!condition.rateOperator &&
                hasPositiveNumber(condition.duration)
            );
        }
        case 'MISSING_DATA':
            return hasPositiveNumber(condition.duration);
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
            return hasPositiveNumber(dampening.trueEvaluations) && hasPositiveNumber(dampening.totalEvaluations);
        case 'RELAXED_TIME':
            return hasPositiveNumber(dampening.trueEvaluations) && hasPositiveNumber(dampening.duration) && !!dampening.timeUnit;
        case 'STRICT_TIME':
            return hasPositiveNumber(dampening.duration) && !!dampening.timeUnit;
        default:
            return false;
    }
}
