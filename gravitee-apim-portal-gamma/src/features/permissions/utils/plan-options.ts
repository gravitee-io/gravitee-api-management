/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { getPlanSecurityLabel } from '../../editor/entities/plan';
import { getApiProductById } from '../../editor/services/api-product.service';
import { getPlansForApi } from '../../editor/services/plans.mock';
import type { PortalGrantScopeType } from '../types/permissions.types';

export interface PlanOption {
    id: string;
    name: string;
    description?: string;
}

/** AI workspaces have no plan catalog in the POC: access is a single included plan. */
export const AI_WORKSPACE_DEFAULT_PLAN: PlanOption = {
    id: 'plan-ai-included',
    name: 'Included AI plan',
    description: 'An AI key is issued the first time a member opens the workspace.',
};

export const PRODUCT_DEFAULT_PLAN: PlanOption = {
    id: 'plan-product-default',
    name: 'Product default plan',
    description: 'Subscribes each member’s default application to every API bundled in the product.',
};

function toPlanOption(plan: { id: string; name: string; security: string; description?: string }): PlanOption {
    const security = getPlanSecurityLabel(plan.security as never);
    return {
        id: plan.id,
        name: security ? `${plan.name} (${security})` : plan.name,
        description: plan.description,
    };
}

export async function loadPlanOptions(
    scopeType: PortalGrantScopeType,
    scopeId: string,
): Promise<PlanOption[]> {
    if (scopeType === 'API') {
        const plans = await getPlansForApi(scopeId);
        return plans.filter(plan => plan.security !== 'KEY_LESS').map(toPlanOption);
    }

    if (scopeType === 'API_PRODUCT') {
        const product = await getApiProductById(scopeId);
        if (!product) {
            return [PRODUCT_DEFAULT_PLAN];
        }

        const plansByApi = await Promise.all((product.apiIds ?? []).map(apiId => getPlansForApi(apiId)));
        const plans = plansByApi.flat().filter(plan => plan.security !== 'KEY_LESS');
        return plans.length > 0 ? [PRODUCT_DEFAULT_PLAN, ...plans.map(toPlanOption)] : [PRODUCT_DEFAULT_PLAN];
    }

    if (scopeType === 'AI_WORKSPACE') {
        return [AI_WORKSPACE_DEFAULT_PLAN];
    }

    return [];
}

export function planOptionName(options: readonly PlanOption[], planId: string | undefined): string | undefined {
    if (!planId) {
        return undefined;
    }

    return options.find(option => option.id === planId)?.name ?? planId;
}
