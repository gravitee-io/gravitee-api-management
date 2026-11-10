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
import type { Flow, FlowExecution, Plan, Policy, SharedPolicyGroupPolicy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { policyStudioKeys, toStudioPolicies, toStudioSharedPolicyGroups } from '../../apis/hooks/usePolicyStudioData';
import { listPlans } from '../../apis/services/plans';
import { listPolicies, listSharedPolicyGroupPlugins } from '../../apis/services/policyStudioService';
import type { PlanContext, PlanStatus } from '../../apis/types/plan';
import { apiPlanKeys } from '../../apis/utils/queryKeys';
import { getApiProductFlows } from '../services/apiProductFlows';
import { apiProductKeys } from '../utils/queryKeys';

/** CLOSED plans stay out of the studio: their flows can no longer change. */
const STUDIO_PLAN_STATUSES: PlanStatus[] = ['PUBLISHED', 'DEPRECATED'];
/** The studio renders every plan's flows at once; paginated loading is not supported. */
const ALL_PLANS_PAGE_SIZE = 9999;
/** What the gateway applies to a product that never saved a flow mode. */
export const DEFAULT_FLOW_EXECUTION: FlowExecution = { mode: 'DEFAULT', matchRequired: false };

export interface ApiProductPolicyStudioData {
    policies: readonly Policy[];
    sharedPolicyGroups: readonly SharedPolicyGroupPolicy[];
    plans: readonly Plan[];
    commonFlows: readonly Flow[];
    flowExecution: FlowExecution;
    isLoading: boolean;
    isError: boolean;
}

/** Plans are only loaded with `canReadPlans`: their endpoint needs the API Product plan read permission. */
export function useApiProductPolicyStudioData(productId: string | undefined, canReadPlans: boolean): ApiProductPolicyStudioData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && productId);
    const planContext: PlanContext = { type: 'api-product', entityId: productId ?? '' };

    const flowsQuery = useQuery({
        queryKey: apiProductKeys.flows(envId, productId ?? ''),
        queryFn: () => getApiProductFlows(envId, productId!),
        enabled,
        staleTime: 60_000,
    });

    const plansQuery = useQuery({
        // Under the Plans page's key, so its plan changes reload the studio's plans too.
        queryKey: apiPlanKeys.list(envId, planContext, STUDIO_PLAN_STATUSES, 1, ALL_PLANS_PAGE_SIZE),
        queryFn: () => listPlans(envId, planContext, STUDIO_PLAN_STATUSES, 1, ALL_PLANS_PAGE_SIZE),
        enabled: enabled && canReadPlans,
        staleTime: 60_000,
    });

    const policiesQuery = useQuery({
        queryKey: policyStudioKeys.policies(),
        queryFn: listPolicies,
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    // Refetched on every mount for the reason given in `usePolicyStudioData`: renames happen in another module.
    const spgQuery = useQuery({
        queryKey: policyStudioKeys.sharedPolicyGroups(envId),
        queryFn: () => listSharedPolicyGroupPlugins(envId),
        enabled: Boolean(env),
        staleTime: 300_000,
        refetchOnMount: 'always',
    });

    const policies = useMemo(() => toStudioPolicies(policiesQuery.data), [policiesQuery.data]);
    const sharedPolicyGroups = useMemo(() => toStudioSharedPolicyGroups(spgQuery.data), [spgQuery.data]);

    const plans: readonly Plan[] = useMemo(
        () =>
            (plansQuery.data?.data ?? []).map(plan => ({
                id: plan.id,
                name: plan.name,
                flows: (plan.flows ?? []) as readonly Flow[],
            })),
        [plansQuery.data],
    );

    const commonFlows = useMemo(() => flowsQuery.data?.flows ?? [], [flowsQuery.data]);
    const flowExecution = useMemo(() => flowsQuery.data?.flowExecution ?? DEFAULT_FLOW_EXECUTION, [flowsQuery.data]);

    return {
        policies,
        sharedPolicyGroups,
        plans,
        commonFlows,
        flowExecution,
        isLoading: flowsQuery.isLoading || plansQuery.isLoading || policiesQuery.isLoading || spgQuery.isLoading,
        isError: flowsQuery.isError || plansQuery.isError || policiesQuery.isError || spgQuery.isError,
    };
}
