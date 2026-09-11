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
import {
    clampPage,
    diagnosticMatchesSearch,
    filterAssetsBySeverity,
    formatDateAgo,
    formatEvaluationErrors,
    formatLineColumn,
    formatScorePercent,
    isApiScoreEnabled,
    latestScoringJob,
    paginateItems,
    scoringJobToastMessage,
    scoreTone,
} from './scoring';
import type { ApiScoring, ScoringAsset, ScoringAsyncJob, ScoringDiagnostic } from '../types/scoring';

const diagnostic = (overrides: Partial<ScoringDiagnostic> = {}): ScoringDiagnostic => ({
    severity: 'WARN',
    message: 'Operation is missing a security requirement.',
    range: { start: { line: 84, character: 4 }, end: { line: 84, character: 10 } },
    path: '$.paths./pet.findByStatus.get',
    ...overrides,
});

const asset = (overrides: Partial<ScoringAsset> = {}): ScoringAsset => ({
    name: 'petstore.yaml',
    type: 'SWAGGER',
    diagnostics: [diagnostic()],
    ...overrides,
});

describe('formatScorePercent', () => {
    it('rounds a 0-1 score to a whole percent', () => {
        expect(formatScorePercent(0.67)).toBe('67%');
        expect(formatScorePercent(1)).toBe('100%');
        expect(formatScorePercent(0.835)).toBe('84%');
    });
});

describe('scoreTone', () => {
    it('uses success at 80% and above, warning at 40%, error below', () => {
        expect(scoreTone(0.8)).toBe('success');
        expect(scoreTone(0.67)).toBe('warning');
        expect(scoreTone(0.4)).toBe('warning');
        expect(scoreTone(0.39)).toBe('error');
    });
});

describe('formatLineColumn', () => {
    it('formats the diagnostic start as line:character', () => {
        expect(formatLineColumn(diagnostic())).toBe('84:4');
    });
});

describe('formatDateAgo', () => {
    const now = new Date('2026-09-10T12:00:00.000Z').getTime();

    it('returns just now for timestamps under 29 seconds', () => {
        expect(formatDateAgo(new Date(now - 10_000).toISOString(), now)).toBe('just now');
    });

    it('returns a singular relative unit', () => {
        expect(formatDateAgo(new Date(now - 86_400_000).toISOString(), now)).toBe('1 day ago');
    });

    it('returns a plural relative unit matching the Gamma screen (10 days ago)', () => {
        expect(formatDateAgo(new Date(now - 10 * 86_400_000).toISOString(), now)).toBe('10 days ago');
    });
});

describe('isApiScoreEnabled', () => {
    it('is true only when the portal flag is explicitly enabled', () => {
        expect(isApiScoreEnabled({ apiScore: { enabled: true } })).toBe(true);
        expect(isApiScoreEnabled({ apiScore: { enabled: false } })).toBe(false);
        expect(isApiScoreEnabled({})).toBe(false);
        expect(isApiScoreEnabled(undefined)).toBe(false);
    });
});

describe('filterAssetsBySeverity', () => {
    const assets = [
        asset({
            diagnostics: [
                diagnostic({ severity: 'ERROR', message: 'missing security' }),
                diagnostic({ severity: 'WARN', message: 'short description' }),
                diagnostic({ severity: 'INFO', message: 'add an example' }),
            ],
        }),
    ];

    it('returns the original assets for ALL', () => {
        expect(filterAssetsBySeverity(assets, 'ALL')).toBe(assets);
    });

    it('keeps assets but retains only matching diagnostics', () => {
        const filtered = filterAssetsBySeverity(assets, 'ERROR');
        expect(filtered[0].diagnostics).toHaveLength(1);
        expect(filtered[0].diagnostics[0].severity).toBe('ERROR');
    });
});

describe('diagnosticMatchesSearch', () => {
    it('matches severity, recommendation, or path case-insensitively', () => {
        const row = diagnostic();
        expect(diagnosticMatchesSearch(row, '')).toBe(true);
        expect(diagnosticMatchesSearch(row, 'WARN')).toBe(true);
        expect(diagnosticMatchesSearch(row, 'security requirement')).toBe(true);
        expect(diagnosticMatchesSearch(row, 'findByStatus')).toBe(true);
        expect(diagnosticMatchesSearch(row, 'no-such-text')).toBe(false);
    });
});

