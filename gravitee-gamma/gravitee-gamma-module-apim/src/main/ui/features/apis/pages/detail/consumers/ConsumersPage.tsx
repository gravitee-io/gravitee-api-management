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
import { Button, useLayoutConfig } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { ConsumersEmptyState } from './ConsumersEmptyState';
import { ConsumersFilterBar } from './ConsumersFilterBar';
import { ConsumersSummaryCards } from './ConsumersSummaryCards';
import { ConsumersTable } from './ConsumersTable';
import { CreateSubscription } from './CreateSubscription';
import { notify } from '../../../../../shared/notify';
import { useCreateSubscription } from '../../../hooks/useSubscriptionActions';
import { isSubscriptionFiltersDirty, useApiPlans, useSubscriptionCount, useSubscriptionList } from '../../../hooks/useSubscriptions';
import type { SubscriptionContext, SubscriptionFilters } from '../../../types/subscription';

const EMPTY_FILTERS: SubscriptionFilters = {
    statuses: [],
    planIds: [],
    applicationIds: [],
    apiKey: '',
};

interface ConsumersPageProps {
    ctx: SubscriptionContext;
    canCreate: boolean;
    canRead: boolean;
}

export function ConsumersPage({ ctx, canCreate, canRead }: ConsumersPageProps) {
    useLayoutConfig({ contentVariant: 'wide' }, []);
    const [filters, setFilters] = useState<SubscriptionFilters>(EMPTY_FILTERS);
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);
    const [dialogOpen, setDialogOpen] = useState(false);

    const entityLabel = ctx.type === 'api-product' ? 'API Product' : 'API';
    const { data, isLoading: isLoadingList } = useSubscriptionList(ctx, filters, page, perPage);
    const { data: acceptedCount = 0, isLoading: isLoadingAccepted } = useSubscriptionCount(ctx, ['ACCEPTED']);
    const { data: pendingCount = 0, isLoading: isLoadingPending } = useSubscriptionCount(ctx, ['PENDING']);
    const isLoading = isLoadingList || isLoadingAccepted || isLoadingPending;
    const { data: plans = [] } = useApiPlans(ctx);
    const createMutation = useCreateSubscription(ctx);

    const handleFilterChange = useCallback((next: SubscriptionFilters) => {
        setFilters(next);
        setPage(1);
    }, []);

    const handlePerPageChange = useCallback((next: number) => {
        setPerPage(next);
        setPage(1);
    }, []);

    const handleCreate = useCallback(
        (applicationId: string, planId: string) => {
            createMutation.mutate(
                { applicationId, planId },
                {
                    onSuccess: () => {
                        notify.success('Subscription created');
                        setDialogOpen(false);
                    },
                },
            );
        },
        [createMutation],
    );

    const totalCount = data?.pagination.totalCount ?? 0;
    const hasAnySubscriptions = totalCount > 0 || isLoading;
    const isFiltered = isSubscriptionFiltersDirty(filters);

    if (!canRead) {
        return (
            <div className="flex flex-col gap-6">
                <h1 className="text-2xl font-semibold tracking-tight">Consumers</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view subscriptions.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold tracking-tight">Consumers</h1>
                    <p className="text-sm text-muted-foreground">View and manage {entityLabel} consumers and their usage.</p>
                </div>
                {canCreate && (
                    <Button type="button" size="sm" onClick={() => setDialogOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden />
                        Create subscription
                    </Button>
                )}
            </div>

            {!hasAnySubscriptions && !isFiltered ? (
                <ConsumersEmptyState />
            ) : (
                <>
                    <ConsumersSummaryCards
                        totalCount={totalCount}
                        acceptedCount={acceptedCount}
                        pendingCount={pendingCount}
                        isLoading={isLoading}
                    />

                    <ConsumersFilterBar filters={filters} plans={plans} onChange={handleFilterChange} />

                    <ConsumersTable
                        subscriptions={data?.data ?? []}
                        totalCount={data?.pagination.totalCount ?? 0}
                        page={page}
                        perPage={perPage}
                        isLoading={isLoading}
                        onPage={setPage}
                        onPerPageChange={handlePerPageChange}
                    />
                </>
            )}

            {canCreate && (
                <CreateSubscription
                    ctx={ctx}
                    open={dialogOpen}
                    isPending={createMutation.isPending}
                    error={createMutation.error?.message ?? null}
                    onConfirm={handleCreate}
                    onClose={() => {
                        setDialogOpen(false);
                        createMutation.reset();
                    }}
                />
            )}
        </div>
    );
}
