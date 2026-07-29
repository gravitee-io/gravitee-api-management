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
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
    Input,
    Label,
    Skeleton,
} from '@gravitee/graphene-core';
import { CheckIcon, CopyIcon, InfoIcon, LockIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState } from 'react';

import { copyTextToClipboardWithNotifyHandler } from '../../../shared/copyToClipboard';
import { useSaveEntrypointConfigurations } from '../hooks/useSaveEntrypointConfigurations';
import type { EnvironmentEntrypointConfig } from '../types/entrypoint';
import {
    buildEntrypointPortalSettingsPayload,
    CONFIG_PORT_MAX,
    CONFIG_PORT_MIN,
    isEntrypointConfigFieldReadonly,
    isEntrypointConfigFormDirty,
    isEntrypointConfigFormValid,
    isValidConfigPort,
    KAFKA_DOMAIN_PLACEHOLDER,
    toEntrypointConfigFormValues,
    type EntrypointConfigFieldKey,
    type EntrypointConfigFormValues,
} from '../utils/entrypointConfigForm';
import { isValidKafkaDomain } from '../utils/entrypointForm';

type FormStateByEnv = Record<string, EntrypointConfigFormValues>;

function buildFormState(configs: EnvironmentEntrypointConfig[]): FormStateByEnv {
    return Object.fromEntries(configs.map(config => [config.environment.id, toEntrypointConfigFormValues(config.portalSettings)]));
}

/** Fingerprint of env ids + their server-side settings, so a refetch with unchanged data is a no-op. */
function configsFingerprint(configs: EnvironmentEntrypointConfig[]): string {
    return JSON.stringify(configs.map(config => [config.environment.id, toEntrypointConfigFormValues(config.portalSettings)]));
}

