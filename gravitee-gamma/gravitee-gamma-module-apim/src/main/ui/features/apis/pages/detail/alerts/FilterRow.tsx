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

import {
    ALERT_OPERATORS,
    ALERT_STRING_OPERATORS,
    getConditionTypesForMetric,
    isStringMetric,
    type AlertMetricDefinition,
} from '../../../constants/alertConstants';
import type { AlertConditionType, AlertFormCondition, AlertOperator, AlertStringOperator } from '../../../types';

interface Props {
    filter: AlertFormCondition;
    index: number;
    metrics: AlertMetricDefinition[];
    onChange: (index: number, f: AlertFormCondition) => void;
    onRemove: (index: number) => void;
}

export function FilterRow({ filter, index, metrics, onChange, onRemove }: Props) {
    const selectedMetric = filter.property ?? metrics[0]?.key ?? '';
    const isStr = isStringMetric(selectedMetric);
    const availableTypes = getConditionTypesForMetric(selectedMetric, metrics);
    const condType: AlertConditionType = filter.type && availableTypes.includes(filter.type) ? filter.type : availableTypes[0];

    const handleMetricChange = (val: string) => {
        const newTypes = getConditionTypesForMetric(val, metrics);
        onChange(index, { ...filter, property: val, type: newTypes[0], operator: undefined, threshold: undefined, pattern: undefined });
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
                    {availableTypes.length > 1 && (
                        <div className="space-y-1.5">
                            <Label className="text-xs">Condition type</Label>
                            <Select value={condType} onValueChange={(val: AlertConditionType) => onChange(index, { ...filter, type: val })}>
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

                {isStr || condType === 'STRING' ? (
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <Label className="text-xs">Operator</Label>
                            <Select
                                value={(filter.operator as string) || 'EQUALS'}
                                onValueChange={(val: AlertStringOperator) => onChange(index, { ...filter, operator: val })}
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
                                placeholder="Value to match"
                                value={filter.pattern ?? ''}
                                onChange={e => onChange(index, { ...filter, pattern: e.target.value })}
                            />
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
                                placeholder="e.g. 500"
                                value={filter.threshold ?? ''}
                                onChange={e =>
                                    onChange(index, { ...filter, threshold: e.target.value ? Number(e.target.value) : undefined })
                                }
                            />
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
