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
import { Skeleton } from '@gravitee/graphene-core';
import { useParams } from 'react-router-dom';

import { ConsumersPage } from './ConsumersPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import type { SubscriptionContext } from '../../../types/subscription';
import { hasTcpListeners } from '../../../utils/apiHttpProxy';
import { TcpProxyUnavailableNotice } from '../response-templates/TcpProxyUnavailableNotice';

export function ApiConsumersPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const ctx: SubscriptionContext = { type: 'api', entityId: apiId ?? '' };
    const canCreate = useHasPermission({ anyOf: ['api-subscription-c'] });
    const canRead = useHasPermission({ anyOf: ['api-subscription-r'] });
    const { data: api, isLoading } = useApiDetail(apiId);

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-12 w-full rounded" />
                <Skeleton className="h-64 w-full rounded" />
            </div>
        );
    }

    if (hasTcpListeners(api)) {
        return <TcpProxyUnavailableNotice feature="Subscriptions" />;
    }

    return <ConsumersPage ctx={ctx} canCreate={canCreate} canRead={canRead} />;
}
