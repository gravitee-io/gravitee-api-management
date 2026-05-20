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
import { useParams } from 'react-router-dom';

import { PlansPage } from '../../../../apis/pages/detail/plans/PlansPage';
import type { PlanContext } from '../../../../apis/types/plan';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';

export function ApiProductPlansPage() {
    const { productId } = useParams<{ productId: string }>();
    const ctx: PlanContext = { type: 'api-product', entityId: productId ?? '' };
    const { canRead, canCreate, canUpdate, canDelete, isLoading } = useApiProductResourcePermissions(productId, 'PLAN');

    if (isLoading) {
        return (
            <div className="space-y-3 p-6">
                <Skeleton className="h-24 w-full rounded-lg" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    return <PlansPage ctx={ctx} canRead={canRead} canCreate={canCreate} canUpdate={canUpdate} canDelete={canDelete} />;
}
