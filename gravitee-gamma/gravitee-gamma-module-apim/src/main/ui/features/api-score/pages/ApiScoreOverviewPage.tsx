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
import { Alert, AlertDescription } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import { ScoredApisTable } from '../components/overview/ScoredApisTable';
import { ScoringOverviewCards } from '../components/overview/ScoringOverviewCards';
import { useApiScoreOverview } from '../hooks/useApiScoreOverview';
import { deriveScoreSummary } from '../utils/scoreFormat';

const DEFAULT_PAGE = 1;
const DEFAULT_PER_PAGE = 10;

export function ApiScoreOverviewPage() {
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [perPage, setPerPage] = useState(DEFAULT_PER_PAGE);

    const { apis, isLoading, isRefreshing, isError, hasPendingJobs, pendingCount, refresh } = useApiScoreOverview();

    // The list is fully loaded and filtered to V4 HTTP proxies, so cards and pagination are derived client-side.
    const summary = useMemo(() => (apis ? deriveScoreSummary(apis) : undefined), [apis]);
    const totalCount = apis?.length ?? 0;
    const pageRows = useMemo(() => (apis ?? []).slice((page - 1) * perPage, page * perPage), [apis, page, perPage]);

    const handlePerPageChange = (nextPerPage: number) => {
        setPerPage(nextPerPage);
        setPage(DEFAULT_PAGE);
    };

    if (isError) {
        return (
            <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-6 text-center text-sm text-destructive">
                Failed to load API scores. Please refresh and try again.
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {hasPendingJobs ? (
                <Alert variant="default" className="border-warning/30 bg-warning/5">
                    <AlertDescription>
                        {pendingCount === 1
                            ? 'A scoring request is in progress. Results will update automatically when it completes.'
                            : `${pendingCount} scoring requests are in progress. Results will update automatically when they complete.`}
                    </AlertDescription>
                </Alert>
            ) : null}
            <ScoringOverviewCards summary={summary} isLoading={isLoading} />
            <ScoredApisTable
                apis={pageRows}
                isLoading={isLoading}
                isRefreshing={isRefreshing}
                skeletonRowCount={perPage}
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                onPageChange={setPage}
                onPageSizeChange={handlePerPageChange}
                onRefresh={() => void refresh()}
            />
        </div>
    );
}
