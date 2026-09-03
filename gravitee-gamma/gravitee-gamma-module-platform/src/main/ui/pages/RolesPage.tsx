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
import { useHasFeature, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { CustomRolesLicenseDialog } from '../features/roles/components/CustomRolesLicenseDialog';
import { RoleDeleteDialog } from '../features/roles/components/RoleDeleteDialog';
import { RolesByScopeSection } from '../features/roles/components/RolesByScopeSection';
import { useDeleteRole } from '../features/roles/hooks/useRoleMutations';
import { useRolesByScope } from '../features/roles/hooks/useRoles';
import { CUSTOM_ROLES_LICENSE_FEATURE } from '../features/roles/license/customRolesLicense';
import type { Role, RoleScope } from '../features/roles/types/role';
import {
    ORGANIZATION_ROLE_CREATE_PERMISSION,
    ORGANIZATION_ROLE_DELETE_PERMISSION,
    ORGANIZATION_ROLE_UPDATE_PERMISSION,
} from '../features/roles/utils/rolePermissionConstants';
import { notify } from '../shared/notify';

interface DeleteTarget {
    scope: RoleScope;
    role: Role;
}

export function RolesPage() {
    const navigate = useNavigate();
    const { groups } = useRolesByScope();
    const hasCustomRolesLicense = useHasFeature(CUSTOM_ROLES_LICENSE_FEATURE);
    const canCreate = useHasPermission({ anyOf: [ORGANIZATION_ROLE_CREATE_PERMISSION] });
    const canDelete = useHasPermission({ anyOf: [ORGANIZATION_ROLE_DELETE_PERMISSION] });
    const canManageMembers = useHasPermission({ anyOf: [ORGANIZATION_ROLE_UPDATE_PERMISSION] });

    const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);
    const [licenseDialogOpen, setLicenseDialogOpen] = useState(false);
    const deleteMutation = useDeleteRole();

    async function handleConfirmDelete() {
        if (!deleteTarget) return;
        const { scope, role } = deleteTarget;
        try {
            await deleteMutation.mutateAsync({ scope, name: role.name });
            notify.success(`Role ${role.name} successfully deleted!`);
            setDeleteTarget(null);
        } catch (error) {
            notify.error(error, `Failed to delete Role ${role.name}`);
        }
    }

    return (
        <div className="space-y-6">
            <h1 className="text-2xl font-semibold tracking-tight">Roles</h1>

            {groups.map(group => (
                <RolesByScopeSection
                    key={group.scope}
                    group={group}
                    canCreate={canCreate}
                    canDelete={canDelete}
                    canManageMembers={canManageMembers}
                    hasCustomRolesLicense={hasCustomRolesLicense}
                    onCreateRole={scope => navigate(`${scope}`)}
                    onSelectRole={(scope, roleName) => navigate(`${scope}/${roleName}`)}
                    onDeleteRole={(scope, role) => setDeleteTarget({ scope, role })}
                    onViewMembers={(scope, roleName) => navigate(`${scope}/${roleName}/members`)}
                    onShowLicenseDialog={() => setLicenseDialogOpen(true)}
                />
            ))}

            <RoleDeleteDialog
                open={deleteTarget !== null}
                role={deleteTarget?.role}
                onClose={() => setDeleteTarget(null)}
                onConfirm={handleConfirmDelete}
                isDeleting={deleteMutation.isPending}
            />
            <CustomRolesLicenseDialog open={licenseDialogOpen} onOpenChange={setLicenseDialogOpen} />
        </div>
    );
}
