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
import type { ApiType as PSApiType, ConnectorInfo, Flow, Plan, Policy, SharedPolicyGroupPolicy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import {
    getFullApiDetail,
    listEndpointPlugins,
    listEntrypointPlugins,
    listPolicies,
    listPublishedPlans,
    listSharedPolicyGroupPlugins,
} from '../services/policyStudioService';
import type { FlowExecution, FlowV2, PolicyStudioApiDetail } from '../types/policyStudio';
import { mapFlowModeToExecution, mapFlowsV2ToV4, V2_DEFAULT_ENDPOINTS_INFO, V2_DEFAULT_ENTRYPOINTS_INFO } from '../utils/v2FlowAdapter';

const policyStudioKeys = {
    all: ['policy-studio'] as const,
    api: (envId: string, apiId: string) => [...policyStudioKeys.all, 'api', envId, apiId] as const,
    plans: (envId: string, apiId: string) => [...policyStudioKeys.all, 'plans', envId, apiId] as const,
    policies: () => [...policyStudioKeys.all, 'policies'] as const,
    entrypoints: () => [...policyStudioKeys.all, 'entrypoints'] as const,
    endpoints: () => [...policyStudioKeys.all, 'endpoints'] as const,
    sharedPolicyGroups: (envId: string) => [...policyStudioKeys.all, 'shared-policy-groups', envId] as const,
};

export { policyStudioKeys };

function buildEntrypointsInfo(
    api: PolicyStudioApiDetail,
    plugins: { id: string; name: string; icon?: string; supportedModes: string[] }[],
): ConnectorInfo[] {
    if (!api.listeners) return [];
    return api.listeners.flatMap(listener =>
        (listener.entrypoints ?? []).map(ep => {
            const plugin = plugins.find(p => p.id === ep.type);
            return {
                type: ep.type,
                icon: plugin?.icon ?? 'gio:language',
                name: plugin?.name ?? ep.type,
                supportedModes: (plugin?.supportedModes ?? []) as ConnectorInfo['supportedModes'],
            };
        }),
    );
}

function buildEndpointsInfo(
    api: PolicyStudioApiDetail,
    plugins: { id: string; name: string; icon?: string; supportedModes: string[] }[],
): ConnectorInfo[] {
    if (!api.endpointGroups) return [];
    return api.endpointGroups.flatMap(group =>
        (group.endpoints ?? []).map(ep => {
            const plugin = plugins.find(p => p.id === ep.type);
            return {
                type: ep.type,
                icon: plugin?.icon ?? 'gio:language',
                name: plugin?.name ?? ep.type,
                supportedModes: (plugin?.supportedModes ?? []) as ConnectorInfo['supportedModes'],
            };
        }),
    );
}

export interface PolicyStudioData {
    apiType: PSApiType;
    policies: readonly Policy[];
    sharedPolicyGroups: readonly SharedPolicyGroupPolicy[];
    plans: readonly Plan[];
    commonFlows: readonly Flow[];
    entrypointsInfo: readonly ConnectorInfo[];
    endpointsInfo: readonly ConnectorInfo[];
    flowExecution: FlowExecution;
    isLoading: boolean;
    isError: boolean;
    /** Full API detail for save mutations. */
    apiDetail: PolicyStudioApiDetail | undefined;
    /** True when the API uses definition version V2 (read-only in studio). */
    isV2: boolean;
}

export function usePolicyStudioData(apiId: string | undefined): PolicyStudioData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && apiId);

    const apiQuery = useQuery({
        queryKey: policyStudioKeys.api(envId, apiId ?? ''),
        queryFn: () => getFullApiDetail(envId, apiId!),
        enabled,
        staleTime: 60_000,
    });

    const plansQuery = useQuery({
        queryKey: policyStudioKeys.plans(envId, apiId ?? ''),
        queryFn: () => listPublishedPlans(envId, apiId!),
        enabled,
        staleTime: 60_000,
    });

    const policiesQuery = useQuery({
        queryKey: policyStudioKeys.policies(),
        queryFn: listPolicies,
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    const entrypointsQuery = useQuery({
        queryKey: policyStudioKeys.entrypoints(),
        queryFn: listEntrypointPlugins,
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    const endpointsQuery = useQuery({
        queryKey: policyStudioKeys.endpoints(),
        queryFn: listEndpointPlugins,
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    const spgQuery = useQuery({
        queryKey: policyStudioKeys.sharedPolicyGroups(envId),
        queryFn: () => listSharedPolicyGroupPlugins(envId),
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    const api = apiQuery.data;
    const isV2 = api?.definitionVersion === 'V2';

    const policies: readonly Policy[] = useMemo(
        () =>
            (policiesQuery.data ?? []).map(p => ({
                id: p.id,
                name: p.name,
                description: p.description,
                icon: p.icon,
                category: p.category,
                flowPhaseCompatibility: p.flowPhaseCompatibility,
            })),
        [policiesQuery.data],
    );

    const sharedPolicyGroups: readonly SharedPolicyGroupPolicy[] = useMemo(
        () =>
            (spgQuery.data ?? []).map(s => ({
                id: s.id,
                name: s.name,
                description: s.description,
                prerequisiteMessage: s.prerequisiteMessage,
                policyId: s.policyId,
                phase: s.phase as SharedPolicyGroupPolicy['phase'],
                apiType: s.apiType as SharedPolicyGroupPolicy['apiType'],
            })),
        [spgQuery.data],
    );

    const plans: readonly Plan[] = useMemo(
        () =>
            (plansQuery.data?.data ?? []).map(p => ({
                id: p.id,
                name: p.name,
                flows: isV2 ? mapFlowsV2ToV4((p.flows ?? []) as FlowV2[]) : ((p.flows ?? []) as readonly Flow[]),
            })),
        [plansQuery.data, isV2],
    );

    const commonFlows: readonly Flow[] = useMemo(() => {
        if (!api?.flows) return [];
        return isV2 ? mapFlowsV2ToV4(api.flows as FlowV2[]) : (api.flows as readonly Flow[]);
    }, [api?.flows, isV2]);

    const entrypointsInfo: readonly ConnectorInfo[] = useMemo(() => {
        if (isV2) return V2_DEFAULT_ENTRYPOINTS_INFO;
        return api && entrypointsQuery.data ? buildEntrypointsInfo(api, entrypointsQuery.data) : [];
    }, [api, entrypointsQuery.data, isV2]);

    const endpointsInfo: readonly ConnectorInfo[] = useMemo(() => {
        if (isV2) return V2_DEFAULT_ENDPOINTS_INFO;
        return api && endpointsQuery.data ? buildEndpointsInfo(api, endpointsQuery.data) : [];
    }, [api, endpointsQuery.data, isV2]);

    const flowExecution: FlowExecution = useMemo(() => {
        if (isV2) return mapFlowModeToExecution(api?.flowMode);
        return api?.flowExecution ?? { mode: 'DEFAULT' };
    }, [api?.flowExecution, api?.flowMode, isV2]);

    const apiType: PSApiType = isV2 ? 'PROXY' : ((api?.type as PSApiType) ?? 'PROXY');

    const isLoading =
        apiQuery.isLoading ||
        plansQuery.isLoading ||
        policiesQuery.isLoading ||
        entrypointsQuery.isLoading ||
        endpointsQuery.isLoading ||
        spgQuery.isLoading;

    const isError =
        apiQuery.isError ||
        plansQuery.isError ||
        policiesQuery.isError ||
        entrypointsQuery.isError ||
        endpointsQuery.isError ||
        spgQuery.isError;

    return {
        apiType,
        policies,
        sharedPolicyGroups,
        plans,
        commonFlows,
        entrypointsInfo,
        endpointsInfo,
        flowExecution,
        isLoading,
        isError,
        apiDetail: api,
        isV2,
    };
}
