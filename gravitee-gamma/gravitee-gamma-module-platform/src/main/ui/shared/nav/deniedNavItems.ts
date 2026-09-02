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
import { useSyncExternalStore } from 'react';

const NONE: ReadonlySet<string> = new Set();

let deniedKeys: ReadonlySet<string> = NONE;
let currentEnvironmentId: string | undefined;
const listeners = new Set<() => void>();

function emit(): void {
    listeners.forEach(listener => listener());
}

/**
 * Records that a live 403 denied `itemKey`, so the sidebar hides it and the landing key skips it.
 *
 * The permission strip in `useForbiddenResourceRedirect` only rewrites the environment scope, so a
 * 403 on a page whose permission is organization-scoped strips nothing: the grant still comes from
 * the host's organization scope, the item stays visible, and it stays the landing target. Every
 * redirect out of the page then resolves back to it. Recording the denial by nav key instead of by
 * permission works whatever scope granted it.
 */
export function markNavItemDenied(itemKey: string): void {
    if (deniedKeys.has(itemKey)) return;
    deniedKeys = new Set(deniedKeys).add(itemKey);
    emit();
}

/**
 * Drops the denials when the user switches environment — they describe one environment's backend,
 * not the user's role. The first environment seen is not a switch: page-level effects run before the
 * layout's, so a denial recorded during the very first commit would otherwise be wiped immediately.
 */
export function resetDeniedNavItemsForEnvironment(environmentId: string | undefined): void {
    const isFirstEnvironment = currentEnvironmentId === undefined;
    if (currentEnvironmentId === environmentId) return;
    currentEnvironmentId = environmentId;
    if (isFirstEnvironment || deniedKeys === NONE) return;
    deniedKeys = NONE;
    emit();
}

function subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

/** Nav items a live 403 has denied in the current environment. */
export function getDeniedNavItemKeys(): ReadonlySet<string> {
    return deniedKeys;
}

/** Re-renders when a 403 denies a nav item, or when an environment switch clears the denials. */
export function useDeniedNavItemKeys(): ReadonlySet<string> {
    return useSyncExternalStore(subscribe, getDeniedNavItemKeys, getDeniedNavItemKeys);
}
