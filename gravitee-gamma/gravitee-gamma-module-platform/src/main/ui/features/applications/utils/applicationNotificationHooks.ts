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
import type { ApplicationNotificationHook, ApplicationNotificationHookCategory } from '../types/applicationNotification';

/** Categories no longer offered on application notification settings. */
const HIDDEN_APPLICATION_HOOK_CATEGORIES = new Set(['SUPPORT']);

/** Hook ids in those categories — stripped from saved settings so they cannot linger. */
const HIDDEN_APPLICATION_HOOK_IDS = new Set(['NEW_SUPPORT_TICKET']);

export const DEFAULT_CLOSE_TO_EXPIRY_DAYS = 30;
export const MIN_CLOSE_TO_EXPIRY_DAYS = 1;
export const MAX_CLOSE_TO_EXPIRY_DAYS = 366;

export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID = 'SUBSCRIPTION_CLOSE_TO_EXPIRY';
export const SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_';

export const CERTIFICATE_EXPIRY_HOOK_ID = 'CERTIFICATE_EXPIRY';
export const CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID = 'CERTIFICATE_CLOSE_TO_EXPIRY';
export const CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_';

export const CLOSE_TO_EXPIRY_HOOK_IDS = new Set([SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID]);

export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK: ApplicationNotificationHook = {
    id: SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
    label: 'Subscription close to expiry',
    description: 'Triggered when a subscription is close to its expiry date.',
    scope: 'APPLICATION',
    category: 'SUBSCRIPTION',
};

export const CERTIFICATE_EXPIRY_HOOK: ApplicationNotificationHook = {
    id: CERTIFICATE_EXPIRY_HOOK_ID,
    label: 'Certificate Expiry',
    description: 'Triggered when a certificate expires.',
    scope: 'APPLICATION',
    category: 'CERTIFICATE',
};

export const CERTIFICATE_CLOSE_TO_EXPIRY_HOOK: ApplicationNotificationHook = {
    id: CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
    label: 'Certificate close to expiry',
    description: 'Triggered when a certificate is close to its expiry date.',
    scope: 'APPLICATION',
    category: 'CERTIFICATE',
};

export function isCompanionDaysHook(hookId: string): boolean {
    return hookId.startsWith(SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX) || hookId.startsWith(CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX);
}

export function withoutHiddenApplicationHooks(hooks: readonly string[]): string[] {
    return hooks.filter(h => !HIDDEN_APPLICATION_HOOK_IDS.has(h) && !isCompanionDaysHook(h));
}

export function clampCloseToExpiryDays(days: number): number {
    if (!Number.isFinite(days)) return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
    return Math.min(MAX_CLOSE_TO_EXPIRY_DAYS, Math.max(MIN_CLOSE_TO_EXPIRY_DAYS, Math.round(days)));
}

export function isCloseToExpiryDaysValid(days: number): boolean {
    return Number.isInteger(days) && days >= MIN_CLOSE_TO_EXPIRY_DAYS && days <= MAX_CLOSE_TO_EXPIRY_DAYS;
}

export function parseCloseToExpiryDays(hooks: readonly string[], prefix: string): number {
    const found = hooks.find(h => h.startsWith(prefix));
    if (!found) return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
    return clampCloseToExpiryDays(Number(found.slice(prefix.length)));
}

function withCloseToExpiryDaysHook(hooks: readonly string[], eventId: string, prefix: string, days: number): string[] {
    const cleaned = hooks.filter(h => !h.startsWith(prefix));
    if (!cleaned.includes(eventId)) {
        return cleaned;
    }
    return [...cleaned, `${prefix}${clampCloseToExpiryDays(days)}`];
}

/** Drops companion day hooks, then appends one per selected close-to-expiry event. */
export function withApplicationCloseToExpiryDaysHooks(
    hooks: readonly string[],
    subscriptionDays: number,
    certificateDays: number,
): string[] {
    return withCloseToExpiryDaysHook(
        withCloseToExpiryDaysHook(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX, subscriptionDays),
        CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
        CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX,
        certificateDays,
    );
}

export function persistApplicationNotificationHooks(
    selectedHooks: readonly string[],
    groupHookIds: ReadonlySet<string>,
    subscriptionDays: number,
    certificateDays: number,
): string[] {
    const userHooks = withoutHiddenApplicationHooks(selectedHooks.filter(hookId => !groupHookIds.has(hookId)));
    return withApplicationCloseToExpiryDaysHooks(userHooks, subscriptionDays, certificateDays);
}

export function visibleSubscribedEventCount(hooks: readonly string[] = [], groupHooks: readonly string[] = []): number {
    return [...hooks, ...groupHooks].filter(h => !HIDDEN_APPLICATION_HOOK_IDS.has(h) && !isCompanionDaysHook(h)).length;
}

function upsertHookInCategory(
    categories: readonly ApplicationNotificationHookCategory[],
    categoryName: string,
    hook: ApplicationNotificationHook,
): ApplicationNotificationHookCategory[] {
    const idx = categories.findIndex(c => c.name.toUpperCase() === categoryName.toUpperCase());
    if (idx === -1) {
        return [...categories, { name: categoryName, hooks: [hook] }];
    }
    const category = categories[idx]!;
    if (category.hooks.some(h => h.id === hook.id)) {
        return [...categories];
    }
    const next = [...categories];
    next[idx] = { ...category, hooks: [...category.hooks, hook] };
    return next;
}

function ensureCertificateHooks(categories: readonly ApplicationNotificationHookCategory[]): ApplicationNotificationHookCategory[] {
    const withExpiry = upsertHookInCategory(categories, 'CERTIFICATE', CERTIFICATE_EXPIRY_HOOK);
    return upsertHookInCategory(withExpiry, 'CERTIFICATE', CERTIFICATE_CLOSE_TO_EXPIRY_HOOK);
}

/** Group hooks by category, dropping Support and injecting subscription / certificate expiry events. */
export function groupVisibleApplicationHooks(hooks: readonly ApplicationNotificationHook[]): ApplicationNotificationHookCategory[] {
    const seen = new Map<string, ApplicationNotificationHook[]>();
    for (const hook of hooks) {
        if (!hook.category || HIDDEN_APPLICATION_HOOK_CATEGORIES.has(hook.category.toUpperCase())) continue;
        if (isCompanionDaysHook(hook.id) || HIDDEN_APPLICATION_HOOK_IDS.has(hook.id)) continue;
        const group = seen.get(hook.category) ?? [];
        group.push(hook);
        seen.set(hook.category, group);
    }
    const grouped = [...seen.entries()].map(([name, hs]) => ({ name, hooks: hs }));
    return ensureCertificateHooks(upsertHookInCategory(grouped, 'SUBSCRIPTION', SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK));
}
