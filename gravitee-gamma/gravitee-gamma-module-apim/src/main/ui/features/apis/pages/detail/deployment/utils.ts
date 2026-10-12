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
import type { SideLine, UnifiedLine } from './types';
import { diffLines } from '../../../../../shared/utils/lineDiff';
import type { ApiDetailDto, ApiEvent } from '../../../types';

export function formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

/** Synthetic event for the unpublished definition. Not a real event id, so rollback must stay hidden. */
export const PENDING_DEPLOYMENT_EVENT_ID = 'to-be-deployed';

export function pendingDeploymentEvent(definition: unknown): ApiEvent {
    return {
        id: PENDING_DEPLOYMENT_EVENT_ID,
        createdAt: new Date().toISOString(),
        payload: JSON.stringify(definition ?? {}),
        initiator: { id: '', displayName: 'Pending changes' },
        properties: { DEPLOYMENT_NUMBER: 'to be deployed' },
    };
}

/**
 * Classic hides compare-dialog rollback for the live deployment when the API is in sync,
 * and for every side of a V4 native API (`hideRollback` in `api-history-v4.component.ts`).
 */
export function hideCompareRollback(
    event: ApiEvent,
    options: { liveEventId: string | null; needsRedeploy: boolean; isNative: boolean },
): boolean {
    if (event.id === PENDING_DEPLOYMENT_EVENT_ID) return true;
    if (options.isNative) return true;
    return !options.needsRedeploy && options.liveEventId !== null && event.id === options.liveEventId;
}

export function chronologicalEvents(events: readonly ApiEvent[]): [ApiEvent, ApiEvent] | null {
    if (events.length !== 2) return null;
    const [first, second] = events;
    return first.createdAt <= second.createdAt ? [first, second] : [second, first];
}

export function extractDefinition(event: ApiEvent): string {
    try {
        const outer = JSON.parse(event.payload ?? '{}') as Record<string, unknown>;
        const raw = typeof outer.definition === 'string' ? (JSON.parse(outer.definition) as unknown) : outer;
        return JSON.stringify(raw, null, 2);
    } catch {
        return event.payload ?? '';
    }
}

function splitLines(text: string): string[] {
    const lines = text.split('\n');
    if (lines[lines.length - 1] === '') lines.pop();
    return lines;
}

export function computeUnifiedDiff(left: string, right: string): UnifiedLine[] {
    return diffLines(left, right).flatMap(change =>
        splitLines(change.value).map(text => ({
            type: (change.added ? '+' : change.removed ? '-' : ' ') as '+' | '-' | ' ',
            text,
        })),
    );
}

export function computeSideBySideDiff(left: string, right: string): SideLine[] {
    const changes = diffLines(left, right);
    const result: SideLine[] = [];
    let i = 0;

    while (i < changes.length) {
        const curr = changes[i];
        if (!curr.added && !curr.removed) {
            for (const line of splitLines(curr.value)) result.push({ left: line, right: line, kind: 'equal' });
            i++;
        } else if (curr.removed) {
            const removedLines = splitLines(curr.value);
            const nextIsAdded = i + 1 < changes.length && changes[i + 1].added;
            const addedLines = nextIsAdded ? splitLines(changes[i + 1].value) : [];
            const maxLen = Math.max(removedLines.length, addedLines.length);
            for (let j = 0; j < maxLen; j++) {
                result.push({ left: removedLines[j] ?? null, right: addedLines[j] ?? null, kind: 'changed' });
            }
            i += nextIsAdded ? 2 : 1;
        } else {
            for (const line of splitLines(curr.value)) result.push({ left: null, right: line, kind: 'added' });
            i++;
        }
    }
    return result;
}

export function hasDiffChanges(lines: UnifiedLine[]): boolean {
    return lines.some(l => l.type !== ' ');
}

/** The management API returns a native API as a V4 API of type NATIVE. */
export function isNativeApi(api: ApiDetailDto | null): boolean {
    return api?.definitionVersion === 'V4' && api.type === 'NATIVE';
}

type RollbackContext = { liveEventId: string | undefined; isNative: boolean; needsRedeploy: boolean };

/**
 * The live version is already deployed, so rolling back to it only makes sense to discard pending
 * changes. Native APIs cannot be rolled back.
 */
export function canRollbackTo(event: ApiEvent, { liveEventId, isNative, needsRedeploy }: RollbackContext): boolean {
    if (isNative) return false;
    const isLiveVersion = event.id === liveEventId;
    return !isLiveVersion || needsRedeploy;
}
