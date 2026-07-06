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
    countColorClasses,
    deriveScoreSummary,
    FUNCTION_NAME_PATTERN,
    resolveImportFormat,
    rulesetFormatLabel,
    scoreTextColor,
    scoreToPercent,
} from './scoreFormat';
import type { EnvironmentApiScore } from '../types/apiScore';

describe('scoreToPercent', () => {
    it('converts a 0..1 fraction to a rounded percentage', () => {
        expect(scoreToPercent(0.79)).toBe(79);
        expect(scoreToPercent(0.856)).toBe(86);
        expect(scoreToPercent(1)).toBe(100);
        expect(scoreToPercent(0)).toBe(0);
    });

    it('returns null when the score is missing or negative (never evaluated)', () => {
        expect(scoreToPercent(undefined)).toBeNull();
        expect(scoreToPercent(null)).toBeNull();
        expect(scoreToPercent(-1)).toBeNull();
    });
});

describe('scoreTextColor', () => {
    it('maps score thresholds to colors (>=0.8 success, >=0.4 warning, else error)', () => {
        expect(scoreTextColor(0.8)).toBe('text-success');
        expect(scoreTextColor(0.5)).toBe('text-warning');
        expect(scoreTextColor(0.39)).toBe('text-destructive');
    });

    it('is neutral when unavailable', () => {
        expect(scoreTextColor(null)).toBe('text-muted-foreground');
    });
});

describe('countColorClasses', () => {
    it('is neutral for a null or zero count', () => {
        expect(countColorClasses('errors', null)).toContain('text-muted-foreground');
        expect(countColorClasses('errors', 0)).toContain('text-muted-foreground');
    });

    it('uses the severity color for a positive count', () => {
        expect(countColorClasses('errors', 3)).toContain('text-destructive');
        expect(countColorClasses('warnings', 1)).toContain('text-warning');
        expect(countColorClasses('infos', 2)).toContain('text-primary');
        expect(countColorClasses('hints', 5)).toContain('text-success');
    });
});

describe('resolveImportFormat', () => {
    it('resolves the flat OpenAPI/AsyncAPI kinds directly', () => {
        expect(resolveImportFormat('OPENAPI', null)).toBe('OPENAPI');
        expect(resolveImportFormat('ASYNCAPI', null)).toBe('ASYNCAPI');
    });

    it('uses the selected Gravitee sub-format for the GRAVITEE kind', () => {
        expect(resolveImportFormat('GRAVITEE', 'GRAVITEE_PROXY')).toBe('GRAVITEE_PROXY');
    });

    it('returns null when the GRAVITEE sub-format is not chosen yet', () => {
        expect(resolveImportFormat('GRAVITEE', null)).toBeNull();
    });
});

describe('rulesetFormatLabel', () => {
    it('maps a format to its human label', () => {
        expect(rulesetFormatLabel('OPENAPI')).toBe('OpenAPI');
        expect(rulesetFormatLabel('GRAVITEE_NATIVE')).toBe('Native Kafka');
    });

    it('returns undefined for a missing format', () => {
        expect(rulesetFormatLabel(undefined)).toBeUndefined();
    });
});

describe('deriveScoreSummary', () => {
    const api = (over: Partial<EnvironmentApiScore>): EnvironmentApiScore => ({ id: 'x', name: 'x', pictureUrl: '', ...over });

    it('averages score only over scored APIs and sums severities from scored APIs only', () => {
        const summary = deriveScoreSummary([
            api({ score: 0.9, errors: 1, warnings: 2, infos: 0, hints: 3 }),
            api({ score: 0.5, errors: 4, warnings: 0, infos: 1, hints: 0 }),
            api({ errors: 99 }), // never scored — excluded from average and severity totals
        ]);
        expect(summary.score).toBeCloseTo(0.7);
        expect(summary).toMatchObject({ errors: 5, warnings: 2, infos: 1, hints: 3 });
    });

    it('returns a null score when nothing has been scored', () => {
        expect(deriveScoreSummary([api({}), api({})]).score).toBeNull();
        expect(deriveScoreSummary([]).score).toBeNull();
    });
});

describe('FUNCTION_NAME_PATTERN', () => {
    it('accepts a bare .js file name', () => {
        expect(FUNCTION_NAME_PATTERN.test('ensure-semver.js')).toBe(true);
    });

    it('rejects names without .js or containing a path separator', () => {
        expect(FUNCTION_NAME_PATTERN.test('ensure-semver')).toBe(false);
        expect(FUNCTION_NAME_PATTERN.test('dir/ensure-semver.js')).toBe(false);
    });
});
