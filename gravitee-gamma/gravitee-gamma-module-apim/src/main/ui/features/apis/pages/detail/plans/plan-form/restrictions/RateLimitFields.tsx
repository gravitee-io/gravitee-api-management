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

import type { RateLimitFormData } from '../../../../../types/plan';

interface RateLimitFieldsProps {
    value: RateLimitFormData;
    onChange: (v: RateLimitFormData) => void;
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

export function RateLimitFields({ value, onChange, readOnly = false }: Readonly<RateLimitFieldsProps>) {
    return (
        <div className="space-y-4">
            {/* Error strategy */}
            <div className="space-y-2">
                <Label htmlFor="rl-error-strategy">Error strategy</Label>
                <Select
                    value={value.errorStrategy}
                    onValueChange={v => onChange({ ...value, errorStrategy: v as RateLimitFormData['errorStrategy'] })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="rl-error-strategy" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="FALLBACK_PASS_TROUGH">Accept queries in case of internal failure (default behavior)</SelectItem>
                        <SelectItem value="BLOCK_ON_INTERNAL_ERROR">Block queries in case of internal failure</SelectItem>
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">Strategy applied to handle rate-limiting internal errors.</p>
            </div>

            {/* Toggles */}
            <SwitchRow
                id="rl-async"
                label="Non-strict mode (async)"
                description="By activating this option, rate-limiting is applied in an asynchronous way meaning that the distributed counter value is not strict (backend can receive more queries than configured)."
                checked={value.async}
                onChange={checked => onChange({ ...value, async: checked })}
                disabled={readOnly}
            />
            <SwitchRow
                id="rl-add-headers"
                label="Add response headers"
                description="Add X-Rate-Limit-Limit, X-Rate-Limit-Remaining and X-Rate-Limit-Reset headers in HTTP response."
                checked={value.addHeaders}
                onChange={checked => onChange({ ...value, addHeaders: checked })}
                disabled={readOnly}
            />

            <p className="text-sm font-semibold">Apply rate-limiting</p>

            {/* Key */}
            <div className="space-y-2">
                <Label htmlFor="rl-key">Key</Label>
                <Input
                    id="rl-key"
                    value={value.key}
                    onChange={e => onChange({ ...value, key: e.target.value })}
                    placeholder="{#request.headers['X-Consumer-Id'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Key to identify a consumer against whom the rate-limiting will be applied. Leave it empty to use the default behavior
                    (plan/subscription pair). Supports EL.
                </p>
            </div>

            <SwitchRow
                id="rl-use-key-only"
                label="Use key only"
                description="Only uses the custom key to identify the consumer, regardless of the subscription and plan."
                checked={value.useKeyOnly}
                onChange={checked => onChange({ ...value, useKeyOnly: checked })}
                disabled={readOnly || !value.key}
            />

            {/* Max requests (static) */}
            <div className="space-y-2">
                <Label htmlFor="rl-max">Max requests (static)</Label>
                <Input
                    id="rl-max"
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
                <Label htmlFor="rl-dynamic-limit">Max requests (dynamic)</Label>
                <Input
                    id="rl-dynamic-limit"
                    value={value.dynamicLimit}
                    onChange={e => onChange({ ...value, dynamicLimit: e.target.value })}
                    placeholder="{#request.headers['X-Rate-Limit'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    Dynamic limit on the number of requests that can be sent (this limit is used if static limit = 0). The dynamic value is
                    based on EL expressions.
                </p>
            </div>

            {/* Static time duration */}
            <div className="space-y-2">
                <Label htmlFor="rl-period">Static time duration</Label>
                <Input
                    id="rl-period"
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
                <Label htmlFor="rl-unit">Static time unit</Label>
                <Select
                    value={value.unit}
                    onValueChange={v => onChange({ ...value, unit: v as RateLimitFormData['unit'] })}
                    disabled={readOnly}
                >
                    <SelectTrigger id="rl-unit" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="SECONDS">SECONDS</SelectItem>
                        <SelectItem value="MINUTES">MINUTES</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {/* Dynamic time duration */}
            <div className="space-y-2">
                <Label htmlFor="rl-dynamic-period">Dynamic time duration (Supports EL)</Label>
                <Input
                    id="rl-dynamic-period"
                    value={value.dynamicPeriodTime}
                    onChange={e => onChange({ ...value, dynamicPeriodTime: e.target.value })}
                    placeholder="{#request.headers['X-Rate-Limit-Duration'][0]}"
                    disabled={readOnly}
                />
                <p className="text-xs text-muted-foreground">
                    If provided, expression takes priority over static time duration, sets time unit to SECONDS and fallback value as 1 when
                    left blank.
                </p>
            </div>
        </div>
    );
}
