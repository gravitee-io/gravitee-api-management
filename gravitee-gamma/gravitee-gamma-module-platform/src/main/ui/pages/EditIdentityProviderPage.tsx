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
import { Button, PageFocused, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { IdentityProviderCatalogLoadError } from '../features/authentication/components/IdentityProviderCatalogLoadError';
import { IdentityProviderEditForm } from '../features/authentication/components/IdentityProviderEditForm';
import type { IdentityProviderMappingOption } from '../features/authentication/components/IdentityProviderMappingMultiSelect';
import { useIdentityProvider, useIdentityProviderMappingCatalog } from '../features/authentication/hooks/useIdentityProvider';
import { identityProviderTypeLabel } from '../features/authentication/utils/identityProviderDisplay';
import { ORGANIZATION_IDENTITY_PROVIDER_UPDATE_PERMISSION } from '../features/authentication/utils/identityProviderPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

function toRoleOptions(roles: readonly { id: string; name?: string }[]): IdentityProviderMappingOption[] {
    return roles.map(role => {
        const name = role.name?.trim() || role.id;
        return { id: name, name };
    });
}

export function EditIdentityProviderPage() {
    const { identityProviderId } = useParams<{ identityProviderId: string }>();
    const navigate = useNavigate();
    const canUpdate = useHasPermission({ anyOf: [ORGANIZATION_IDENTITY_PROVIDER_UPDATE_PERMISSION] });
    const providerQuery = useIdentityProvider(identityProviderId);
    const { groupsQuery, environmentsQuery, organizationRolesQuery, environmentRolesQuery, refetchCatalogs } =
        useIdentityProviderMappingCatalog();
    const [dirty, setDirty] = useState(false);
    const [leaveOpen, setLeaveOpen] = useState(false);

    const isForbidden = isForbiddenApiError(providerQuery.isError, providerQuery.error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'authentication',
        permissionPrefix: 'organization-identity_provider-',
        redirectTo: '..',
    });

    useEffect(() => {
        if (!dirty) {
            return;
        }
        function onBeforeUnload(event: BeforeUnloadEvent) {
            event.preventDefault();
            event.returnValue = '';
        }
        window.addEventListener('beforeunload', onBeforeUnload);
        return () => window.removeEventListener('beforeunload', onBeforeUnload);
    }, [dirty]);

    const provider = providerQuery.data;

    const groups = useMemo<IdentityProviderMappingOption[]>(
        () => (groupsQuery.data ?? []).map(group => ({ id: group.id, name: group.name })),
        [groupsQuery.data],
    );
    const environments = useMemo<IdentityProviderMappingOption[]>(
        () =>
            (environmentsQuery.data ?? []).map(environment => ({
                id: environment.id,
                name: environment.name?.trim() || environment.id,
                description: environment.description,
            })),
        [environmentsQuery.data],
    );
    const organizationRoles = useMemo(() => toRoleOptions(organizationRolesQuery.data ?? []), [organizationRolesQuery.data]);
    const environmentRoles = useMemo(() => toRoleOptions(environmentRolesQuery.data ?? []), [environmentRolesQuery.data]);

    const mappingCatalogsLoading =
        groupsQuery.isLoading || environmentsQuery.isLoading || organizationRolesQuery.isLoading || environmentRolesQuery.isLoading;
    const hasCatalogError = [groupsQuery, environmentsQuery, organizationRolesQuery, environmentRolesQuery].some(query => query.isError);
    const mappingsDisabled = hasCatalogError || mappingCatalogsLoading;

    function requestLeave() {
        if (dirty) {
            setLeaveOpen(true);
            return;
        }
        navigate('..', { relative: 'path' });
    }

    if (providerQuery.isLoading || (!providerQuery.isError && environmentsQuery.isLoading)) {
        return (
            <PageFocused>
                <div className="space-y-6">
                    <Skeleton className="h-8 w-48" />
                    <Skeleton className="h-40 w-full rounded-xl" />
                    <Skeleton className="h-40 w-full rounded-xl" />
                </div>
            </PageFocused>
        );
    }

    if (isForbidden) {
        return null;
    }

    if (providerQuery.isError || !provider) {
        return (
            <PageFocused>
                <div className="space-y-4">
                    <Button
                        type="button"
                        variant="ghost"
                        className="gap-1.5 px-0 text-muted-foreground"
                        onClick={() => navigate('..', { relative: 'path' })}
                    >
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Authentication
                    </Button>
                    <p className="text-sm text-muted-foreground">Identity provider not found or failed to load.</p>
                </div>
            </PageFocused>
        );
    }

    return (
        <PageFocused>
            <div className="space-y-6">
                <div className="space-y-2">
                    <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={requestLeave}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Authentication
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">
                        Update {identityProviderTypeLabel(provider.type)} identity provider
                    </h1>
                </div>
                {hasCatalogError ? <IdentityProviderCatalogLoadError onRetry={refetchCatalogs} /> : null}
                <IdentityProviderEditForm
                    key={provider.id}
                    provider={provider}
                    groups={groups}
                    environments={environments}
                    organizationRoles={organizationRoles}
                    environmentRoles={environmentRoles}
                    canUpdate={canUpdate}
                    mappingsDisabled={mappingsDisabled}
                    onDirtyChange={setDirty}
                    onCancel={requestLeave}
                />
                <ConfirmDialog
                    open={leaveOpen}
                    onOpenChange={setLeaveOpen}
                    title="Unsaved changes"
                    description="Leave this page? Unsaved changes will be lost."
                    confirmLabel="Leave"
                    onConfirm={() => navigate('..', { relative: 'path' })}
                />
            </div>
        </PageFocused>
    );
}
