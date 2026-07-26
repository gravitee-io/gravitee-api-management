/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { AddConsolePrincipalDialog } from './AddConsolePrincipalDialog';
import { notify } from '../../../shared/notify/notify';
import { MOCK_CONSOLE_PRINCIPALS } from '../storage/mock-console-principals';
import {
    CONSOLE_DOC_ROLE_LABELS,
    CONSOLE_DOC_ROLES,
    PORTAL_GRANT_SCOPE_TYPE_LABELS,
    type ConsoleDocGrant,
    type ConsoleDocGrantInput,
    type ConsoleDocRole,
    type PortalGrantScopeType,
} from '../types/permissions.types';

interface ConsoleAuthoringGrantsTableProps {
    readonly grants: readonly ConsoleDocGrant[];
    /** When set, new grants target this scope and the scope column is hidden. */
    readonly fixedScope?: { scopeType: PortalGrantScopeType; scopeId: string; scopeName: string };
    readonly scopeOptions?: readonly { scopeType: PortalGrantScopeType; id: string; name: string }[];
    readonly scopeLabelFor: (scopeType: PortalGrantScopeType, scopeId: string) => string;
    readonly readOnly?: boolean;
    readonly onAdd: (input: ConsoleDocGrantInput) => Promise<void>;
    readonly onRoleChange: (grantId: string, role: ConsoleDocRole) => Promise<void>;
    readonly onRemove: (grantId: string) => Promise<void>;
}

export function ConsoleAuthoringGrantsTable({
    grants,
    fixedScope,
    scopeOptions = [],
    scopeLabelFor,
    readOnly = false,
    onAdd,
    onRoleChange,
    onRemove,
}: ConsoleAuthoringGrantsTableProps) {
    const [addOpen, setAddOpen] = useState(false);

    const principalById = useMemo(
        () => new Map(MOCK_CONSOLE_PRINCIPALS.map(principal => [principal.id, principal])),
        [],
    );

    const takenPrincipalIds = useMemo(
        () =>
            new Set(
                grants
                    .filter(grant => !fixedScope || grant.scopeId === fixedScope.scopeId)
                    .map(grant => grant.principalId),
            ),
        [fixedScope, grants],
    );

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-muted-foreground">
                    Console users and teams that may author documentation
                    {fixedScope ? ` for ${fixedScope.scopeName}` : ''}.
                </p>
                <Button type="button" disabled={readOnly} onClick={() => setAddOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add user or team
                </Button>
            </div>

            <div className="overflow-hidden rounded-lg border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>User or team</TableHead>
                            {!fixedScope && <TableHead className="w-56">Scope</TableHead>}
                            <TableHead className="w-56">Role</TableHead>
                            <TableHead className="w-12" />
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {grants.length === 0 ? (
                            <TableRow>
                                <TableCell
                                    colSpan={fixedScope ? 3 : 4}
                                    className="py-8 text-center text-muted-foreground"
                                >
                                    Nobody has authoring access yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            grants.map(grant => {
                                const principal = principalById.get(grant.principalId);

                                return (
                                    <TableRow key={grant.id}>
                                        <TableCell>
                                            <span className="flex items-center gap-2">
                                                <span className="font-medium">
                                                    {principal?.name ?? grant.principalId}
                                                </span>
                                                <Badge variant="outline">
                                                    {grant.principalType === 'TEAM' ? 'Team' : 'User'}
                                                </Badge>
                                            </span>
                                            {principal?.email && (
                                                <span className="block text-muted-foreground">
                                                    {principal.email}
                                                </span>
                                            )}
                                        </TableCell>
                                        {!fixedScope && (
                                            <TableCell className="text-muted-foreground">
                                                {scopeLabelFor(grant.scopeType, grant.scopeId)}
                                                <span className="block text-xs">
                                                    {PORTAL_GRANT_SCOPE_TYPE_LABELS[grant.scopeType]}
                                                </span>
                                            </TableCell>
                                        )}
                                        <TableCell>
                                            <Select
                                                value={grant.role}
                                                disabled={readOnly}
                                                onValueChange={value => {
                                                    void onRoleChange(
                                                        grant.id,
                                                        value as ConsoleDocRole,
                                                    ).then(() => notify.success('Role updated.'));
                                                }}
                                            >
                                                <SelectTrigger
                                                    className="h-8"
                                                    aria-label={`Role of ${principal?.name ?? grant.principalId}`}
                                                >
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {CONSOLE_DOC_ROLES.map(role => (
                                                        <SelectItem key={role} value={role}>
                                                            {CONSOLE_DOC_ROLE_LABELS[role].title}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <span className="mt-1 block text-xs text-muted-foreground">
                                                {CONSOLE_DOC_ROLE_LABELS[grant.role].description}
                                            </span>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                aria-label={`Remove ${principal?.name ?? grant.principalId}`}
                                                disabled={readOnly}
                                                onClick={() => {
                                                    void onRemove(grant.id).then(() =>
                                                        notify.success('Authoring access removed.'),
                                                    );
                                                }}
                                            >
                                                <Trash2Icon className="size-4" aria-hidden />
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                );
                            })
                        )}
                    </TableBody>
                </Table>
            </div>

            <AddConsolePrincipalDialog
                open={addOpen}
                fixedScope={fixedScope}
                scopeOptions={scopeOptions}
                takenPrincipalIds={takenPrincipalIds}
                onOpenChange={setAddOpen}
                onSubmit={input => {
                    void onAdd(input).then(() => notify.success('Authoring access granted.'));
                }}
            />
        </div>
    );
}
