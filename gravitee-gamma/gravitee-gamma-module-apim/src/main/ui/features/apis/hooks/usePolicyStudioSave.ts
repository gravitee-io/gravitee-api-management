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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import type { Plan, SaveOutput } from '@gravitee/graphene-policy-studio';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import { policyStudioKeys } from './usePolicyStudioData';
import { getAndUpdatePlanFlows, getFullApiDetail, updateApi } from '../services/policyStudioService';

/**
 * Returns a memoized `onSave` callback for the PolicyStudio component.
 *
 * Follows the GET-then-PUT pattern used throughout the module:
 * - If commonFlows or flowExecution changed → GET api, merge, PUT api
 * - If plansToUpdate present → for each plan, GET plan, merge flows, PUT plan
 * - Invalidates policy-studio query keys after success
 */
export function usePolicyStudioSave(apiId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useCallback(
        async (output: SaveOutput) => {
            if (!env || !apiId) return;
            const envId = env.id;

            try {
                const { commonFlows, flowExecution, plansToUpdate } = output;

                if (commonFlows || flowExecution) {
                    const current = await getFullApiDetail(envId, apiId);
                    const updated: Record<string, unknown> = { ...current };
                    if (commonFlows) updated.flows = commonFlows;
                    if (flowExecution) updated.flowExecution = flowExecution;
                    await updateApi(envId, apiId, updated);
                }

                if (plansToUpdate) {
                    await Promise.all(plansToUpdate.map((plan: Plan) => getAndUpdatePlanFlows(envId, apiId, plan.id, [...plan.flows])));
                }

                await queryClient.invalidateQueries({ queryKey: policyStudioKeys.api(envId, apiId) });
                await queryClient.invalidateQueries({ queryKey: policyStudioKeys.plans(envId, apiId) });
            } catch (error) {
                console.error('[PolicyStudio] Save failed:', error);
                throw error;
            }
        },
        [env, apiId, queryClient],
    );
}
