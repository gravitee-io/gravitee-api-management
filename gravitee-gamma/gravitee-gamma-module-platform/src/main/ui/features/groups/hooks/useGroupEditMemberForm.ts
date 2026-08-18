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

import { useEffect, useMemo, useState } from 'react';

import type { GroupMember, GroupMembershipPayload } from '../types/group';
import { PRIMARY_OWNER_ROLE } from '../types/group';
import { getMemberRoleLockFlags, sortedSuccessorCandidates, type MemberRoleSelections, type RoleField } from '../utils/memberRoles';
import {
    analyzeEditOwnershipTransfer,
    buildEditMembershipPayloads,
    buildEditOwnershipTransferMessage,
    isPrimaryOwnerUnavailable,
} from '../utils/primaryOwnership';

export function useGroupEditMemberForm({
    open,
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
    open: boolean;
    member: GroupMember | undefined;
    members: GroupMember[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    apiPrimaryOwnerMode: string | undefined;
    apiProductPrimaryOwnerMode: string | undefined;
    onSubmit: (memberships: GroupMembershipPayload[]) => void;
}) {
    const [roleValues, setRoleValues] = useState<Record<RoleField, string>>({
        apiRole: '',
        apiProductRole: '',
        applicationRole: '',
        integrationRole: '',
        clusterRole: '',
        explorerRole: '',
    });
    const [groupAdmin, setGroupAdmin] = useState(false);
    const [selectedSuccessorId, setSelectedSuccessorId] = useState<string | null>(null);

    useEffect(() => {
        if (!open || !member) return;
        setRoleValues({
            apiRole: member.roles?.API ?? '',
            apiProductRole: member.roles?.API_PRODUCT ?? '',
            applicationRole: member.roles?.APPLICATION ?? '',
            integrationRole: member.roles?.INTEGRATION ?? '',
            clusterRole: member.roles?.CLUSTER ?? '',
            explorerRole: member.roles?.EXPLORER ?? '',
        });
        setGroupAdmin(member.roles?.GROUP === 'ADMIN');
        setSelectedSuccessorId(null);
    }, [open, member]);

    useEffect(() => {
        setSelectedSuccessorId(null);
    }, [roleValues.apiRole, roleValues.apiProductRole]);

    const successorCandidates = useMemo(() => sortedSuccessorCandidates(members, member?.id), [members, member]);
    const roleLocks = getMemberRoleLockFlags({ lockApiRole, lockApiProductRole, lockApplicationRole }, canOverrideLocks);

    const transfer = member ? analyzeEditOwnershipTransfer(member, members, roleValues) : null;
    const selectedSuccessor = selectedSuccessorId ? (successorCandidates.find(m => m.id === selectedSuccessorId) ?? null) : null;
    const transferMessage = member && transfer ? buildEditOwnershipTransferMessage(member, transfer, selectedSuccessor, roleValues) : null;

    // Classic edit disables PRIMARY_OWNER only in USER mode (fail closed until settings load). Existing PO
    // stays selectable so upgrades can transfer ownership from another member.
    const disabledOptionNames = {
        api: isPrimaryOwnerUnavailable(apiPrimaryOwnerMode) ? new Set([PRIMARY_OWNER_ROLE]) : undefined,
        apiProduct: isPrimaryOwnerUnavailable(apiProductPrimaryOwnerMode) ? new Set([PRIMARY_OWNER_ROLE]) : undefined,
    };

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
        handleRoleChange: (field: RoleField, value: string) => setRoleValues(prev => ({ ...prev, [field]: value })),
        canSubmit: !transfer?.needsSuccessor || Boolean(selectedSuccessor),
        handleSubmit: () => {
            if (!member || !transfer) return;
            const selections: MemberRoleSelections = { ...roleValues, groupAdmin };
            onSubmit(buildEditMembershipPayloads(member, selections, transfer, selectedSuccessor));
        },
    };
}
