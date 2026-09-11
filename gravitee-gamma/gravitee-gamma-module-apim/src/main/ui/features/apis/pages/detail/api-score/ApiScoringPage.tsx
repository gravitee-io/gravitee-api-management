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
import { Alert, AlertDescription, Button, DataTableEmptyState, Skeleton, ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';
import { RocketIcon, SearchIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Navigate, useParams } from 'react-router-dom';

import { ApiScoringAssetCard } from './ApiScoringAssetCard';
import { notify } from '../../../../../shared/notify';
import { useApiScoreEnabled } from '../../../hooks/useApiScoreEnabled';
import { useApiScoring } from '../../../hooks/useApiScoring';
import type { ApiScoringSummary, ScoringFilter } from '../../../types/scoring';
import {
    filterAssetsBySeverity,
    formatDateAgo,
    formatEvaluationErrors,
    formatScorePercent,
    scoringJobToastMessage,
    scoreTone,
} from '../../../utils/scoring';

const FILTERS: { value: ScoringFilter; label: (count: number) => string }[] = [
    { value: 'ALL', label: count => `All (${count})` },
    { value: 'ERROR', label: count => `Errors (${count})` },
    { value: 'WARN', label: count => `Warnings (${count})` },
    { value: 'INFO', label: count => `Infos (${count})` },
    { value: 'HINT', label: count => `Hints (${count})` },
];

const FILTER_COUNT_KEY: Record<ScoringFilter, keyof ApiScoringSummary> = {
    ALL: 'all',
    ERROR: 'errors',
    WARN: 'warnings',
    INFO: 'infos',
    HINT: 'hints',
};

const SCORE_TONE_CLASS = {
    success: 'text-success',
    warning: 'text-warning',
    error: 'text-destructive',
} as const;

export function ApiScoringPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const { enabled: apiScoreEnabled, isFetched: apiScoreFlagFetched } = useApiScoreEnabled();
    const { scoring, jobs, isLoading, isError, error, pending, evaluate } = useApiScoring(apiScoreEnabled ? apiId : undefined);
    const [severity, setSeverity] = useState<ScoringFilter>('ALL');
    const toastedJobRef = useRef<string | null>(null);
    const toastedAssetsRef = useRef<string | null>(null);

    const jobToastMessage = useMemo(() => scoringJobToastMessage(jobs), [jobs]);
    const filteredAssets = useMemo(() => (scoring ? filterAssetsBySeverity(scoring.assets, severity) : []), [scoring, severity]);

    useEffect(() => {
        if (!jobToastMessage) return;
        if (toastedJobRef.current === jobToastMessage) return;
        toastedJobRef.current = jobToastMessage;
        notify.error(jobToastMessage);
    }, [jobToastMessage]);

    useEffect(() => {
        if (pending || !scoring) return;
        const message = formatEvaluationErrors(scoring);
        if (!message) return;
        if (toastedAssetsRef.current === message) return;
        toastedAssetsRef.current = message;
        notify.error(message);
    }, [pending, scoring]);

    useEffect(() => {
        if (!isError) return;
        notify.error(error, 'An error occurred while getting your API Scoring.');
    }, [isError, error]);

    if (!apiScoreFlagFetched || (apiScoreEnabled && isLoading)) {
        return (
            <div className="space-y-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-8 w-40 rounded" />
                        <Skeleton className="h-4 w-56 rounded" />
                    </div>
                    <Skeleton className="h-8 w-24 rounded" />
                </div>
                <Skeleton className="h-48 w-full rounded-lg" />
            </div>
        );
    }

    if (!apiScoreEnabled) {
        return <Navigate to=".." replace />;
    }

    const neverEvaluated = !isError && (scoring === null || scoring === undefined);
    const summary = scoring?.summary;
    const scoreAvailable = summary !== undefined;
    const allClear = scoreAvailable && summary.all === 0 && summary.score === 1;
    const hasAssetErrors = Boolean(scoring && formatEvaluationErrors(scoring));
    const noScorableAssets = !isError && !neverEvaluated && !scoreAvailable && !hasAssetErrors;
    const lastEvaluatedAt = scoring?.createdAt;
    const showFilters = scoreAvailable && summary.all !== 0;

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">
                        API Score
                        {scoreAvailable ? (
                            <span className={`ml-2 ${SCORE_TONE_CLASS[scoreTone(summary.score)]}`}>
                                {formatScorePercent(summary.score)}
                            </span>
                        ) : null}
                    </h1>
                    {scoreAvailable && lastEvaluatedAt ? (
                        <p className="text-sm text-muted-foreground">Last evaluated {formatDateAgo(lastEvaluatedAt)}</p>
                    ) : null}
                </div>
                <Button type="button" size="sm" onClick={() => evaluate()} disabled={pending}>
                    Evaluate
                </Button>
            </div>

            {pending ? (
                <Alert variant="warning">
                    <AlertDescription>A request is currently processing, updated result will appear below once completed.</AlertDescription>
                </Alert>
            ) : null}

            {showFilters ? (
                <ToggleGroup
                    type="single"
                    value={severity}
                    onValueChange={value => {
                        if (value) setSeverity(value as ScoringFilter);
                    }}
                    className="flex flex-wrap justify-start"
                    aria-label="Filter findings by severity"
                >
                    {FILTERS.map(filter => {
                        const count = summary[FILTER_COUNT_KEY[filter.value]];
                        return (
                            <ToggleGroupItem key={filter.value} value={filter.value} disabled={filter.value !== 'ALL' && count === 0}>
                                {filter.label(count)}
                            </ToggleGroupItem>
                        );
                    })}
                </ToggleGroup>
            ) : null}

            {neverEvaluated ? (
                <DataTableEmptyState
                    variant="first-use"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="This API has never been scored before"
                    description="Click on the Evaluate button to get the first score."
                />
            ) : null}

            {allClear ? (
                <DataTableEmptyState
                    variant="first-use"
                    icon={<RocketIcon className="size-8" aria-hidden />}
                    title="All clear"
                    description="There is no recommendations or issues for your API. Everything looks great!"
                />
            ) : null}

            {noScorableAssets ? (
                <DataTableEmptyState
                    variant="first-use"
                    icon={<ShieldIcon className="size-8" aria-hidden />}
                    title="No scorable assets"
                    description="This API's assets did not match any rulesets."
                />
            ) : null}

            {scoreAvailable && !allClear
                ? filteredAssets.map(asset => <ApiScoringAssetCard key={`${asset.name}-${asset.type}`} asset={asset} />)
                : null}
        </div>
    );
}
