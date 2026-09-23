/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ApiHook, HookCategory } from '../types/notification';

/** Categories no longer offered on API proxy notification settings. */
const HIDDEN_API_PROXY_HOOK_CATEGORIES = new Set(['SUPPORT', 'RATING', 'REVIEW']);

/** Hook ids in those categories — stripped from saved settings so they cannot linger. */
const HIDDEN_API_PROXY_HOOK_IDS = new Set([
    'NEW_SUPPORT_TICKET',
    'NEW_RATING',
    'NEW_RATING_ANSWER',
    'ASK_FOR_REVIEW',
    'REVIEW_OK',
    'REQUEST_FOR_CHANGES',
]);

export function withoutHiddenApiProxyHooks(hooks: readonly string[]): string[] {
    return hooks.filter(h => !HIDDEN_API_PROXY_HOOK_IDS.has(h));
}

/** Group hooks by category, dropping Support / Rating / Review. */
export function groupVisibleApiProxyHooks(hooks: readonly ApiHook[]): HookCategory[] {
    const seen = new Map<string, ApiHook[]>();
    for (const hook of hooks) {
        if (!hook.category || HIDDEN_API_PROXY_HOOK_CATEGORIES.has(hook.category.toUpperCase())) continue;
        const group = seen.get(hook.category) ?? [];
        group.push(hook);
        seen.set(hook.category, group);
    }
    return [...seen.entries()].map(([name, hs]) => ({ name, hooks: hs }));
}

/** API notification hook: warn when a subscription is close to expiry. */
export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID = 'SUBSCRIPTION_CLOSE_TO_EXPIRY';

/** Companion hook persisted alongside the event so days survive without a dedicated API field. */
export const SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_';

export const DEFAULT_CLOSE_TO_EXPIRY_DAYS = 30;
export const MIN_CLOSE_TO_EXPIRY_DAYS = 1;
export const MAX_CLOSE_TO_EXPIRY_DAYS = 366;

export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK: ApiHook = {
    id: SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
    label: 'Subscription close to expiry',
    description: 'Triggered when a subscription is close to its expiry date.',
    scope: 'API',
    category: 'SUBSCRIPTION',
};

/** API notification hook: warn when an API key is close to expiry. */
export const APIKEY_CLOSE_TO_EXPIRY_HOOK_ID = 'APIKEY_CLOSE_TO_EXPIRY';

export const APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'APIKEY_CLOSE_TO_EXPIRY_DAYS_';

export const APIKEY_CLOSE_TO_EXPIRY_HOOK: ApiHook = {
    id: APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
    label: 'API Key close to expiry',
    description: 'Triggered when an API key is close to its expiry date.',
    scope: 'API',
    category: 'API KEY',
};

export const CLOSE_TO_EXPIRY_HOOK_IDS = new Set([SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, APIKEY_CLOSE_TO_EXPIRY_HOOK_ID]);

export function isCloseToExpiryDaysHook(hookId: string): boolean {
    return hookId.startsWith(SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX) || hookId.startsWith(APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX);
}

export function clampCloseToExpiryDays(days: number): number {
    if (!Number.isFinite(days)) return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
    return Math.min(MAX_CLOSE_TO_EXPIRY_DAYS, Math.max(MIN_CLOSE_TO_EXPIRY_DAYS, Math.round(days)));
}

export function parseCloseToExpiryDays(
    hooks: readonly string[],
    prefix: string = SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX,
): number {
    const found = hooks.find(h => h.startsWith(prefix));
    if (!found) return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
    return clampCloseToExpiryDays(Number(found.slice(prefix.length)));
}

export function isCloseToExpiryDaysValid(days: number): boolean {
    return Number.isInteger(days) && days >= MIN_CLOSE_TO_EXPIRY_DAYS && days <= MAX_CLOSE_TO_EXPIRY_DAYS;
}

function withEventDaysHook(hooks: readonly string[], eventId: string, prefix: string, days: number): string[] {
    const cleaned = hooks.filter(h => !h.startsWith(prefix));
    if (!cleaned.includes(eventId)) {
        return cleaned;
    }
    return [...cleaned, `${prefix}${clampCloseToExpiryDays(days)}`];
}

/** Drops companion day hooks, then appends one when the close-to-expiry event is selected. */
export function withCloseToExpiryDaysHook(hooks: readonly string[], days: number): string[] {
    return withEventDaysHook(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX, days);
}

export function persistApiNotificationHooks(hooks: readonly string[], subscriptionDays: number, apiKeyDays: number): string[] {
    return withEventDaysHook(
        withEventDaysHook(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX, subscriptionDays),
        APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
        APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX,
        apiKeyDays,
    );
}

function ensureHookInCategory(categories: readonly HookCategory[], hook: ApiHook, categoryNames: readonly string[]): HookCategory[] {
    const wanted = new Set(categoryNames.map(name => name.toUpperCase()));
    const idx = categories.findIndex(c => wanted.has(c.name.toUpperCase()));
    if (idx === -1) {
        return [...categories, { name: hook.category, hooks: [hook] }];
    }
    const category = categories[idx]!;
    if (category.hooks.some(h => h.id === hook.id)) {
        return [...categories];
    }
    const next = [...categories];
    next[idx] = { ...category, hooks: [...category.hooks, hook] };
    return next;
}

/** Ensures the Subscription category includes the close-to-expiry event, even if `/apis/hooks` omits it. */
export function ensureSubscriptionCloseToExpiry(categories: readonly HookCategory[]): HookCategory[] {
    return ensureHookInCategory(categories, SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK, ['SUBSCRIPTION']);
}

/** Ensures the API KEY category includes the close-to-expiry event, even if `/apis/hooks` omits it. */
export function ensureApiKeyCloseToExpiry(categories: readonly HookCategory[]): HookCategory[] {
    return ensureHookInCategory(categories, APIKEY_CLOSE_TO_EXPIRY_HOOK, ['API KEY', 'APIKEY']);
}

export function ensureApiNotificationExtraHooks(categories: readonly HookCategory[]): HookCategory[] {
    return ensureApiKeyCloseToExpiry(ensureSubscriptionCloseToExpiry(categories));
}
