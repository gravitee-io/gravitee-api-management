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
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { InfoIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { MemberSuccessorCombobox } from './MemberSuccessorCombobox';
import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupMemberRoleScope } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { sortedSuccessorCandidates } from '../utils/memberRoles';

const PRIMARY_OWNER_SCOPES: GroupMemberRoleScope[] = ['API', 'APPLICATION', 'API_PRODUCT', 'INTEGRATION', 'CLUSTER'];

const SCOPE_LABELS: Readonly<Record<string, string>> = {
    API: 'API',
    APPLICATION: 'Application',
    API_PRODUCT: 'API Product',
    INTEGRATION: 'Integration',
    CLUSTER: 'Cluster',
};

function joinScopeLabels(scopes: string[]): string {
    const labels = scopes.map(scope => SCOPE_LABELS[scope]);
    if (labels.length <= 1) return labels.join('');
    return `${labels.slice(0, -1).join(', ')} and ${labels[labels.length - 1]}`;
}

function buildTransferMembership(successor: GroupMember, scopes: string[]): GroupMembershipPayload {
    const merged: Record<string, string> = { ...(successor.roles ?? {}) };
    scopes.forEach(scope => {
        merged[scope] = PRIMARY_OWNER_ROLE;
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
    members: GroupMember[];
    groupName: string;
    onClose: () => void;
    onConfirm: (transferMembership?: GroupMembershipPayload) => void;
    isRemoving: boolean;
}>) {
    const [successor, setSuccessor] = useState<GroupMember | null>(null);

    useEffect(() => {
        if (open) {
            setSuccessor(null);
        }
    }, [open, member]);

    const primaryOwnerScopes = useMemo(() => PRIMARY_OWNER_SCOPES.filter(scope => member?.roles?.[scope] === PRIMARY_OWNER_ROLE), [member]);
    const isPrimaryOwner = primaryOwnerScopes.length > 0;

    const candidates = useMemo(() => sortedSuccessorCandidates(members, member?.id), [members, member]);

    if (!member) return null;

    const scopesLabel = joinScopeLabels(primaryOwnerScopes);
    const transferMessage = successor
        ? `${member.displayName} is the ${scopesLabel} primary owner. Primary ownership of the group will be transferred from ${member.displayName} to ${successor.displayName}.`
        : null;

    const canConfirm = !isPrimaryOwner || Boolean(successor);

    function handleConfirm() {
        onConfirm(successor ? buildTransferMembership(successor, primaryOwnerScopes) : undefined);
    }

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <TriangleAlertIcon className="size-5 shrink-0 text-destructive" aria-hidden />
                        Remove member?
                    </DialogTitle>
                    <DialogDescription>
                        Are you sure you want to remove <span className="font-medium text-foreground">{member.displayName}</span> from{' '}
                        <span className="font-medium text-foreground">{groupName}</span>?
                    </DialogDescription>
                </DialogHeader>

                {isPrimaryOwner && (
                    <div className="space-y-3 px-6">
                        <MemberSuccessorCombobox
                            id="remove-member-successor"
                            candidates={candidates}
                            value={successor}
                            onChange={setSuccessor}
                            hint="Select a member to transfer primary ownership."
                        />
                        {transferMessage && (
                            <Alert variant="default">
                                <InfoIcon className="size-4" aria-hidden />
                                <AlertDescription>{transferMessage}</AlertDescription>
                            </Alert>
                        )}
                    </div>
                )}

                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isRemoving}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" onClick={handleConfirm} disabled={isRemoving || !canConfirm}>
                        {isRemoving ? 'Removing…' : 'Remove'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
