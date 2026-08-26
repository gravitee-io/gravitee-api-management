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
import { Checkbox, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';

import type { RoleRight, RoleScope } from '../types/role';
import { isPermissionMovedToOrganizationScope, type RolePermissionsForm } from '../utils/rolePermissions';

const ROLE_RIGHTS_CONFIG: readonly { key: RoleRight; label: string }[] = [
    { key: 'C', label: 'Create' },
    { key: 'R', label: 'Read' },
    { key: 'U', label: 'Update' },
    { key: 'D', label: 'Delete' },
];

function computeSelectAllState(
    value: RolePermissionsForm,
    right: RoleRight,
    permissionNames: readonly string[],
    scope: RoleScope,
): boolean | 'indeterminate' {
    // Mirror toggleAll's exclusion below: a permission moved to ORGANIZATION scope can never be checked here,
    // so it must not count against "all selected" either — otherwise select-all could never reach `true`.
    const manageablePermissionNames = permissionNames.filter(permission => !isPermissionMovedToOrganizationScope(scope, permission));
    if (manageablePermissionNames.length === 0) {
        return false;
    }
    const checkedCount = manageablePermissionNames.filter(permission => value[permission]?.[right]).length;
    if (checkedCount === 0) return false;
    if (checkedCount === manageablePermissionNames.length) return true;
    return 'indeterminate';
}

export function RolePermissionsTable({
    scope,
    permissionNames,
    value,
    onChange,
    disabled = false,
}: Readonly<{
    scope: RoleScope;
    permissionNames: readonly string[];
    value: RolePermissionsForm;
    onChange: (value: RolePermissionsForm) => void;
    disabled?: boolean;
}>) {
    function toggleAll(right: RoleRight, checked: boolean) {
        const next: RolePermissionsForm = { ...value };
        permissionNames.forEach(permission => {
            if (disabled || isPermissionMovedToOrganizationScope(scope, permission)) {
                return;
            }
            next[permission] = { ...next[permission], [right]: checked };
        });
        onChange(next);
    }

    function toggleCell(permission: string, right: RoleRight, checked: boolean) {
        onChange({ ...value, [permission]: { ...value[permission], [right]: checked } });
    }

    if (permissionNames.length === 0) {
        return <p className="text-sm text-muted-foreground">No permissions can be managed for this scope yet.</p>;
    }

    return (
        <Table id="rolePermissionsTable" aria-label="Role CRUD permissions table">
            <TableHeader>
                <TableRow>
                    <TableHead scope="col">Permission</TableHead>
                    {ROLE_RIGHTS_CONFIG.map(right => {
                        const state = computeSelectAllState(value, right.key, permissionNames, scope);
                        const isIndeterminate = state === 'indeterminate';
                        return (
                            <TableHead key={right.key} scope="col">
                                <div className="flex flex-col items-start gap-1">
                                    <span>{right.label}</span>
                                    {/* The design-system Checkbox renders the same check glyph for "checked" and
                                        "indeterminate", which makes a partial selection look identical to "all
                                        selected". Force a visibly distinct treatment for the indeterminate case:
                                        a muted/outlined box with our own dash mark instead of the check icon.
                                        This overrides Graphene internals via nested selectors, which graphene.md
                                        lists as a mistake to avoid — the real fix belongs upstream as a Graphene
                                        `indeterminate` visual variant. Treat this as a temporary, referenced
                                        workaround until that lands, not a pattern to copy elsewhere. */}
                                    <div className="relative inline-flex">
                                        <Checkbox
                                            checked={state}
                                            disabled={disabled}
                                            onCheckedChange={checked => toggleAll(right.key, checked === true)}
                                            aria-label={
                                                isIndeterminate
                                                    ? `Some ${right.label} permissions selected — select all ${right.label}`
                                                    : `${state === true ? 'Deselect' : 'Select'} all ${right.label}`
                                            }
                                            className={
                                                isIndeterminate
                                                    ? 'border-primary bg-background data-[state=indeterminate]:bg-background [&_svg]:invisible'
                                                    : undefined
                                            }
                                        />
                                        {isIndeterminate ? (
                                            <span
                                                aria-hidden="true"
                                                className="pointer-events-none absolute inset-0 m-auto h-[2px] w-2 rounded-full bg-primary"
                                            />
                                        ) : null}
                                    </div>
                                </div>
                            </TableHead>
                        );
                    })}
                </TableRow>
            </TableHeader>
            <TableBody>
                {permissionNames.map(permission => {
                    const moved = isPermissionMovedToOrganizationScope(scope, permission);
                    return (
                        <TableRow key={permission}>
                            <TableCell>
                                <span>{permission}</span>
                                {moved ? (
                                    <div className="text-xs text-muted-foreground">
                                        This permission has been moved to ORGANIZATION scope
                                    </div>
                                ) : null}
                            </TableCell>
                            {ROLE_RIGHTS_CONFIG.map(right => (
                                <TableCell key={right.key}>
                                    <Checkbox
                                        checked={Boolean(value[permission]?.[right.key])}
                                        disabled={disabled || moved}
                                        onCheckedChange={checked => toggleCell(permission, right.key, checked === true)}
                                        aria-label={`${right.label} permission for ${permission}`}
                                    />
                                </TableCell>
                            ))}
                        </TableRow>
                    );
                })}
            </TableBody>
        </Table>
    );
}
