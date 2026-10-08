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
import { useCallback, useEffect, useState } from 'react';

// Per browser tab, so the tree stays as it was when coming back to it, even after a reload.
const storageKey = (apiId: string) => `gamma-apim:api-documentation:expanded-folders:${apiId}`;

export function useExpandedDocumentationFolders(apiId: string) {
    const [expandedIds, setExpandedIds] = useState<ReadonlySet<string>>(() => loadExpandedIds(apiId));

    useEffect(() => saveExpandedIds(apiId, expandedIds), [apiId, expandedIds]);

    const toggle = useCallback((id: string) => {
        setExpandedIds(previous => {
            const next = new Set(previous);
            if (!next.delete(id)) next.add(id);
            return next;
        });
    }, []);

    const expand = useCallback((id: string) => {
        setExpandedIds(previous => (previous.has(id) ? previous : new Set(previous).add(id)));
    }, []);

    return { expandedIds, toggle, expand };
}

function loadExpandedIds(apiId: string): ReadonlySet<string> {
    try {
        const parsed: unknown = JSON.parse(globalThis.sessionStorage?.getItem(storageKey(apiId)) ?? '[]');
        return Array.isArray(parsed) && parsed.every(id => typeof id === 'string') ? new Set(parsed) : new Set();
    } catch {
        return new Set();
    }
}

function saveExpandedIds(apiId: string, expandedIds: ReadonlySet<string>) {
    try {
        globalThis.sessionStorage?.setItem(storageKey(apiId), JSON.stringify([...expandedIds]));
    } catch {
        // Without storage the tree still works; it only collapses again when shown anew.
    }
}
