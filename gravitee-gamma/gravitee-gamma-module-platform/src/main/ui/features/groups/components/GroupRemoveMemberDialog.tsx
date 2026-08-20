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
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState, useTransition } from 'react';

import { MemberOwnershipTransferField } from './MemberOwnershipTransferField';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useOpenRemountKey } from '../../../shared/hooks/useOpenRemountKey';
import type { GroupMember } from '../types/group';
import { sortedSuccessorCandidates } from '../utils/memberRoles';
import {
    blockedPrimaryOwnerScopes,
    buildPrimaryOwnerAssociationBlockMessage,
    buildRemovalOwnershipTransfer,
    buildRemovalOwnershipTransferMessage,
    requiresPrimaryOwnerSuccessor,
    type RemovalOwnershipTransfer,
} from '../utils/primaryOwnership';

type GroupRemoveMemberDialogProps = Readonly<{
    open: boolean;
    member: GroupMember | undefined;
    members: GroupMember[];
    groupName: string;
    associatedApiCount: number | null;
    associatedApiProductCount: number | null;
    onClose: () => void;
    onConfirm: (ownershipTransfer?: RemovalOwnershipTransfer) => Promise<void>;
}>;

export function GroupRemoveMemberDialog(props: GroupRemoveMemberDialogProps) {
    const resetKey = useOpenRemountKey(props.open, props.member?.id ?? '');
    return <GroupRemoveMemberDialogContent key={resetKey} {...props} />;
}

function GroupRemoveMemberDialogContent({
    open,
    member,
    members,
    groupName,
    associatedApiCount,
    associatedApiProductCount,
    onClose,
    onConfirm,
}: GroupRemoveMemberDialogProps) {
    const [successor, setSuccessor] = useState<GroupMember | null>(null);
    const [isRemoving, startRemoveTransition] = useTransition();

    const needsSuccessor = requiresPrimaryOwnerSuccessor(member);
    const candidates = useMemo(() => sortedSuccessorCandidates(members, member?.id), [members, member]);

    if (!member) return null;

    const blockedScopes = blockedPrimaryOwnerScopes(member, {
        apiCount: associatedApiCount,
        apiProductCount: associatedApiProductCount,
    });
    const ownershipBlockMessage = buildPrimaryOwnerAssociationBlockMessage(
        blockedScopes,
        { apiCount: associatedApiCount, apiProductCount: associatedApiProductCount },
        'remove',
    );
    const transferMessage = successor ? buildRemovalOwnershipTransferMessage(member, successor) : null;
    const canConfirm = !ownershipBlockMessage && (!needsSuccessor || Boolean(successor));

    function handleConfirm() {
        if (!member || (needsSuccessor && !successor)) {
            return;
        }

        const ownershipTransfer = successor ? buildRemovalOwnershipTransfer(member, successor) : undefined;
        startRemoveTransition(async () => {
            await onConfirm(ownershipTransfer);
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
            {ownershipBlockMessage ? (
                <Alert variant="default">
                    <TriangleAlertIcon className="size-4" aria-hidden />
                    <AlertDescription>{ownershipBlockMessage}</AlertDescription>
                </Alert>
            ) : null}
            {needsSuccessor && !ownershipBlockMessage ? (
                <MemberOwnershipTransferField
                    id="remove-member-successor"
                    candidates={candidates}
                    value={successor}
                    onChange={setSuccessor}
                    message={transferMessage}
                    disabled={isRemoving}
                />
            ) : null}
        </ConfirmDialog>
    );
}
