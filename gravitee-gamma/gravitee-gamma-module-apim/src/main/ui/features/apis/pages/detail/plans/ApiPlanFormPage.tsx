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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { PlanFormWizard } from './plan-form/PlanFormWizard';
import { usePlan } from '../../../hooks/usePlans';
import { PLAN_TYPES_BY_CTX } from '../../../types/plan';
import type { PlanContext, PlanSecurityType } from '../../../types/plan';

/**
 * Adapter for /apis/:apiId/plans/new/:securityType (create) and
 * /apis/:apiId/plans/:planId (edit / view).
 */
export function ApiPlanFormPage() {
    const { apiId, securityType, planId } = useParams<{ apiId: string; securityType?: string; planId?: string }>();
    const ctx: PlanContext = { type: 'api', entityId: apiId ?? '' };
    const canCreate = useHasPermission({ anyOf: ['api-plan-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-plan-u'] });

    if (securityType) {
        if (!(PLAN_TYPES_BY_CTX[ctx.type] as string[]).includes(securityType)) {
            return <Navigate to=".." replace />;
        }
        if (!canCreate) {
            return <Navigate to=".." replace />;
        }
        return <PlanFormWizard ctx={ctx} securityType={securityType as PlanSecurityType} />;
    }

    return <PlanEditWrapper ctx={ctx} planId={planId ?? ''} canUpdate={canUpdate} />;
}

function PlanEditWrapper({ ctx, planId, canUpdate }: Readonly<{ ctx: PlanContext; planId: string; canUpdate: boolean }>) {
    const navigate = useNavigate();
    const { data: plan, isLoading, isError } = usePlan(ctx, planId);

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-12 w-full rounded" />
                <Skeleton className="h-64 w-full rounded" />
            </div>
        );
    }

    if (isError || !plan) {
        return (
            <div className="flex flex-col items-center justify-center gap-4 p-8">
                <p className="text-sm text-muted-foreground">Failed to load plan. It may have been deleted or you may not have access.</p>
                <Button variant="outline" size="sm" onClick={() => navigate('..')}>
                    Back to Plans
                </Button>
            </div>
        );
    }

    return (
        <PlanFormWizard
            ctx={ctx}
            securityType={plan.security.type}
            planId={planId}
            readOnly={plan.status === 'CLOSED' || !canUpdate}
            securityLocked={plan.status !== 'STAGING' && plan.status !== 'CLOSED'}
        />
    );
}
