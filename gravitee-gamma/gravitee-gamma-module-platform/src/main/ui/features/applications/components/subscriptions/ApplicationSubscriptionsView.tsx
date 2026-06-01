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
import { Alert, AlertDescription, Button, DataTablePagination, Input, cn } from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { ApplicationSubscriptionCloseDialog } from './ApplicationSubscriptionCloseDialog';
import { ApplicationSubscriptionCreateDialog } from './ApplicationSubscriptionCreateDialog';
import { ApplicationSubscriptionMultiSelectFilter } from './ApplicationSubscriptionMultiSelectFilter';
import { ApplicationSubscriptionsTable } from './ApplicationSubscriptionsTable';
import { ApplicationSubscriptionStatusDetails } from './ApplicationSubscriptionStatusDetails';
import { notify } from '../../../../shared/notify';
import { useDetailBasePath } from '../../../shared/hooks/useDetailBasePath';
import { useApplicationSubscriptionPermissions } from '../../hooks/useApplicationSubscriptionPermissions';
import { useApplicationSubscriptions, useSubscribedApis } from '../../hooks/useApplicationSubscriptions';
import { useCloseApplicationSubscription } from '../../hooks/useCloseApplicationSubscription';
import type { ApplicationListItem } from '../../types/application';
import type { ApplicationSubscriptionTableRow, SubscriptionStatus } from '../../types/applicationSubscription';
import {
    DEFAULT_SUBSCRIPTION_FILTER_STATUSES,
    SUBSCRIPTION_PAGE_SIZE_OPTIONS,
    SUBSCRIPTION_STATUS_OPTIONS,
} from '../../utils/applicationSubscriptionConstants';
import {
    buildApplicationSubscriptionListSearchParams,
    parseApplicationSubscriptionListSearchParams,
} from '../../utils/applicationSubscriptionListSearchParams';

const FILTER_FIELD_WIDTH = 'w-[220px]';
const API_KEY_DEBOUNCE_MS = 250;

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

            <ApplicationSubscriptionStatusDetails />

            <div className="flex flex-wrap items-center gap-3">
                <ApplicationSubscriptionMultiSelectFilter
                    placeholder="API"
                    ariaLabel="Filter by API"
                    options={apiOptions}
                    selectedValues={apiFilters}
                    onSelectedValuesChange={setApiFilters}
                    emptyMessage="No subscribed APIs yet"
                />
                <ApplicationSubscriptionMultiSelectFilter
                    placeholder="Status"
                    ariaLabel="Filter by status"
                    options={statusOptions}
                    selectedValues={statusFilters}
                    onSelectedValuesChange={values => setStatusFilters(values as SubscriptionStatus[])}
                />
                <Input
                    placeholder="API Key"
                    value={apiKeyInput}
                    onChange={e => setApiKeyInput(e.target.value)}
                    className={cn(FILTER_FIELD_WIDTH, 'h-9 shrink-0')}
                    aria-label="Filter by API key"
                />
                <Button type="button" variant="outline" size="sm" className="shrink-0" onClick={resetFilters}>
                    <RefreshCwIcon className="size-4" aria-hidden />
                    Reset filters
                </Button>
            </div>

            {isError ? (
                <Alert variant="destructive">
                    <AlertDescription>Unable to get subscriptions. Please try again.</AlertDescription>
                </Alert>
            ) : null}

            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={isError ? 0 : totalCount}
                    pageSizeOptions={SUBSCRIPTION_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>

            <ApplicationSubscriptionsTable
                rows={isError ? [] : rows}
                isLoading={isLoading && !isError}
                skeletonRowCount={pageSize}
                readOnly={readOnly}
                canViewDetail={canViewDetail}
                canClose={canDelete}
                onView={row => navigate(`${basePath}/subscriptions/${row.id}`)}
                onClose={row => setCloseTarget(row)}
            />

            <div className="flex justify-end">
                <DataTablePagination
                    page={page}
                    pageSize={pageSize}
                    totalCount={isError ? 0 : totalCount}
                    pageSizeOptions={SUBSCRIPTION_PAGE_SIZE_OPTIONS}
                    onPageChange={setPage}
                    onPageSizeChange={handlePageSizeChange}
                />
            </div>

            {canCreateSubscription ? (
                <ApplicationSubscriptionCreateDialog
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
