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
import { Badge, Card, CardContent, CardHeader, CardTitle, cn, Skeleton } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import type { ScoreSummary } from '../../types/apiScore';
import { OVERVIEW_SEVERITY_CARDS, scoreColorClasses, scoreToPercent } from '../../utils/scoreFormat';

function StatCard({ label, children }: { label: string; children: React.ReactNode }) {
    return (
        <Card className="flex-1">
            <CardContent className="pt-5 pb-4">
                <div className="text-2xl font-semibold tabular-nums">{children}</div>
                <p className="mt-1 text-sm text-muted-foreground">{label}</p>
            </CardContent>
        </Card>
    );
}

function AverageScoreValue({ score, isLoading }: { score: number | null | undefined; isLoading: boolean }) {
    if (isLoading) {
        return <Skeleton className="h-8 w-20 rounded" />;
    }

    const percent = scoreToPercent(score);
    if (percent === null) {
        return (
            <span className={cn('inline-flex items-center gap-1.5 text-muted-foreground')}>
                <ShieldCheckIcon className="size-5" aria-hidden />—
            </span>
        );
    }

    return (
        <Badge variant="outline" className={cn('h-auto gap-1.5 px-2.5 py-1 text-2xl font-semibold tabular-nums', scoreColorClasses(score))}>
            <ShieldCheckIcon className="size-5" aria-hidden />
            {percent}%
        </Badge>
    );
}

interface ScoringOverviewCardsProps {
    summary: ScoreSummary | undefined;
    isLoading: boolean;
}

/** Overview stat cards — always shown, including when no API has been scored yet. */
export function ScoringOverviewCards({ summary, isLoading }: ScoringOverviewCardsProps) {
    const hasScores = typeof summary?.score === 'number';

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Overview</CardTitle>
            </CardHeader>
            <CardContent className="flex gap-4 pt-0">
                <StatCard label="Average score">
                    <AverageScoreValue score={summary?.score} isLoading={isLoading} />
                </StatCard>
                {OVERVIEW_SEVERITY_CARDS.map(({ key, label }) => (
                    <StatCard key={key} label={label}>
                        {isLoading ? (
                            <Skeleton className="h-8 w-10 rounded" />
                        ) : (
                            <span className={hasScores ? undefined : 'text-muted-foreground'}>
                                {(summary?.[key] ?? 0).toLocaleString()}
                            </span>
                        )}
                    </StatCard>
                ))}
            </CardContent>
        </Card>
    );
}
