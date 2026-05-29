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
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Textarea,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { SwitchRow } from '../../../../components/CollapsibleSection';
import { CronScheduleInput } from '../../../../components/CronScheduleInput';
import {
    HTTP_HEALTH_CHECK_METHODS,
    type HealthCheckConfigFormState,
    type HealthCheckFormState,
    type HealthCheckHeaderRow,
} from '../../../../utils/healthCheckForm';

interface HealthCheckStepProps {
    mode: 'group' | 'endpoint';
    healthCheck: HealthCheckFormState;
    groupHealthCheck?: HealthCheckFormState;
    errors: Record<string, string>;
    readOnly?: boolean;
    onChange: (patch: Partial<HealthCheckFormState>) => void;
    onConfigChange: (patch: Partial<HealthCheckConfigFormState>) => void;
}

function NumInput({
    id,
    label,
    value,
    min,
    error,
    onChange,
    hint,
    disabled,
}: {
    id: string;
    label: string;
    value: number;
    min?: number;
    error?: string;
    onChange: (n: number) => void;
    hint?: string;
    disabled?: boolean;
}) {
    return (
        <div className="space-y-2">
            <Label htmlFor={id} className="text-sm">
                {label}
            </Label>
            <Input
                id={id}
                type="number"
                min={min}
                value={value}
                disabled={disabled}
                onChange={e => {
                    const n = parseInt(e.target.value, 10);
                    if (!isNaN(n)) onChange(n);
                }}
                aria-invalid={Boolean(error)}
            />
            {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
            {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
    );
}

function HeadersEditor({
    headers,
    disabled,
    onChange,
}: {
    headers: HealthCheckHeaderRow[];
    disabled?: boolean;
    onChange: (headers: HealthCheckHeaderRow[]) => void;
}) {
    return (
        <div className="space-y-2">
            <Label className="text-sm">HTTP headers</Label>
            <p className="text-xs text-muted-foreground">Headers added to the health check request.</p>
            {headers.map(h => (
                <div key={h._id} className="flex items-center gap-2">
                    <Input
                        value={h.name}
                        placeholder="Name"
                        disabled={disabled}
                        onChange={e => onChange(headers.map(row => (row._id === h._id ? { ...row, name: e.target.value } : row)))}
                        className="flex-1"
                    />
                    <Input
                        value={h.value}
                        placeholder="Value"
                        disabled={disabled}
                        onChange={e => onChange(headers.map(row => (row._id === h._id ? { ...row, value: e.target.value } : row)))}
                        className="flex-1"
                    />
                    <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        className="size-8 p-0 text-destructive hover:text-destructive shrink-0"
                        aria-label="Remove header"
                        disabled={disabled}
                        onClick={() => onChange(headers.filter(row => row._id !== h._id))}
                    >
                        <Trash2Icon className="size-3.5" aria-hidden />
                    </Button>
                </div>
            ))}
            <Button
                type="button"
                size="sm"
                variant="outline"
                className="gap-1.5"
                disabled={disabled}
                onClick={() => onChange([...headers, { _id: Math.random().toString(36).slice(2, 10), name: '', value: '' }])}
            >
                <PlusIcon className="size-3.5" aria-hidden />
                Add header
            </Button>
        </div>
    );
}

export function HealthCheckStep({
    mode,
    healthCheck,
    groupHealthCheck,
    errors,
    readOnly = false,
    onChange,
    onConfigChange,
}: Readonly<HealthCheckStepProps>) {
    const inherit = healthCheck.inherit ?? false;
    const configDisabled = readOnly || !healthCheck.enabled || inherit;
    const config = healthCheck.configuration;

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">Health-check</h2>
                <p className="text-sm text-muted-foreground">Monitor the availability and health of your endpoints and API gateways.</p>
            </div>

            {mode === 'endpoint' && groupHealthCheck && (
                <SwitchRow
                    id="hc-inherit"
                    label="Inherit configuration"
                    desc="Inherit health check service configuration from the endpoint group."
                    checked={inherit}
                    disabled={readOnly}
                    onChange={v => onChange({ inherit: v })}
                />
            )}

            {inherit && groupHealthCheck && (
                <Alert>
                    <AlertDescription>
                        Using endpoint group settings: {groupHealthCheck.enabled ? 'enabled' : 'disabled'}
                        {groupHealthCheck.enabled && groupHealthCheck.configuration.target
                            ? ` — target ${groupHealthCheck.configuration.target}`
                            : ''}
                    </AlertDescription>
                </Alert>
            )}

            <SwitchRow
                id="hc-enabled"
                label="Enabled"
                desc="This service requires an API deployment. Deploy the API to start the health-check service."
                checked={healthCheck.enabled}
                disabled={readOnly || inherit}
                onChange={v => onChange({ enabled: v })}
            />

            <Card>
                <CardContent className="pt-4 pb-3 px-4">
                    <CronScheduleInput
                        idPrefix="hc"
                        value={config.schedule}
                        error={errors.schedule}
                        disabled={configDisabled}
                        onChange={v => onConfigChange({ schedule: v })}
                    />
                </CardContent>
            </Card>

            <div className="space-y-4 rounded-lg border p-4">
                <div className="space-y-2">
                    <Label className="text-sm">Request</Label>
                    <div className="flex gap-2">
                        <Select value={config.method} onValueChange={v => onConfigChange({ method: v })} disabled={configDisabled}>
                            <SelectTrigger id="hc-method" className="w-28 shrink-0">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {HTTP_HEALTH_CHECK_METHODS.map(m => (
                                    <SelectItem key={m} value={m}>
                                        {m}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <div className="flex-1 space-y-1">
                            <Input
                                id="hc-target"
                                value={config.target}
                                disabled={configDisabled}
                                onChange={e => onConfigChange({ target: e.target.value })}
                                placeholder="/health"
                                aria-invalid={Boolean(errors.target)}
                            />
                            {errors.target && <p className="text-xs text-destructive">{errors.target}</p>}
                        </div>
                    </div>
                    <p className="text-xs text-muted-foreground">
                        Path or full URL for the probe. By default, appended to the endpoint path unless override is enabled.
                    </p>
                </div>

                <SwitchRow
                    id="hc-override-path"
                    label="Override endpoint path"
                    desc="When enabled, the path above replaces the endpoint path instead of being appended."
                    checked={config.overrideEndpointPath}
                    disabled={configDisabled}
                    onChange={v => onConfigChange({ overrideEndpointPath: v })}
                />

                <HeadersEditor headers={config.headers} disabled={configDisabled} onChange={headers => onConfigChange({ headers })} />

                <div className="space-y-2">
                    <Label htmlFor="hc-assertion" className="text-sm">
                        Assertion <span className="text-destructive">*</span>
                    </Label>
                    <Textarea
                        id="hc-assertion"
                        value={config.assertion}
                        disabled={configDisabled}
                        onChange={e => onConfigChange({ assertion: e.target.value })}
                        rows={3}
                        aria-invalid={Boolean(errors.assertion)}
                        style={{ fieldSizing: 'fixed' } as React.CSSProperties}
                    />
                    <p className="text-xs text-muted-foreground">
                        EL expression evaluated on the health check response, e.g. {'{#response.status == 200}'}.
                    </p>
                    {errors.assertion && <p className="text-xs text-destructive">{errors.assertion}</p>}
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <NumInput
                        id="hc-success-threshold"
                        label="Success threshold"
                        value={config.successThreshold}
                        min={1}
                        error={errors.successThreshold}
                        disabled={configDisabled}
                        hint="Consecutive successes before marking available."
                        onChange={v => onConfigChange({ successThreshold: v })}
                    />
                    <NumInput
                        id="hc-failure-threshold"
                        label="Failure threshold"
                        value={config.failureThreshold}
                        min={1}
                        error={errors.failureThreshold}
                        disabled={configDisabled}
                        hint="Consecutive failures before marking unavailable."
                        onChange={v => onConfigChange({ failureThreshold: v })}
                    />
                </div>
            </div>
        </div>
    );
}
