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

import { Card, CardContent, CardHeader, CardTitle, Input, Separator, Switch } from '@gravitee/graphene-core';

import { SystemReadonlyHint } from '../../organization-settings/components/SystemReadonlyHint';
import type { ApiLoggingFieldReadonly } from '../utils/apiLoggingFormState';
import type { ApiLoggingFieldErrors, ApiLoggingFormState } from '../utils/apiLoggingValidators';

function ToggleRow({
    id,
    label,
    checked,
    disabled,
    systemReadonly,
    onToggle,
}: Readonly<{
    id: string;
    label: string;
    checked: boolean;
    disabled: boolean;
    systemReadonly: boolean;
    onToggle: (checked: boolean) => void;
}>) {
    const control = (
        <Switch id={id} checked={checked} onCheckedChange={next => onToggle(next === true)} disabled={disabled} aria-label={label} />
    );

    return (
        <div className="flex items-center justify-between gap-4 py-3">
            <label htmlFor={id} className={`text-sm font-medium ${disabled ? 'cursor-default' : 'cursor-pointer'}`}>
                {label}
            </label>
            <SystemReadonlyHint locked={systemReadonly} className="inline-flex">
                {control}
            </SystemReadonlyHint>
        </div>
    );
}

function Field({
    id,
    label,
    value,
    error,
    disabled,
    systemReadonly,
    onChange,
    type = 'text',
}: Readonly<{
    id: string;
    label: string;
    value: string;
    error?: string;
    disabled: boolean;
    systemReadonly: boolean;
    onChange: (value: string) => void;
    type?: 'text' | 'number';
}>) {
    const errorId = `${id}-error`;
    return (
        <div className="space-y-1.5">
            <label htmlFor={id} className="text-sm font-medium">
                {label}
            </label>
            <SystemReadonlyHint locked={systemReadonly}>
                <Input
                    id={id}
                    type={type}
                    value={value}
                    onChange={event => onChange(event.target.value)}
                    disabled={disabled}
                    aria-invalid={Boolean(error)}
                    aria-describedby={error ? errorId : undefined}
                />
            </SystemReadonlyHint>
            {error ? (
                <p id={errorId} className="text-sm text-destructive" role="alert">
                    {error}
                </p>
            ) : null}
        </div>
    );
}

