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
import { useLayoutConfig } from '@gravitee/graphene-core';
import { PolicyStudio } from '@gravitee/graphene-policy-studio';
import type { Policy } from '@gravitee/graphene-policy-studio';
import { useCallback } from 'react';
import { useParams } from 'react-router-dom';

import { usePolicyStudioData } from '../../hooks/usePolicyStudioData';
import { usePolicyStudioSave } from '../../hooks/usePolicyStudioSave';
import { getPolicyDocumentation, getPolicySchema } from '../../services/policyStudioService';
import { getApiProtocolType } from '../../types/policyStudio';

export function PolicyStudioPage() {
    const { apiId } = useParams<{ apiId: string }>();

    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    const studioData = usePolicyStudioData(apiId);
    const onSave = usePolicyStudioSave(apiId);

    const apiType = studioData.apiDetail?.type ?? (studioData.isV2 ? 'PROXY' : undefined);

    const onFetchPolicySchema = useCallback(
        async (policy: Policy) => {
            if (!apiType) return {};
            return getPolicySchema(policy.id, getApiProtocolType(apiType));
        },
        [apiType],
    );

    const onFetchPolicyDocumentation = useCallback(
        async (policy: Policy) => {
            if (!apiType) return '';
            return getPolicyDocumentation(policy.id, getApiProtocolType(apiType));
        },
        [apiType],
    );

    if (studioData.isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-destructive">Failed to load Policy Studio data. Please try again.</p>
            </div>
        );
    }

    return (
        <PolicyStudio
            apiType={studioData.apiType}
            policies={studioData.policies}
            sharedPolicyGroups={studioData.sharedPolicyGroups}
            plans={studioData.plans}
            commonFlows={studioData.commonFlows}
            entrypointsInfo={studioData.entrypointsInfo}
            endpointsInfo={studioData.endpointsInfo}
            flowExecution={studioData.flowExecution}
            readOnly={studioData.isV2}
            onSave={onSave}
            onFetchPolicySchema={onFetchPolicySchema}
            onFetchPolicyDocumentation={onFetchPolicyDocumentation}
        />
    );
}
