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
import { DataTableEmptyState, Skeleton } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

import { notify } from '../../../shared/notify';
import { ApiScoreApisTable } from '../components/ApiScoreApisTable';
import { ApiScoreSummaryRow } from '../components/ApiScoreSummaryRow';
import { useApisScoring } from '../hooks/useApisScoring';
import { useScoringOverview } from '../hooks/useScoringOverview';
import { apiScoreDetailPath } from '../utils/apiScoreDetailPath';
import { DEFAULT_API_SCORE_LIST_PAGE_SIZE } from '../utils/paginationConstants';
import { hasOverviewScore } from '../utils/scoring';

const OVERVIEW_ERROR = 'An error occurred while loading list.';

export function ApiScoreDashboardPage() {
    const location = useLocation();
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_API_SCORE_LIST_PAGE_SIZE);

    const overviewQuery = useScoringOverview();
    const apisQuery = useApisScoring({ page, perPage: pageSize });

    useEffect(() => {
        if (!overviewQuery.isError) return;
        notify.error(overviewQuery.error, OVERVIEW_ERROR);
    }, [overviewQuery.error, overviewQuery.isError]);

    useEffect(() => {
        if (!apisQuery.isError) return;
        notify.error(apisQuery.error, OVERVIEW_ERROR);
    }, [apisQuery.error, apisQuery.isError]);

    const overview = overviewQuery.data;
    const showOverview = hasOverviewScore(overview);

    return (
        <div className="space-y-6" data-testid="api-score-dashboard-page">
            {overviewQuery.isLoading ? <Skeleton className="h-36 w-full rounded-lg" /> : null}
            {!overviewQuery.isLoading && !overviewQuery.isError && !showOverview ? (
                <DataTableEmptyState
                    variant="first-use"
                    icon={<ShieldCheckIcon className="size-8" aria-hidden />}
                    title="No score results yet"
                    description="There are no results to display. Please open an API and run the API score evaluation to start scoring your APIs."
                />
            ) : null}
            {!overviewQuery.isLoading && showOverview ? <ApiScoreSummaryRow overview={overview} /> : null}

            <ApiScoreApisTable
                apis={apisQuery.apis}
                totalCount={apisQuery.totalCount}
                loading={apisQuery.isLoading}
                page={page}
                pageSize={pageSize}
                detailHref={apiId => apiScoreDetailPath(location.pathname, apiId)}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
            />
        </div>
    );
}
