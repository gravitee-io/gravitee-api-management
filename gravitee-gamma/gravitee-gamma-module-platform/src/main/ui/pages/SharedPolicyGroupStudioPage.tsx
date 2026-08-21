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

import { Card, CardContent } from '@gravitee/graphene-core';
import { useOutletContext } from 'react-router-dom';

import { SharedPolicyGroupStudioEmptyState } from '../features/shared-policy-groups/components/SharedPolicyGroupStudioEmptyState';
import type { SharedPolicyGroup, SharedPolicyGroupStep } from '../features/shared-policy-groups/types/sharedPolicyGroup';

function stepLabel(step: SharedPolicyGroupStep, index: number): string {
    const name = typeof step.name === 'string' ? step.name.trim() : '';
    const policy = typeof step.policy === 'string' ? step.policy.trim() : '';
    return name || policy || `Policy ${index + 1}`;
}

function ConfiguredPoliciesSummary({ steps }: Readonly<{ steps: SharedPolicyGroupStep[] }>) {
    return (
        <Card data-testid="shared-policy-group-studio-configured">
            <CardContent className="space-y-3 p-5">
                <div className="space-y-1">
                    <h2 className="text-base font-semibold">
                        {steps.length} {steps.length === 1 ? 'policy' : 'policies'} configured
                    </h2>
                    <p className="text-sm text-muted-foreground">Policies currently configured in this Shared Policy Group.</p>
                </div>
                <ul className="list-disc space-y-1 pl-5 text-sm">
                    {steps.map((step, index) => {
                        const label = stepLabel(step, index);
                        return <li key={`${label}-${index}`}>{label}</li>;
                    })}
                </ul>
            </CardContent>
        </Card>
    );
}

export function SharedPolicyGroupStudioPage() {
    const sharedPolicyGroup = useOutletContext<SharedPolicyGroup>();
    const steps = sharedPolicyGroup.steps ?? [];
    if (steps.length === 0) {
        return <SharedPolicyGroupStudioEmptyState />;
    }

    return <ConfiguredPoliciesSummary steps={steps} />;
}
