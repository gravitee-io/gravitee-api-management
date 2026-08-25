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
import { AGGREGATION_FUNCTIONS, ALERT_OPERATORS, TIME_UNITS, type AlertMetricDefinition } from '../constants/alertConstants';
import type { AlertAggregationFunction, AlertFormCondition, AlertOperator, AlertTimeUnit } from '../types';
import { ALERT_POSITIVE_NUMBER_MIN, nextAlertPositiveNumber } from '../utils/alertPositiveNumber';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    projectionMetrics?: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
}

export function AggregationConditionForm({ condition, metrics, projectionMetrics = [], onChange }: Props) {
    const aggregationFunction = condition.aggregationFunction || 'AVG';
    const isCount = aggregationFunction === 'COUNT';
    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-end gap-3">
                <p className="pb-2 text-sm text-muted-foreground">Calculate</p>
                <div className="min-w-[10rem] flex-1 space-y-1.5">
                    <Label className="text-xs">Function</Label>
                    <Select
                        value={aggregationFunction}
                        onValueChange={(val: AlertAggregationFunction) => onChange({ ...condition, aggregationFunction: val })}
                    >
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {AGGREGATION_FUNCTIONS.map(f => (
                                <SelectItem key={f.value} value={f.value}>
                                    {f.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
                {!isCount && <p className="pb-2 text-sm text-muted-foreground">on</p>}
                {!isCount && (
                    <div className="min-w-[10rem] flex-1 space-y-1.5">
                        <Label className="text-xs">Metric</Label>
                        <Select
                            value={condition.property ?? metrics[0]?.key}
                            onValueChange={val => onChange({ ...condition, property: val })}
                        >
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
                )}
            </div>
            <p className="text-sm text-muted-foreground">If result is</p>
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
