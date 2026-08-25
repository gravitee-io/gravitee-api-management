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
import type { AlertMetricDefinition, AlertMetricValueOption } from '../constants/alertConstants';

export interface AlertMetricLookups {
    tenants?: AlertMetricValueOption[];
    apis?: AlertMetricValueOption[];
}

/** Classic `Metrics.loader`: enumerated options, empty list, or undefined (Pattern). */
export function getMetricValueChoices(
    metric: AlertMetricDefinition | undefined,
    lookups: AlertMetricLookups,
): AlertMetricValueOption[] | undefined {
    if (!metric) {
        return undefined;
    }
    if (metric.valueOptions) {
        return metric.valueOptions;
    }
    if (metric.valueSource === 'tenants') {
        return lookups.tenants ?? [];
    }
    if (metric.valueSource === 'apis') {
        return lookups.apis ?? [];
    }
    if (metric.valueSource === 'empty') {
        return [];
    }
    return undefined;
}

export function shouldShowStringValueSelect(options: AlertMetricValueOption[] | undefined, operator: string | undefined): boolean {
    return options !== undefined && (operator === 'EQUALS' || operator === 'NOT_EQUALS');
}

/** Drops a free-text regex pattern left over from MATCHES when the operator switches to a Value select (EQUALS/NOT_EQUALS). */
export function sanitizePatternForOperator(
    pattern: string | undefined,
    options: AlertMetricValueOption[] | undefined,
    operator: string | undefined,
): string | undefined {
    if (shouldShowStringValueSelect(options, operator) && pattern !== undefined && !options!.some(o => o.value === pattern)) {
        return undefined;
    }
    return pattern;
}
