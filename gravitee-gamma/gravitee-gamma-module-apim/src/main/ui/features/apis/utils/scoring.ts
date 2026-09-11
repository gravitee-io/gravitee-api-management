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
import type { ApiScoring, ScoringAsset, ScoringAsyncJob, ScoringDiagnostic, ScoringFilter } from '../types/scoring';

const DATE_AGO_INTERVALS = [
    ['year', 31_536_000],
    ['month', 2_592_000],
    ['day', 86_400],
    ['hour', 3600],
    ['minute', 60],
    ['second', 1],
] as const;

/** Classic Console treats a job as relevant for ERROR/TIMEOUT toasts for one hour. */
export const SCORING_JOB_TOAST_WINDOW_MS = 3_600_000;

export function formatScorePercent(score: number): string {
    return `${Math.round(score * 100)}%`;
}

export function scoreTone(score: number): 'success' | 'warning' | 'error' {
    if (score >= 0.8) return 'success';
    if (score >= 0.4) return 'warning';
    return 'error';
}

export function formatLineColumn(diagnostic: ScoringDiagnostic): string {
    return `${diagnostic.range.start.line}:${diagnostic.range.start.character}`;
}

/** Classic Console `dateAgo` pipe — always relative, including beyond one week. */
export function formatDateAgo(value: string | number | Date | undefined, now = Date.now()): string {
    if (value === undefined || value === null || value === '') return '';
    const then = new Date(value).getTime();
    if (Number.isNaN(then)) return String(value);
    const seconds = Math.floor((now - then) / 1000);
    if (seconds < 29) return 'just now';
    for (const [unit, size] of DATE_AGO_INTERVALS) {
        const count = Math.floor(seconds / size);
        if (count > 0) {
            return count === 1 ? `${count} ${unit} ago` : `${count} ${unit}s ago`;
        }
    }
    return String(value);
}

export function isApiScoreEnabled(config: { apiScore?: { enabled?: boolean } } | null | undefined): boolean {
    return Boolean(config?.apiScore?.enabled);
}

export function filterAssetsBySeverity(assets: ScoringAsset[], severity: ScoringFilter): ScoringAsset[] {
    if (severity === 'ALL') return assets;
    return assets.map(item => ({
        ...item,
        diagnostics: item.diagnostics.filter(diagnostic => diagnostic.severity === severity),
    }));
}

export function diagnosticMatchesSearch(diagnostic: ScoringDiagnostic, search: string): boolean {
    const term = search.trim().toLowerCase();
    if (!term) return true;
    return [diagnostic.severity, diagnostic.message, diagnostic.path].some(value => value.toLowerCase().includes(term));
}

export function paginateItems<T>(items: readonly T[], page: number, pageSize: number): T[] {
    const start = (page - 1) * pageSize;
    return items.slice(start, start + pageSize);
}

export function clampPage(page: number, itemCount: number, pageSize: number): number {
    if (pageSize <= 0) return 1;
    const pageCount = Math.max(1, Math.ceil(itemCount / pageSize));
    return Math.min(Math.max(1, page), pageCount);
}

export function formatEvaluationErrors(scoring: ApiScoring): string | null {
    const assetsWithErrors = scoring.assets.filter(item => (item.errors?.length ?? 0) > 0);
    if (assetsWithErrors.length === 0) return null;
    let message = 'Errors occurred while scoring this API:';
    for (const item of assetsWithErrors) {
        message += '\n';
        if (item.name) {
            message += `\nAsset: ${item.name}`;
        }
        for (const error of item.errors ?? []) {
            message += `\nCode: ${error.code}\nPath: ${error.path.toString()}`;
        }
    }
    return message;
}

export function latestScoringJob(jobs: ScoringAsyncJob[]): ScoringAsyncJob | undefined {
    return [...jobs].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())[0];
}

export function scoringJobToastMessage(jobs: ScoringAsyncJob[], now = Date.now()): string | null {
    const lastJob = latestScoringJob(jobs);
    if (!lastJob) return null;
    const recent = now - new Date(lastJob.updatedAt).getTime() < SCORING_JOB_TOAST_WINDOW_MS;
    if (!recent) return null;
    if (lastJob.status === 'ERROR') {
        return `The last evaluation was failed at ${lastJob.createdAt} ${lastJob.errorMessage ?? ''}`;
    }
    if (lastJob.status === 'TIMEOUT') {
        return `Evaluation timed out at ${lastJob.createdAt}. The API score service might be unavailable. Please try again later.`;
    }
    return null;
}
