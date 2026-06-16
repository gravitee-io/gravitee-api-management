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
import { Alert, AlertDescription, Button, Input } from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { ApplicationSubscriptionCloseDialog } from './ApplicationSubscriptionCloseDialog';
import { ApplicationSubscriptionCreateSheet } from './ApplicationSubscriptionCreateSheet';
import { ApplicationSubscriptionMultiSelectFilter } from './ApplicationSubscriptionMultiSelectFilter';
import { ApplicationSubscriptionsTable } from './ApplicationSubscriptionsTable';
import { ApplicationSubscriptionStatusDetails } from './ApplicationSubscriptionStatusDetails';
import { ApplicationSubscriptionSummaryCards } from './ApplicationSubscriptionSummaryCards';
import { notify } from '../../../../shared/notify';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { useApplicationSubscriptionPermissions } from '../../hooks/useApplicationSubscriptionPermissions';
import { useApplicationSubscriptionCount, useApplicationSubscriptions, useSubscribedApis } from '../../hooks/useApplicationSubscriptions';
import { useCloseApplicationSubscription } from '../../hooks/useCloseApplicationSubscription';
import type { ApplicationListItem } from '../../types/application';
import type {
    ApplicationSubscriptionsFilters,
    ApplicationSubscriptionTableRow,
    SubscriptionStatus,
} from '../../types/applicationSubscription';
import {
    ALL_SUBSCRIPTION_STATUSES,
    DEFAULT_SUBSCRIPTION_FILTER_STATUSES,
    SUBSCRIPTION_STATUS_OPTIONS,
} from '../../utils/applicationSubscriptionConstants';
import {
    buildApplicationSubscriptionListSearchParams,
    parseApplicationSubscriptionListSearchParams,
} from '../../utils/applicationSubscriptionListSearchParams';

const API_KEY_DEBOUNCE_MS = 250;

/** Stable filter references so the summary-count queries stay cache-keyed independently of the table filters. */
const ALL_STATUS_COUNT_FILTER: ApplicationSubscriptionsFilters = { status: ALL_SUBSCRIPTION_STATUSES };
const ACCEPTED_COUNT_FILTER: ApplicationSubscriptionsFilters = { status: ['ACCEPTED'] };
const PENDING_COUNT_FILTER: ApplicationSubscriptionsFilters = { status: ['PENDING'] };

