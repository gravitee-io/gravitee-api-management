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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { ArrowLeftIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { AddRoleMembersSheet } from '../features/roles/components/AddRoleMembersSheet';
import { RoleMembersTable } from '../features/roles/components/RoleMembersTable';
import { useAddRoleMembers, useDeleteRoleMember, useRoleMemberships } from '../features/roles/hooks/useRoleMemberships';
import type { RoleMembershipListItem } from '../features/roles/types/role';
import { ORGANIZATION_ROLE_UPDATE_PERMISSION } from '../features/roles/utils/rolePermissionConstants';
import { isRoleScope } from '../features/roles/utils/rolePermissions';
import { ROLE_SCOPE_LABELS } from '../features/roles/utils/roleScopeLabels';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { SectionError } from '../shared/components/SectionError';
import { notify } from '../shared/notify';
import type { SearchableUser } from '../shared/types/userSearch';

export function RoleMembersPage() {
    const { roleScope, roleName } = useParams<{ roleScope: string; roleName: string }>();
    const navigate = useNavigate();
    // undefined for an invalid param keeps useRoleMemberships' fetch disabled below; hooks must still run
    // unconditionally on every render, so the redirect itself happens after them, not before.
    const validScope = isRoleScope(roleScope) ? roleScope : undefined;

    const { data: members = [], isLoading, isError } = useRoleMemberships(validScope, roleName);
    const canManage = useHasPermission({ anyOf: [ORGANIZATION_ROLE_UPDATE_PERMISSION] });
    const addMutation = useAddRoleMembers();
    const deleteMutation = useDeleteRoleMember();

    const [isAddSheetOpen, setAddSheetOpen] = useState(false);
    // Bumped on every open so AddRoleMembersSheet remounts with fresh search/selection state instead of
    // resetting it via an effect (see REACT_19_PATTERNS.md §2) — left unchanged on close so the sheet's
    // own closing animation isn't cut short by an unmount.
    const [addSheetSession, setAddSheetSession] = useState(0);
    const [memberToDelete, setMemberToDelete] = useState<RoleMembershipListItem | null>(null);

    // A route param outside ROLE_SCOPES (e.g. a hand-edited URL) would otherwise fetch and post against
    // /rolescopes/<garbage>/... — send it back to the list instead.
    if (!validScope) {
        return <Navigate to=".." replace />;
    }
    const scope = validScope;

    function openAddSheet() {
        setAddSheetSession(session => session + 1);
        setAddSheetOpen(true);
    }

    // ":roleScope/:roleName/members" is a single flat route directly under "roles" (not nested `<Route>`s),
    // so it sits at the same match depth as ":roleScope" — one ".." always reaches "roles".
    function goBack() {
        navigate('..');
    }

    async function handleAdd(users: SearchableUser[]) {
        try {
            const { succeededCount, failedCount } = await addMutation.mutateAsync({
                scope,
                roleName: roleName!,
                users: users.map(user => ({ ...(user.id ? { id: user.id } : {}), reference: user.reference })),
            });
            if (failedCount > 0) {
                notify.error(
                    succeededCount > 0
                        ? `${succeededCount} of ${succeededCount + failedCount} members added; ${failedCount} failed.`
                        : 'Failed to add the selected members.',
                );
            } else {
                notify.success('Membership successfully created');
            }
            // Close on any success so the sheet doesn't hide the memberships that did get added; keep it open
            // on total failure so the user can retry without re-selecting everyone.
            if (succeededCount > 0) {
                setAddSheetOpen(false);
            }
        } catch (error) {
            notify.error(error);
        }
    }

    async function handleConfirmDelete() {
        if (!memberToDelete) return;
        try {
            await deleteMutation.mutateAsync({ scope, roleName: roleName!, userId: memberToDelete.id });
            notify.success('Membership has been successfully deleted');
            setMemberToDelete(null);
        } catch (error) {
            notify.error(error);
        }
    }

    return (
        <div className="space-y-4">
            <div>
                <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={goBack}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to roles
                </Button>
                <h1 className="text-2xl font-semibold tracking-tight">
                    {ROLE_SCOPE_LABELS[scope]} - {roleName}
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    List all the users having the role {roleName} in the {ROLE_SCOPE_LABELS[scope]} scope.
                </p>
            </div>

            <Card>
                <CardContent className="flex items-center justify-between gap-4">
                    <h2 className="text-lg font-medium">Members</h2>
                    {canManage ? (
                        <Button type="button" aria-label="Button to add a member" onClick={openAddSheet}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add a member
                        </Button>
                    ) : null}
                </CardContent>
            </Card>

            {isError ? (
                <SectionError message="Failed to load members for this role. Please refresh and try again." />
            ) : (
                <RoleMembersTable members={members} isLoading={isLoading} canManage={canManage} onDeleteMember={setMemberToDelete} />
            )}

            <AddRoleMembersSheet
                key={addSheetSession}
                open={isAddSheetOpen}
                existingMembers={members}
                onClose={() => setAddSheetOpen(false)}
                onAdd={handleAdd}
                isAdding={addMutation.isPending}
            />

            <ConfirmDialog
                open={memberToDelete !== null}
                onOpenChange={isOpen => {
                    if (!isOpen) setMemberToDelete(null);
                }}
                title="Delete a membership"
                description={
                    <>
                        Are you sure you want to delete the role{' '}
                        <strong>
                            {scope} - {roleName}
                        </strong>{' '}
                        to user <strong>{memberToDelete?.displayName}</strong>?
                    </>
                }
                confirmLabel="Delete"
                pendingLabel="Deleting…"
                destructive
                isPending={deleteMutation.isPending}
                onConfirm={handleConfirmDelete}
            />
        </div>
    );
}
