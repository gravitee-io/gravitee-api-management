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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import { AggregationProjectionSection } from './AggregationProjectionSection';
import { SimpleConditionForm } from './SimpleConditionForm';
import { ALERT_OPERATORS, TIME_UNITS, isStringMetric, type AlertMetricDefinition } from '../constants/alertConstants';
import type { AlertComparisonType, AlertFormCondition, AlertOperator, AlertTimeUnit } from '../types';
import type { AlertMetricLookups } from '../utils/alertMetricValues';
import { ALERT_POSITIVE_NUMBER_MIN, ALERT_RATE_PERCENT_MAX, nextAlertPositiveNumber } from '../utils/alertPositiveNumber';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    projectionMetrics?: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
    lookups?: AlertMetricLookups;
}

function isComparisonType(type: AlertFormCondition['type']): type is AlertComparisonType {
    return type === 'STRING' || type === 'THRESHOLD' || type === 'THRESHOLD_RANGE' || type === 'COMPARE';
}

export function rateComparisonFrom(rate: AlertFormCondition): AlertFormCondition {
    return {
        type: rate.comparisonType ?? (isStringMetric(rate.property ?? '') ? 'STRING' : 'THRESHOLD'),
        property: rate.property,
        operator: rate.operator,
        threshold: rate.threshold,
        thresholdLow: rate.thresholdLow,
        thresholdHigh: rate.thresholdHigh,
        pattern: rate.pattern,
        property2: rate.property2,
        multiplier: rate.multiplier,
    };
}

export function applyRateComparison(rate: AlertFormCondition, comparison: AlertFormCondition): AlertFormCondition {
    return {
        ...rate,
        type: 'RATE',
        comparisonType: isComparisonType(comparison.type) ? comparison.type : 'THRESHOLD',
        property: comparison.property,
        operator: comparison.operator,
        threshold: comparison.threshold,
        thresholdLow: comparison.thresholdLow,
        thresholdHigh: comparison.thresholdHigh,
        pattern: comparison.pattern,
        property2: comparison.property2,
        multiplier: comparison.multiplier,
    };
}

export function RateConditionForm({ condition, metrics, projectionMetrics = [], onChange, lookups }: Props) {
    return (
        <div className="space-y-4">
            <SimpleConditionForm
                condition={rateComparisonFrom(condition)}
                metrics={metrics}
                lookups={lookups}
                onChange={comparison => onChange(applyRateComparison(condition, comparison))}
            />
            <p className="text-sm text-muted-foreground">If rate is</p>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">Operator</Label>
                    <Select
                        value={(condition.rateOperator as string) || 'GT'}
                        onValueChange={(val: AlertOperator) => onChange({ ...condition, rateOperator: val })}
                    >
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {ALERT_OPERATORS.map(op => (
                                <SelectItem key={op.value} value={op.value}>
                                    {op.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
                <div className="space-y-1.5">
                    <Label className="text-xs">Threshold (%)</Label>
                    <Input
                        type="number"
                        min={ALERT_POSITIVE_NUMBER_MIN}
                        max={ALERT_RATE_PERCENT_MAX}
                        placeholder="e.g. 50"
                        value={condition.rateThreshold ?? ''}
                        onChange={e =>
                            onChange({
                                ...condition,
                                rateThreshold: nextAlertPositiveNumber(e.target.value, condition.rateThreshold, {
                                    max: ALERT_RATE_PERCENT_MAX,
                                }),
                            })
                        }
                    />
                </div>
            </div>
            <p className="text-sm text-muted-foreground">For</p>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">Duration</Label>
                    <Input
                        type="number"
                        min={ALERT_POSITIVE_NUMBER_MIN}
                        placeholder="e.g. 1"
                        value={condition.duration ?? ''}
                        onChange={e =>
                            onChange({
                                ...condition,
                                duration: nextAlertPositiveNumber(e.target.value, condition.duration),
                            })
                        }
                    />
                </div>
                <div className="space-y-1.5">
                    <Label className="text-xs">Time unit</Label>
                    <Select
                        value={condition.timeUnit || 'MINUTES'}
                        onValueChange={(val: AlertTimeUnit) => onChange({ ...condition, timeUnit: val })}
                    >
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {TIME_UNITS.map(tu => (
                                <SelectItem key={tu.value} value={tu.value}>
                                    {tu.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            </div>
            <AggregationProjectionSection condition={condition} metrics={projectionMetrics} onChange={onChange} />
        </div>
    );
}
