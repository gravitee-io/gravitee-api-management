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
import { Card, CardContent, CardHeader, CardTitle, Switch, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';

import type { ApiReviewSettingsKey, ApiReviewSettingsState } from '../utils/apiReviewSettings';

const SYSTEM_READONLY_TOOLTIP = 'Configuration provided by the system';

function ToggleRow({
    id,
    label,
    description,
    checked,
    disabled,
    systemReadonly,
    onToggle,
}: Readonly<{
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    disabled: boolean;
    systemReadonly: boolean;
    onToggle: (checked: boolean) => void;
}>) {
    const switchControl = <Switch id={id} checked={checked} onCheckedChange={onToggle} disabled={disabled} aria-label={label} />;

    return (
        <div className="flex items-center justify-between gap-4 py-2">
            <div className="min-w-0 space-y-0.5">
                <label htmlFor={id} className={`text-sm font-medium ${disabled ? 'cursor-default' : 'cursor-pointer'}`}>
                    {label}
                </label>
                {description ? <p className="text-xs text-muted-foreground">{description}</p> : null}
            </div>
            {systemReadonly ? (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="inline-flex" data-system-readonly="true">
                            {switchControl}
                        </span>
                    </TooltipTrigger>
                    <TooltipContent>{SYSTEM_READONLY_TOOLTIP}</TooltipContent>
                </Tooltip>
            ) : (
                switchControl
            )}
        </div>
    );
}

export function ApiReviewTogglesSection({
    value,
    disabled,
    readonly,
    onChange,
}: Readonly<{
    value: ApiReviewSettingsState;
    disabled: boolean;
    readonly: Record<ApiReviewSettingsKey, boolean>;
    onChange: (next: ApiReviewSettingsState) => void;
}>) {
    function toggle(key: ApiReviewSettingsKey, checked: boolean) {
        if (readonly[key]) return;
        onChange({ ...value, [key]: checked });
    }

    return (
        <>
            <Card>
                <CardHeader>
                    <CardTitle>API Score</CardTitle>
                </CardHeader>
                <CardContent>
                    <p className="mb-2 text-sm text-muted-foreground">
                        Ensure that your APIs match your organization&apos;s security, documentation, and design standards.
                    </p>
                    <ToggleRow
                        id="api-score-enabled"
                        label="Enable API Score"
                        checked={value.apiScoreEnabled}
                        disabled={disabled || readonly.apiScoreEnabled}
                        systemReadonly={readonly.apiScoreEnabled}
                        onToggle={checked => toggle('apiScoreEnabled', checked)}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardContent className="pt-6">
                    <ToggleRow
                        id="api-review-enabled"
                        label="Enable API Review"
                        description="When enabled, HTTP Proxy, Message, and Kafka APIs must be reviewed before they can be started or published. Authors ask from General; reviewers accept or reject from the banner on the API."
                        checked={value.apiReviewEnabled}
                        disabled={disabled || readonly.apiReviewEnabled}
                        systemReadonly={readonly.apiReviewEnabled}
                        onToggle={checked => toggle('apiReviewEnabled', checked)}
                    />
                </CardContent>
            </Card>
        </>
    );
}
