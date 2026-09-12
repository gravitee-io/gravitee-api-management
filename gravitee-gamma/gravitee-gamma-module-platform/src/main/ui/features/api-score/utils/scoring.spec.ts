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
import { formatScorePercent, hasOverviewScore, isApiScoreEnabled, isScoreAvailable, scoreTone } from './scoring';

describe('formatScorePercent', () => {
    it('rounds a 0-1 score to a whole percent', () => {
        expect(formatScorePercent(0.83)).toBe('83%');
        expect(formatScorePercent(1)).toBe('100%');
        expect(formatScorePercent(0.835)).toBe('84%');
    });
});

describe('scoreTone', () => {
    it('uses success at 80% and above, warning at 40%, error below', () => {
        expect(scoreTone(0.8)).toBe('success');
        expect(scoreTone(0.66)).toBe('warning');
        expect(scoreTone(0.4)).toBe('warning');
        expect(scoreTone(0.39)).toBe('error');
    });
});

describe('isScoreAvailable', () => {
    it('is false for a missing or negative score', () => {
        expect(isScoreAvailable(undefined)).toBe(false);
        expect(isScoreAvailable(null)).toBe(false);
        expect(isScoreAvailable(-1)).toBe(false);
        expect(isScoreAvailable(0)).toBe(true);
        expect(isScoreAvailable(0.84)).toBe(true);
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

describe('hasOverviewScore', () => {
    it('requires a non-null overview score, matching Classic Console', () => {
        expect(hasOverviewScore(undefined)).toBe(false);
        expect(hasOverviewScore({ id: 'env-1', score: null, errors: 0, warnings: 0, infos: 0, hints: 0 })).toBe(false);
        expect(hasOverviewScore({ id: 'env-1', score: 0.83, errors: 3, warnings: 5, infos: 2, hints: 1 })).toBe(true);
    });
});
