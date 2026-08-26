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

import { Skeleton } from '@gravitee/graphene-core';
import { getProtocolType, type Policy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';

import { SharedPolicyGroupPolicyStudio } from '../features/shared-policy-groups/components/SharedPolicyGroupPolicyStudio';
import {
    useDeploySharedPolicyGroup,
    useUpdateSharedPolicyGroup,
} from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import type { SharedPolicyGroup, SharedPolicyGroupStep } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
    isKubernetesOrigin,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { useHasEnvironmentPermission } from '../shared/hooks/useEnvironmentPermissions';
import { notify } from '../shared/notify';
import { getPolicyDocumentation, getPolicySchema, listPolicies } from '../shared/services/policyPlugins';
import { policyPluginKeys } from '../shared/utils/queryKeys';

export function SharedPolicyGroupStudioPage() {
    const sharedPolicyGroup = useOutletContext<SharedPolicyGroup>();
    const canUpdate = useHasEnvironmentPermission([ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION]);
    const updateMutation = useUpdateSharedPolicyGroup();
    const deployMutation = useDeploySharedPolicyGroup();
    const kubernetesOrigin = isKubernetesOrigin(sharedPolicyGroup);
    const protocolType = getProtocolType(sharedPolicyGroup.apiType);
    const policiesQuery = useQuery({
        queryKey: policyPluginKeys.list(),
        queryFn: listPolicies,
        staleTime: 5 * 60 * 1000,
    });
    const onFetchPolicySchema = useCallback((policy: Policy) => getPolicySchema(policy.id, protocolType), [protocolType]);
    const onFetchPolicyDocumentation = useCallback((policy: Policy) => getPolicyDocumentation(policy.id, protocolType), [protocolType]);
    const handleSave = useCallback(
        async (steps: SharedPolicyGroupStep[]) => {
            try {
                const updatedSharedPolicyGroup = await updateMutation.mutateAsync({
                    id: sharedPolicyGroup.id,
                    payload: {
                        name: sharedPolicyGroup.name,
                        description: sharedPolicyGroup.description,
                        prerequisiteMessage: sharedPolicyGroup.prerequisiteMessage,
                        steps,
                    },
                });
                notify.success('Shared Policy Group updated');
                return updatedSharedPolicyGroup;
            } catch (error) {
                notify.error(error, 'Error during Shared Policy Group update!');
                throw error;
            }
        },
        [sharedPolicyGroup, updateMutation],
    );

    async function handleDeploy() {
        try {
            await deployMutation.mutateAsync(sharedPolicyGroup.id);
            notify.success('Shared Policy Group deployed successfully');
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group deployment!');
        }
    }

    if (policiesQuery.isLoading) {
        return <Skeleton className="h-[32rem] w-full rounded-lg" />;
    }

    if (policiesQuery.isError || !policiesQuery.data) {
        return <p className="text-sm text-destructive">Failed to load the policy catalog. Please refresh and try again.</p>;
    }

    return (
        <SharedPolicyGroupPolicyStudio
            key={sharedPolicyGroup.id}
            sharedPolicyGroup={sharedPolicyGroup}
            policies={policiesQuery.data}
            readOnly={!canUpdate || kubernetesOrigin}
            isDeploying={deployMutation.isPending}
            onSave={handleSave}
            onDeploy={handleDeploy}
            onFetchPolicySchema={onFetchPolicySchema}
            onFetchPolicyDocumentation={onFetchPolicyDocumentation}
        />
    );
}
