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

import { useState } from 'react';

import { useAddGroupMembers } from './useGroupMutations';
import { notify } from '../../../shared/notify';
import type { GroupMembershipPayload } from '../types/group';

type MemberSheetState = 'closed' | 'search';

export function useGroupMemberActions(groupId: string | undefined) {
    const [memberSheet, setMemberSheet] = useState<MemberSheetState>('closed');

    const addMembersMutation = useAddGroupMembers();

    function closeMemberSheet() {
        setMemberSheet('closed');
    }

    async function handleAddMembers(memberships: GroupMembershipPayload[]) {
        if (!groupId) return;
        try {
            await addMembersMutation.mutateAsync({ groupId, memberships });
            notify.success(memberships.length > 1 ? `${memberships.length} members added successfully` : 'Member added successfully');
            closeMemberSheet();
        } catch (error) {
            notify.error(error, 'Failed to add members');
        }
    }

    return {
        memberSheet,
        setMemberSheet,
        closeMemberSheet,
        addMembersMutation,
        handleAddMembers,
    };
}
