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
import { bucketAvailability, summarizeReportBuckets } from './reportBuckets';

describe('reportBuckets', () => {
    it('buckets availability at the Classic 80 / 95 thresholds', () => {
        expect(bucketAvailability(0)).toBe('error');
        expect(bucketAvailability(80)).toBe('error');
        expect(bucketAvailability(80.01)).toBe('warning');
        expect(bucketAvailability(95)).toBe('warning');
        expect(bucketAvailability(95.01)).toBe('operational');
        expect(bucketAvailability(100)).toBe('operational');
    });

    it('skips null, undefined, and non-numeric samples', () => {
        expect(bucketAvailability(null)).toBeNull();
        expect(bucketAvailability(undefined)).toBeNull();
        expect(bucketAvailability(Number.NaN)).toBeNull();
    });

    it('counts operational, warning, and error APIs and ignores skipped samples', () => {
        expect(summarizeReportBuckets([99, 88, 64, null, undefined])).toEqual({
            operational: 1,
            inWarning: 1,
            inError: 1,
        });
    });
});
