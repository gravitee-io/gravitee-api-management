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
import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    Skeleton,
    Switch,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { LockIcon, PlusIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ClientRegistrationDeleteDialog } from '../features/client-registration/components/ClientRegistrationDeleteDialog';
import { ClientRegistrationEmptyProviders } from '../features/client-registration/components/ClientRegistrationEmptyProviders';
import { ClientRegistrationProvidersTable } from '../features/client-registration/components/ClientRegistrationProvidersTable';
import { useDeleteClientRegistrationProvider } from '../features/client-registration/hooks/useClientRegistrationMutations';
import { useClientRegistrationPermissions } from '../features/client-registration/hooks/useClientRegistrationPermissions';
import { useClientRegistrationProviders } from '../features/client-registration/hooks/useClientRegistrationProviders';
import { DCR_REGISTRATION_LICENSE_FEATURE, DCR_REGISTRATION_UPGRADE } from '../features/client-registration/license/dcrRegistrationLicense';
import type { ClientRegistrationProvider } from '../features/client-registration/types/clientRegistrationProvider';
import { buildApplicationRegistrationSavePayload } from '../features/client-registration/utils/buildApplicationRegistrationSavePayload';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import type { PortalSettingsApplication } from '../features/security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../features/security-plan-types/utils/isPortalSettingReadonly';
import { FeatureLicenseDialog } from '../shared/components/FeatureLicenseDialog';
import { notify } from '../shared/notify';

const SYSTEM_READONLY_TOOLTIP = 'Configuration provided by the system';

function enabledFlag(value: { enabled?: boolean } | undefined): boolean {
    return value?.enabled ?? false;
}

