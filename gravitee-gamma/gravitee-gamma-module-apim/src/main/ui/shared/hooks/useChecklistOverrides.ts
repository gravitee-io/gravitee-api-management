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
import { useCallback, useState } from 'react';

interface ChecklistEntry {
    done: Set<string>;
    undone: Set<string>;
}

// Module-level cache: survives component unmount/remount (navigation) but resets on page refresh.
const cache = new Map<string, ChecklistEntry>();

function getEntry(entityId: string): ChecklistEntry {
    if (!cache.has(entityId)) {
        cache.set(entityId, { done: new Set(), undone: new Set() });
    }
    return cache.get(entityId)!;
}

export function useChecklistOverrides(entityId: string | undefined) {
    // Used only to trigger re-renders when Sets are mutated.
    const [, forceUpdate] = useState(0);

    const toggle = useCallback(
        (id: string, newDone: boolean) => {
            if (!entityId) return;
            const entry = getEntry(entityId);
            if (newDone) {
                entry.done.add(id);
                entry.undone.delete(id);
            } else {
                entry.done.delete(id);
                entry.undone.add(id);
            }
            forceUpdate(v => v + 1);
        },
        [entityId],
    );

    const entry = entityId ? getEntry(entityId) : { done: new Set<string>(), undone: new Set<string>() };

    return {
        overrideDone: entry.done,
        overrideUndone: entry.undone,
        toggle,
    };
}
