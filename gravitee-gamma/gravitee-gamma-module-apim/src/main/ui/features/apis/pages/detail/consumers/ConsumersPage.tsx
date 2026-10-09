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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { Button, Skeleton } from '@gravitee/graphene-core';
import { DownloadIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { ConsumersEmptyState } from './ConsumersEmptyState';
import { ConsumersFilterBar } from './ConsumersFilterBar';
import { ConsumersSummaryCards } from './ConsumersSummaryCards';
import { ConsumersTable } from './ConsumersTable';
import { CreateSubscription } from './CreateSubscription';
import { downloadBlob } from '../../../../../shared/browser';
import { notify } from '../../../../../shared/notify';
import { useCreateSubscription } from '../../../hooks/useSubscriptionActions';
import {
    ALL_SUBSCRIPTION_STATUSES,
    DEFAULT_STATUSES,
    useApiPlans,
    useSubscriptionCount,
    useSubscriptionList,
} from '../../../hooks/useSubscriptions';
import { exportSubscriptionsCsv } from '../../../services/subscriptions';
import type { ApiKeyMode, SubscriptionContext, SubscriptionFilters } from '../../../types/subscription';

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
    isFederated?: boolean;
}

export function ConsumersPage({ ctx, canCreate, canRead, isFederated = false }: ConsumersPageProps) {
    const [filters, setFilters] = useState<SubscriptionFilters>(EMPTY_FILTERS);
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [isExporting, setIsExporting] = useState(false);
    const env = useEnvironment();

    const entityLabel = ctx.type === 'api-product' ? 'API Product' : 'API';
    const { data, isLoading: isLoadingList, isFetching: isFetchingList } = useSubscriptionList(ctx, filters, page, perPage);
    const { data: acceptedCount = 0, isLoading: isLoadingAccepted } = useSubscriptionCount(ctx, ['ACCEPTED']);
    const { data: pendingCount = 0, isLoading: isLoadingPending } = useSubscriptionCount(ctx, ['PENDING']);
    const { data: anySubscriptionCount = 0, isLoading: isLoadingAny } = useSubscriptionCount(ctx, ALL_SUBSCRIPTION_STATUSES);
    // First load only. A later filter change must not replace this page — that unmounts the status multiselect.
    const isInitialLoading = isLoadingAny || isLoadingAccepted || isLoadingPending;
    const hasAnySubscription = anySubscriptionCount > 0;
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
        (applicationId: string, planId: string, options?: { customApiKey?: string; apiKeyMode?: ApiKeyMode }) => {
            createMutation.mutate(
                { applicationId, planId, customApiKey: options?.customApiKey, apiKeyMode: options?.apiKeyMode },
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

    const handleExport = useCallback(async () => {
        if (!env) return;
        setIsExporting(true);
        try {
            const blob = await exportSubscriptionsCsv(env.id, ctx.entityId, {
                ...filters,
                statuses: filters.statuses.length ? filters.statuses : [...DEFAULT_STATUSES],
                page: 1,
                perPage: totalCount,
            });
            downloadBlob(blob, `subscriptions-${ctx.entityId}.csv`);
        } catch (error) {
            notify.error(error instanceof Error ? error.message : 'Failed to export subscriptions');
        } finally {
            setIsExporting(false);
        }
    }, [env, ctx.entityId, filters, totalCount]);

    if (!canRead) {
        return (
            <div className="flex flex-col gap-6">
                <h1 className="text-2xl font-semibold tracking-tight">Consumers</h1>
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view subscriptions.</p>
            </div>
        );
    }

    const header = (
        <div className="flex items-center justify-between gap-4">
            <div>
                <h1 className="text-2xl font-semibold tracking-tight">Consumers</h1>
                <p className="text-sm text-muted-foreground">View and manage {entityLabel} consumers and their usage.</p>
            </div>
            <div className="flex items-center gap-2">
                {hasAnySubscription && ctx.type === 'api' && (
                    <Button type="button" variant="outline" size="sm" disabled={isExporting || totalCount === 0} onClick={handleExport}>
                        <DownloadIcon className="size-4" aria-hidden />
                        Export CSV
                    </Button>
                )}
                {canCreate && (
                    <Button type="button" size="sm" onClick={() => setDialogOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden />
                        Create subscription
                    </Button>
                )}
            </div>
        </div>
    );

    const createDialog = canCreate ? (
        <CreateSubscription
            ctx={ctx}
            open={dialogOpen}
            isPending={createMutation.isPending}
            error={createMutation.error?.message ?? null}
            isFederated={isFederated}
            onConfirm={handleCreate}
            onClose={() => {
                setDialogOpen(false);
                createMutation.reset();
            }}
        />
    ) : null;

    if (isInitialLoading) {
        return (
            <div className="flex flex-col gap-6">
                {header}
                <Skeleton className="h-20 w-full rounded-lg" />
                <Skeleton className="h-10 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-lg" />
            </div>
        );
    }

    if (!hasAnySubscription) {
        return (
            <div className="flex flex-col gap-6">
                {header}
                <ConsumersEmptyState />
                {createDialog}
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            {header}

            <ConsumersSummaryCards totalCount={totalCount} acceptedCount={acceptedCount} pendingCount={pendingCount} isLoading={false} />

            <ConsumersFilterBar filters={filters} plans={plans} ctx={ctx} onChange={handleFilterChange} />

            <ConsumersTable
                subscriptions={data?.data ?? []}
                totalCount={data?.pagination.totalCount ?? 0}
                page={page}
                perPage={perPage}
                isLoading={isLoadingList || isFetchingList}
                onPage={setPage}
                onPerPageChange={handlePerPageChange}
            />

            {createDialog}
        </div>
    );
}
