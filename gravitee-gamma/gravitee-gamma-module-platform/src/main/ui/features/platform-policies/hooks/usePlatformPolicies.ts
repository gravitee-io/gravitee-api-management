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

import type { Flow, FlowExecution, OrganizationTag, Policy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { getOrganization } from '../../../shared/services/organization';
import { listPolicies } from '../../../shared/services/policyPlugins';
import { organizationKeys, policyPluginKeys } from '../../../shared/utils/queryKeys';
import { toFlowExecution } from '../../../shared/v2-flows';
import { listOrgTags } from '../../entrypoints/services/tags';
import { orgTagKeys } from '../../entrypoints/utils/queryKeys';
import { toStudioFlows } from '../utils/platformFlowAdapter';

const CATALOG_STALE_TIME = 5 * 60 * 1000;
const EMPTY_POLICIES: readonly Policy[] = [];

export interface PlatformPolicyStudioData {
    readonly policies: readonly Policy[];
    readonly commonFlows: readonly Flow[];
    readonly organizationTags: readonly OrganizationTag[];
    readonly flowExecution: FlowExecution;
    readonly isLoading: boolean;
    readonly isError: boolean;
}

/** Everything the organization Policy Studio renders: the platform flows, the policy catalog and the sharding tags. */
export function usePlatformPolicies(): PlatformPolicyStudioData {
    const organizationQuery = useQuery({
        queryKey: organizationKeys.detail(),
        queryFn: getOrganization,
    });

    const policiesQuery = useQuery({
        queryKey: policyPluginKeys.list(),
        queryFn: listPolicies,
        staleTime: CATALOG_STALE_TIME,
    });

    const tagsQuery = useQuery({
        queryKey: orgTagKeys.list(),
        queryFn: listOrgTags,
        staleTime: CATALOG_STALE_TIME,
    });

    const commonFlows = useMemo(() => toStudioFlows(organizationQuery.data?.flows ?? []), [organizationQuery.data?.flows]);
    const flowExecution = useMemo(() => toFlowExecution(organizationQuery.data?.flowMode), [organizationQuery.data?.flowMode]);
    const organizationTags = useMemo<readonly OrganizationTag[]>(
        () => (tagsQuery.data ?? []).map(tag => ({ id: tag.id, name: tag.name })),
        [tagsQuery.data],
    );

    return {
        policies: policiesQuery.data ?? EMPTY_POLICIES,
        commonFlows,
        organizationTags,
        flowExecution,
        isLoading: organizationQuery.isLoading || policiesQuery.isLoading || tagsQuery.isLoading,
        // Tags only feed the flow form's Tags field: losing them narrows the studio, it does not break it.
        isError: organizationQuery.isError || policiesQuery.isError,
    };
}
