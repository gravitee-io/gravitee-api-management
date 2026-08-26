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

import type { SaveOutput } from '@gravitee/graphene-policy-studio';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notify } from '../../../shared/notify';
import { getOrganization, updateOrganization } from '../../../shared/services/organization';
import type { Organization } from '../../../shared/types/organization';
import { organizationKeys } from '../../../shared/utils/queryKeys';
import { toFlowMode, toPlatformFlows } from '../utils/platformFlowAdapter';

/**
 * Saving is a full-entity PUT on the organization. The entity is re-read first so fields the studio
 * knows nothing about survive the write, and so an untouched part of the studio — flows or flow mode —
 * is sent back as stored rather than as an empty value, which would delete every platform flow.
 */
export function useSavePlatformPolicies() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (output: SaveOutput) => {
            const organization = await getOrganization();
            const updated: Organization = {
                ...organization,
                flows: output.commonFlows ? toPlatformFlows(output.commonFlows) : organization.flows,
                flowMode: output.flowExecution ? toFlowMode(output.flowExecution) : organization.flowMode,
            };
            return updateOrganization(updated);
        },
        onSuccess: () => {
            // The PUT replaces the whole entity, so every read of the organization is stale, not just ours.
            queryClient.invalidateQueries({ queryKey: organizationKeys.all });
            notify.success('Platform policies successfully updated!');
        },
        onError: error => notify.error(error, 'An error occurred while updating the platform policies.'),
    });
}