export function ApiLoggingSection({
    value,
    errors,
    disabled,
    readonly,
    onChange,
}: Readonly<{
    value: ApiLoggingFormState;
    errors: ApiLoggingFieldErrors;
    disabled: boolean;
    readonly: ApiLoggingFieldReadonly;
    onChange: (next: ApiLoggingFormState) => void;
}>) {
    function updateField<K extends keyof ApiLoggingFormState>(key: K, fieldValue: ApiLoggingFormState[K]) {
        onChange({ ...value, [key]: fieldValue });
    }

    function isFieldDisabled(key: keyof ApiLoggingFieldReadonly): boolean {
        return disabled || readonly[key];
    }

    return (
        <div className="space-y-6">
            <Card>
                <CardHeader>
                    <CardTitle>Duration</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Limit the duration of API full logging (0 means no max duration). This avoids API publishers logging headers and /
                        or body payload for too long and consuming too much CPU and memory. By default, the calls are logged with the
                        minimal information.
                    </p>
                    <Field
                        id="api-logging-max-duration"
                        type="number"
                        label="Max Duration (in ms)"
                        value={value.maxDurationMillis}
                        error={errors.maxDurationMillis}
                        disabled={isFieldDisabled('maxDurationMillis')}
                        systemReadonly={readonly.maxDurationMillis}
                        onChange={next => updateField('maxDurationMillis', next)}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Audit</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <p className="text-sm text-muted-foreground">
                        Enable to audit the consultation of log details to track who accessed a specific data from the audit view.
                    </p>
                    <ToggleRow
                        id="api-logging-audit-enabled"
                        label="Enable audit on API Logging consultation"
                        checked={value.auditEnabled}
                        disabled={isFieldDisabled('auditEnabled')}
                        systemReadonly={readonly.auditEnabled}
                        onToggle={checked => updateField('auditEnabled', checked)}
                    />
                    <Separator />
                    <ToggleRow
                        id="api-logging-audit-trail-enabled"
                        label="Generate API Logging audit events (API_LOGGING_ENABLED, API_LOGGING_DISABLED, API_LOGGING_UPDATED)"
                        checked={value.auditTrailEnabled}
                        disabled={isFieldDisabled('auditTrailEnabled')}
                        systemReadonly={readonly.auditTrailEnabled}
                        onToggle={checked => updateField('auditTrailEnabled', checked)}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>User</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <p className="text-sm text-muted-foreground">
                        Display or not the end user in case of a OAuth2 / JWT plan (extracted from the sub claim).
                    </p>
                    <ToggleRow
                        id="api-logging-user-displayed"
                        label="Display end user on API Logging (in case of OAuth2/JWT plan)"
                        checked={value.userDisplayed}
                        disabled={isFieldDisabled('userDisplayed')}
                        systemReadonly={readonly.userDisplayed}
                        onToggle={checked => updateField('userDisplayed', checked)}
                    />
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Message Sampling</CardTitle>
                </CardHeader>
                <CardContent className="space-y-8">
                    <p className="text-sm text-muted-foreground">
                        A sampling strategy is applied to prevent issues while logging messages.
                    </p>

                    <div className="space-y-4">
                        <div className="space-y-1">
                            <h3 className="text-sm font-semibold">Probabilistic</h3>
                            <p className="text-sm text-muted-foreground">Samples messages based on a specified probability.</p>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2">
                            <Field
                                id="api-logging-probabilistic-default"
                                type="number"
                                label="Probabilistic default"
                                value={value.probabilisticDefault}
                                error={errors.probabilisticDefault}
                                disabled={isFieldDisabled('probabilisticDefault')}
                                systemReadonly={readonly.probabilisticDefault}
                                onChange={next => updateField('probabilisticDefault', next)}
                            />
                            <Field
                                id="api-logging-probabilistic-limit"
                                type="number"
                                label="Probabilistic limit"
                                value={value.probabilisticLimit}
                                error={errors.probabilisticLimit}
                                disabled={isFieldDisabled('probabilisticLimit')}
                                systemReadonly={readonly.probabilisticLimit}
                                onChange={next => updateField('probabilisticLimit', next)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">The limit is the maximum allowed probability</p>
                    </div>

                    <div className="space-y-4">
                        <div className="space-y-1">
                            <h3 className="text-sm font-semibold">Count</h3>
                            <p className="text-sm text-muted-foreground">Samples one message for every number of specified messages.</p>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2">
                            <Field
                                id="api-logging-count-default"
                                type="number"
                                label="Count default"
                                value={value.countDefault}
                                error={errors.countDefault}
                                disabled={isFieldDisabled('countDefault')}
                                systemReadonly={readonly.countDefault}
                                onChange={next => updateField('countDefault', next)}
                            />
                            <Field
                                id="api-logging-count-limit"
                                type="number"
                                label="Count limit"
                                value={value.countLimit}
                                error={errors.countLimit}
                                disabled={isFieldDisabled('countLimit')}
                                systemReadonly={readonly.countLimit}
                                onChange={next => updateField('countLimit', next)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">The limit is the minimum messages number to sample</p>
                    </div>

                    <div className="space-y-4">
                        <div className="space-y-1">
                            <h3 className="text-sm font-semibold">Temporal</h3>
                            <p className="text-sm text-muted-foreground">Samples messages based on time duration.</p>
                            <p className="text-sm text-muted-foreground">
                                The value should conform to ISO-8601 duration format, e.g. PT1S for a duration of 1 second.
                            </p>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2">
                            <Field
                                id="api-logging-temporal-default"
                                label="Temporal default"
                                value={value.temporalDefault}
                                error={errors.temporalDefault}
                                disabled={isFieldDisabled('temporalDefault')}
                                systemReadonly={readonly.temporalDefault}
                                onChange={next => updateField('temporalDefault', next)}
                            />
                            <Field
                                id="api-logging-temporal-limit"
                                label="Temporal limit"
                                value={value.temporalLimit}
                                error={errors.temporalLimit}
                                disabled={isFieldDisabled('temporalLimit')}
                                systemReadonly={readonly.temporalLimit}
                                onChange={next => updateField('temporalLimit', next)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">The limit is the minimum allowed period to sample</p>
                    </div>

                    <div className="space-y-4">
                        <div className="space-y-1">
                            <h3 className="text-sm font-semibold">Windowed count</h3>
                            <p className="text-sm text-muted-foreground">Samples a count of messages during a sliding duration window.</p>
                            <p className="text-sm text-muted-foreground">
                                Format is COUNT/DURATION (duration should conform to ISO-8601 duration format). Example:{' '}
                                <strong>2/PT15S</strong> will sample, at most, two messages during a 15-second sliding window.
                            </p>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2">
                            <Field
                                id="api-logging-windowed-count-default"
                                label="Windowed count default"
                                value={value.windowedCountDefault}
                                error={errors.windowedCountDefault}
                                disabled={isFieldDisabled('windowedCountDefault')}
                                systemReadonly={readonly.windowedCountDefault}
                                onChange={next => updateField('windowedCountDefault', next)}
                            />
                            <Field
                                id="api-logging-windowed-count-limit"
                                label="Windowed count limit"
                                value={value.windowedCountLimit}
                                error={errors.windowedCountLimit}
                                disabled={isFieldDisabled('windowedCountLimit')}
                                systemReadonly={readonly.windowedCountLimit}
                                onChange={next => updateField('windowedCountLimit', next)}
                            />
                        </div>
                        <p className="text-xs text-muted-foreground">The limit is the minimum allowed period to sample</p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
