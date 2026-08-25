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

import { StringValueField } from './StringValueField';
import {
    ALERT_OPERATORS,
    ALERT_STRING_OPERATORS,
    getCompareTargetMetrics,
    getConditionTypesForMetric,
    isStringMetric,
    type AlertMetricDefinition,
} from '../constants/alertConstants';
import type { AlertConditionType, AlertFormCondition, AlertOperator, AlertStringOperator } from '../types';
import { conditionWithType } from '../utils/alertConditionComplete';
import { getMetricValueChoices, sanitizePatternForOperator, type AlertMetricLookups } from '../utils/alertMetricValues';
import { ALERT_POSITIVE_NUMBER_MIN, nextAlertPositiveNumber } from '../utils/alertPositiveNumber';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
    lookups?: AlertMetricLookups;
}

export function SimpleConditionForm({ condition, metrics, onChange, lookups = {} }: Props) {
    const selectedMetric = condition.property ?? metrics[0]?.key ?? '';
    const availableTypes = getConditionTypesForMetric(selectedMetric, metrics);
    const condType: AlertConditionType =
        availableTypes.length === 0 || availableTypes.includes(condition.type) ? condition.type : (availableTypes[0] ?? condition.type);
    const metricDefinition = metrics.find(m => m.key === selectedMetric);
    const valueChoices = getMetricValueChoices(metricDefinition, lookups);

    const handleMetricChange = (val: string) => {
        const newTypes = getConditionTypesForMetric(val, metrics);
        const nextType = newTypes[0];
        if (!nextType) {
            return;
        }
        const defaultOperator = isStringMetric(val) || nextType === 'STRING' ? 'EQUALS' : 'GT';
        onChange({
            ...condition,
            property: val,
            type: nextType,
            operator: defaultOperator,
            threshold: undefined,
            thresholdLow: undefined,
            thresholdHigh: undefined,
            pattern: undefined,
            property2: undefined,
            multiplier: undefined,
        });
    };

    return (
        <div className="space-y-4">
            <p className="text-sm text-muted-foreground">When</p>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">Metric</Label>
                    <Select value={selectedMetric} onValueChange={handleMetricChange}>
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {metrics.map(m => (
                                <SelectItem key={m.key} value={m.key}>
                                    {m.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                {availableTypes.length >= 1 && (
                    <div className="space-y-1.5">
                        <Label htmlFor="alert-condition-type" className="text-xs">
                            Type
                        </Label>
                        <Select
                            value={condType}
                            onValueChange={(val: AlertConditionType) =>
                                onChange(conditionWithType(condition, val, getCompareTargetMetrics(metrics, selectedMetric)[0]?.key))
                            }
                        >
                            <SelectTrigger id="alert-condition-type">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {availableTypes.map(t => (
                                    <SelectItem key={t} value={t}>
                                        {t.replace(/_/g, ' ').toLowerCase()}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                )}
            </div>

            {condType === 'STRING' ? (
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Operator</Label>
                        <Select
                            value={(condition.operator as string) || 'EQUALS'}
                            onValueChange={(val: AlertStringOperator) =>
                                onChange({
                                    ...condition,
                                    operator: val,
                                    pattern: sanitizePatternForOperator(condition.pattern, valueChoices, val),
                                })
                            }
                        >
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {ALERT_STRING_OPERATORS.map(op => (
                                    <SelectItem key={op.value} value={op.value}>
                                        {op.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <StringValueField
                        id="alert-condition-pattern"
                        operator={condition.operator as string}
                        pattern={condition.pattern}
                        options={valueChoices}
                        onPatternChange={pattern => onChange({ ...condition, pattern })}
                    />
                </div>
            ) : condType === 'THRESHOLD_RANGE' ? (
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Low threshold</Label>
                        <Input
                            type="number"
                            min={ALERT_POSITIVE_NUMBER_MIN}
                            placeholder="e.g. 200"
                            value={condition.thresholdLow ?? ''}
                            onChange={e =>
                                onChange({
                                    ...condition,
                                    thresholdLow: nextAlertPositiveNumber(e.target.value, condition.thresholdLow),
                                })
                            }
                        />
                    </div>
                    <div className="space-y-1.5">
                        <Label className="text-xs">High threshold</Label>
                        <Input
                            type="number"
                            min={condition.thresholdLow ?? ALERT_POSITIVE_NUMBER_MIN}
                            placeholder="e.g. 500"
                            value={condition.thresholdHigh ?? ''}
                            aria-invalid={
                                typeof condition.thresholdLow === 'number' &&
                                typeof condition.thresholdHigh === 'number' &&
                                condition.thresholdHigh < condition.thresholdLow
                            }
                            onChange={e =>
                                onChange({
                                    ...condition,
                                    thresholdHigh: nextAlertPositiveNumber(e.target.value, condition.thresholdHigh),
                                })
                            }
                        />
                        {typeof condition.thresholdLow === 'number' &&
                            typeof condition.thresholdHigh === 'number' &&
                            condition.thresholdHigh < condition.thresholdLow && (
                                <p className="text-xs text-destructive">High threshold must be greater than or equal to low threshold.</p>
                            )}
                    </div>
                </div>
            ) : condType === 'COMPARE' ? (
                <div className="grid grid-cols-3 gap-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Operator</Label>
                        <Select
                            value={(condition.operator as string) || 'GT'}
                            onValueChange={(val: AlertOperator) => onChange({ ...condition, operator: val })}
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
                        <Label className="text-xs">Multiplier</Label>
                        <Input
                            type="number"
                            min={ALERT_POSITIVE_NUMBER_MIN}
                            placeholder="e.g. 150"
                            value={condition.multiplier ?? ''}
                            onChange={e =>
                                onChange({
                                    ...condition,
                                    multiplier: nextAlertPositiveNumber(e.target.value, condition.multiplier),
                                })
                            }
                        />
                    </div>
                    <div className="space-y-1.5">
                        <Label className="text-xs">Property</Label>
                        <Select value={condition.property2} onValueChange={val => onChange({ ...condition, property2: val })}>
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {getCompareTargetMetrics(metrics, selectedMetric).map(m => (
                                    <SelectItem key={m.key} value={m.key}>
                                        {m.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>
            ) : (
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Operator</Label>
                        <Select
                            value={(condition.operator as string) || 'GT'}
                            onValueChange={(val: AlertOperator) => onChange({ ...condition, operator: val })}
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
                        <Label className="text-xs">Threshold</Label>
                        <Input
                            type="number"
                            min={ALERT_POSITIVE_NUMBER_MIN}
                            placeholder="e.g. 500"
                            value={condition.threshold ?? ''}
                            onChange={e =>
                                onChange({
                                    ...condition,
                                    threshold: nextAlertPositiveNumber(e.target.value, condition.threshold),
                                })
                            }
                        />
                    </div>
                </div>
            )}
        </div>
    );
}
