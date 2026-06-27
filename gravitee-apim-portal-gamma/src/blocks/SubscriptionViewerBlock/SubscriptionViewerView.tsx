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
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { Application } from '../../features/editor/entities/application';
import type { Subscription, SubscriptionStatus } from '../../features/editor/entities/subscription';
import { searchApis } from '../../features/editor/services/api.service';
import { listApplications } from '../../features/editor/services/applications.service';
import { listSubscriptions } from '../../features/editor/services/subscriptions.service';
import { SubscriptionDetailsPanel } from './SubscriptionDetailsPanel';
import { SubscriptionsTable } from './SubscriptionsTable';
import { SUBSCRIPTION_STATUSES, toTableRow, formatStatus } from './utils';

import styles from './SubscriptionViewerBlock.module.scss';

const PAGE_SIZE = 5;

interface FilterOption {
    value: string;
    label: string;
}

interface SubscriptionViewerViewProps {
    readonly isEditable?: boolean;
}

export function SubscriptionViewerView({ isEditable = false }: SubscriptionViewerViewProps) {
    const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
    const [totalItems, setTotalItems] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [loading, setLoading] = useState(true);

    const [apiFilter, setApiFilter] = useState<string>('');
    const [applicationFilter, setApplicationFilter] = useState<string>('');
    const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | ''>('');
    const [page, setPage] = useState(1);

    const [apiOptions, setApiOptions] = useState<FilterOption[]>([]);
    const [applicationOptions, setApplicationOptions] = useState<FilterOption[]>([]);

    const [selectedId, setSelectedId] = useState<string | null>(null);
    const [selectedSubscription, setSelectedSubscription] = useState<Subscription | null>(null);

    useEffect(() => {
        void (async () => {
            const [apisResponse, appsResponse] = await Promise.all([
                searchApis({ size: 50 }),
                listApplications({ size: 50 }),
            ]);
            setApiOptions(
                (apisResponse.data ?? []).map(api => ({ value: api.id, label: api.name })),
            );
            setApplicationOptions(
                appsResponse.data.map((app: Application) => ({ value: app.id, label: app.name })),
            );
        })();
    }, []);

    const loadSubscriptions = useCallback(async () => {
        setLoading(true);
        try {
            const response = await listSubscriptions({
                apiIds: apiFilter ? [apiFilter] : undefined,
                applicationIds: applicationFilter ? [applicationFilter] : undefined,
                statuses: statusFilter ? [statusFilter] : undefined,
                page,
                size: PAGE_SIZE,
            });
            setSubscriptions(response.data);
            setTotalItems(response.metadata?.pagination?.total ?? response.data.length);
            setTotalPages(response.metadata?.pagination?.total_pages ?? 1);
        } finally {
            setLoading(false);
        }
    }, [apiFilter, applicationFilter, statusFilter, page]);

    useEffect(() => {
        void loadSubscriptions();
    }, [loadSubscriptions]);

    useEffect(() => {
        if (!selectedId) {
            setSelectedSubscription(null);
            return;
        }
        const found = subscriptions.find(sub => sub.id === selectedId);
        setSelectedSubscription(found ?? null);
    }, [selectedId, subscriptions]);

    const rows = useMemo(() => subscriptions.map(toTableRow), [subscriptions]);

    const clearFilters = () => {
        setApiFilter('');
        setApplicationFilter('');
        setStatusFilter('');
        setPage(1);
    };

    const hasFilters = !!apiFilter || !!applicationFilter || !!statusFilter;

    const handleSelectRow = (id: string) => {
        if (isEditable) return;
        setSelectedId(id);
    };

    const handleClosePanel = () => setSelectedId(null);

    const handleSubscriptionUpdated = (updated: Subscription) => {
        setSelectedSubscription(updated);
        setSubscriptions(prev => prev.map(sub => (sub.id === updated.id ? updated : sub)));
    };

    if (loading && subscriptions.length === 0 && !hasFilters) {
        return (
            <div className={styles.wrapper}>
                <p className={styles.loadingMessage}>Loading subscriptions…</p>
            </div>
        );
    }

    const showEmptyState = !loading && subscriptions.length === 0 && !hasFilters;

    if (showEmptyState) {
        return (
            <div className={styles.wrapper}>
                <div className={styles.emptyState}>
                    <p className={styles.emptyTitle}>No API subscriptions yet</p>
                    <p className={styles.emptyMessage}>
                        Browse the catalog to discover available APIs and subscribe with an application.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.wrapper}>
            <div className={styles.filters}>
                <FilterSingleSelect
                    label="API"
                    options={apiOptions}
                    selected={apiFilter}
                    onChange={value => {
                        setApiFilter(value);
                        setPage(1);
                    }}
                />
                <FilterSingleSelect
                    label="Application"
                    options={applicationOptions}
                    selected={applicationFilter}
                    onChange={value => {
                        setApplicationFilter(value);
                        setPage(1);
                    }}
                />
                <FilterSingleSelect
                    label="Status"
                    options={SUBSCRIPTION_STATUSES.map(status => ({
                        value: status,
                        label: formatStatus(status),
                    }))}
                    selected={statusFilter}
                    onChange={value => {
                        setStatusFilter(value as SubscriptionStatus | '');
                        setPage(1);
                    }}
                />
                {hasFilters && (
                    <button type="button" className={styles.clearFiltersBtn} onClick={clearFilters}>
                        Clear filters
                    </button>
                )}
            </div>

            {loading && subscriptions.length === 0 ? (
                <p className={styles.loadingMessage}>Loading subscriptions…</p>
            ) : rows.length === 0 ? (
                <div className={styles.emptyFiltered}>
                    <p className={styles.emptyTitle}>No subscriptions found</p>
                    <p className={styles.emptyMessage}>Try adjusting your filters.</p>
                </div>
            ) : (
                <SubscriptionsTable
                    rows={rows}
                    selectedId={selectedId}
                    currentPage={page}
                    totalPages={totalPages}
                    totalItems={totalItems}
                    pageSize={PAGE_SIZE}
                    onSelectRow={handleSelectRow}
                    onPageChange={setPage}
                />
            )}

            {selectedSubscription && !isEditable && (
                <SubscriptionDetailsPanel
                    subscription={selectedSubscription}
                    onClose={handleClosePanel}
                    onUpdated={handleSubscriptionUpdated}
                />
            )}
        </div>
    );
}

interface FilterSingleSelectProps {
    readonly label: string;
    readonly options: FilterOption[];
    readonly selected: string;
    readonly onChange: (value: string) => void;
}

function FilterSingleSelect({ label, options, selected, onChange }: FilterSingleSelectProps) {
    return (
        <label className={styles.filterField}>
            <span className={styles.filterLabel}>{label}</span>
            <select
                className={styles.filterSelect}
                value={selected}
                onChange={event => onChange(event.target.value)}
            >
                <option value="">All</option>
                {options.map(option => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>
        </label>
    );
}
