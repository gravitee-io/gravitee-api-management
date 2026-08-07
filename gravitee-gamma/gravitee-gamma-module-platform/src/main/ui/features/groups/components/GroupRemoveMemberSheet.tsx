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
    Alert,
    AlertDescription,
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import type { GroupMember, GroupMembershipPayload, GroupMembershipRole } from '../types/group';

const PRIMARY_OWNER = 'PRIMARY_OWNER';
const PRIMARY_OWNER_SCOPES = ['API', 'API_PRODUCT'] as const;
type PrimaryOwnerScope = (typeof PRIMARY_OWNER_SCOPES)[number];

const SCOPE_LABELS: Readonly<Record<PrimaryOwnerScope, string>> = {
    API: 'API',
    API_PRODUCT: 'API Product',
};

/** Rebuilds the successor's full membership, preserving their existing roles and promoting them to
 *  PRIMARY_OWNER for whichever scope(s) the removed member held it in (mirrors delete-member-dialog
 *  .component.ts's mapGroupMembership()). */
function buildTransferMembership(successor: GroupMember, scopes: PrimaryOwnerScope[]): GroupMembershipPayload {
    const merged: Record<string, string> = { ...(successor.roles ?? {}) };
    scopes.forEach(scope => {
        merged[scope] = PRIMARY_OWNER;
    });
    const roles: GroupMembershipRole[] = Object.entries(merged)
        .filter((entry): entry is [string, string] => Boolean(entry[1]))
        .map(([scope, name]) => ({ scope: scope as GroupMembershipRole['scope'], name }));
    return { id: successor.id, roles };
}

export function GroupRemoveMemberSheet({
    open,
    member,
    members,
    groupName,
    onClose,
    onConfirm,
    isRemoving,
}: Readonly<{
    open: boolean;
    member: GroupMember | undefined;
    /** Full group membership list — searched for a successor when `member` is a primary owner. The
     *  backend rejects removing a member who's still PRIMARY_OWNER for a scope the group owns items in
     *  (StillPrimaryOwnerException / StillApiProductPrimaryOwnerException, same guard as role edits), so
     *  mirroring classic's delete-member-dialog.component.ts: force picking a successor first and submit
     *  the reassignment as a follow-up request once the removal itself succeeds. */
    members: GroupMember[];
    groupName: string;
    onClose: () => void;
    onConfirm: (transferMembership?: GroupMembershipPayload) => void;
    isRemoving: boolean;
}>) {
    const [search, setSearch] = useState('');
    const [successor, setSuccessor] = useState<GroupMember | null>(null);

    useEffect(() => {
        if (open) {
            setSearch('');
            setSuccessor(null);
        }
    }, [open, member]);

    const primaryOwnerScopes = useMemo(() => PRIMARY_OWNER_SCOPES.filter(scope => member?.roles?.[scope] === PRIMARY_OWNER), [member]);
    const isPrimaryOwner = primaryOwnerScopes.length > 0;

    const candidates = useMemo(() => {
        if (!isPrimaryOwner || !search.trim() || !member) return [];
        const term = search.toLowerCase();
        return members.filter(m => m.id !== member.id && m.displayName.toLowerCase().includes(term));
    }, [members, member, search, isPrimaryOwner]);

    if (!member) return null;

    const scopesLabel = primaryOwnerScopes.map(scope => SCOPE_LABELS[scope]).join(' and ');
    const transferMessage = successor
        ? `${member.displayName} is the ${scopesLabel} primary owner. Primary ownership of the group will be transferred from ${member.displayName} to ${successor.displayName}.`
        : null;

    const canConfirm = !isPrimaryOwner || Boolean(successor);

    function handleConfirm() {
        onConfirm(successor ? buildTransferMembership(successor, primaryOwnerScopes) : undefined);
    }

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Remove member</DialogTitle>
                    <DialogDescription>
                        Are you sure you want to remove <span className="font-medium text-foreground">{member.displayName}</span> from{' '}
                        <span className="font-medium text-foreground">{groupName}</span>? They will lose any access granted through this
                        group.
                    </DialogDescription>
                </DialogHeader>

                {isPrimaryOwner && (
                    <div className="space-y-3 px-6">
                        {successor ? (
                            <div className="flex items-center justify-between gap-2 rounded-lg border px-3 py-2">
                                <div className="flex min-w-0 items-center gap-2">
                                    <MemberAvatar name={successor.displayName} />
                                    <span className="truncate text-sm font-medium">{successor.displayName}</span>
                                </div>
                                <Button type="button" variant="ghost" size="sm" onClick={() => setSuccessor(null)}>
                                    Change
                                </Button>
                            </div>
                        ) : (
                            <>
                                <Input
                                    placeholder="Search for a new primary owner…"
                                    value={search}
                                    onChange={e => setSearch(e.target.value)}
                                />
                                {search.trim() && (
                                    <div className="max-h-40 overflow-auto rounded-lg border">
                                        {candidates.length === 0 ? (
                                            <p className="px-3 py-2 text-sm text-muted-foreground">No members found.</p>
                                        ) : (
                                            candidates.map(candidate => (
                                                <button
                                                    key={candidate.id}
                                                    type="button"
                                                    onClick={() => setSuccessor(candidate)}
                                                    className="flex w-full items-center gap-2 border-b px-3 py-2 last:border-b-0 hover:bg-muted/50"
                                                >
                                                    <MemberAvatar name={candidate.displayName} />
                                                    <span className="truncate text-sm">{candidate.displayName}</span>
                                                </button>
                                            ))
                                        )}
                                    </div>
                                )}
                            </>
                        )}
                        {transferMessage && (
                            <Alert variant="default">
                                <InfoIcon className="size-4" aria-hidden />
                                <AlertDescription>{transferMessage}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                )}

                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isRemoving}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={handleConfirm} disabled={isRemoving || !canConfirm}>
                        {isRemoving ? 'Removing…' : 'Remove'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
