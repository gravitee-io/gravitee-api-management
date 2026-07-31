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
import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Button,
    Skeleton,
    Switch,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { CheckIcon, InfoIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { usePortalSettings } from './hooks/usePortalSettings';
import { useSavePortalSettings } from './hooks/useSavePortalSettings';
import type { PortalSettings } from './services/portalSettings';
import type { SecurityState } from './utils/buildPortalSettingsSavePayload';
import { buildPortalSettingsSavePayload } from './utils/buildPortalSettingsSavePayload';
import { applyReadonlySecurityState, getSecurityReadonlyState, type SecurityKey } from './utils/securityPlanReadonly';

const SYSTEM_READONLY_TOOLTIP = 'Configuration provided by the system';

function buildState(settings: PortalSettings | undefined): SecurityState {
    const security = settings?.plan?.security;
    return {
        keyless: security?.keyless?.enabled ?? true,
        apikey: security?.apikey?.enabled ?? true,
        customApiKey: security?.customApiKey?.enabled ?? true,
        customApiKeyReuse: security?.customApiKeyReuse?.enabled ?? true,
        sharedApiKey: security?.sharedApiKey?.enabled ?? true,
        oauth2: security?.oauth2?.enabled ?? true,
        jwt: security?.jwt?.enabled ?? true,
        push: security?.push?.enabled ?? true,
        mtls: security?.mtls?.enabled ?? true,
    };
}

interface ToggleRowProps {
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    disabled: boolean;
    systemReadonly?: boolean;
    indented?: boolean;
    onToggle: (checked: boolean) => void;
}

function ToggleRow({ id, label, description, checked, disabled, systemReadonly = false, indented, onToggle }: ToggleRowProps) {
    const switchControl = <Switch id={id} checked={checked} onCheckedChange={onToggle} disabled={disabled} aria-label={label} />;

    return (
        <div className={`flex items-center justify-between border-b py-4 ${indented ? 'pl-8' : ''}`}>
            <div className="space-y-0.5">
                <label htmlFor={id} className={`text-sm font-medium ${disabled ? 'cursor-default' : 'cursor-pointer'}`}>
                    {label}
                </label>
                {description && <p className="text-xs text-muted-foreground">{description}</p>}
            </div>
            <div className="flex items-center gap-2">
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
                <span className="w-16 text-xs text-muted-foreground">{checked ? 'Enabled' : 'Disabled'}</span>
            </div>
        </div>
    );
}

export function SecurityPlanTypesPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const env = useEnvironment();

    const { data: settings, isLoading, isError } = usePortalSettings();

    const readonlyState = useMemo(() => getSecurityReadonlyState(settings), [settings]);

    const [localState, setLocalState] = useState<SecurityState>(() => buildState(settings));
    const [savedState, setSavedState] = useState<SecurityState>(() => buildState(settings));
    const initializedForEnvId = useRef<string | null>(null);

    useEffect(() => {
        if (settings && env?.id && initializedForEnvId.current !== env.id) {
            const state = buildState(settings);
            setLocalState(state);
            setSavedState(state);
            initializedForEnvId.current = env.id;
        }
    }, [settings, env?.id]);

    const isDirty = useMemo(
        () =>
            (Object.keys(savedState) as SecurityKey[]).filter(key => !readonlyState[key]).some(key => localState[key] !== savedState[key]),
        [localState, savedState, readonlyState],
    );

    const handleToggle = useCallback(
        (key: SecurityKey, checked: boolean) => {
            if (!canEdit || readonlyState[key]) return;
            setLocalState(prev => {
                const next = { ...prev, [key]: checked };

                if (key === 'apikey' && !checked) {
                    next.customApiKey = false;
                    next.customApiKeyReuse = false;
                    next.sharedApiKey = false;
                }
                if (key === 'customApiKey' && !checked) {
                    next.customApiKeyReuse = false;
                }

                return next;
            });
        },
        [canEdit, readonlyState],
    );

    const saveMutation = useSavePortalSettings();

    const handleSave = () => {
        if (!isDirty || saveMutation.isPending || !settings) return;
        const stateAtSaveTime = applyReadonlySecurityState(localState, savedState, readonlyState);
        saveMutation.mutate(buildPortalSettingsSavePayload(settings, stateAtSaveTime), {
            onSuccess: () => setSavedState(stateAtSaveTime),
        });
    };

    const handleDiscard = () => {
        setLocalState(savedState);
    };

    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full rounded-lg" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="space-y-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Security Plan Types</h1>
                    <p className="text-sm text-muted-foreground">
                        Configure which security plan types are available for APIs across the environment.
                    </p>
                </div>
                <div className="flex items-center justify-center rounded-lg border p-8">
                    <p className="text-sm text-muted-foreground">Failed to load settings. Please refresh and try again.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Security Plan Types</h1>
                    <p className="text-sm text-muted-foreground">
                        Configure which security plan types are available for APIs across the environment.
                    </p>
                </div>
                {isDirty && canEdit && (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button variant="outline" size="sm" onClick={handleDiscard} disabled={saveMutation.isPending}>
                            Discard
                        </Button>
                        <Button size="sm" onClick={handleSave} disabled={saveMutation.isPending}>
                            <CheckIcon className="size-4" aria-hidden />
                            {saveMutation.isPending ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                )}
            </div>

            {!canEdit && (
                <Alert>
                    <InfoIcon className="size-4" />
                    <AlertDescription>
                        You do not have permission to modify these settings. Contact your administrator for access.
                    </AlertDescription>
                </Alert>
            )}

            <div className="rounded-lg border">
                <div className="flex items-center gap-2 border-b px-4 py-3">
                    <ShieldCheckIcon className="size-5 text-muted-foreground" aria-hidden />
                    <h2 className="text-base font-semibold">Security plan types available</h2>
                </div>

                <div className="px-4">
                    <TooltipProvider delayDuration={200}>
                        <ToggleRow
                            id="plan-keyless"
                            label="Keyless plans"
                            checked={localState.keyless}
                            disabled={!canEdit || readonlyState.keyless}
                            systemReadonly={readonlyState.keyless}
                            onToggle={checked => handleToggle('keyless', checked)}
                        />

                        <ToggleRow
                            id="plan-apikey"
                            label="API Key plans"
                            checked={localState.apikey}
                            disabled={!canEdit || readonlyState.apikey}
                            systemReadonly={readonlyState.apikey}
                            onToggle={checked => handleToggle('apikey', checked)}
                        />
                        <ToggleRow
                            id="plan-custom-apikey"
                            label="Allow custom API Key"
                            checked={localState.customApiKey}
                            disabled={!canEdit || readonlyState.customApiKey || !localState.apikey}
                            systemReadonly={readonlyState.customApiKey}
                            indented
                            onToggle={checked => handleToggle('customApiKey', checked)}
                        />
                        <ToggleRow
                            id="plan-custom-apikey-reuse"
                            label="Allow custom API Key reuse"
                            checked={localState.customApiKeyReuse}
                            disabled={!canEdit || readonlyState.customApiKeyReuse || !localState.apikey || !localState.customApiKey}
                            systemReadonly={readonlyState.customApiKeyReuse}
                            indented
                            onToggle={checked => handleToggle('customApiKeyReuse', checked)}
                        />
                        <ToggleRow
                            id="plan-shared-apikey"
                            label="Allow to share API Key on an application"
                            checked={localState.sharedApiKey}
                            disabled={!canEdit || readonlyState.sharedApiKey || !localState.apikey}
                            systemReadonly={readonlyState.sharedApiKey}
                            indented
                            onToggle={checked => handleToggle('sharedApiKey', checked)}
                        />

                        <ToggleRow
                            id="plan-oauth2"
                            label="OAuth2 plans"
                            checked={localState.oauth2}
                            disabled={!canEdit || readonlyState.oauth2}
                            systemReadonly={readonlyState.oauth2}
                            onToggle={checked => handleToggle('oauth2', checked)}
                        />

                        <ToggleRow
                            id="plan-jwt"
                            label="JWT plans"
                            checked={localState.jwt}
                            disabled={!canEdit || readonlyState.jwt}
                            systemReadonly={readonlyState.jwt}
                            onToggle={checked => handleToggle('jwt', checked)}
                        />

                        <ToggleRow
                            id="plan-push"
                            label="Push plans"
                            description="Only available for API V4"
                            checked={localState.push}
                            disabled={!canEdit || readonlyState.push}
                            systemReadonly={readonlyState.push}
                            onToggle={checked => handleToggle('push', checked)}
                        />

                        <ToggleRow
                            id="plan-mtls"
                            label="mTLS plans"
                            description="Only available for API V4"
                            checked={localState.mtls}
                            disabled={!canEdit || readonlyState.mtls}
                            systemReadonly={readonlyState.mtls}
                            onToggle={checked => handleToggle('mtls', checked)}
                        />
                    </TooltipProvider>
                </div>
            </div>
        </div>
    );
}
