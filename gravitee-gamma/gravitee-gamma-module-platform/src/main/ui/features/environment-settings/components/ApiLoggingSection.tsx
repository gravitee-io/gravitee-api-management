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

import { Card, CardContent, CardHeader, CardTitle, Input, Switch } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import { SystemReadonlyHint } from '../../organization-settings/components/SystemReadonlyHint';
import { getApiLoggingFieldErrors, type ApiLoggingFieldReadonly, type ApiLoggingFormState } from '../utils/loggingValidators';

export function ApiLoggingSection({
    value,
    disabled,
    readonly = {},
    onChange,
}: Readonly<{
    value: ApiLoggingFormState;
    disabled: boolean;
    readonly?: ApiLoggingFieldReadonly;
    onChange: (next: ApiLoggingFormState) => void;
}>) {
    const errors = getApiLoggingFieldErrors(value);

    function isFieldDisabled(key: keyof ApiLoggingFieldReadonly): boolean {
        return disabled || Boolean(readonly[key]);
    }

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle>Duration</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Limit the duration of API full logging (0 means no max duration). This avoids API publishers logging headers
                        and / or body payload for too long and consuming too much CPU and memory. By default, the calls are logged
                        with the minimal information.
                    </p>
                    <Field
                        id="api-logging-max-duration"
                        label="Max Duration (in ms)"
                        type="number"
                        min={0}
                        value={value.maxDurationMillis}
                        disabled={isFieldDisabled('maxDurationMillis')}
                        systemReadonly={Boolean(readonly.maxDurationMillis)}
                        error={errors.maxDurationMillis}
                        onChange={maxDurationMillis => onChange({ ...value, maxDurationMillis })}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Audit</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Enable to audit the consultation of log details to track who accessed a specific data from the audit view.
                    </p>
                    <ToggleRow
                        id="api-logging-audit-enabled"
                        label="Enable audit on API Logging consultation"
                        checked={value.auditEnabled}
                        disabled={isFieldDisabled('auditEnabled')}
                        systemReadonly={Boolean(readonly.auditEnabled)}
                        onCheckedChange={auditEnabled => onChange({ ...value, auditEnabled })}
                    />
                    <ToggleRow
                        id="api-logging-audit-trail-enabled"
                        label="Generate API Logging audit events (API_LOGGING_ENABLED, API_LOGGING_DISABLED, API_LOGGING_UPDATED)"
                        checked={value.auditTrailEnabled}
                        disabled={isFieldDisabled('auditTrailEnabled')}
                        systemReadonly={Boolean(readonly.auditTrailEnabled)}
                        onCheckedChange={auditTrailEnabled => onChange({ ...value, auditTrailEnabled })}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>User</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Display or not the end-user in case of a OAuth2 / JWT plan (extracted from the sub claim).
                    </p>
                    <ToggleRow
                        id="api-logging-user-displayed"
                        label="Display end user on API Logging (in case of OAuth2/JWT plan)"
                        checked={value.userDisplayed}
                        disabled={isFieldDisabled('userDisplayed')}
                        systemReadonly={Boolean(readonly.userDisplayed)}
                        onCheckedChange={userDisplayed => onChange({ ...value, userDisplayed })}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Message Sampling</CardTitle>
                </CardHeader>
                <CardContent className="space-y-8">
                    <p className="text-sm text-muted-foreground">A sampling strategy is applied to prevent issues while logging messages.</p>

                    <SamplingGroup title="Probabilistic" description="Samples messages based on a specified probability.">
                        <Field
                            id="api-logging-probabilistic-default"
                            label="Default"
                            type="number"
                            min={0.01}
                            max={1}
                            step="any"
                            value={value.probabilisticDefault}
                            disabled={isFieldDisabled('probabilisticDefault')}
                            systemReadonly={Boolean(readonly.probabilisticDefault)}
                            error={errors.probabilisticDefault}
                            onChange={probabilisticDefault => onChange({ ...value, probabilisticDefault })}
                        />
                        <Field
                            id="api-logging-probabilistic-limit"
                            label="Limit"
                            type="number"
                            min={0.01}
                            max={1}
                            step="any"
                            value={value.probabilisticLimit}
                            disabled={isFieldDisabled('probabilisticLimit')}
                            systemReadonly={Boolean(readonly.probabilisticLimit)}
                            error={errors.probabilisticLimit}
                            hint="The limit is the maximum allowed probability"
                            onChange={probabilisticLimit => onChange({ ...value, probabilisticLimit })}
                        />
                    </SamplingGroup>

                    <SamplingGroup title="Count" description="Samples every nth message.">
                        <Field
                            id="api-logging-count-default"
                            label="Default"
                            type="number"
                            min={1}
                            step="1"
                            value={value.countDefault}
                            disabled={isFieldDisabled('countDefault')}
                            systemReadonly={Boolean(readonly.countDefault)}
                            error={errors.countDefault}
                            onChange={countDefault => onChange({ ...value, countDefault })}
                        />
                        <Field
                            id="api-logging-count-limit"
                            label="Limit"
                            type="number"
                            min={1}
                            step="1"
                            value={value.countLimit}
                            disabled={isFieldDisabled('countLimit')}
                            systemReadonly={Boolean(readonly.countLimit)}
                            error={errors.countLimit}
                            hint="The limit is the minimum messages number to sample"
                            onChange={countLimit => onChange({ ...value, countLimit })}
                        />
                    </SamplingGroup>

                    <SamplingGroup
                        title="Temporal"
                        description="Samples messages based on time duration. The value should conform to ISO-8601 duration format, e.g. PT1S for a duration of 1 second."
                    >
                        <Field
                            id="api-logging-temporal-default"
                            label="Default"
                            value={value.temporalDefault}
                            disabled={isFieldDisabled('temporalDefault')}
                            systemReadonly={Boolean(readonly.temporalDefault)}
                            error={errors.temporalDefault}
                            onChange={temporalDefault => onChange({ ...value, temporalDefault })}
                        />
                        <Field
                            id="api-logging-temporal-limit"
                            label="Limit"
                            value={value.temporalLimit}
                            disabled={isFieldDisabled('temporalLimit')}
                            systemReadonly={Boolean(readonly.temporalLimit)}
                            error={errors.temporalLimit}
                            hint="The limit is the minimum allowed period to sample"
                            onChange={temporalLimit => onChange({ ...value, temporalLimit })}
                        />
                    </SamplingGroup>

                    <SamplingGroup
                        title="Windowed count"
                        description="Samples a count of messages during a sliding duration window. Format is COUNT/DURATION (duration should conform to ISO-8601 duration format). Example: 2/PT15S will sample, at most, two messages during a 15-second sliding window."
                    >
                        <Field
                            id="api-logging-windowed-count-default"
                            label="Default"
                            value={value.windowedCountDefault}
                            disabled={isFieldDisabled('windowedCountDefault')}
                            systemReadonly={Boolean(readonly.windowedCountDefault)}
                            error={errors.windowedCountDefault}
                            onChange={windowedCountDefault => onChange({ ...value, windowedCountDefault })}
                        />
                        <Field
                            id="api-logging-windowed-count-limit"
                            label="Limit"
                            value={value.windowedCountLimit}
                            disabled={isFieldDisabled('windowedCountLimit')}
                            systemReadonly={Boolean(readonly.windowedCountLimit)}
                            error={errors.windowedCountLimit}
                            hint="The limit is the minimum allowed period to sample"
                            onChange={windowedCountLimit => onChange({ ...value, windowedCountLimit })}
                        />
                    </SamplingGroup>
                </CardContent>
            </Card>
        </div>
    );
}

