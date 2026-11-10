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
import { Alert, AlertDescription, Skeleton, useLayoutConfig } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { PolicyStudio } from '@gravitee/graphene-policy-studio';
import type { Policy } from '@gravitee/graphene-policy-studio';
import { useParams } from 'react-router-dom';

import { getPolicyDocumentation, getPolicySchema } from '../../../../apis/services/policyStudioService';
import { V2_DEFAULT_ENDPOINTS_INFO, V2_DEFAULT_ENTRYPOINTS_INFO } from '../../../../apis/utils/v2FlowAdapter';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { useApiProductPolicyStudioData } from '../../../hooks/useApiProductPolicyStudioData';
import { useApiProductPolicyStudioSave } from '../../../hooks/useApiProductPolicyStudioSave';

/**
 * A product has no connectors of its own: its flows run on its member APIs' HTTP proxies. The studio shows a generic
 * HTTP proxy on each side, because it only lets policies be added to a phase with a connector.
 */
const HTTP_PROXY_ENTRYPOINTS = V2_DEFAULT_ENTRYPOINTS_INFO;
const HTTP_PROXY_ENDPOINTS = V2_DEFAULT_ENDPOINTS_INFO;

const fetchPolicySchema = (policy: Policy) => getPolicySchema(policy.id, 'HTTP_PROXY');
const fetchPolicyDocumentation = (policy: Policy) => getPolicyDocumentation(policy.id, 'HTTP_PROXY');

/**
 * The studio can only be read-only as a whole, so a user who may edit one side only is told upfront which side;
 * the save hook refuses the other side if they change it anyway.
 */
function partialEditMessage(canUpdateFlows: boolean, canUpdatePlanFlows: boolean): string | null {
    if (canUpdateFlows === canUpdatePlanFlows) return null;
    return canUpdatePlanFlows
        ? 'You can edit plan flows only. Product flows and flow mode need the API Product definition update permission.'
        : 'You can edit product flows and flow mode only. Plan flows need the API Product plan update permission.';
}

export function ApiProductPolicyStudioPage() {
    const { productId } = useParams<{ productId: string }>();

    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    const definition = useApiProductResourcePermissions(productId, 'DEFINITION');
    const plan = useApiProductResourcePermissions(productId, 'PLAN');
    const studioData = useApiProductPolicyStudioData(productId, plan.canRead);
    const onSave = useApiProductPolicyStudioSave(productId, {
        canUpdateFlows: definition.canUpdate,
        canUpdatePlanFlows: plan.canUpdate,
    });

    if (definition.isLoading) {
        return (
            <div className="space-y-3 p-6">
                <Skeleton className="h-96 w-full rounded-lg" />
            </div>
        );
    }

    if (!definition.canRead) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">You do not have permission to view the Policy Studio of this API Product.</p>
            </div>
        );
    }

    if (studioData.isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-destructive">Failed to load Policy Studio data. Please try again.</p>
            </div>
        );
    }

    const editMessage = partialEditMessage(definition.canUpdate, plan.canUpdate);

    return (
        <div className="flex h-full min-h-0 flex-col">
            {editMessage ? (
                <Alert role="status" className="rounded-none border-0 border-b px-6">
                    <InfoIcon aria-hidden="true" />
                    <AlertDescription>{editMessage}</AlertDescription>
                </Alert>
            ) : null}
            <div className="min-h-0 flex-1">
                <PolicyStudio
                    apiType="PROXY"
                    policies={studioData.policies}
                    sharedPolicyGroups={studioData.sharedPolicyGroups}
                    plans={studioData.plans}
                    commonFlows={studioData.commonFlows}
                    entrypointsInfo={HTTP_PROXY_ENTRYPOINTS}
                    endpointsInfo={HTTP_PROXY_ENDPOINTS}
                    flowExecution={studioData.flowExecution}
                    readOnly={!definition.canUpdate && !plan.canUpdate}
                    loading={studioData.isLoading}
                    onSave={onSave}
                    onFetchPolicySchema={fetchPolicySchema}
                    onFetchPolicyDocumentation={fetchPolicyDocumentation}
                />
            </div>
        </div>
    );
}
