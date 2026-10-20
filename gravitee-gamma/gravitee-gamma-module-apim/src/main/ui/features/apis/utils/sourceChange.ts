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
import type { PortalNavigationItemSource } from '../types/apiDocumentation';

/**
 * Whether an edited source is the one stored. Only what the user sets counts: the fetcher, its settings and the schedule.
 * What the server records about the fetches is left out. A setting left empty or off counts as unset: a settings form
 * fills in the fields it shows, and the server leaves settings that are false out.
 */
export function isSameSource(stored: PortalNavigationItemSource, edited: PortalNavigationItemSource): boolean {
    return comparable(stored) === comparable(edited);
}

const UNSET_VALUES = new Set<unknown>(['', undefined, null, false]);

function comparable(source: PortalNavigationItemSource): string {
    const configuration = Object.keys(source.configuration)
        .filter(key => !UNSET_VALUES.has(source.configuration[key]))
        .sort()
        .map(key => [key, source.configuration[key]]);
    const useAutoFetch = source.useAutoFetch ?? false;
    return JSON.stringify([source.type, configuration, useAutoFetch, useAutoFetch ? (source.fetchCron ?? null) : null]);
}

/**
 * The source an update sends: an edited source that differs from the stored one, null to remove the stored one, or
 * undefined to keep it — also when the edit could not be made, as when the source types failed to load.
 */
export function sourceChangeOf(
    stored: PortalNavigationItemSource | undefined,
    fetched: boolean,
    edited: { source: PortalNavigationItemSource | null; unchanged?: boolean },
): PortalNavigationItemSource | null | undefined {
    if (!fetched) return stored ? null : undefined;
    if (!edited.source) return undefined;
    return stored && edited.unchanged ? undefined : edited.source;
}
