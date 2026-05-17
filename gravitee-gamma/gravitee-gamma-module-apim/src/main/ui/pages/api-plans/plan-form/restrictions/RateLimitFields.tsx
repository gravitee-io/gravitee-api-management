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

import type { RestrictionsFormData } from '../../../../features/apis/types/plan';

type RateLimitData = RestrictionsFormData['rateLimit'];

interface RateLimitFieldsProps {
    value: RateLimitData;
    onChange: (v: RateLimitData) => void;
    readOnly?: boolean;
}

export function RateLimitFields({ value, onChange, readOnly = false }: Readonly<RateLimitFieldsProps>) {
    return (
        <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr 1fr' }}>
            <div className="space-y-2">
                <Label htmlFor="rl-max">Max requests</Label>
                <Input
                    id="rl-max"
                    type="number"
                    min={1}
                    value={value.max}
                    onChange={e => onChange({ ...value, max: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
            </div>
            <div className="space-y-2">
                <Label htmlFor="rl-period">Time period</Label>
                <Input
                    id="rl-period"
                    type="number"
                    min={1}
                    value={value.period}
                    onChange={e => onChange({ ...value, period: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
            </div>
            <div className="space-y-2">
                <Label htmlFor="rl-unit">Time unit</Label>
                <Select
                    value={value.unit}
                    onValueChange={v => onChange({ ...value, unit: v as RateLimitData['unit'] })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="rl-unit">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="SECONDS">Seconds</SelectItem>
                        <SelectItem value="MINUTES">Minutes</SelectItem>
                    </SelectContent>
                </Select>
            </div>
        </div>
    );
}
