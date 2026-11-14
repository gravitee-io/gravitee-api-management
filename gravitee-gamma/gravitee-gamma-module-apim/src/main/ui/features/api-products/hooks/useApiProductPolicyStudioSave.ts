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
import type { SaveOutput } from '@gravitee/graphene-policy-studio';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import { DEFAULT_FLOW_EXECUTION } from './useApiProductPolicyStudioData';
import { extractErrorMessage, notify } from '../../../shared/notify';
import { apiPlanKeys } from '../../apis/utils/queryKeys';
import { getApiProductFlows, updateApiProductFlows, updateApiProductPlanFlows } from '../services/apiProductFlows';
import { apiProductKeys } from '../utils/queryKeys';

export interface ApiProductPolicyStudioSavePermissions {
    /** Product flows and the flow mode: API Product DEFINITION update. */
    canUpdateFlows: boolean;
    /** Plan flows: API Product PLAN update. */
    canUpdatePlanFlows: boolean;
}

/**
 * Returns the studio's `onSave`. It saves the product flows and mode first, then the flows of the changed plans in one
 * request, and never deploys: it refreshes the product so its deploy banner shows.
 *
 * The studio can only be read-only as a whole, so a user allowed to edit one side can still change the other in it;
 * such a save is refused before anything is sent.
 */
export function useApiProductPolicyStudioSave(productId: string | undefined, permissions: ApiProductPolicyStudioSavePermissions) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { canUpdateFlows, canUpdatePlanFlows } = permissions;

    return useCallback(
        async ({ commonFlows, flowExecution, plansToUpdate }: SaveOutput) => {
            if (!env || !productId) return;
            const envId = env.id;
            const productFlowsChanged = Boolean(commonFlows || flowExecution);
            const changedPlans = plansToUpdate ?? [];
            const planFlowsChanged = changedPlans.length > 0;

            if (productFlowsChanged && !canUpdateFlows) {
                throw new Error('You do not have permission to edit the API Product flows or the flow mode.');
            }
            if (planFlowsChanged && !canUpdatePlanFlows) {
                throw new Error('You do not have permission to edit plan flows.');
            }

            const refresh = () =>
                Promise.all([
                    queryClient.invalidateQueries({ queryKey: apiProductKeys.flows(envId, productId) }),
                    queryClient.invalidateQueries({ queryKey: [apiPlanKeys.all[0], 'api-product', productId] }),
                    queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(envId, productId) }),
                ]);

            if (productFlowsChanged) {
                // The PUT replaces both the flows and the mode, and the studio only hands back the one that changed.
                const saved = await getApiProductFlows(envId, productId);
                await updateApiProductFlows(envId, productId, {
                    flowExecution: flowExecution ?? saved.flowExecution ?? DEFAULT_FLOW_EXECUTION,
                    flows: commonFlows ?? saved.flows ?? [],
                });
            }

            if (planFlowsChanged) {
                try {
                    await updateApiProductPlanFlows(
                        envId,
                        productId,
                        changedPlans.map(plan => ({ planId: plan.id, flows: plan.flows })),
                    );
                } catch (error) {
                    if (!productFlowsChanged) throw error;
                    const partialFailure = new Error(
                        `The API Product flows were saved, but the plan flows were not: ${extractErrorMessage(error)}`,
                    );
                    // The studio drops the error of a save whose data was reloaded meanwhile, so the toast carries it.
                    notify.error(partialFailure);
                    await refresh();
                    throw partialFailure;
                }
            }

            await refresh();
        },
        [env, productId, canUpdateFlows, canUpdatePlanFlows, queryClient],
    );
}