function ToggleRow({
    id,
    label,
    description,
    checked,
    onToggle,
    disabled,
    systemReadonly,
}: Readonly<{
    id: string;
    label: string;
    description?: string;
    checked: boolean;
    onToggle: (checked: boolean) => void;
    disabled: boolean;
    systemReadonly?: boolean;
}>) {
    const switchControl = <Switch id={id} checked={checked} onCheckedChange={onToggle} disabled={disabled} aria-label={label} />;

    return (
        <div className="flex items-center justify-between py-4">
            <div className="space-y-1">
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

export function ClientRegistrationPage() {
    const navigate = useNavigate();
    const { canCreate, canDelete, canUpdateSettings } = useClientRegistrationPermissions();
    const hasDcrLicense = useHasFeature(DCR_REGISTRATION_LICENSE_FEATURE);
    const { data: settings, isLoading: settingsLoading, isError: settingsError, refetch: refetchSettings } = usePortalSettings();
    const {
        data: providers = [],
        isLoading: providersLoading,
        isError: providersError,
        refetch: refetchProviders,
    } = useClientRegistrationProviders();
    const saveSettings = useSavePortalSettings({
        successMessage: 'Configuration has been saved.',
        errorMessage: 'Failed to save configuration',
    });
    const deleteMutation = useDeleteClientRegistrationProvider();

    const [application, setApplication] = useState<PortalSettingsApplication | undefined>(settings?.application);
    const [providerToDelete, setProviderToDelete] = useState<ClientRegistrationProvider | undefined>();
    const [licenseDialogOpen, setLicenseDialogOpen] = useState(false);

    useEffect(() => {
        if (settings) setApplication(settings.application ?? {});
    }, [settings]);

    const isLoading = settingsLoading || providersLoading;
    const isError = settingsError || providersError;
    const addDisabled = providers.length >= 1;
    const savingSettings = saveSettings.isPending;

    const patchApplication = useCallback(
        (next: PortalSettingsApplication) => {
            if (!canUpdateSettings || savingSettings || !settings) return;
            const previous = application;
            setApplication(next);
            saveSettings.mutate(buildApplicationRegistrationSavePayload(settings, next), {
                onError: () => setApplication(previous),
            });
        },
        [application, canUpdateSettings, saveSettings, savingSettings, settings],
    );

    function handleDelete() {
        if (!providerToDelete) return;
        const name = providerToDelete.name;
        deleteMutation.mutate(providerToDelete.id, {
            onSuccess: () => {
                notify.success(`"${name}" has been deleted.`);
                setProviderToDelete(undefined);
            },
            onError: error => notify.error(error, 'Failed to delete provider'),
        });
    }

    function handleAdd() {
        if (addDisabled) return;
        if (!hasDcrLicense) {
            setLicenseDialogOpen(true);
            return;
        }
        navigate('new');
    }

    const header = (
        <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight">Client Registration</h1>
            <p className="text-sm text-muted-foreground">
                Choose which application types this environment accepts, and optionally plug in an OpenID Connect Dynamic Client
                Registration provider.
            </p>
        </div>
    );

    if (isError) {
        return (
            <div className="space-y-6">
                {header}
                <Alert variant="destructive">
                    <TriangleAlertIcon className="size-4" aria-hidden />
                    <AlertDescription className="flex flex-wrap items-center gap-3">
                        Could not load client registration.
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                                void refetchSettings();
                                void refetchProviders();
                            }}
                        >
                            Try again
                        </Button>
                    </AlertDescription>
                </Alert>
            </div>
        );
    }

    if (isLoading || !settings || application === undefined) {
        return (
            <div className="space-y-6">
                {header}
                <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, index) => (
                        <Skeleton key={index} className="w-full rounded-xl" style={{ height: 56 }} />
                    ))}
                </div>
            </div>
        );
    }

    const types = application.types;
    const toggleDisabled = (property: string) => !canUpdateSettings || savingSettings || isPortalSettingReadonly(settings, property);

    const addButton = canCreate ? (
        <TooltipProvider delayDuration={200}>
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-flex">
                        <Button disabled={addDisabled} onClick={handleAdd}>
                            {hasDcrLicense ? <PlusIcon className="size-4" aria-hidden /> : <LockIcon className="size-4" aria-hidden />}
                            Add a provider
                        </Button>
                    </span>
                </TooltipTrigger>
                {addDisabled ? <TooltipContent>Only one DCR provider is allowed.</TooltipContent> : null}
            </Tooltip>
        </TooltipProvider>
    ) : null;

    return (
        <div className="space-y-6">
            {header}

            <Card>
                <CardContent className="pt-6">
                    <h2 className="text-base font-semibold">Default application type</h2>
                    <TooltipProvider delayDuration={200}>
                        <ToggleRow
                            id="type-simple"
                            label="Simple"
                            description="A hands-free application. Using this type, you will be able to define the client_id by your own."
                            checked={enabledFlag(types?.simple)}
                            disabled={toggleDisabled('application.types.simple.enabled')}
                            systemReadonly={isPortalSettingReadonly(settings, 'application.types.simple.enabled')}
                            onToggle={enabled =>
                                patchApplication({
                                    ...application,
                                    types: { ...types, simple: { enabled } },
                                })
                            }
                        />
                    </TooltipProvider>
                </CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-6 pt-6">
                    <div className="space-y-1">
                        <h2 className="text-base font-semibold">Dynamic Client Registration (DCR) for applications</h2>
                        <p className="text-sm text-muted-foreground">
                            Client registration providers let you plug in any authorization server that implements{' '}
                            <a
                                href="https://openid.net/specs/openid-connect-registration-1_0.html"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="underline underline-offset-4"
                            >
                                OpenID Connect Dynamic Client Registration
                            </a>
                            . Defining a provider associates an OAuth client with an application and applies security best practices for the
                            type you choose.
                        </p>
                    </div>

                    <TooltipProvider delayDuration={200}>
                        <ToggleRow
                            id="dcr-enabled"
                            label="Enable Dynamic Client Registration"
                            checked={enabledFlag(application.registration)}
                            disabled={toggleDisabled('application.registration.enabled')}
                            systemReadonly={isPortalSettingReadonly(settings, 'application.registration.enabled')}
                            onToggle={enabled => patchApplication({ ...application, registration: { enabled } })}
                        />

                        <div className="border-t">
                            <h3 className="pt-4 text-sm font-semibold">Allowed application types</h3>
                            <div className="divide-y">
                                <ToggleRow
                                    id="type-browser"
                                    label="Browser"
                                    description="Angular, React, …"
                                    checked={enabledFlag(types?.browser)}
                                    disabled={toggleDisabled('application.types.browser.enabled')}
                                    systemReadonly={isPortalSettingReadonly(settings, 'application.types.browser.enabled')}
                                    onToggle={enabled => patchApplication({ ...application, types: { ...types, browser: { enabled } } })}
                                />
                                <ToggleRow
                                    id="type-web"
                                    label="Web"
                                    description="Java, .Net, …"
                                    checked={enabledFlag(types?.web)}
                                    disabled={toggleDisabled('application.types.web.enabled')}
                                    systemReadonly={isPortalSettingReadonly(settings, 'application.types.web.enabled')}
                                    onToggle={enabled => patchApplication({ ...application, types: { ...types, web: { enabled } } })}
                                />
                                <ToggleRow
                                    id="type-native"
                                    label="Native"
                                    description="iOS, Android, …"
                                    checked={enabledFlag(types?.native)}
                                    disabled={toggleDisabled('application.types.native.enabled')}
                                    systemReadonly={isPortalSettingReadonly(settings, 'application.types.native.enabled')}
                                    onToggle={enabled => patchApplication({ ...application, types: { ...types, native: { enabled } } })}
                                />
                                <ToggleRow
                                    id="type-b2b"
                                    label="Backend-to-Backend"
                                    checked={enabledFlag(types?.backend_to_backend)}
                                    disabled={toggleDisabled('application.types.backend_to_backend.enabled')}
                                    systemReadonly={isPortalSettingReadonly(settings, 'application.types.backend_to_backend.enabled')}
                                    onToggle={enabled =>
                                        patchApplication({
                                            ...application,
                                            types: { ...types, backend_to_backend: { enabled } },
                                        })
                                    }
                                />
                            </div>
                        </div>
                    </TooltipProvider>

                    <div className="space-y-3 border-t pt-5">
                        <div className="flex items-center justify-between gap-3">
                            <h3 className="text-sm font-semibold">Providers configuration</h3>
                            {addButton}
                        </div>
                        {providers.length === 0 ? (
                            <ClientRegistrationEmptyProviders />
                        ) : (
                            <ClientRegistrationProvidersTable
                                providers={providers}
                                canDelete={canDelete}
                                onEdit={provider => navigate(provider.id)}
                                onDelete={setProviderToDelete}
                            />
                        )}
                    </div>
                </CardContent>
            </Card>

            <ClientRegistrationDeleteDialog
                open={providerToDelete !== undefined}
                provider={providerToDelete}
                onClose={() => setProviderToDelete(undefined)}
                onConfirm={handleDelete}
                isDeleting={deleteMutation.isPending}
            />
            <FeatureLicenseDialog upgrade={DCR_REGISTRATION_UPGRADE} open={licenseDialogOpen} onOpenChange={setLicenseDialogOpen} />
        </div>
    );
}
