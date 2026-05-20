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

import { CreatePlanDropdown } from './CreatePlanDropdown';
import { PlansLearningPage } from './PlansLearningPage';
import { PlansListPage } from './PlansListPage';
import { usePlanStatusCounts } from '../../../hooks/usePlans';
import type { PlanContext } from '../../../types/plan';

interface PlansPageProps {
    ctx: PlanContext;
    canRead: boolean;
    canCreate: boolean;
    canUpdate: boolean;
    canDelete: boolean;
}

export function PlansPage({ ctx, canRead, canCreate, canUpdate, canDelete }: Readonly<PlansPageProps>) {
    const counts = usePlanStatusCounts(ctx);

    if (!canRead) {
        return (
            <div className="flex flex-col gap-6 p-6">
                <h1 className="text-2xl font-semibold tracking-tight">Plans</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view plans.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6 p-6">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Plans</h1>
                    <p className="text-sm text-muted-foreground">Manage subscription plans and their lifecycle.</p>
                </div>
                {canCreate && <CreatePlanDropdown ctx={ctx} />}
            </div>

            {counts.isLoading ? (
                <div className="space-y-3">
                    <Skeleton className="h-24 w-full rounded-lg" />
                    <Skeleton className="h-64 w-full rounded-lg" />
                </div>
            ) : counts.total === 0 ? (
                <PlansLearningPage ctx={ctx} />
            ) : (
                <PlansListPage ctx={ctx} counts={counts} canUpdate={canUpdate} canDelete={canDelete} />
            )}
        </div>
    );
}