export function ApplicationSubscriptionsView({ application }: Readonly<{ application: ApplicationListItem }>) {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const basePath = useDetailBasePath('applications', application.id);
    const readOnly = application.status === 'ARCHIVED';
    const { canCreate, canDelete, canViewDetail } = useApplicationSubscriptionPermissions();

    const [apiFilters, setApiFilters] = useState<string[]>(() => parseApplicationSubscriptionListSearchParams(searchParams).apiFilters);
    const [statusFilters, setStatusFilters] = useState<SubscriptionStatus[]>(
        () => parseApplicationSubscriptionListSearchParams(searchParams).statusFilters,
    );
    const [apiKeyInput, setApiKeyInput] = useState(() => parseApplicationSubscriptionListSearchParams(searchParams).apiKeyInput);
    const [debouncedApiKey, setDebouncedApiKey] = useState(() => parseApplicationSubscriptionListSearchParams(searchParams).apiKeyInput);
    const [page, setPage] = useState(() => parseApplicationSubscriptionListSearchParams(searchParams).page);
    const [pageSize, setPageSize] = useState(() => parseApplicationSubscriptionListSearchParams(searchParams).pageSize);
    const [createOpen, setCreateOpen] = useState(false);
    const [closeTarget, setCloseTarget] = useState<ApplicationSubscriptionTableRow | null>(null);
    const canCreateSubscription = !readOnly && canCreate;
    const loadErrorNotifiedRef = useRef(false);

    useEffect(() => {
        const timer = window.setTimeout(() => setDebouncedApiKey(apiKeyInput.trim()), API_KEY_DEBOUNCE_MS);
        return () => window.clearTimeout(timer);
    }, [apiKeyInput]);

    useEffect(() => {
        if (!canCreateSubscription && createOpen) {
            setCreateOpen(false);
        }
    }, [canCreateSubscription, createOpen]);

    useEffect(() => {
        setPage(1);
    }, [apiFilters, statusFilters, debouncedApiKey]);

    useEffect(() => {
        setSearchParams(
            buildApplicationSubscriptionListSearchParams({
                page,
                pageSize,
                apiFilters,
                statusFilters,
                apiKeyInput: debouncedApiKey,
            }),
            { replace: true },
        );
    }, [apiFilters, debouncedApiKey, page, pageSize, setSearchParams, statusFilters]);

    const filters = useMemo(
        () => ({
            apis: apiFilters.length > 0 ? apiFilters : undefined,
            status: statusFilters.length > 0 ? statusFilters : undefined,
            apiKey: debouncedApiKey || undefined,
        }),
        [apiFilters, statusFilters, debouncedApiKey],
    );

    const { data, isLoading, isError } = useApplicationSubscriptions(application.id, filters, page, pageSize, application.api_key_mode);
    const { data: subscribedApis = [] } = useSubscribedApis(application.id);
    const closeMutation = useCloseApplicationSubscription(application.id);

    // Summary cards reflect all subscriptions for the application, independent of the active table filters.
    // Count all statuses explicitly — the v1 endpoint defaults to ACCEPTED-only when no status is provided.
    const { data: summaryTotalCount = 0, isLoading: isTotalLoading } = useApplicationSubscriptionCount(
        application.id,
        ALL_STATUS_COUNT_FILTER,
    );
    const { data: acceptedCount = 0, isLoading: isAcceptedLoading } = useApplicationSubscriptionCount(
        application.id,
        ACCEPTED_COUNT_FILTER,
    );
    const { data: pendingCount = 0, isLoading: isPendingLoading } = useApplicationSubscriptionCount(application.id, PENDING_COUNT_FILTER);
    const isSummaryLoading = isTotalLoading || isAcceptedLoading || isPendingLoading;

    const apiOptions = useMemo(
        () =>
            subscribedApis
                .filter(api => api.id && api.name)
                .map(api => ({ value: api.id!, label: api.name! }))
                .sort((a, b) => a.label.localeCompare(b.label)),
        [subscribedApis],
    );

    const statusOptions = useMemo(() => SUBSCRIPTION_STATUS_OPTIONS.map(s => ({ value: s.id, label: s.name })), []);

    const resetFilters = () => {
        setApiFilters([]);
        setStatusFilters([...DEFAULT_SUBSCRIPTION_FILTER_STATUSES]);
        setApiKeyInput('');
        setPage(1);
    };

    const handlePageSizeChange = (size: number) => {
        setPageSize(size);
        setPage(1);
    };

    useEffect(() => {
        if (isError && !loadErrorNotifiedRef.current) {
            notify.error('Unable to get subscriptions, please try again');
            loadErrorNotifiedRef.current = true;
        }
        if (!isError) {
            loadErrorNotifiedRef.current = false;
        }
    }, [isError]);

    const handleClose = async () => {
        if (!closeTarget) return;
        try {
            await closeMutation.mutateAsync(closeTarget.id);
            notify.success('The subscription has been closed');
            setCloseTarget(null);
        } catch (error) {
            notify.error(error, 'An error occurred while closing the subscription!');
        }
    };

    const rows = data?.rows ?? [];
    const totalCount = data?.totalCount ?? 0;

    const subscriptionsToolbar = (
        <>
            <ApplicationSubscriptionMultiSelectFilter
                placeholder="All APIs"
                ariaLabel="Filter by API"
                options={apiOptions}
                selectedValues={apiFilters}
                onSelectedValuesChange={setApiFilters}
                emptyMessage="No subscribed APIs yet"
            />
            <ApplicationSubscriptionMultiSelectFilter
                placeholder="All statuses"
                ariaLabel="Filter by status"
                options={statusOptions}
                selectedValues={statusFilters}
                onSelectedValuesChange={values => setStatusFilters(values as SubscriptionStatus[])}
            />
            <Input
                placeholder="Search by API key…"
                value={apiKeyInput}
                onChange={e => setApiKeyInput(e.target.value)}
                className="h-8 w-52"
                aria-label="Filter by API key"
            />
            <Button type="button" variant="ghost" size="sm" className="gap-1.5 text-muted-foreground" onClick={resetFilters}>
                <RefreshCwIcon className="size-4" aria-hidden />
                Reset filters
            </Button>
        </>
    );

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold">Subscriptions</h1>
                    <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                        Retrieve all subscriptions for this application. A subscription represents the plans this application has subscribed
                        to.
                    </p>
                </div>
                {canCreateSubscription ? (
                    <Button type="button" size="sm" className="shrink-0" onClick={() => setCreateOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden />
                        Create a subscription
                    </Button>
                ) : null}
            </div>

            <ApplicationSubscriptionSummaryCards
                totalCount={summaryTotalCount}
                acceptedCount={acceptedCount}
                pendingCount={pendingCount}
                isLoading={isSummaryLoading}
            />

            <ApplicationSubscriptionStatusDetails />

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Unable to get subscriptions. Please try again.</AlertDescription>
                </Alert>
            ) : (
                <ApplicationSubscriptionsTable
                    rows={rows}
                    isLoading={isLoading}
                    skeletonRowCount={pageSize}
                    readOnly={readOnly}
                    canViewDetail={canViewDetail}
                    canClose={canDelete}
                    onView={row => navigate(`${basePath}/subscriptions/${row.id}`)}
                    onClose={row => setCloseTarget(row)}
                    page={page}
                    pageSize={pageSize}
                    totalCount={totalCount}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                    toolbar={subscriptionsToolbar}
                />
            )}

            {canCreateSubscription ? (
                <ApplicationSubscriptionCreateSheet
                    application={application}
                    basePath={basePath}
                    open={createOpen}
                    onOpenChange={setCreateOpen}
                />
            ) : null}

            <ApplicationSubscriptionCloseDialog
                subscription={closeTarget}
                onClose={() => setCloseTarget(null)}
                onConfirm={() => void handleClose()}
                isLoading={closeMutation.isPending}
            />
        </div>
    );
}
