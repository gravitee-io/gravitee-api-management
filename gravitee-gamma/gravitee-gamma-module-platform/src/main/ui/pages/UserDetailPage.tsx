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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { UserEnvironmentRolesCard } from '../features/users/components/UserEnvironmentRolesCard';
import { UserGroupMembershipsCard } from '../features/users/components/UserGroupMembershipsCard';
import { UserOrganizationRolesCard } from '../features/users/components/UserOrganizationRolesCard';
import { UserPersonalAccessTokensCard } from '../features/users/components/UserPersonalAccessTokensCard';
import { UserProfileCard } from '../features/users/components/UserProfileCard';
import { UserRegistrationPendingBanner } from '../features/users/components/UserRegistrationPendingBanner';
import {
    useEnvironmentRoleCatalog,
    useOrganizationEnvironments,
    useOrganizationRoleCatalog,
    useOrganizationUser,
} from '../features/users/hooks/useOrganizationUser';
import {
    useProcessUserRegistration,
    useResetOrganizationUserPassword,
    useUpdateOrganizationUserRoles,
    useUpdateOrganizationUserServiceAccount,
} from '../features/users/hooks/useUserMutations';
import {
    canConvertToServiceAccount,
    canResetPassword,
    formatUserDisplayName,
    roleLabelsForIds,
} from '../features/users/utils/userDetailDisplay';
import {
    ORGANIZATION_USER_CREATE_PERMISSION,
    ORGANIZATION_USER_DELETE_PERMISSION,
    ORGANIZATION_USER_UPDATE_PERMISSION,
} from '../features/users/utils/userPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

type RegistrationConfirmAction = 'accept' | 'reject';

