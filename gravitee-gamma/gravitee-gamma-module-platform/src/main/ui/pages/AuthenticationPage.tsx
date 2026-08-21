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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Button, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { AuthenticationEmptyProviders } from '../features/authentication/components/AuthenticationEmptyProviders';
import { IdentityProvidersTable } from '../features/authentication/components/IdentityProvidersTable';
import { ToggleRow } from '../features/authentication/components/ToggleRow';
import { useAuthenticationPage } from '../features/authentication/hooks/useAuthenticationPage';
import {
    useDeleteIdentityProvider,
    useSaveLocalLogin,
    useUpdateActivatedIdentityProviders,
} from '../features/authentication/hooks/useIdentityProviderMutations';
import type { IdentityProviderRow } from '../features/authentication/types/identityProvider';
import { isLocalLoginReadonly, localLoginSettingTooltip } from '../features/authentication/utils/identityProviderDisplay';
import {
    ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION_UPDATE_PERMISSION,
    ORGANIZATION_IDENTITY_PROVIDER_CREATE_PERMISSION,
    ORGANIZATION_IDENTITY_PROVIDER_DELETE_PERMISSION,
    ORGANIZATION_SETTINGS_UPDATE_PERMISSION,
} from '../features/authentication/utils/identityProviderPermissions';
import { toIdentityProviderRows } from '../features/authentication/utils/identityProviderTableUtils';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import type { ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { notify } from '../shared/notify';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

function buildLocalLoginSettings(settings: ConsoleSettings, localLogin: boolean): ConsoleSettings {
    const previousAuth = settings.authentication ?? {};
    return {
        ...settings,
        authentication: {
            ...previousAuth,
            localLogin: { enabled: localLogin },
        },
    };
}

export function AuthenticationPage() {
    const navigate = useNavigate();
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_IDENTITY_PROVIDER_CREATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ORGANIZATION_IDENTITY_PROVIDER_DELETE_PERMISSION] });
    const canActivate = useHasPermission({ anyOf: [ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION_UPDATE_PERMISSION] });
    const canUpdateSettings = useHasPermission({ anyOf: [ORGANIZATION_SETTINGS_UPDATE_PERMISSION] });

    const { providersQuery, activationsQuery } = useAuthenticationPage();
    const { data: settings, isLoading: isSettingsLoading, isError: isSettingsError } = useOrgConsoleSettings();
    const deleteMutation = useDeleteIdentityProvider();
    const activationMutation = useUpdateActivatedIdentityProviders();
    const localLoginMutation = useSaveLocalLogin();

    const isForbidden = isForbiddenApiError(providersQuery.isError, providersQuery.error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: 'organization-identity_provider-',
        redirectTo: '../applications',
    });

    const [providerToDelete, setProviderToDelete] = useState<IdentityProviderRow | undefined>();
    const [providerToToggle, setProviderToToggle] = useState<IdentityProviderRow | undefined>();

    const activationsLoaded = activationsQuery.isSuccess;
    const rows = useMemo(
        () => toIdentityProviderRows(providersQuery.data ?? [], activationsLoaded ? activationsQuery.data : undefined),
        [activationsLoaded, activationsQuery.data, providersQuery.data],
    );
    const hasActivatedIdp = activationsLoaded && rows.some(row => row.activated === true);
    const localLoginEnabled = settings?.authentication?.localLogin?.enabled === true;
    const localLoginSystemReadonly = isLocalLoginReadonly(settings?.metadata?.readonly);
    const providersFailed = providersQuery.isError && !isForbidden;
    const activationsFailed = activationsQuery.isError;
    const identityProviderDataFailed = providersFailed || activationsFailed;
    const identityProviderDataLoading = providersQuery.isLoading || activationsQuery.isLoading;
    const canToggleActivation = canActivate && activationsLoaded;
    const localLoginDisabled =
        !canUpdateSettings ||
        localLoginSystemReadonly ||
        !hasActivatedIdp ||
        localLoginMutation.isPending ||
        !settings ||
        isSettingsLoading ||
        identityProviderDataLoading ||
        identityProviderDataFailed;
    const localLoginTooltip = localLoginSettingTooltip({
        isLoading: identityProviderDataLoading || isSettingsLoading,
        isError: isSettingsError || identityProviderDataFailed,
        canUpdateSettings,
        systemReadonly: localLoginSystemReadonly,
        hasActivatedIdp,
    });

    async function handleLocalLogin(checked: boolean) {
        if (localLoginDisabled || !settings) return;
        try {
            await localLoginMutation.mutateAsync(buildLocalLoginSettings(settings, checked));
            notify.success('Configuration successfully updated!');
        } catch (saveError: unknown) {
            notify.error(saveError, 'Failed to save configuration');
        }
    }

    async function handleDelete() {
        if (!providerToDelete) return;
        try {
            await deleteMutation.mutateAsync(providerToDelete.id);
            notify.success(`Identity Provider ${providerToDelete.name} successfully deleted!`);
            setProviderToDelete(undefined);
        } catch (deleteError: unknown) {
            notify.error(deleteError, 'Failed to delete identity provider');
        }
    }

    async function handleToggle() {
        if (providerToToggle?.activated === undefined) return;
        const nextActivated =
            providerToToggle.activated === true
                ? rows.filter(row => row.activated === true && row.id !== providerToToggle.id).map(row => row.id)
                : [...rows.filter(row => row.activated === true).map(row => row.id), providerToToggle.id];
        try {
            await activationMutation.mutateAsync(nextActivated);
            notify.success(
                `Identity Provider ${providerToToggle.name} successfully ${providerToToggle.activated ? 'deactivated' : 'activated'}!`,
            );
            setProviderToToggle(undefined);
        } catch (toggleError: unknown) {
            notify.error(toggleError, 'Failed to update identity provider');
        }
    }

    const loginToggle = (
        <ToggleRow
            id="local-login"
            label="Show login form on management console"
            checked={localLoginEnabled}
            disabled={localLoginDisabled}
            description={localLoginTooltip ?? undefined}
            onToggle={handleLocalLogin}
        />
    );

    function renderProviders() {
        if (providersQuery.isLoading) {
            return (
                <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, index) => (
                        <Skeleton key={index} className="h-12 w-full rounded-md" />
                    ))}
                </div>
            );
        }

        if (isForbidden) {
            return null;
        }

        if (providersFailed) {
            return (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Failed to load identity providers. Please refresh and try again.</p>
                </div>
            );
        }

        if (rows.length === 0) {
            return <AuthenticationEmptyProviders canCreate={canCreate} onAdd={() => navigate('new')} />;
        }

        return (
            <IdentityProvidersTable
                rows={rows}
                canActivate={canToggleActivation}
                canDelete={canDelete}
                onToggle={setProviderToToggle}
                onDelete={setProviderToDelete}
            />
        );
    }

    const toggleAction = providerToToggle?.activated === true ? 'Deactivate' : 'Activate';

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Authentication</h1>
                <p className="text-sm text-muted-foreground">
                    By creating an identity provider, you are providing capabilities to users to login into the portal / management UI using
                    external user accounts from GitHub, Google, OpenID Connect server or Gravitee.io AM.
                </p>
            </div>

            <Card size="sm">
                <CardContent>{loginToggle}</CardContent>
            </Card>

            <Card>
                <CardContent className="space-y-4 pt-6">
                    <div className="flex items-center justify-between gap-3">
                        <h2 className="text-base font-semibold">Identity Providers</h2>
                        {canCreate && rows.length > 0 ? (
                            <Button size="sm" className="shrink-0" onClick={() => navigate('new')}>
                                <PlusIcon className="size-4" aria-hidden />
                                Add an identity provider
                            </Button>
                        ) : null}
                    </div>
                    {renderProviders()}
                </CardContent>
            </Card>

            {providerToDelete ? (
                <ConfirmDialog
                    open
                    onOpenChange={isOpen => {
                        if (!isOpen && !deleteMutation.isPending) setProviderToDelete(undefined);
                    }}
                    title="Delete an Identity Provider"
                    description={
                        <>
                            Are you sure you want to delete the identity provider <strong>{providerToDelete.name}</strong>?
                        </>
                    }
                    confirmLabel="Delete"
                    pendingLabel="Deleting…"
                    destructive
                    isPending={deleteMutation.isPending}
                    onConfirm={() => {
                        void handleDelete();
                    }}
                />
            ) : null}
            {providerToToggle && providerToToggle.activated !== undefined ? (
                <ConfirmDialog
                    open
                    onOpenChange={isOpen => {
                        if (!isOpen && !activationMutation.isPending) setProviderToToggle(undefined);
                    }}
                    title={`${toggleAction} an Identity Provider`}
                    description={
                        <>
                            Are you sure you want to {toggleAction.toLowerCase()} the identity provider{' '}
                            <strong>{providerToToggle.name}</strong>?
                        </>
                    }
                    confirmLabel="Ok"
                    pendingLabel="Saving…"
                    isPending={activationMutation.isPending}
                    onConfirm={() => {
                        void handleToggle();
                    }}
                />
            ) : null}
        </div>
    );
}
