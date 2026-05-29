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
import { describeCronSchedule, validateCron } from './cronSchedule';

describe('cronSchedule', () => {
    it('returns preset label for known expressions', () => {
        expect(describeCronSchedule('0 */5 * * * *')).toBe('Every 5 minutes');
    });

    it('describes custom valid cron expressions', () => {
        expect(describeCronSchedule('0 */6 * * * *')).toMatch(/6 minute/i);
    });

    it('describes custom cron with day-of-week constraint', () => {
        expect(describeCronSchedule('0 */10 * * * 2')).toBe('Every 10 minutes, on Tuesday');
    });

    it('returns null for invalid cron', () => {
        expect(describeCronSchedule('not-a-cron')).toBeNull();
        expect(validateCron('not-a-cron')).toBeTruthy();
    });
});
