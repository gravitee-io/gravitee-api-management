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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { RoleForm, type RoleFormSubmitValues } from '../features/roles/components/RoleForm';
import { useCreateRole, useUpdateRole } from '../features/roles/hooks/useRoleMutations';
import { usePermissionsByScopes, useRole } from '../features/roles/hooks/useRoles';
import { CUSTOM_ROLES_LICENSE_FEATURE } from '../features/roles/license/customRolesLicense';
import { isPermissionEligibleScope, isRoleReadOnly, isRoleScope } from '../features/roles/utils/rolePermissions';
import { ROLE_SCOPE_LABELS } from '../features/roles/utils/roleScopeLabels';
import { SectionError } from '../shared/components/SectionError';
import { useConsoleSettings } from '../shared/console-settings';
import { notify } from '../shared/notify';

export function RoleFormPage() {
    const { roleScope, roleName } = useParams<{ roleScope: string; roleName?: string }>();
    const navigate = useNavigate();
    const isEditMode = Boolean(roleName);
    // undefined for an invalid param keeps useRole's fetch disabled below; hooks must still run
    // unconditionally on every render, so the redirect itself happens after them, not before.
    const validScope = isRoleScope(roleScope) ? roleScope : undefined;

    const { data: role, isLoading: isRoleLoading, isError: isRoleError } = useRole(validScope, roleName);
    const { data: permissionsByScopes, isLoading: arePermissionsLoading } = usePermissionsByScopes();
    const settings = useConsoleSettings();
    const hasCustomRolesLicense = useHasFeature(CUSTOM_ROLES_LICENSE_FEATURE);

    const createMutation = useCreateRole();
    const updateMutation = useUpdateRole();

    // A route param outside ROLE_SCOPES (e.g. a hand-edited URL) would otherwise render an "undefined scope"
    // form and could POST to /rolescopes/<garbage>/roles — send it back to the list instead.
    if (!validScope) {
        return <Navigate to=".." replace />;
    }
    const scope = validScope;

    // Mirrors organization-settings-routing.module.ts: only the create route (role/:roleScope) requires the
    // license and redirects back to the list; the edit and members routes do not.
    if (!isEditMode && !hasCustomRolesLicense) {
        return <Navigate to=".." replace />;
    }

    if (isRoleLoading || arePermissionsLoading) {
        return <Skeleton className="h-64 w-full rounded-md" />;
    }

    // Surface a failed role fetch instead of falling through to RoleForm, which would otherwise treat a
    // missing `role` as create mode and render editable fields with a create button.
    if (isEditMode && isRoleError) {
        return <SectionError message="Failed to load this role. Please refresh and try again." />;
    }

    // EXPLORER/AI_WORKSPACE have no permissions-by-scope entry (Angular's getPermissionsByScope throws for
    // them, so Classic's create form never renders for these scopes at all). Rendering an empty, still-usable
    // matrix here is new capability, not a port of existing behavior — flag it as such in review, since it's
    // a product decision (whether custom roles should be creatable in these scopes) rather than a parity fix.
    // See PR discussion; do not treat this comment's presence alone as product sign-off.
    const permissionNames = isPermissionEligibleScope(scope) ? (permissionsByScopes?.[scope] ?? []) : [];
    const systemRoleEditionEnabled = Boolean(settings?.management?.systemRoleEdition?.enabled);
    const isReadOnly = isRoleReadOnly(role ?? { scope, name: roleName ?? '', system: false }, systemRoleEditionEnabled);
    const isSaving = createMutation.isPending || updateMutation.isPending;

    // ":roleScope" and ":roleScope/:roleName" are each a single flat route directly under "roles" (not
    // nested `<Route>`s), so both sit at the same match depth — one ".." always reaches "roles" regardless
    // of how many URL segments the current route's own path pattern has.
    function goBack() {
        navigate('..');
    }

    async function handleSubmit(values: RoleFormSubmitValues) {
        if (isEditMode) {
            if (!role) {
                // The role fetch settled without data (e.g. a 404) — fail loudly instead of silently falling
                // through to the create path below and creating an unrelated role under this name.
                throw new Error('Role data is unavailable for editing.');
            }
            await updateMutation.mutateAsync({ ...role, ...values, scope });
            notify.success('Role successfully saved!');
            return;
        }
        const created = await createMutation.mutateAsync({ ...values, scope, system: false });
        notify.success('Role successfully saved!');
        navigate(created.name, { replace: true });
    }

    return (
        <div className="space-y-4">
            <div>
                <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={goBack}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to roles
                </Button>
                <h1 className="text-2xl font-semibold tracking-tight">
                    {isEditMode ? 'Update' : 'Create'} role in the {ROLE_SCOPE_LABELS[scope]} scope
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    {isEditMode
                        ? `Manage CRUD (Create, Read, Update, Delete) permissions for this role ${roleName} in the ${ROLE_SCOPE_LABELS[scope]} scope.`
                        : `Manage CRUD (Create, Read, Update, Delete) permissions for this role in the ${ROLE_SCOPE_LABELS[scope]} scope.`}
                </p>
            </div>
            <RoleForm
                key={`${scope}-${roleName ?? 'new'}`}
                scope={scope}
                role={role}
                permissionNames={permissionNames}
                isReadOnly={isReadOnly}
                isSaving={isSaving}
                onSubmit={handleSubmit}
                onCancel={goBack}
            />
        </div>
    );
}
