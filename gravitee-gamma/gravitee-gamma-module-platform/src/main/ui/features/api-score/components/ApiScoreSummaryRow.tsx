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
import { Badge, Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import type { EnvironmentScoringOverview } from '../types/scoring';
import { formatScorePercent, scoreTone } from '../utils/scoring';

const SCORE_PILL_CLASS = {
    success: 'border-success/20 text-success bg-success/10',
    warning: 'text-warning border-warning/30 bg-warning/10',
    error: 'border-destructive/20 text-destructive bg-destructive/10',
} as const;

function formatCount(value: number | null | undefined): string {
    return value === null || value === undefined ? '—' : String(value);
}

export function ApiScoreSummaryRow({ overview }: Readonly<{ overview: EnvironmentScoringOverview }>) {
    const score = overview.score ?? 0;
    const tone = scoreTone(score);

    return (
        <Card>
            <CardHeader>
                <CardTitle>Overview</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="grid grid-cols-2 gap-6 sm:grid-cols-5" data-testid="api-score-overview">
                    <div className="space-y-2">
                        <Badge variant="outline" className={`gap-1 ${SCORE_PILL_CLASS[tone]}`}>
                            <ShieldCheckIcon className="size-3.5" aria-hidden />
                            {formatScorePercent(score)}
                        </Badge>
                        <p className="text-muted-foreground text-sm">Average score</p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-2xl font-semibold tabular-nums">{formatCount(overview.errors)}</p>
                        <p className="text-muted-foreground text-sm">Errors</p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-2xl font-semibold tabular-nums">{formatCount(overview.warnings)}</p>
                        <p className="text-muted-foreground text-sm">Warnings</p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-2xl font-semibold tabular-nums">{formatCount(overview.hints)}</p>
                        <p className="text-muted-foreground text-sm">Hints</p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-2xl font-semibold tabular-nums">{formatCount(overview.infos)}</p>
                        <p className="text-muted-foreground text-sm">Infos</p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
