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
import { formatAlertCounters, formatAlertCountersTooltip, formatLastAlertAt, formatLastAlertMessage } from './alertListFormat';

describe('alertListFormat', () => {
    it('formats counters as 5m / 1h / 1d / 1M', () => {
        expect(formatAlertCounters({ '5m': 1, '1h': 2, '1d': 3, '1M': 4 })).toBe('1 / 2 / 3 / 4');
        expect(formatAlertCounters(undefined)).toBe('—');
        expect(formatAlertCountersTooltip({ '5m': 1, '1h': 0, '1d': 0, '1M': 0 })).toContain('during the last 5 minutes');
    });

    it('formats last alert timestamp and message with em dash fallbacks', () => {
        expect(formatLastAlertAt(undefined)).toBe('—');
        expect(formatLastAlertAt('not-a-date')).toBe('—');
        expect(formatLastAlertAt('2026-08-12T10:00:00.000Z')).not.toBe('—');
        expect(formatLastAlertMessage(undefined)).toBe('—');
        expect(formatLastAlertMessage('Node stopped')).toBe('Node stopped');
    });
});
