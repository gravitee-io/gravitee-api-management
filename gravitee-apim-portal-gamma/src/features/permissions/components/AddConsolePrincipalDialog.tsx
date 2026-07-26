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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { MOCK_CONSOLE_PRINCIPALS } from '../storage/mock-console-principals';
import {
    CONSOLE_DOC_ROLE_LABELS,
    CONSOLE_DOC_ROLES,
    PORTAL_GRANT_SCOPE_TYPE_LABELS,
    type ConsoleDocGrantInput,
    type ConsoleDocRole,
    type PortalGrantScopeType,
} from '../types/permissions.types';

interface AddConsolePrincipalDialogProps {
    readonly open: boolean;
    readonly fixedScope?: { scopeType: PortalGrantScopeType; scopeId: string; scopeName: string };
    readonly scopeOptions: readonly { scopeType: PortalGrantScopeType; id: string; name: string }[];
    readonly takenPrincipalIds: ReadonlySet<string>;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (input: ConsoleDocGrantInput) => void;
}

export function AddConsolePrincipalDialog({
    open,
    fixedScope,
    scopeOptions,
    takenPrincipalIds,
    onOpenChange,
    onSubmit,
}: AddConsolePrincipalDialogProps) {
    const [principalId, setPrincipalId] = useState('');
    const [role, setRole] = useState<ConsoleDocRole>('AUTHOR');
    const [scopeKey, setScopeKey] = useState('');

    useEffect(() => {
        if (!open) {
            return;
        }
        setPrincipalId('');
        setRole('AUTHOR');
        setScopeKey(fixedScope ? `${fixedScope.scopeType}:${fixedScope.scopeId}` : '');
    }, [fixedScope, open]);

    const candidates = useMemo(
        () => MOCK_CONSOLE_PRINCIPALS.filter(principal => !takenPrincipalIds.has(principal.id)),
        [takenPrincipalIds],
    );

    const canSubmit = principalId.length > 0 && (fixedScope !== undefined || scopeKey.length > 0);

    const handleSubmit = () => {
        const principal = candidates.find(candidate => candidate.id === principalId);
        if (!principal || !canSubmit) {
            return;
        }

        const [scopeType, scopeId] = fixedScope
            ? [fixedScope.scopeType, fixedScope.scopeId]
            : (scopeKey.split(':') as [PortalGrantScopeType, string]);

        onSubmit({ principalType: principal.type, principalId: principal.id, scopeType, scopeId, role });
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Grant documentation access</DialogTitle>
                    <DialogDescription>
                        Console users and teams author documentation. They are a separate directory from portal
                        consumers.
                    </DialogDescription>
                </DialogHeader>

                <form
                    id="add-console-principal-form"
                    className="space-y-4"
                    onSubmit={event => {
                        event.preventDefault();
                        handleSubmit();
                    }}
                >
                    <Field>
                        <FieldLabel htmlFor="console-principal">User or team</FieldLabel>
                        <Select value={principalId} onValueChange={setPrincipalId}>
                            <SelectTrigger id="console-principal">
                                <SelectValue placeholder="Select a user or team" />
                            </SelectTrigger>
                            <SelectContent>
                                {candidates.map(principal => (
                                    <SelectItem key={principal.id} value={principal.id}>
                                        {principal.name}
                                        {principal.type === 'TEAM' ? ' (team)' : ''}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </Field>

                    {fixedScope ? (
                        <Field>
                            <FieldLabel>Scope</FieldLabel>
                            <div className="flex items-center gap-2 text-sm">
                                <span className="font-medium">{fixedScope.scopeName}</span>
                                <Badge variant="outline">
                                    {PORTAL_GRANT_SCOPE_TYPE_LABELS[fixedScope.scopeType]}
                                </Badge>
                            </div>
                        </Field>
                    ) : (
                        <Field>
                            <FieldLabel htmlFor="console-scope">Scope</FieldLabel>
                            <Select value={scopeKey} onValueChange={setScopeKey}>
                                <SelectTrigger id="console-scope">
                                    <SelectValue placeholder="Select a scope" />
                                </SelectTrigger>
                                <SelectContent>
                                    {scopeOptions.map(option => (
                                        <SelectItem
                                            key={`${option.scopeType}:${option.id}`}
                                            value={`${option.scopeType}:${option.id}`}
                                        >
                                            {option.name} · {PORTAL_GRANT_SCOPE_TYPE_LABELS[option.scopeType]}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </Field>
                    )}

                    <Field>
                        <FieldLabel htmlFor="console-role">Role</FieldLabel>
                        <Select value={role} onValueChange={value => setRole(value as ConsoleDocRole)}>
                            <SelectTrigger id="console-role">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {CONSOLE_DOC_ROLES.map(candidate => (
                                    <SelectItem key={candidate} value={candidate}>
                                        {CONSOLE_DOC_ROLE_LABELS[candidate].title}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <p className="text-xs text-muted-foreground">
                            {CONSOLE_DOC_ROLE_LABELS[role].description}
                        </p>
                    </Field>
                </form>

                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="submit" form="add-console-principal-form" disabled={!canSubmit}>
                        Grant access
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
