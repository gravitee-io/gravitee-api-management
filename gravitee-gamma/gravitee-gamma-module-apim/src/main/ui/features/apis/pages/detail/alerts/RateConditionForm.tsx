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
    TIME_UNITS,
    isStringMetric,
    type AlertMetricDefinition,
} from '../../../constants/alertConstants';
import type { AlertFormCondition, AlertOperator, AlertStringOperator, AlertTimeUnit } from '../../../types';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
}

export function RateConditionForm({ condition, metrics, onChange }: Props) {
    const selectedMetric = condition.property ?? metrics[0]?.key ?? '';
    const isStr = isStringMetric(selectedMetric);

    return (
        <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">Comparison metric</Label>
                    <Select
                        value={selectedMetric}
                        onValueChange={val => onChange({ ...condition, property: val, threshold: undefined, pattern: undefined })}
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
                <div className="space-y-1.5">
                    <Label className="text-xs">{isStr ? 'Pattern' : 'Threshold value'}</Label>
                    {isStr ? (
                        <Input
                            placeholder="e.g. API_KEY_MISSING"
                            value={condition.pattern ?? ''}
                            onChange={e => onChange({ ...condition, pattern: e.target.value })}
                        />
                    ) : (
                        <Input
                            type="number"
                            placeholder="e.g. 500"
                            value={condition.threshold ?? ''}
                            onChange={e => onChange({ ...condition, threshold: e.target.value ? Number(e.target.value) : undefined })}
                        />
                    )}
                </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">{isStr ? 'Operator' : 'Comparison operator'}</Label>
                    {isStr ? (
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
                    ) : (
                        <Select
                            value={(condition.operator as string) || 'GTE'}
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
                    )}
                </div>
                <div className="space-y-1.5">
                    <Label className="text-xs">Rate threshold (%)</Label>
                    <Input
                        type="number"
                        min={0}
                        max={100}
                        placeholder="e.g. 50"
                        value={condition.rateThreshold ?? ''}
                        onChange={e => onChange({ ...condition, rateThreshold: e.target.value ? Number(e.target.value) : undefined })}
                    />
                </div>
            </div>
            <div className="grid grid-cols-3 gap-4">
                <div className="space-y-1.5">
                    <Label className="text-xs">Rate operator</Label>
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
                    <Label className="text-xs">Duration</Label>
                    <Input
                        type="number"
                        min={1}
                        placeholder="e.g. 1"
                        value={condition.duration ?? ''}
                        onChange={e => onChange({ ...condition, duration: e.target.value ? Number(e.target.value) : undefined })}
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
        </div>
    );
}
