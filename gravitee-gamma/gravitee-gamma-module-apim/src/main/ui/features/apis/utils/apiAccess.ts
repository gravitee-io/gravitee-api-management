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
import type { ApiListItem } from '../types';

/**
 * Extracts the primary access path from an API's listeners.
 * For V4 HTTP proxies: returns the first HTTP listener path.
 * Returns null when no access path can be derived.
 */
export function getApiAccessPath(api: ApiListItem): string | null {
    if (!api.listeners?.length) return null;

    const httpListener = api.listeners.find(l => l.type === 'HTTP');
    if (httpListener?.paths?.length) {
        const first = httpListener.paths[0];
        const raw = first.host ? `${first.host}${first.path}` : first.path;
        return raw.replace(/\/+$/, '') || '/';
    }

    return null;
}
