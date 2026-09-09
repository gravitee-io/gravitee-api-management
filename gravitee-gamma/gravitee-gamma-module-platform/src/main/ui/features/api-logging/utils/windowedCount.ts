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

import { parseIso8601DurationSeconds } from './iso8601Duration';

export class WindowedCountFormatError extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'WindowedCountFormatError';
    }
}

export class WindowedCount {
    constructor(
        readonly count: number,
        readonly windowSeconds: number,
    ) {}

    rate(): number {
        if (this.windowSeconds === 0) {
            return 0;
        }
        return this.count / this.windowSeconds;
    }

    static parse(format: string): WindowedCount {
        const parts = format.split('/');
        if (parts.length !== 2) {
            throw new WindowedCountFormatError('Invalid format: must be in count/duration format');
        }

        const countPart = (parts[0] ?? '').trim();
        if (!/^\d+$/.test(countPart)) {
            throw new WindowedCountFormatError('Invalid format: count must be a positive integer');
        }

        const count = Number(countPart);
        const windowSeconds = parseIso8601DurationSeconds(parts[1] ?? '');

        if (count <= 0 || windowSeconds === null) {
            throw new WindowedCountFormatError('Invalid format: count must be a number and duration must be valid');
        }

        return new WindowedCount(count, windowSeconds);
    }

    isValid(): boolean {
        return this.count > 0 && this.windowSeconds > 0;
    }
}
