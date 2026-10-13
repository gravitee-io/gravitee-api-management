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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import type { SearchableUser } from '../../../shared/types/userSearch';
import { throwIfAnyAddFailed } from '../../shared/utils/addMembersResult';
import { addIntegrationMember } from '../services/integrationMembers';
import { integrationKeys } from '../utils/queryKeys';

export interface AddIntegrationMembersInput {
    users: SearchableUser[];
    roleName: string;
}

async function addEach(environmentId: string, integrationId: string, { users, roleName }: AddIntegrationMembersInput): Promise<void> {
    const results = await Promise.allSettled(
        users.map(user =>
            addIntegrationMember(environmentId, integrationId, {
                userId: user.id ?? undefined,
                externalReference: user.reference,
                roleName,
            }),
        ),
    );
    throwIfAnyAddFailed(users, results);
}

export function useAddIntegrationMembers(integrationId: string) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (input: AddIntegrationMembersInput) => addEach(env!.id, integrationId, input),
        // Settled, not success: a partial failure still added some members.
        onSettled: () => queryClient.invalidateQueries({ queryKey: integrationKeys.members(env!.id, integrationId) }),
    });
}
