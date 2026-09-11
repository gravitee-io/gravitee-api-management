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
import type { EnvironmentScoringOverview } from '../types/scoring';

export function formatScorePercent(score: number): string {
    return `${Math.round(score * 100)}%`;
}

export function scoreTone(score: number): 'success' | 'warning' | 'error' {
    if (score >= 0.8) return 'success';
    if (score >= 0.4) return 'warning';
    return 'error';
}

/** Classic Console treats a missing or negative score as "Not available". */
export function isScoreAvailable(score: number | null | undefined): score is number {
    return score !== null && score !== undefined && score >= 0;
}

export function isApiScoreEnabled(config: { apiScore?: { enabled?: boolean } } | null | undefined): boolean {
    return Boolean(config?.apiScore?.enabled);
}

/** Classic Console hides the Overview stats when `overview.score == null`. */
export function hasOverviewScore(overview: EnvironmentScoringOverview | null | undefined): overview is EnvironmentScoringOverview {
    return overview !== null && overview !== undefined && overview.score !== null && overview.score !== undefined;
}
