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

import type { RestrictionsFormData } from '../../../../../types/plan';

type QuotaData = RestrictionsFormData['quota'];

interface QuotaFieldsProps {
    value: QuotaData;
    onChange: (v: QuotaData) => void;
    readOnly?: boolean;
}

export function QuotaFields({ value, onChange, readOnly = false }: Readonly<QuotaFieldsProps>) {
    return (
        <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr 1fr' }}>
            <div className="space-y-2">
                <Label htmlFor="quota-max">Max requests</Label>
                <Input
                    id="quota-max"
                    type="number"
                    min={1}
                    value={value.max}
                    onChange={e => onChange({ ...value, max: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
            </div>
            <div className="space-y-2">
                <Label htmlFor="quota-period">Time period</Label>
                <Input
                    id="quota-period"
                    type="number"
                    min={1}
                    value={value.period}
                    onChange={e => onChange({ ...value, period: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
            </div>
            <div className="space-y-2">
                <Label htmlFor="quota-unit">Time unit</Label>
                <Select value={value.unit} onValueChange={v => onChange({ ...value, unit: v as QuotaData['unit'] })} disabled={readOnly}>
                    <SelectTrigger id="quota-unit">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="HOURS">Hours</SelectItem>
                        <SelectItem value="DAYS">Days</SelectItem>
                        <SelectItem value="WEEKS">Weeks</SelectItem>
                        <SelectItem value="MONTHS">Months</SelectItem>
                    </SelectContent>
                </Select>
            </div>
        </div>
    );
}