function ConfigField({
    id,
    label,
    value,
    onChange,
    readOnly,
    systemReadonly = false,
    hint,
    error,
    type = 'text',
}: Readonly<{
    id: string;
    label: string;
    value: string;
    onChange?: (value: string) => void;
    readOnly: boolean;
    systemReadonly?: boolean;
    hint?: string;
    error?: string;
    type?: 'text' | 'number';
}>) {
    return (
        <div className="space-y-2">
            <Label htmlFor={id} className="flex items-center gap-1.5">
                {systemReadonly ? <LockIcon className="size-3.5 text-muted-foreground" aria-hidden /> : null}
                {label}
            </Label>
            <div className="flex w-full gap-2">
                <Input
                    id={id}
                    type={type}
                    value={value}
                    onChange={event => onChange?.(event.target.value)}
                    readOnly={readOnly}
                    disabled={readOnly}
                    className={readOnly ? 'min-w-0 flex-1 bg-muted/40' : 'min-w-0 flex-1'}
                    aria-invalid={Boolean(error)}
                    min={type === 'number' ? CONFIG_PORT_MIN : undefined}
                    max={type === 'number' ? CONFIG_PORT_MAX : undefined}
                />
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="shrink-0"
                    onClick={() => copyTextToClipboardWithNotifyHandler(value, 'Copied to clipboard')}
                    disabled={!value}
                    aria-label={`Copy ${label}`}
                >
                    <CopyIcon className="size-4" aria-hidden />
                </Button>
            </div>
            {error ? (
                <p className="text-xs text-destructive" role="alert">
                    {error}
                </p>
            ) : null}
            {hint && !error ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
        </div>
    );
}

function fieldError(field: EntrypointConfigFieldKey, form: EntrypointConfigFormValues, showValidation: boolean): string | undefined {
    if (!showValidation) return undefined;
    if (field === 'tcpPort' && !isValidConfigPort(form.tcpPort)) {
        return `TCP port should be in range between ${CONFIG_PORT_MIN} and ${CONFIG_PORT_MAX}`;
    }
    if (field === 'kafkaPort' && !isValidConfigPort(form.kafkaPort)) {
        return `Port should be in range between ${CONFIG_PORT_MIN} and ${CONFIG_PORT_MAX}`;
    }
    if (field === 'kafkaDomain') {
        const domain = form.kafkaDomain.trim();
        if (!domain) return 'Kafka Bootstrap Domain Pattern is required.';
        if (!isValidKafkaDomain(form.kafkaDomain)) {
            if (!domain.includes(KAFKA_DOMAIN_PLACEHOLDER)) {
                return `Kafka Bootstrap Domain Pattern must contain ${KAFKA_DOMAIN_PLACEHOLDER}.`;
            }
            return 'Kafka Domain must be less than 201 characters.';
        }
    }
    return undefined;
}

function EnvironmentConfigCard({
    config,
    form,
    canEdit,
    isSaving,
    showValidation,
    onFieldChange,
}: Readonly<{
    config: EnvironmentEntrypointConfig;
    form: EntrypointConfigFormValues;
    canEdit: boolean;
    isSaving: boolean;
    showValidation: boolean;
    onFieldChange: (field: EntrypointConfigFieldKey, value: string) => void;
}>) {
    const envId = config.environment.id;
    const settings = config.portalSettings;

    function isFieldReadonly(field: EntrypointConfigFieldKey): boolean {
        return !canEdit || isSaving || isEntrypointConfigFieldReadonly(settings, field);
    }

    return (
        <div className="space-y-4 border-t pt-4 first:border-t-0 first:pt-0">
            <h4 className="flex flex-wrap items-center gap-2 text-sm font-medium">
                Default values for environment:
                <Badge variant="secondary">{config.environment.name || config.environment.id}</Badge>
            </h4>
            <div className="space-y-4">
                <ConfigField
                    id={`http-entrypoint-${envId}`}
                    label="Default HTTP entrypoint"
                    value={form.entrypoint}
                    onChange={value => onFieldChange('entrypoint', value)}
                    readOnly={isFieldReadonly('entrypoint')}
                    systemReadonly={isEntrypointConfigFieldReadonly(settings, 'entrypoint')}
                />
                <ConfigField
                    id={`tcp-port-${envId}`}
                    label="Default TCP port"
                    value={form.tcpPort}
                    onChange={value => onFieldChange('tcpPort', value)}
                    readOnly={isFieldReadonly('tcpPort')}
                    systemReadonly={isEntrypointConfigFieldReadonly(settings, 'tcpPort')}
                    type="number"
                    error={fieldError('tcpPort', form, showValidation)}
                />
                <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_12rem]">
                    <ConfigField
                        id={`kafka-domain-${envId}`}
                        label="Default Kafka Bootstrap Domain Pattern"
                        value={form.kafkaDomain}
                        onChange={value => onFieldChange('kafkaDomain', value)}
                        readOnly={isFieldReadonly('kafkaDomain')}
                        systemReadonly={isEntrypointConfigFieldReadonly(settings, 'kafkaDomain')}
                        hint="To be configured according to the gateway configuration. e.g: {apiHost}.mycompany.org"
                        error={fieldError('kafkaDomain', form, showValidation)}
                    />
                    <ConfigField
                        id={`kafka-port-${envId}`}
                        label="Default Kafka port"
                        value={form.kafkaPort}
                        onChange={value => onFieldChange('kafkaPort', value)}
                        readOnly={isFieldReadonly('kafkaPort')}
                        systemReadonly={isEntrypointConfigFieldReadonly(settings, 'kafkaPort')}
                        type="number"
                        error={fieldError('kafkaPort', form, showValidation)}
                    />
                </div>
            </div>
        </div>
    );
}

