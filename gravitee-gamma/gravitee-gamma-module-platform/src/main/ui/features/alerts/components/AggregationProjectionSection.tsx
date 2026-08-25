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
import { Button, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { AlertMetricDefinition } from '../constants/alertConstants';
import type { AlertFormCondition } from '../types';
import { projectionPropertyKey, propertyProjection } from '../utils/alertProjection';

interface Props {
    condition: AlertFormCondition;
    metrics: AlertMetricDefinition[];
    onChange: (c: AlertFormCondition) => void;
}

export function AggregationProjectionSection({ condition, metrics, onChange }: Props) {
    if (metrics.length === 0) {
        return null;
    }

    const firstMetric = metrics[0];
    if (!firstMetric) {
        return null;
    }

    const selectedKey = projectionPropertyKey(condition.projections);
    const hasProjection = !!selectedKey;

    return (
        <div className="space-y-3 border-t pt-4">
            <div>
                <p className="text-sm font-medium">Aggregation</p>
                <p className="text-xs text-muted-foreground">Aggregation allow you to group results based on a property</p>
            </div>
            {hasProjection ? (
                <div className="flex items-end gap-3">
                    <div className="min-w-0 flex-1 space-y-1.5">
                        <Label className="text-xs">Property</Label>
                        <Select
                            value={selectedKey}
                            onValueChange={val => onChange({ ...condition, projections: [propertyProjection(val)] })}
                        >
                            <SelectTrigger aria-label="Aggregation property">
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
                    <Button type="button" variant="ghost" size="sm" onClick={() => onChange({ ...condition, projections: [] })}>
                        Remove
                    </Button>
                </div>
            ) : (
                <Button
                    type="button"
                    variant="link"
                    className="h-auto px-0"
                    onClick={() => onChange({ ...condition, projections: [propertyProjection(firstMetric.key)] })}
                >
                    Set a projection
                </Button>
            )}
        </div>
    );
}
