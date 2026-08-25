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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { GitBranchIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';

import { sortToOrder, type TableSortingState } from '../features/applications/utils/tableSort';
import { SharedPolicyGroupHistoryCompareDialog } from '../features/shared-policy-groups/components/SharedPolicyGroupHistoryCompareDialog';
import { SharedPolicyGroupHistoryDetailsDialog } from '../features/shared-policy-groups/components/SharedPolicyGroupHistoryDetailsDialog';
import { SharedPolicyGroupHistoryJsonDialog } from '../features/shared-policy-groups/components/SharedPolicyGroupHistoryJsonDialog';
import { SharedPolicyGroupHistoryTable } from '../features/shared-policy-groups/components/SharedPolicyGroupHistoryTable';
import { useRestoreSharedPolicyGroup } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupHistories } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import {
    ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION,
    isKubernetesOrigin,
} from '../features/shared-policy-groups/utils/sharedPolicyGroupPermissions';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { notify } from '../shared/notify';

type CompareState = { left: SharedPolicyGroup; right: SharedPolicyGroup; rightIsPending?: boolean } | null;

function historyKey(sharedPolicyGroup: SharedPolicyGroup): string {
    return `${sharedPolicyGroup.version ?? 'unknown'}-${sharedPolicyGroup.updatedAt ?? sharedPolicyGroup.deployedAt ?? ''}`;
}

function getCompareSelectedLabel(selected: SharedPolicyGroup[]): string {
    if (selected.length === 2) {
        return `Compare version ${selected[0].version ?? '—'} with ${selected[1].version ?? '—'}`;
    }
    if (selected.length === 1) {
        return 'Select another version to compare';
    }
    return 'Select two versions to compare';
}

export function SharedPolicyGroupHistoryPage() {
    const sharedPolicyGroup = useOutletContext<SharedPolicyGroup>();
    const canUpdate = useHasPermission({ anyOf: [ENVIRONMENT_SHARED_POLICY_GROUP_UPDATE_PERMISSION] });
    const navigate = useNavigate();
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(25);
    const [sorting, setSorting] = useState<TableSortingState>([{ id: 'version', desc: true }]);
    const [selected, setSelected] = useState<SharedPolicyGroup[]>([]);
    const [jsonSource, setJsonSource] = useState<SharedPolicyGroup>();
    const [details, setDetails] = useState<SharedPolicyGroup>();
    const [compare, setCompare] = useState<CompareState>(null);
    const [restoreCandidate, setRestoreCandidate] = useState<SharedPolicyGroup>();
    const restoreMutation = useRestoreSharedPolicyGroup();
    const historiesQuery = useSharedPolicyGroupHistories({
        sharedPolicyGroupId: sharedPolicyGroup.id,
        page,
        perPage: pageSize,
        sortBy: sortToOrder(sorting),
    });

    function toggleSelected(history: SharedPolicyGroup) {
        const key = historyKey(history);
        setSelected(previous => {
            if (previous.some(item => historyKey(item) === key)) {
                return previous.filter(item => historyKey(item) !== key);
            }
            return previous.length < 2 ? [...previous, history] : previous;
        });
    }

    function compareSelectedVersions() {
        if (selected.length !== 2) {
            return;
        }
        const [left, right] = [...selected].sort((a, b) => (a.version ?? 0) - (b.version ?? 0));
        setCompare({ left, right });
    }

    function compareWithPending() {
        const selectedVersion = selected.at(-1);
        if (!selectedVersion || sharedPolicyGroup.lifecycleState !== 'PENDING') {
            return;
        }
        setCompare({ left: selectedVersion, right: sharedPolicyGroup, rightIsPending: true });
    }

    function handleSortingChange(updater: TableSortingState | ((previous: TableSortingState) => TableSortingState)) {
        setSorting(updater);
        setPage(1);
    }

    async function restoreVersion() {
        if (!restoreCandidate) {
            return;
        }
        try {
            await restoreMutation.mutateAsync(restoreCandidate);
            notify.success('Version has been restored. Review changes and click ‘Deploy’ to finalize the restoration.');
            setRestoreCandidate(undefined);
            navigate('../studio', { relative: 'path' });
        } catch (error) {
            notify.error(error, 'Error during Shared Policy Group restore!');
        }
    }

    if (historiesQuery.isLoading) {
        return <Skeleton className="h-64 w-full rounded-lg" />;
    }

    if (historiesQuery.isError || !historiesQuery.data) {
        return <p className="text-sm text-destructive">Failed to load Shared Policy Group history. Please refresh and try again.</p>;
    }

    const histories = historiesQuery.data.data;
    const totalCount = historiesQuery.data.pagination.totalCount;
    const selectedForPendingComparison = selected.at(-1);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap justify-end gap-2">
                <Button
                    type="button"
                    variant="outline"
                    aria-label="Compare selected version with pending changes"
                    disabled={sharedPolicyGroup.lifecycleState !== 'PENDING' || selected.length === 0}
                    onClick={compareWithPending}
                >
                    <GitBranchIcon className="size-4" aria-hidden />
                    {selectedForPendingComparison
                        ? `Compare version ${selectedForPendingComparison.version ?? '—'} with version to be deployed`
                        : 'Select a version to compare with version to be deployed'}
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    aria-label="Compare selected versions"
                    disabled={selected.length !== 2}
                    onClick={compareSelectedVersions}
                >
                    <GitBranchIcon className="size-4" aria-hidden />
                    {getCompareSelectedLabel(selected)}
                </Button>
            </div>
            <SharedPolicyGroupHistoryTable
                histories={histories}
                totalCount={totalCount}
                loading={historiesQuery.isFetching}
                selected={selected}
                page={page}
                pageSize={pageSize}
                sorting={sorting}
                onToggleSelected={toggleSelected}
                onShowJson={setJsonSource}
                onShowDetails={setDetails}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                onSortingChange={handleSortingChange}
            />
            <SharedPolicyGroupHistoryJsonDialog sharedPolicyGroup={jsonSource} onOpenChange={open => !open && setJsonSource(undefined)} />
            <SharedPolicyGroupHistoryDetailsDialog
                sharedPolicyGroup={details}
                canRestore={canUpdate && !isKubernetesOrigin(sharedPolicyGroup) && (!details || !isKubernetesOrigin(details))}
                onOpenChange={open => !open && setDetails(undefined)}
                onRestore={history => {
                    setDetails(undefined);
                    setRestoreCandidate(history);
                }}
            />
            <SharedPolicyGroupHistoryCompareDialog
                open={compare !== null}
                left={compare?.left}
                right={compare?.right}
                rightIsPending={compare?.rightIsPending}
                onOpenChange={open => !open && setCompare(null)}
            />
            <ConfirmDialog
                open={restoreCandidate !== undefined}
                onOpenChange={open => !open && setRestoreCandidate(undefined)}
                title={`Restore version ${restoreCandidate?.version ?? '—'}`}
                description="This will overwrite pending changes with the selected version. Review the restored configuration and deploy it to finalize the restoration."
                confirmLabel="Restore"
                pendingLabel="Restoring…"
                isPending={restoreMutation.isPending}
                onConfirm={() => void restoreVersion()}
            />
        </div>
    );
}
