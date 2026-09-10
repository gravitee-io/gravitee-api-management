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
    Badge,
    Button,
    cn,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
} from '@gravitee/graphene-core';
import { LockIcon, MoreVerticalIcon, PlusIcon, Trash2Icon, UsersIcon } from '@gravitee/graphene-core/icons';

import { SectionError } from '../../../shared/components/SectionError';
import type { RolesByScopeGroup } from '../hooks/useRoles';
import type { Role, RoleScope } from '../types/role';
import { canRoleBeDeleted } from '../utils/rolePermissions';
import { getRoleScopeIcon } from '../utils/roleScopeIcon';

interface RoleRowActionsProps {
    role: Role;
    scope: RoleScope;
    canSeeMembers: boolean;
    canDeleteRole: boolean;
    onViewMembers: (scope: RoleScope, roleName: string) => void;
    onDeleteRole: (scope: RoleScope, role: Role) => void;
}

/** Card-list row actions: always a "..." dropdown with the actions this role allows, each paired with its icon. */
function RoleRowActions({ role, scope, canSeeMembers, canDeleteRole, onViewMembers, onDeleteRole }: Readonly<RoleRowActionsProps>) {
    if (!canSeeMembers && !canDeleteRole) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label={`Actions for ${role.name}`}>
                    <MoreVerticalIcon className="size-4" aria-hidden />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {canSeeMembers ? (
                    <DropdownMenuItem onSelect={() => onViewMembers(scope, role.name)}>
                        <UsersIcon className="size-4 shrink-0" aria-hidden />
                        <span className="whitespace-nowrap">View members</span>
                    </DropdownMenuItem>
                ) : null}
                {canSeeMembers && canDeleteRole ? <DropdownMenuSeparator /> : null}
                {canDeleteRole ? (
                    <DropdownMenuItem variant="destructive" onSelect={() => onDeleteRole(scope, role)}>
                        <Trash2Icon className="size-4 shrink-0" aria-hidden />
                        <span className="whitespace-nowrap">Delete</span>
                    </DropdownMenuItem>
                ) : null}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

export function RolesByScopeSection({
    group,
    canCreate,
    canDelete,
    canManageMembers,
    hasCustomRolesLicense,
    onCreateRole,
    onSelectRole,
    onDeleteRole,
    onViewMembers,
    onShowLicenseDialog,
}: Readonly<{
    group: RolesByScopeGroup;
    canCreate: boolean;
    canDelete: boolean;
    canManageMembers: boolean;
    hasCustomRolesLicense: boolean;
    onCreateRole: (scope: RoleScope) => void;
    onSelectRole: (scope: RoleScope, roleName: string) => void;
    onDeleteRole: (scope: RoleScope, role: Role) => void;
    onViewMembers: (scope: RoleScope, roleName: string) => void;
    onShowLicenseDialog: () => void;
}>) {
    const ScopeIcon = getRoleScopeIcon(group.scope);

    function handleCreateClick() {
        if (hasCustomRolesLicense) {
            onCreateRole(group.scope);
        } else {
            onShowLicenseDialog();
        }
    }

    return (
        <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0">
                <div className="flex min-w-0 items-center gap-3">
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/5 text-primary">
                        <ScopeIcon className="size-4" aria-hidden />
                    </div>
                    <CardTitle className="text-base">{group.label}</CardTitle>
                </div>
                <div className="flex shrink-0 items-center">
                    {canCreate ? (
                        <Button size="sm" className="shrink-0" onClick={handleCreateClick}>
                            {hasCustomRolesLicense ? (
                                <PlusIcon className="size-4" aria-hidden />
                            ) : (
                                <LockIcon className="size-4" aria-hidden />
                            )}
                            Add a role
                        </Button>
                    ) : null}
                </div>
            </CardHeader>
            <CardContent className="px-0 pb-0">
                {group.isLoading ? (
                    <div className="space-y-2 px-6 pb-6">
                        <Skeleton className="h-10 w-full rounded-md" />
                        <Skeleton className="h-10 w-full rounded-md" />
                    </div>
                ) : group.isError ? (
                    <div className="px-6 pb-6">
                        <SectionError message="Failed to load roles for this scope. Please refresh and try again." />
                    </div>
                ) : group.roles.length === 0 ? (
                    <p className="px-6 pb-6 text-sm text-muted-foreground">No role</p>
                ) : (
                    // A plain list, not DataTable, is a deliberate Classic-faithful choice (Classic's roles
                    // page is a mat-list, not a table) — it costs this page a search affordance DataTable
                    // would have given for free on scopes with many roles. Revisit if that becomes a problem.
                    <ul>
                        {group.roles.map(role => {
                            const description = role.description?.trim();
                            return (
                                <li key={role.name} className="flex items-stretch border-t border-border transition-colors hover:bg-muted">
                                    <button
                                        type="button"
                                        className={cn(
                                            'min-h-16 min-w-0 flex-1 px-6 py-3 text-left',
                                            description ? undefined : 'flex items-center',
                                        )}
                                        onClick={() => onSelectRole(group.scope, role.name)}
                                    >
                                        <div className="min-w-0">
                                            <div className="flex min-w-0 items-center gap-2">
                                                <span className="min-w-0 shrink truncate text-sm font-medium">{role.name}</span>
                                                {role.system ? (
                                                    <Badge variant="secondary" className="h-5 shrink-0 px-1.5 text-xs font-normal">
                                                        System
                                                    </Badge>
                                                ) : null}
                                                {role.default ? (
                                                    <Badge variant="highlight" className="shrink-0">
                                                        Default
                                                    </Badge>
                                                ) : null}
                                            </div>
                                            {description ? (
                                                <span className="mt-1 block truncate text-xs text-muted-foreground">{description}</span>
                                            ) : null}
                                        </div>
                                    </button>
                                    <div className="flex shrink-0 items-center pr-4">
                                        <RoleRowActions
                                            role={role}
                                            scope={group.scope}
                                            canSeeMembers={canManageMembers && group.scope === 'ORGANIZATION'}
                                            canDeleteRole={canDelete && canRoleBeDeleted(role)}
                                            onViewMembers={onViewMembers}
                                            onDeleteRole={onDeleteRole}
                                        />
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </CardContent>
        </Card>
    );
}
