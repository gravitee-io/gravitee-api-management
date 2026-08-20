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

import { Alert, AlertDescription } from '@gravitee/graphene-core';
import { InfoIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState, useTransition } from 'react';

import { MemberSuccessorCombobox } from './MemberSuccessorCombobox';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useOpenRemountKey } from '../../../shared/hooks/useOpenRemountKey';
import type { GroupMember, GroupMembershipPayload } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { sortedSuccessorCandidates } from '../utils/memberRoles';
import { joinScopeLabels, membershipFromMember, primaryOwnerScopesOf } from '../utils/primaryOwnership';

type GroupRemoveMemberDialogProps = Readonly<{
    open: boolean;
    member: GroupMember | undefined;
    members: GroupMember[];
    groupName: string;
    onClose: () => void;
    onConfirm: (transferMembership?: GroupMembershipPayload) => Promise<void>;
}>;

export function GroupRemoveMemberDialog(props: GroupRemoveMemberDialogProps) {
    const resetKey = useOpenRemountKey(props.open, props.member?.id ?? '');
    return <GroupRemoveMemberDialogContent key={resetKey} {...props} />;
}

function GroupRemoveMemberDialogContent({ open, member, members, groupName, onClose, onConfirm }: GroupRemoveMemberDialogProps) {
    const [successor, setSuccessor] = useState<GroupMember | null>(null);
    const [isRemoving, startRemoveTransition] = useTransition();

    const primaryOwnerScopes = useMemo(() => primaryOwnerScopesOf(member), [member]);
    const isPrimaryOwner = primaryOwnerScopes.length > 0;
    const candidates = useMemo(() => sortedSuccessorCandidates(members, member?.id), [members, member]);

    if (!member) return null;

    const scopesLabel = joinScopeLabels(primaryOwnerScopes);
    const transferMessage = successor
        ? `${member.displayName} is the ${scopesLabel} primary owner. ${scopesLabel} primary ownership will be transferred from ${member.displayName} to ${successor.displayName}.`
        : null;

    const canConfirm = !isPrimaryOwner || Boolean(successor);

    function handleConfirm() {
        if (isPrimaryOwner && !successor) {
            return;
        }

        const transferMembership = successor
            ? membershipFromMember(successor, Object.fromEntries(primaryOwnerScopes.map(scope => [scope, PRIMARY_OWNER_ROLE])))
            : undefined;
        startRemoveTransition(async () => {
            await onConfirm(transferMembership);
        });
    }

    return (
        <ConfirmDialog
            open={open}
            onOpenChange={isOpen => !isOpen && !isRemoving && onClose()}
            title={
                <span className="flex items-center gap-2">
                    <TriangleAlertIcon className="size-5 shrink-0 text-destructive" aria-hidden />
                    Remove member?
                </span>
            }
            description={
                <>
                    Are you sure you want to remove <span className="font-medium text-foreground">{member.displayName}</span> from{' '}
                    <span className="font-medium text-foreground">{groupName}</span>?
                </>
            }
            confirmLabel="Remove"
            pendingLabel="Removing…"
            destructive
            isPending={isRemoving}
            confirmDisabled={!canConfirm}
            onConfirm={handleConfirm}
        >
            {isPrimaryOwner && (
                <div className="space-y-3 px-6">
                    <MemberSuccessorCombobox
                        id="remove-member-successor"
                        candidates={candidates}
                        value={successor}
                        onChange={setSuccessor}
                        hint="Select a member to transfer primary ownership."
                        disabled={isRemoving}
                    />
                    {transferMessage && (
                        <Alert variant="default">
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>{transferMessage}</AlertDescription>
                        </Alert>
                    )}
                </div>
            )}
        </ConfirmDialog>
    );
}
