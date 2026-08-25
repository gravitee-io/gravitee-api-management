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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { Alert, AlertDescription, Skeleton, useLayoutConfig } from '@gravitee/graphene-core';
import { PolicyStudio, type ApiProtocolType, type Policy, type SaveOutput } from '@gravitee/graphene-policy-studio';
import { useCallback, useState } from 'react';

import { DeployPlatformPoliciesDialog } from '../features/platform-policies/components/DeployPlatformPoliciesDialog';
import { usePlatformPolicies } from '../features/platform-policies/hooks/usePlatformPolicies';
import { useSavePlatformPolicies } from '../features/platform-policies/hooks/useSavePlatformPolicies';
import { ORGANIZATION_POLICIES_UPDATE_PERMISSION } from '../features/platform-policies/utils/platformPolicyPermissions';
import { getPolicyDocumentation, getPolicySchema } from '../shared/services/policyPlugins';

/** Platform flows run around every HTTP API, so they belong to no plan and to no connector. */
const EMPTY_LIST = [] as const;

/** Platform flows only run around HTTP proxies, so schemas and docs are always asked for that protocol. */
const PLATFORM_PROTOCOL_TYPE: ApiProtocolType = 'HTTP_PROXY';

interface PendingDeployment {
    readonly output: SaveOutput;
    readonly resolve: () => void;
    readonly reject: (error: Error) => void;
}

export function OrganizationPolicyStudioPage() {
    const canUpdate = useHasPermission({ anyOf: [ORGANIZATION_POLICIES_UPDATE_PERMISSION] });
    const studioData = usePlatformPolicies();
    const saveMutation = useSavePlatformPolicies();
    const [pendingDeployment, setPendingDeployment] = useState<PendingDeployment | null>(null);

    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    const onFetchPolicySchema = useCallback((policy: Policy) => getPolicySchema(policy.id, PLATFORM_PROTOCOL_TYPE), []);
    const onFetchPolicyDocumentation = useCallback((policy: Policy) => getPolicyDocumentation(policy.id, PLATFORM_PROTOCOL_TYPE), []);

    // Saving deploys to every gateway in the organization, so the studio's save waits on a confirmation.
    const onSave = useCallback(
        (output: SaveOutput) => new Promise<void>((resolve, reject) => setPendingDeployment({ output, resolve, reject })),
        [],
    );

    const cancelDeployment = useCallback(() => {
        if (!pendingDeployment) return;
        pendingDeployment.reject(new Error('Deployment cancelled'));
        setPendingDeployment(null);
    }, [pendingDeployment]);

    const confirmDeployment = useCallback(async () => {
        if (!pendingDeployment) return;
        try {
            await saveMutation.mutateAsync(pendingDeployment.output);
            pendingDeployment.resolve();
        } catch (error) {
            pendingDeployment.reject(error instanceof Error ? error : new Error('Failed to deploy the platform policies'));
        } finally {
            setPendingDeployment(null);
        }
    }, [pendingDeployment, saveMutation]);

    if (studioData.isError) {
        return (
            <div className="p-6">
                <Alert variant="destructive">
                    <AlertDescription>Failed to load the platform policies. Please refresh and try again.</AlertDescription>
                </Alert>
            </div>
        );
    }

    if (studioData.isLoading) {
        return (
            <div className="p-6">
                <Skeleton className="h-[32rem] w-full rounded-lg" />
            </div>
        );
    }

    return (
        <>
            <PolicyStudio
                scope="ORGANIZATION"
                apiType="PROXY"
                policies={studioData.policies}
                organizationTags={studioData.organizationTags}
                commonFlows={studioData.commonFlows}
                flowExecution={studioData.flowExecution}
                sharedPolicyGroups={EMPTY_LIST}
                plans={EMPTY_LIST}
                entrypointsInfo={EMPTY_LIST}
                endpointsInfo={EMPTY_LIST}
                readOnly={!canUpdate}
                onSave={onSave}
                onFetchPolicySchema={onFetchPolicySchema}
                onFetchPolicyDocumentation={onFetchPolicyDocumentation}
            />
            <DeployPlatformPoliciesDialog
                open={pendingDeployment !== null}
                isDeploying={saveMutation.isPending}
                onCancel={cancelDeployment}
                onConfirm={() => void confirmDeployment()}
            />
        </>
    );
}
