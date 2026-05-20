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

import { ConsumersPage } from '../../../../apis/pages/detail/consumers/ConsumersPage';
import type { SubscriptionContext } from '../../../../apis/types/subscription';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';

export function ApiProductConsumersPage() {
    const { productId } = useParams<{ productId: string }>();
    const ctx: SubscriptionContext = { type: 'api-product', entityId: productId ?? '' };
    const { canRead, canCreate, isLoading } = useApiProductResourcePermissions(productId, 'subscription');

    if (isLoading) {
        return (
            <div className="space-y-3 p-6">
                <Skeleton className="h-24 w-full rounded-lg" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    return <ConsumersPage ctx={ctx} canCreate={canCreate} canRead={canRead} />;
}
