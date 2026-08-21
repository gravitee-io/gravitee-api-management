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

export const DEFAULT_VISIBLE_NAME_COUNT = 3;

/** First N names for compact display; full list is intended for tooltip. */
export function formatTruncatedNameSummary(
    names: readonly string[],
    visibleCount = DEFAULT_VISIBLE_NAME_COUNT,
): { display: string; full: string; truncated: boolean } {
    if (names.length === 0) {
        return { display: '—', full: '—', truncated: false };
    }
    const full = names.join(', ');
    if (names.length <= visibleCount) {
        return { display: full, full, truncated: false };
    }
    return {
        display: `${names.slice(0, visibleCount).join(', ')}...`,
        full,
        truncated: true,
    };
}