export function EntrypointConfigurationSection({
    configs,
    failedEnvironmentNames,
    isLoading,
    isError,
    canEdit,
}: Readonly<{
    configs: EnvironmentEntrypointConfig[];
    failedEnvironmentNames: string[];
    isLoading: boolean;
    isError: boolean;
    canEdit: boolean;
}>) {
    const [drafts, setDrafts] = useState<FormStateByEnv>(() => buildFormState(configs));
    const [saved, setSaved] = useState<FormStateByEnv>(() => buildFormState(configs));
    const saveMutation = useSaveEntrypointConfigurations();

    const fingerprint = configsFingerprint(configs);
    const lastFingerprintRef = useRef<string>(fingerprint);

    const dirtyEnvIds = configs
        .map(config => config.environment.id)
        .filter(envId => {
            const draft = drafts[envId];
            const savedForm = saved[envId];
            return Boolean(draft && savedForm && isEntrypointConfigFormDirty(draft, savedForm));
        });

    const isDirty = dirtyEnvIds.length > 0;
    const dirtyFormsValid = dirtyEnvIds.every(envId => {
        const draft = drafts[envId];
        return draft ? isEntrypointConfigFormValid(draft) : false;
    });
    const showFooter = canEdit && isDirty;
    const canSave = showFooter && dirtyFormsValid && !saveMutation.isPending;

    useEffect(() => {
        // Skip while the user has unsaved edits so a background refetch can't clobber them;
        // once isDirty flips back to false (discard/save) this re-checks the latest fingerprint.
        if (isDirty) return;
        if (lastFingerprintRef.current === fingerprint) return;
        lastFingerprintRef.current = fingerprint;
        const next = buildFormState(configs);
        setDrafts(next);
        setSaved(next);
    }, [fingerprint, isDirty, configs]);

    function setField(envId: string, field: EntrypointConfigFieldKey, value: string) {
        setDrafts(prev => {
            const current = prev[envId];
            if (!current) return prev;
            return { ...prev, [envId]: { ...current, [field]: value } };
        });
    }

    function handleDiscard() {
        setDrafts(saved);
    }

    async function handleSave() {
        if (!canSave) return;
        const inputs = configs
            .filter(config => dirtyEnvIds.includes(config.environment.id))
            .map(config => {
                const draft = drafts[config.environment.id]!;
                return {
                    environmentId: config.environment.id,
                    settings: buildEntrypointPortalSettingsPayload(config.portalSettings, draft),
                };
            });

        try {
            const result = await saveMutation.mutateAsync(inputs);
            if (result.succeededEnvironmentIds.length === 0) return;
            // Only mark the environments that actually saved as clean; failed ones stay dirty
            // so Discard/Save keep reflecting what really happened on the server.
            setSaved(prev => {
                const next = { ...prev };
                for (const envId of result.succeededEnvironmentIds) {
                    const draft = drafts[envId];
                    if (draft) next[envId] = draft;
                }
                return next;
            });
        } catch {
            // Failure toast is already shown by the mutation's onError handler.
        }
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle>Entrypoint Configuration</CardTitle>
                <CardDescription>Default entrypoint values to be shown in the Developer Portal</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
                {isLoading ? (
                    <div className="space-y-3">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-24 w-full rounded-md" />
                        ))}
                    </div>
                ) : isError ? (
                    <Alert variant="destructive">
                        <AlertDescription>Failed to load environments. Please refresh and try again.</AlertDescription>
                    </Alert>
                ) : (
                    <>
                        {failedEnvironmentNames.length > 0 ? (
                            <Alert>
                                <InfoIcon className="size-4" aria-hidden />
                                <AlertDescription>
                                    Could not load entrypoint defaults for: {failedEnvironmentNames.join(', ')}.
                                </AlertDescription>
                            </Alert>
                        ) : null}
                        {configs.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No environments found.</p>
                        ) : (
                            configs.map(config => {
                                const form = drafts[config.environment.id] ?? toEntrypointConfigFormValues(config.portalSettings);
                                const envDirty = dirtyEnvIds.includes(config.environment.id);
                                return (
                                    <EnvironmentConfigCard
                                        key={config.environment.id}
                                        config={config}
                                        form={form}
                                        canEdit={canEdit}
                                        isSaving={saveMutation.isPending}
                                        showValidation={envDirty}
                                        onFieldChange={(field, value) => setField(config.environment.id, field, value)}
                                    />
                                );
                            })
                        )}
                    </>
                )}
            </CardContent>
            {showFooter ? (
                <CardFooter className="justify-end gap-2 border-t">
                    <Button type="button" variant="outline" size="sm" onClick={handleDiscard} disabled={saveMutation.isPending}>
                        Discard
                    </Button>
                    <Button type="button" size="sm" onClick={handleSave} disabled={!canSave}>
                        <CheckIcon className="size-4" aria-hidden />
                        {saveMutation.isPending ? 'Saving…' : 'Save'}
                    </Button>
                </CardFooter>
            ) : null}
        </Card>
    );
}
