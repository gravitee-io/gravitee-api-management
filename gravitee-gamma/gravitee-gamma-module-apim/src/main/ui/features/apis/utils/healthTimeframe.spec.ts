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
import { DEFAULT_TIMEFRAME, resolveHealthTimeRange, TIMEFRAMES, TIMEFRAME_DURATIONS_MS } from './healthTimeframe';

describe('resolveHealthTimeRange', () => {
    const NOW = 1_000_000_000_000;

    it('computes from/to from the timeframe duration', () => {
        const range = resolveHealthTimeRange('1h', 30, NOW);
        expect(range.to).toBe(NOW);
        expect(range.from).toBe(NOW - TIMEFRAME_DURATIONS_MS['1h']);
    });

    it('derives the interval from the bucket count', () => {
        const range = resolveHealthTimeRange('1d', 30, NOW);
        expect(range.interval).toBe(Math.floor(TIMEFRAME_DURATIONS_MS['1d'] / 30));
    });

    it('defaults the bucket count to 30', () => {
        const range = resolveHealthTimeRange('1m', undefined, NOW);
        expect(range.interval).toBe(Math.floor(TIMEFRAME_DURATIONS_MS['1m'] / 30));
    });
});

describe('timeframe constants', () => {
    it('exposes one option per supported timeframe with 1d as default', () => {
        expect(TIMEFRAMES.map(t => t.id)).toEqual(['1m', '1h', '1d', '1w', '1M']);
        expect(DEFAULT_TIMEFRAME).toBe('1d');
    });
});
