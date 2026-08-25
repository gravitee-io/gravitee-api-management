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
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Input,
} from '@gravitee/graphene-core';
import { Trash2Icon } from '@gravitee/graphene-core/icons';

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
    filter: AlertFormCondition;
    index: number;
    metrics: AlertMetricDefinition[];
    onChange: (index: number, f: AlertFormCondition) => void;
    onRemove: (index: number) => void;
    lookups?: AlertMetricLookups;
}

export function FilterRow({ filter, index, metrics, onChange, onRemove, lookups = {} }: Props) {
    const selectedMetric = filter.property ?? metrics[0]?.key ?? '';
    const isStr = isStringMetric(selectedMetric);
    const availableTypes = getConditionTypesForMetric(selectedMetric, metrics);
    const condType: AlertConditionType =
        availableTypes.length === 0 || availableTypes.includes(filter.type) ? filter.type : (availableTypes[0] ?? filter.type);
    const metricDefinition = metrics.find(m => m.key === selectedMetric);
    const valueChoices = getMetricValueChoices(metricDefinition, lookups);

    const handleMetricChange = (val: string) => {
        const newTypes = getConditionTypesForMetric(val, metrics);
        const nextType = newTypes[0];
        if (!nextType) {
            return;
        }
        const defaultOperator = isStringMetric(val) || nextType === 'STRING' ? 'EQUALS' : 'GT';
        onChange(index, {
            ...filter,
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
        <Card>
            <CardHeader className="py-3 px-4">
                <div className="flex items-center justify-between">
                    <CardTitle className="text-sm">Filter {index + 1}</CardTitle>
                    <Button variant="ghost" size="icon" className="size-7" onClick={() => onRemove(index)}>
                        <Trash2Icon className="size-3.5 text-muted-foreground" />
                    </Button>
                </div>
            </CardHeader>
            <CardContent className="px-4 pb-4 pt-0 space-y-3">
                <div className="grid grid-cols-2 gap-3">
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
                            <Label htmlFor={`alert-filter-type-${index}`} className="text-xs">
                                Type
                            </Label>
                            <Select
                                value={condType}
                                onValueChange={(val: AlertConditionType) =>
                                    onChange(
                                        index,
                                        conditionWithType(filter, val, getCompareTargetMetrics(metrics, selectedMetric)[0]?.key),
                                    )
                                }
                            >
                                <SelectTrigger id={`alert-filter-type-${index}`}>
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

                {isStr || condType === 'STRING' ? (
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-xs">Operator</Label>
                            <Select
                                value={(filter.operator as string) || 'EQUALS'}
                                onValueChange={(val: AlertStringOperator) =>
                                    onChange(index, {
                                        ...filter,
                                        operator: val,
                                        pattern: sanitizePatternForOperator(filter.pattern, valueChoices, val),
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
                            id={`alert-filter-pattern-${index}`}
                            operator={filter.operator as string}
                            pattern={filter.pattern}
                            options={valueChoices}
                            onPatternChange={pattern => onChange(index, { ...filter, pattern })}
                            patternPlaceholder="Value to match"
                        />
                    </div>
                ) : condType === 'THRESHOLD_RANGE' ? (
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-xs">Low threshold</Label>
                            <Input
                                type="number"
                                min={ALERT_POSITIVE_NUMBER_MIN}
                                placeholder="e.g. 200"
                                value={filter.thresholdLow ?? ''}
                                onChange={e =>
                                    onChange(index, {
                                        ...filter,
                                        type: 'THRESHOLD_RANGE',
                                        thresholdLow: nextAlertPositiveNumber(e.target.value, filter.thresholdLow),
                                    })
                                }
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-xs">High threshold</Label>
                            <Input
                                type="number"
                                min={filter.thresholdLow ?? ALERT_POSITIVE_NUMBER_MIN}
                                placeholder="e.g. 500"
                                value={filter.thresholdHigh ?? ''}
                                aria-invalid={
                                    typeof filter.thresholdLow === 'number' &&
                                    typeof filter.thresholdHigh === 'number' &&
                                    filter.thresholdHigh < filter.thresholdLow
                                }
                                onChange={e =>
                                    onChange(index, {
                                        ...filter,
                                        type: 'THRESHOLD_RANGE',
                                        thresholdHigh: nextAlertPositiveNumber(e.target.value, filter.thresholdHigh),
                                    })
                                }
                            />
                            {typeof filter.thresholdLow === 'number' &&
                                typeof filter.thresholdHigh === 'number' &&
                                filter.thresholdHigh < filter.thresholdLow && (
                                    <p className="text-xs text-destructive">
                                        High threshold must be greater than or equal to low threshold.
                                    </p>
                                )}
                        </div>
                    </div>
                ) : condType === 'COMPARE' ? (
                    <div className="grid grid-cols-3 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-xs">Operator</Label>
                            <Select
                                value={(filter.operator as string) || 'GT'}
                                onValueChange={(val: AlertOperator) => onChange(index, { ...filter, operator: val })}
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
                                value={filter.multiplier ?? ''}
                                onChange={e =>
                                    onChange(index, {
                                        ...filter,
                                        multiplier: nextAlertPositiveNumber(e.target.value, filter.multiplier),
                                    })
                                }
                            />
                        </div>
                        <div className="space-y-1.5">
                            <Label className="text-xs">Property</Label>
                            <Select value={filter.property2} onValueChange={val => onChange(index, { ...filter, property2: val })}>
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
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-xs">Operator</Label>
                            <Select
                                value={(filter.operator as string) || 'GT'}
                                onValueChange={(val: AlertOperator) => onChange(index, { ...filter, operator: val })}
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
                                value={filter.threshold ?? ''}
                                onChange={e =>
                                    onChange(index, {
                                        ...filter,
                                        threshold: nextAlertPositiveNumber(e.target.value, filter.threshold),
                                    })
                                }
                            />
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