function SamplingGroup({ title, description, children }: Readonly<{ title: string; description: string; children: ReactNode }>) {
    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h3 className="text-base font-medium">{title}</h3>
                <p className="text-sm text-muted-foreground">{description}</p>
            </div>
            {children}
        </div>
    );
}

function ToggleRow({
    id,
    label,
    checked,
    disabled,
    systemReadonly,
    onCheckedChange,
}: Readonly<{
    id: string;
    label: string;
    checked: boolean;
    disabled: boolean;
    systemReadonly: boolean;
    onCheckedChange: (checked: boolean) => void;
}>) {
    return (
        <div className="flex items-center justify-between gap-4">
            <label htmlFor={id} className="text-sm font-medium">
                {label}
            </label>
            <SystemReadonlyHint locked={systemReadonly} className="inline-flex">
                <Switch
                    id={id}
                    checked={checked}
                    onCheckedChange={next => onCheckedChange(next === true)}
                    disabled={disabled}
                    aria-label={label}
                />
            </SystemReadonlyHint>
        </div>
    );
}

function Field({
    id,
    label,
    value,
    disabled,
    systemReadonly = false,
    error,
    hint,
    onChange,
    type = 'text',
    min,
    max,
    step,
}: Readonly<{
    id: string;
    label: string;
    value: string;
    disabled: boolean;
    systemReadonly?: boolean;
    error?: string;
    hint?: string;
    onChange: (value: string) => void;
    type?: string;
    min?: number;
    max?: number;
    step?: string;
}>) {
    const errorId = `${id}-error`;
    const hintId = `${id}-hint`;
    return (
        <div className="space-y-1.5">
            <label htmlFor={id} className="text-sm font-medium">
                {label}
            </label>
            <SystemReadonlyHint locked={systemReadonly}>
                <Input
                    id={id}
                    data-testid={id}
                    type={type}
                    min={min}
                    max={max}
                    step={step}
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    disabled={disabled}
                    aria-invalid={Boolean(error)}
                    aria-describedby={error ? errorId : hint ? hintId : undefined}
                />
            </SystemReadonlyHint>
            {hint ? (
                <p id={hintId} className="text-xs text-muted-foreground">
                    {hint}
                </p>
            ) : null}
            {error ? (
                <p id={errorId} className="text-sm text-destructive" role="alert">
                    {error}
                </p>
            ) : null}
        </div>
    );
}