export function UserDetailPage() {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const canUpdate = useHasPermission({ anyOf: [ORGANIZATION_USER_UPDATE_PERMISSION] });
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_USER_CREATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ORGANIZATION_USER_DELETE_PERMISSION] });
    const [registrationConfirm, setRegistrationConfirm] = useState<RegistrationConfirmAction | null>(null);
    const [convertToServiceAccountConfirmOpen, setConvertToServiceAccountConfirmOpen] = useState(false);
    const [resetPasswordConfirmOpen, setResetPasswordConfirmOpen] = useState(false);

    const { data: user, isLoading: userLoading, isError: userError } = useOrganizationUser(userId);
    const { data: environments = [], isLoading: environmentsLoading } = useOrganizationEnvironments();
    const { data: organizationRoles = [], isLoading: organizationRolesLoading } = useOrganizationRoleCatalog();
    const { data: environmentRoles = [], isLoading: environmentRolesLoading } = useEnvironmentRoleCatalog();
    const processRegistration = useProcessUserRegistration(userId);
    const convertToServiceAccount = useUpdateOrganizationUserServiceAccount(userId);
    const resetPassword = useResetOrganizationUserPassword(userId);
    const updateUserRoles = useUpdateOrganizationUserRoles(userId);
    const shellEnvironment = useEnvironment();

    const isActiveUser = user?.status?.toUpperCase() === 'ACTIVE';
    const rolesEditable = canUpdate && isActiveUser;
    const canAddToGroup = canCreate && isActiveUser;
    const canRemoveFromGroup = canDelete && isActiveUser;
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

    function handleConvertToServiceAccountConfirm() {
        convertToServiceAccount.mutate(true, {
            onSuccess: () => {
                setConvertToServiceAccountConfirmOpen(false);
                const displayName = user ? formatUserDisplayName(user) : 'User';
                notify.success(`User "${displayName}" has been converted to a service account`);
            },
            onError: error => {
                notify.error(error, 'Failed to convert user to a service account.');
            },
        });
    }

    function handleResetPasswordConfirm() {
        if (!user) {
            return;
        }

        resetPassword.mutate(undefined, {
            onSuccess: () => {
                setResetPasswordConfirmOpen(false);
                notify.success(`The password of user "${formatUserDisplayName(user)}" has been successfully reset`);
            },
            onError: error => {
                notify.error(error, 'Failed to reset user password.');
            },
        });
    }

    function handleOrganizationRolesChange(roleIds: string[]) {
        return new Promise<void>((resolve, reject) => {
            updateUserRoles.mutate(
                { referenceType: 'ORGANIZATION', roles: roleIds },
                {
                    onSuccess: () => {
                        const labels = roleLabelsForIds(roleIds, organizationRoles);
                        if (labels.length === 1) {
                            notify.success(`Role changed to ${labels[0]}`);
                        } else {
                            notify.success('Organization roles successfully updated');
                        }
                        resolve();
                    },
                    onError: error => {
                        notify.error(error, 'Failed to update organization roles');
                        reject(error);
                    },
                },
            );
        });
    }

    function handleEnvironmentRolesChange(environmentId: string, roleIds: string[]) {
        return new Promise<void>((resolve, reject) => {
            updateUserRoles.mutate(
                { referenceType: 'ENVIRONMENT', referenceId: environmentId, roles: roleIds },
                {
                    onSuccess: () => {
                        notify.success('Environment roles successfully updated');
                        resolve();
                    },
                    onError: error => {
                        notify.error(error, 'Failed to update environment roles');
                        reject(error);
                    },
                },
            );
        });
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
    const showConvertToServiceAccount = canUpdate && canConvertToServiceAccount(user);
    const showResetPassword = canUpdate && canResetPassword(user);
    const hasProfileHeaderActions = showResetPassword || showConvertToServiceAccount;
    const userDisplayName = formatUserDisplayName(user);
    const tokenEnvironmentId = shellEnvironment?.id ?? environments[0]?.id ?? 'DEFAULT';

    const profileHeaderActions = (
        <>
            {showResetPassword ? (
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    aria-label="Reset password"
                    disabled={resetPassword.isPending}
                    onClick={() => setResetPasswordConfirmOpen(true)}
                >
                    Reset password
                </Button>
            ) : null}
            {showConvertToServiceAccount ? (
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    aria-label="Convert to service account"
                    disabled={convertToServiceAccount.isPending}
                    onClick={() => setConvertToServiceAccountConfirmOpen(true)}
                >
                    Convert to service account
                </Button>
            ) : null}
        </>
    );

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Users
                </Link>
            </Button>

            <UserProfileCard user={user} headerActions={hasProfileHeaderActions ? profileHeaderActions : undefined} />

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

            {convertToServiceAccountConfirmOpen ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !convertToServiceAccount.isPending && setConvertToServiceAccountConfirmOpen(false)}
                    title="Convert to service account"
                    description={
                        <>
                            Are you sure you want to convert <strong>{userDisplayName}</strong> to a service account? This action cannot be
                            undone.
                        </>
                    }
                    confirmLabel="Convert"
                    pendingLabel="Converting…"
                    isPending={convertToServiceAccount.isPending}
                    onConfirm={handleConvertToServiceAccountConfirm}
                />
            ) : null}

            {resetPasswordConfirmOpen ? (
                <ConfirmDialog
                    open
                    onOpenChange={open => !open && !resetPassword.isPending && setResetPasswordConfirmOpen(open)}
                    title="Reset user password"
                    description={
                        <>
                            Are you sure you want to reset the password of user <strong>{userDisplayName}</strong>?
                            <br />
                            The user will receive an email with a link to set a new password.
                        </>
                    }
                    confirmLabel="Reset"
                    pendingLabel="Resetting…"
                    isPending={resetPassword.isPending}
                    onConfirm={handleResetPasswordConfirm}
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

            <UserGroupMembershipsCard
                userId={user.id}
                userDisplayName={userDisplayName}
                environments={environments}
                environmentsLoading={environmentsLoading}
                rolesEditable={rolesEditable}
                canAddToGroup={canAddToGroup}
                canRemoveFromGroup={canRemoveFromGroup}
            />

            <UserPersonalAccessTokensCard
                userId={user.id}
                environmentId={tokenEnvironmentId}
                canGenerate={canUpdate}
                canRevoke={canDelete}
            />
        </div>
    );
}
