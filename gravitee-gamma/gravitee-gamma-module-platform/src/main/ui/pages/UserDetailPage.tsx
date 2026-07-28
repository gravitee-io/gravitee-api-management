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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { UserEnvironmentRolesCard } from '../features/users/components/UserEnvironmentRolesCard';
import { UserGroupMembershipsCard } from '../features/users/components/UserGroupMembershipsCard';
import { UserOrganizationRolesCard } from '../features/users/components/UserOrganizationRolesCard';
import { UserProfileCard } from '../features/users/components/UserProfileCard';
import { UserRegistrationPendingBanner } from '../features/users/components/UserRegistrationPendingBanner';
import {
    useEnvironmentRoleCatalog,
    useOrganizationEnvironments,
    useOrganizationRoleCatalog,
    useOrganizationUser,
    useOrganizationUserGroups,
} from '../features/users/hooks/useOrganizationUser';
import { useProcessUserRegistration, useUpdateOrganizationUserRoles } from '../features/users/hooks/useUserMutations';
import { formatUserDisplayName, roleLabelsForIds } from '../features/users/utils/userDetailDisplay';
import { ORGANIZATION_USER_UPDATE_PERMISSION } from '../features/users/utils/userPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

type RegistrationConfirmAction = 'accept' | 'reject';

export function UserDetailPage() {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const canUpdate = useHasPermission({ anyOf: [ORGANIZATION_USER_UPDATE_PERMISSION] });
    const [registrationConfirm, setRegistrationConfirm] = useState<RegistrationConfirmAction | null>(null);

    const { data: user, isLoading: userLoading, isError: userError } = useOrganizationUser(userId);
    const { data: environments = [], isLoading: environmentsLoading } = useOrganizationEnvironments();
    const { data: groups = [], isLoading: groupsLoading } = useOrganizationUserGroups(userId);
    const { data: organizationRoles = [], isLoading: organizationRolesLoading } = useOrganizationRoleCatalog();
    const { data: environmentRoles = [], isLoading: environmentRolesLoading } = useEnvironmentRoleCatalog();
    const processRegistration = useProcessUserRegistration(userId);
    const updateUserRoles = useUpdateOrganizationUserRoles(userId);

    const rolesEditable = canUpdate && user?.status?.toUpperCase() === 'ACTIVE';
    const savingEnvironmentId =
        updateUserRoles.isPending && updateUserRoles.variables?.referenceType === 'ENVIRONMENT'
            ? updateUserRoles.variables.referenceId
            : undefined;
    const rolesSaving = updateUserRoles.isPending;

    function handleProcessRegistration(accepted: boolean) {
        processRegistration.mutate(accepted, {
            onSuccess: () => {
                setRegistrationConfirm(null);
                const displayName = user ? formatUserDisplayName(user) : 'User';
                notify.success(accepted ? `User "${displayName}" has been accepted` : `User "${displayName}" has been rejected`);
            },
            onError: error => {
                notify.error(error, accepted ? 'Failed to accept registration.' : 'Failed to reject registration.');
            },
        });
    }

    function handleRegistrationConfirm() {
        if (!registrationConfirm) return;
        handleProcessRegistration(registrationConfirm === 'accept');
    }

    function handleOrganizationRolesChange(roleIds: string[]) {
        updateUserRoles.mutate(
            { referenceType: 'ORGANIZATION', roles: roleIds },
            {
                onSuccess: () => {
                    const labels = roleLabelsForIds(roleIds, organizationRoles);
                    if (labels.length === 1) {
                        notify.success(`Role changed to ${labels[0]}`);
                        return;
                    }
                    notify.success('Organization roles successfully updated');
                },
                onError: error => notify.error(error, 'Failed to update organization roles'),
            },
        );
    }

    function handleEnvironmentRolesChange(environmentId: string, roleIds: string[]) {
        updateUserRoles.mutate(
            { referenceType: 'ENVIRONMENT', referenceId: environmentId, roles: roleIds },
            {
                onSuccess: () => {
                    notify.success('Environment roles successfully updated');
                },
                onError: error => notify.error(error, 'Failed to update environment roles'),
            },
        );
    }

    if (userLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-32 w-full rounded-xl" />
                <Skeleton className="h-32 w-full rounded-xl" />
                <Skeleton className="h-32 w-full rounded-xl" />
            </div>
        );
    }

    if (userError || !user) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Users
                </Button>
                <p className="text-sm text-muted-foreground">User not found or failed to load.</p>
            </div>
        );
    }

    const showRegistrationBanner = canUpdate && user.status?.toUpperCase() === 'PENDING';
    const userDisplayName = formatUserDisplayName(user);

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Users
                </Link>
            </Button>

            <UserProfileCard user={user} />

            {showRegistrationBanner ? (
                <UserRegistrationPendingBanner
                    isPending={processRegistration.isPending}
                    onAccept={() => setRegistrationConfirm('accept')}
                    onReject={() => setRegistrationConfirm('reject')}
                />
            ) : null}

            {registrationConfirm ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !processRegistration.isPending && setRegistrationConfirm(null)}
                    title="User registration"
                    description={
                        registrationConfirm === 'accept'
                            ? `Are you sure you want to accept the registration request of ${userDisplayName}?`
                            : `Are you sure you want to reject the registration request of ${userDisplayName}?`
                    }
                    confirmLabel={registrationConfirm === 'accept' ? 'Accept' : 'Reject'}
                    pendingLabel={registrationConfirm === 'accept' ? 'Accepting…' : 'Rejecting…'}
                    destructive={registrationConfirm === 'reject'}
                    isPending={processRegistration.isPending}
                    onConfirm={handleRegistrationConfirm}
                />
            ) : null}

            <UserOrganizationRolesCard
                user={user}
                roles={organizationRoles}
                loading={organizationRolesLoading}
                disabled={!rolesEditable || rolesSaving}
                saving={updateUserRoles.isPending && updateUserRoles.variables?.referenceType === 'ORGANIZATION'}
                onRolesChange={handleOrganizationRolesChange}
            />

            <UserEnvironmentRolesCard
                user={user}
                environments={environments}
                roles={environmentRoles}
                loading={environmentsLoading || environmentRolesLoading}
                disabled={!rolesEditable || rolesSaving}
                savingEnvironmentId={savingEnvironmentId}
                onEnvironmentRolesChange={handleEnvironmentRolesChange}
            />

            <UserGroupMembershipsCard groups={groups} loading={groupsLoading} />
        </div>
    );
}
