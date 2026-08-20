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

import { useMemo, useState } from 'react';

import type { GroupMember, GroupMembershipPayload } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { getMemberRoleLockFlags, sortedSuccessorCandidates, type MemberRoleSelections, type RoleField } from '../utils/memberRoles';
import {
    analyzeEditOwnershipTransfer,
    buildEditMembershipPayloads,
    buildEditOwnershipTransferMessage,
    isPrimaryOwnerUnavailable,
} from '../utils/primaryOwnership';

const PRIMARY_OWNER_DISABLED_OPTIONS = new Set([PRIMARY_OWNER_ROLE]);

function roleSelectionsFromMember(member: GroupMember | undefined): Record<RoleField, string> {
    return {
        apiRole: member?.roles?.API ?? '',
        apiProductRole: member?.roles?.API_PRODUCT ?? '',
        applicationRole: member?.roles?.APPLICATION ?? '',
        integrationRole: member?.roles?.INTEGRATION ?? '',
        clusterRole: member?.roles?.CLUSTER ?? '',
        explorerRole: member?.roles?.EXPLORER ?? '',
    };
}

export function useGroupEditMemberForm({
    member,
    members,
    lockApiRole,
    lockApiProductRole,
    lockApplicationRole,
    canOverrideLocks,
    apiPrimaryOwnerMode,
    apiProductPrimaryOwnerMode,
    onSubmit,
}: {
    member: GroupMember | undefined;
    members: GroupMember[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    apiPrimaryOwnerMode: string | undefined;
    apiProductPrimaryOwnerMode: string | undefined;
    onSubmit: (memberships: GroupMembershipPayload[]) => Promise<void>;
}) {
    const [roleValues, setRoleValues] = useState<Record<RoleField, string>>(() => roleSelectionsFromMember(member));
    const [groupAdmin, setGroupAdmin] = useState(() => member?.roles?.GROUP === 'ADMIN');
    const [selectedSuccessorId, setSelectedSuccessorId] = useState<string | null>(null);

    const successorCandidates = useMemo(() => sortedSuccessorCandidates(members, member?.id), [members, member]);
    const roleLocks = getMemberRoleLockFlags({ lockApiRole, lockApiProductRole, lockApplicationRole }, canOverrideLocks);

    const transfer = member ? analyzeEditOwnershipTransfer(member, members, roleValues) : null;
    const selectedSuccessor = selectedSuccessorId ? (successorCandidates.find(m => m.id === selectedSuccessorId) ?? null) : null;
    const transferMessage = member && transfer ? buildEditOwnershipTransferMessage(member, transfer, selectedSuccessor, roleValues) : null;

    const disabledOptionNames = {
        api: isPrimaryOwnerUnavailable(apiPrimaryOwnerMode) ? PRIMARY_OWNER_DISABLED_OPTIONS : undefined,
        apiProduct: isPrimaryOwnerUnavailable(apiProductPrimaryOwnerMode) ? PRIMARY_OWNER_DISABLED_OPTIONS : undefined,
    };

    function handleRoleChange(field: RoleField, value: string) {
        setRoleValues(previous => ({ ...previous, [field]: value }));
        if (field !== 'explorerRole') {
            setSelectedSuccessorId(null);
        }
    }

    const canSubmit = !transfer?.needsSuccessor || Boolean(selectedSuccessor);

    async function handleSubmit() {
        if (!member) return;
        if (!transfer) return;
        if (!canSubmit) return;
        const selections: MemberRoleSelections = { ...roleValues, groupAdmin };
        await onSubmit(buildEditMembershipPayloads(member, selections, transfer, selectedSuccessor));
    }

    return {
        roleValues,
        roleLocks,
        disabledOptionNames,
        groupAdmin,
        setGroupAdmin,
        successorCandidates,
        selectedSuccessor,
        setSelectedSuccessorId,
        transfer,
        transferMessage,
        handleRoleChange,
        canSubmit,
        handleSubmit,
    };
}
