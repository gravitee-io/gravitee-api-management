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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Switch } from '@gravitee/graphene-core';

import type { QuotaFormData } from '../../../../../types/plan';

interface QuotaFieldsProps {
    value: QuotaFormData;
    onChange: (v: QuotaFormData) => void;
    readOnly?: boolean;
}

function SwitchRow({
    id,
    label,
    description,
    checked,
    onChange,
    disabled,
}: {
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    onChange: (v: boolean) => void;
    disabled?: boolean;
}) {
    return (
        <div className="flex items-center justify-between rounded-lg border px-4 py-3">
            <div className="space-y-0.5">
                <Label htmlFor={id} className="text-sm font-medium">
                    {label}
                </Label>
                {description && <p className="text-xs text-muted-foreground">{description}</p>}
            </div>
            <Switch id={id} checked={checked} onCheckedChange={onChange} disabled={disabled} />
        </div>
    );
}

export function QuotaFields({ value, onChange, readOnly = false }: Readonly<QuotaFieldsProps>) {
    return (
        <div className="space-y-4">
            {/* Error strategy */}
            <div className="space-y-2">
                <Label htmlFor="quota-error-strategy">Error strategy</Label>
                <Select
                    value={value.errorStrategy}
                    onValueChange={v => onChange({ ...value, errorStrategy: v as QuotaFormData['errorStrategy'] })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="quota-error-strategy" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="FALLBACK_PASS_TROUGH">
                            Accept queries in case of internal failure (previous default behavior)
                        </SelectItem>
                        <SelectItem value="BLOCK_ON_INTERNAL_ERROR">Block queries in case of internal failure</SelectItem>
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">Strategy applied to handle rate-limiting internal errors.</p>
            </div>

            {/* Toggles */}
            <SwitchRow
                id="quota-async"
                label="Non-strict mode (async)"
                description="By activating this option, quota is applied in an asynchronous way meaning that the distributed counter value is not strict (backend can receive more queries than configured)."
                checked={value.async}
                onChange={checked => onChange({ ...value, async: checked })}
                disabled={readOnly}
            />
            <SwitchRow
                id="quota-add-headers"
                label="Add response headers"
                description="Add X-Quota-Limit, X-Quota-Remaining and X-Quota-Reset headers in HTTP response."
                checked={value.addHeaders}
                onChange={checked => onChange({ ...value, addHeaders: checked })}
                disabled={readOnly}
            />

            <p className="text-sm font-semibold">Apply quota</p>

            {/* Key */}
            <div className="space-y-2">
                <Label htmlFor="quota-key">Key</Label>
                <Input
                    id="quota-key"
                    value={value.key}
                    onChange={e => onChange({ ...value, key: e.target.value })}
                    placeholder="{#request.headers['X-Consumer-Id'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Key to identify a consumer against whom the quota will be applied. Leave it empty to use the default behavior
                    (plan/subscription pair). Supports EL.
                </p>
            </div>

            <SwitchRow
                id="quota-use-key-only"
                label="Use key only"
                description="Only uses the custom key to identify the consumer, regardless of the subscription and plan."
                checked={value.useKeyOnly}
                onChange={checked => onChange({ ...value, useKeyOnly: checked })}
                disabled={readOnly || !value.key}
            />

            {/* Max requests (static) */}
            <div className="space-y-2">
                <Label htmlFor="quota-max">Max requests (static)</Label>
                <Input
                    id="quota-max"
                    type="number"
                    min={0}
                    value={value.max}
                    onChange={e => onChange({ ...value, max: Math.max(0, Number(e.target.value)) })}
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Static limit on the number of requests that can be sent (this limit is used if the value &gt; 0).
                </p>
            </div>

            {/* Max requests (dynamic) */}
            <div className="space-y-2">
                <Label htmlFor="quota-dynamic-limit">Max requests (dynamic)</Label>
                <Input
                    id="quota-dynamic-limit"
                    value={value.dynamicLimit}
                    onChange={e => onChange({ ...value, dynamicLimit: e.target.value })}
                    placeholder="{#request.headers['X-Quota-Limit'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Dynamic limit on the number of requests that can be sent (this limit is used if static limit = 0). The dynamic value is
                    based on EL expressions.
                </p>
            </div>

            {/* Static time duration */}
            <div className="space-y-2">
                <Label htmlFor="quota-period">Static time duration</Label>
                <Input
                    id="quota-period"
                    type="number"
                    min={1}
                    value={value.period}
                    onChange={e => onChange({ ...value, period: Math.max(1, Number(e.target.value)) })}
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    A numeric positive value default set to 1, ignored when a dynamic expression used.
                </p>
            </div>

            {/* Static time unit */}
            <div className="space-y-2">
                <Label htmlFor="quota-unit">Static time unit</Label>
                <Select
                    value={value.unit}
                    onValueChange={v => onChange({ ...value, unit: v as QuotaFormData['unit'] })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="quota-unit" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="HOURS">HOURS</SelectItem>
                        <SelectItem value="DAYS">DAYS</SelectItem>
                        <SelectItem value="WEEKS">WEEKS</SelectItem>
                        <SelectItem value="MONTHS">MONTHS</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {/* Dynamic time duration */}
            <div className="space-y-2">
                <Label htmlFor="quota-dynamic-period">Dynamic time duration (Supports EL)</Label>
                <Input
                    id="quota-dynamic-period"
                    value={value.dynamicPeriodTime}
                    onChange={e => onChange({ ...value, dynamicPeriodTime: e.target.value })}
                    placeholder="{#request.headers['X-Quota-Duration'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    If provided, expression takes priority over static time duration, sets time unit to HOURS and fallback value as 1 when
                    left blank.
                </p>
            </div>
        </div>
    );
}
