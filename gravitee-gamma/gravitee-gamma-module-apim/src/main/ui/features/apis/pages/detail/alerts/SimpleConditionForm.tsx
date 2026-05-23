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

import {
    ALERT_OPERATORS,
    ALERT_STRING_OPERATORS,
    getConditionTypesForMetric,
    type AlertMetricDefinition,
} from '../../../constants/alertConstants';
import type { AlertConditionType, AlertFormCondition, AlertOperator, AlertStringOperator } from '../../../types';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
}

export function SimpleConditionForm({ condition, metrics, onChange }: Props) {
    const selectedMetric = condition.property ?? metrics[0]?.key ?? '';
    const availableTypes = getConditionTypesForMetric(selectedMetric, metrics);
    const condType: AlertConditionType = condition.type && availableTypes.includes(condition.type) ? condition.type : availableTypes[0];

    const handleMetricChange = (val: string) => {
        const newTypes = getConditionTypesForMetric(val, metrics);
        onChange({ ...condition, property: val, type: newTypes[0], operator: undefined, threshold: undefined, pattern: undefined });
    };

    return (
        <div className="space-y-4">
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

                {availableTypes.length > 1 && (
                    <div className="space-y-1.5">
                        <Label className="text-xs">Condition type</Label>
                        <Select value={condType} onValueChange={(val: AlertConditionType) => onChange({ ...condition, type: val })}>
                            <SelectTrigger>
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
                            onValueChange={(val: AlertStringOperator) => onChange({ ...condition, operator: val })}
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
                    <div className="space-y-1.5">
                        <Label className="text-xs">Pattern</Label>
                        <Input
                            placeholder="e.g. API_KEY_MISSING"
                            value={condition.pattern ?? ''}
                            onChange={e => onChange({ ...condition, pattern: e.target.value })}
                        />
                    </div>
                </div>
            ) : condType === 'THRESHOLD_RANGE' ? (
                <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                        <Label className="text-xs">Low threshold</Label>
                        <Input
                            type="number"
                            placeholder="e.g. 200"
                            value={condition.thresholdLow ?? ''}
                            onChange={e => onChange({ ...condition, thresholdLow: e.target.value ? Number(e.target.value) : undefined })}
                        />
                    </div>
                    <div className="space-y-1.5">
                        <Label className="text-xs">High threshold</Label>
                        <Input
                            type="number"
                            placeholder="e.g. 500"
                            value={condition.thresholdHigh ?? ''}
                            onChange={e => onChange({ ...condition, thresholdHigh: e.target.value ? Number(e.target.value) : undefined })}
                        />
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
                        <Label className="text-xs">Multiplier (%)</Label>
                        <Input
                            type="number"
                            placeholder="e.g. 150"
                            value={condition.multiplier ?? ''}
                            onChange={e => onChange({ ...condition, multiplier: e.target.value ? Number(e.target.value) : undefined })}
                        />
                    </div>
                    <div className="space-y-1.5">
                        <Label className="text-xs">Property to compare</Label>
                        <Select
                            value={condition.property2 || metrics[0]?.key}
                            onValueChange={val => onChange({ ...condition, property2: val })}
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
                            placeholder="e.g. 500"
                            value={condition.threshold ?? ''}
                            onChange={e => onChange({ ...condition, threshold: e.target.value ? Number(e.target.value) : undefined })}
                        />
                    </div>
                </div>
            )}
        </div>
    );
}