describe('paginateItems', () => {
    it('returns the requested page with a page size of 5', () => {
        const items = [1, 2, 3, 4, 5, 6, 7];
        expect(paginateItems(items, 1, 5)).toEqual([1, 2, 3, 4, 5]);
        expect(paginateItems(items, 2, 5)).toEqual([6, 7]);
    });
});

describe('clampPage', () => {
    it('keeps the current page when it is still in range', () => {
        expect(clampPage(2, 12, 5)).toBe(2);
    });

    it('clamps to the last page when the item count shrinks', () => {
        expect(clampPage(3, 1, 5)).toBe(1);
    });

    it('stays on page 1 when there are no items', () => {
        expect(clampPage(3, 0, 5)).toBe(1);
    });

    it('stays on page 1 when pageSize is zero', () => {
        expect(clampPage(3, 12, 0)).toBe(1);
    });
});

describe('formatEvaluationErrors', () => {
    it('returns null when no asset errors exist', () => {
        expect(formatEvaluationErrors({ createdAt: '2026-01-01T00:00:00Z', assets: [asset()] })).toBeNull();
    });

    it('formats Console copy for asset scoring errors', () => {
        const scoring: ApiScoring = {
            createdAt: '2026-01-01T00:00:00Z',
            assets: [
                asset({
                    name: 'Asset name',
                    type: 'GRAVITEE_DEFINITION',
                    diagnostics: [],
                    errors: [{ code: 'undefined-function', path: ['rules', 'api-key-security-scheme', 'then', 'function'] }],
                }),
            ],
        };
        expect(formatEvaluationErrors(scoring)).toBe(`Errors occurred while scoring this API:

Asset: Asset name
Code: undefined-function
Path: rules,api-key-security-scheme,then,function`);
    });
});

describe('latestScoringJob', () => {
    const job = (overrides: Partial<ScoringAsyncJob> = {}): ScoringAsyncJob => ({
        id: 'job-1',
        sourceId: 'api-1',
        type: 'SCORING_REQUEST',
        status: 'PENDING',
        createdAt: '2026-09-10T11:59:00.000Z',
        updatedAt: '2026-09-10T11:59:00.000Z',
        ...overrides,
    });

    it('picks the most recently updated job', () => {
        const older = job({ id: 'old', status: 'PENDING', updatedAt: '2026-09-10T11:00:00.000Z' });
        const newer = job({ id: 'new', status: 'SUCCESS', updatedAt: '2026-09-10T11:50:00.000Z' });
        expect(latestScoringJob([older, newer])?.id).toBe('new');
    });
});

describe('scoringJobToastMessage', () => {
    const now = new Date('2026-09-10T12:00:00.000Z').getTime();
    const job = (overrides: Partial<ScoringAsyncJob> = {}): ScoringAsyncJob => ({
        id: 'job-1',
        sourceId: 'api-1',
        type: 'SCORING_REQUEST',
        status: 'PENDING',
        createdAt: '2026-09-10T11:59:00.000Z',
        updatedAt: '2026-09-10T11:59:00.000Z',
        ...overrides,
    });

    it('toasts Console ERROR copy for a recent failed job', () => {
        const failed = job({
            status: 'ERROR',
            createdAt: '2026-09-10T11:50:00.000Z',
            updatedAt: '2026-09-10T11:50:00.000Z',
            errorMessage: 'boom',
        });
        expect(scoringJobToastMessage([failed], now)).toBe('The last evaluation was failed at 2026-09-10T11:50:00.000Z boom');
    });

    it('toasts Console TIMEOUT copy for a recent timed-out job', () => {
        const timedOut = job({
            status: 'TIMEOUT',
            createdAt: '2026-09-10T11:50:00.000Z',
            updatedAt: '2026-09-10T11:50:00.000Z',
        });
        expect(scoringJobToastMessage([timedOut], now)).toBe(
            'Evaluation timed out at 2026-09-10T11:50:00.000Z. The API score service might be unavailable. Please try again later.',
        );
    });

    it('does not toast for jobs older than one hour', () => {
        const stale = job({
            status: 'ERROR',
            createdAt: '2026-09-10T10:00:00.000Z',
            updatedAt: '2026-09-10T10:00:00.000Z',
        });
        expect(scoringJobToastMessage([stale], now)).toBeNull();
    });
});
