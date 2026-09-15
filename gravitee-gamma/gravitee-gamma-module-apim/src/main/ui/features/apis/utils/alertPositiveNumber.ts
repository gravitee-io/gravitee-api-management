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

/** Classic platform alert number fields: `min="1"` (“must be higher than 0”). */
export const ALERT_POSITIVE_NUMBER_MIN = 1;

/** Classic rate threshold (%) is `min="1"` `max="99"`. */
export const ALERT_RATE_PERCENT_MAX = 99;

export function nextAlertPositiveNumber(
    raw: string,
    current: number | undefined,
    bounds?: { min?: number; max?: number },
): number | undefined {
    if (raw === '') {
        return undefined;
    }
    const min = bounds?.min ?? ALERT_POSITIVE_NUMBER_MIN;
    const n = Number(raw);
    if (!Number.isFinite(n) || n < min) {
        return current;
    }
    if (bounds?.max !== undefined && n > bounds.max) {
        return current;
    }
    return n;
}
