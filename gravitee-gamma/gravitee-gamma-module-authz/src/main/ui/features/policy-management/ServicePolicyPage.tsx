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
import {
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon } from '@gravitee/graphene-core/icons';
import { useDeferredValue, useMemo, useState } from 'react';
import { KpiTile } from '../../components/KpiTile';
import { ValidationErrorAlert } from '../../components/ValidationErrorAlert';
import type { PolicyResponse, PolicyStatus, PolicyType } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';
import { usePolicies } from '../../shared/hooks/usePolicies';
import { PolicyListTable } from './PolicyListTable';

type StatusFilter = PolicyStatus | 'ALL';

export interface ServicePageConfig {
    readonly type: PolicyType;
    readonly title: string;
    readonly description: string;
    readonly createButtonLabel: string;
    readonly searchPlaceholder: string;
    readonly icon?: React.ComponentType<{ className?: string }>;
    readonly hasTarget: boolean;
    readonly serviceLabel: string;
    readonly resourceGroups?: readonly { key: string; label: string }[];
    readonly resourceOptions?: readonly ChipOption[];
    readonly conditionSnippets?: readonly { label: string; snippet: string }[];
}

export function ServicePolicyPage({ config }: { readonly config: ServicePageConfig }) {
    const env = useEnvironment();
    const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
    const [search, setSearch] = useState('');
    const deferredSearch = useDeferredValue(search);
    const [pendingDelete, setPendingDelete] = useState<PolicyResponse | null>(null);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<Error | null>(null);

    const policies = usePolicies(env?.id ?? '', {
        type: config.type,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
    });

    const allPolicies = useMemo<readonly PolicyResponse[]>(() => policies.data?.data ?? [], [policies.data]);

    const filteredPolicies = useMemo(() => {
        const needle = deferredSearch.trim().toLowerCase();
        if (!needle) return allPolicies;
        return allPolicies.filter(p => p.name.toLowerCase().includes(needle));
    }, [allPolicies, deferredSearch]);

    const total = policies.data?.total ?? allPolicies.length;
    const isPaginated = total > allPolicies.length;
    const pageSuffix = isPaginated ? ' (this page)' : '';

    const kpis = useMemo(
        () => ({
            total,
            deployed: allPolicies.filter(p => p.status === 'DEPLOYED').length,
            draft: allPolicies.filter(p => p.status === 'DRAFT').length,
            uniqueTargets: new Set(allPolicies.map(p => p.target?.id).filter(Boolean)).size,
        }),
        [allPolicies, total],
    );

    const confirmDelete = async () => {
        if (!pendingDelete) return;
        setDeleting(true);
        setDeleteError(null);
        try {
            await policies.remove(pendingDelete.id);
            setPendingDelete(null);
        } catch (err) {
            setDeleteError(err instanceof Error ? err : new Error(String(err)));
        } finally {
            setDeleting(false);
        }
    };

    const closeDeleteDialog = () => {
        setPendingDelete(null);
        setDeleteError(null);
    };

    const Icon = config.icon;
    const totalCount = policies.data?.total ?? 0;
    const hasNoPolicies = !policies.isLoading && totalCount === 0 && !deferredSearch && statusFilter === 'ALL';
    const hasNoMatches = !policies.isLoading && filteredPolicies.length === 0 && totalCount > 0;

    return (
        <div className="flex flex-col gap-4" data-testid={`page-policies-${config.type.toLowerCase()}`}>
            <header className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                    {Icon ? <Icon className="mt-1 size-5 text-muted-foreground" /> : null}
                    <div>
                        <h1 className="text-xl font-semibold">{config.title}</h1>
                        <p className="mt-1 max-w-3xl text-sm text-muted-foreground">{config.description}</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <Button type="button" variant="ghost" size="icon" onClick={() => policies.reload()} aria-label="Refresh">
                        <RefreshCwIcon aria-hidden className="size-4" />
                    </Button>
                    <Button type="button" disabled title="Policy editor lands in the follow-up PR">
                        <PlusIcon aria-hidden className="size-4" />
                        {config.hasTarget ? 'Create policy' : config.createButtonLabel}
                    </Button>
                </div>
            </header>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-4" aria-label="Key metrics">
                <KpiTile label="Policies" value={kpis.total} loading={policies.isLoading} />
                <KpiTile label={`Deployed${pageSuffix}`} value={kpis.deployed} loading={policies.isLoading} tone="success" />
                <KpiTile label={`Draft${pageSuffix}`} value={kpis.draft} loading={policies.isLoading} tone="muted" />
                {config.hasTarget ? (
                    <KpiTile label={`Unique targets${pageSuffix}`} value={kpis.uniqueTargets} loading={policies.isLoading} />
                ) : null}
            </div>

            <div className="flex items-center gap-2">
                <Input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder={config.searchPlaceholder || 'Search policies…'}
                    className="max-w-sm"
                    aria-label="Search policies"
                />
                <Select value={statusFilter} onValueChange={v => setStatusFilter(v as StatusFilter)}>
                    <SelectTrigger className="w-44" aria-label="Filter by status">
                        <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">All statuses</SelectItem>
                        <SelectItem value="DRAFT">Draft</SelectItem>
                        <SelectItem value="DEPLOYED">Deployed</SelectItem>
                        <SelectItem value="DISABLED">Disabled</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            {hasNoPolicies ? (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No policies yet</EmptyTitle>
                        <EmptyDescription>The editor that creates policies arrives in the next PR.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : hasNoMatches ? (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No policies match your filters</EmptyTitle>
                        <EmptyDescription>Try adjusting the search or status filter.</EmptyDescription>
                    </EmptyHeader>
                </Empty>
            ) : (
                <PolicyListTable
                    config={config}
                    policies={filteredPolicies}
                    totalCount={totalCount}
                    page={policies.page}
                    perPage={policies.perPage}
                    isLoading={policies.isLoading}
                    onPageChange={policies.setPage}
                    onPerPageChange={policies.setPerPage}
                    onEdit={noopEdit}
                    onDelete={setPendingDelete}
                />
            )}

            <Dialog open={pendingDelete !== null} onOpenChange={open => !open && closeDeleteDialog()}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>Delete policy?</DialogTitle>
                        <DialogDescription>
                            {pendingDelete ? `"${pendingDelete.name}" will be permanently removed. This action cannot be undone.` : ''}
                        </DialogDescription>
                    </DialogHeader>
                    {deleteError ? <ValidationErrorAlert error={deleteError} title="Delete failed" /> : null}
                    <DialogFooter>
                        <Button variant="outline" onClick={closeDeleteDialog} disabled={deleting}>
                            Cancel
                        </Button>
                        <Button variant="destructive" onClick={confirmDelete} disabled={deleting}>
                            {deleting ? 'Deleting…' : deleteError ? 'Retry' : 'Delete'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}

// TODO(authz-ui-editor): wire to the policy editor Sheet in the follow-up PR.
function noopEdit() {
    /* no-op until the editor PR lands */
}
