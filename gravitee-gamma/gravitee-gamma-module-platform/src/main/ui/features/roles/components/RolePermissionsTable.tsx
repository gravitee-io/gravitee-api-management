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

const PERMISSION_CHECKBOX_CLASS = 'rounded-sm';

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
                            <TableHead key={right.key} scope="col" className="w-28 text-center">
                                <div className="flex flex-col items-center gap-2 py-1">
                                    <span>{right.label}</span>
                                    {isIndeterminate ? (
                                        // Graphene Checkbox renders a check glyph for indeterminate, so partial column
                                        // selection looks fully checked. Use a matching control with a horizontal dash
                                        // (standard indeterminate UX) until Graphene ships a distinct variant.
                                        <button
                                            type="button"
                                            role="checkbox"
                                            aria-checked="mixed"
                                            disabled={disabled}
                                            aria-label={`Some ${right.label} permissions selected — select all ${right.label}`}
                                            className={`flex size-4 shrink-0 items-center justify-center border border-primary bg-primary p-0 shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 ${PERMISSION_CHECKBOX_CLASS}`}
                                            onClick={() => toggleAll(right.key, true)}
                                        >
                                            <span
                                                aria-hidden="true"
                                                className="block shrink-0 rounded-none bg-primary-foreground"
                                                style={{ height: 2, width: 10 }}
                                            />
                                        </button>
                                    ) : (
                                        <Checkbox
                                            checked={state === true}
                                            disabled={disabled}
                                            onCheckedChange={checked => toggleAll(right.key, checked === true)}
                                            aria-label={`${state === true ? 'Deselect' : 'Select'} all ${right.label}`}
                                            className={PERMISSION_CHECKBOX_CLASS}
                                        />
                                    )}
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
                            <TableCell className="py-3">
                                <span className="text-sm font-medium">{permission}</span>
                                {moved ? (
                                    <div className="text-xs text-muted-foreground">
                                        This permission has been moved to ORGANIZATION scope
                                    </div>
                                ) : null}
                            </TableCell>
                            {ROLE_RIGHTS_CONFIG.map(right => (
                                <TableCell key={right.key} className="py-3 text-center">
                                    <Checkbox
                                        checked={Boolean(value[permission]?.[right.key])}
                                        disabled={disabled || moved}
                                        onCheckedChange={checked => toggleCell(permission, right.key, checked === true)}
                                        aria-label={`${right.label} permission for ${permission}`}
                                        className={PERMISSION_CHECKBOX_CLASS}
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
