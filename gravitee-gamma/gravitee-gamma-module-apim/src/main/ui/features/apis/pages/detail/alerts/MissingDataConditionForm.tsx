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

import { TIME_UNITS } from '../../../constants/alertConstants';
import type { AlertFormCondition, AlertTimeUnit } from '../../../types/api';

interface Props {
    condition: AlertFormCondition;
    onChange: (c: AlertFormCondition) => void;
}

export function MissingDataConditionForm({ condition, onChange }: Props) {
    return (
        <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
                <Label className="text-xs">Duration</Label>
                <Input
                    type="number"
                    min={1}
                    placeholder="e.g. 5"
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
    );
}
