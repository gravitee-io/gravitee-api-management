/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

/** Portal application notification hook catalogue entry (`GET /applications/hooks`). */
export interface PortalNotificationHook {
  id: string;
  label?: string;
  description?: string;
  category?: string;
}

export interface NotificationInput {
  hooks: string[];
}

export interface PortalNotificationHookCategory {
  name: string;
  hooks: PortalNotificationHook[];
}

export const APIKEY_EXPIRED_HOOK: PortalNotificationHook = {
  id: 'APIKEY_EXPIRED',
  label: 'API Key expired',
  description: 'Triggered when an API key expires.',
  category: 'APIKEY',
};

export const APIKEY_RENEWED_HOOK: PortalNotificationHook = {
  id: 'APIKEY_RENEWED',
  label: 'API Key renewed',
  description: 'Triggered when an API key is renewed.',
  category: 'APIKEY',
};

export const APIKEY_REVOKED_HOOK: PortalNotificationHook = {
  id: 'APIKEY_REVOKED',
  label: 'API Key revoked',
  description: 'Triggered when an API key is revoked.',
  category: 'APIKEY',
};

export const APIKEY_CLOSE_TO_EXPIRY_HOOK: PortalNotificationHook = {
  id: 'APIKEY_CLOSE_TO_EXPIRY',
  label: 'API Key close to expiry',
  description: 'Triggered when an API key is close to its expiry date.',
  category: 'APIKEY',
};

export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK: PortalNotificationHook = {
  id: 'SUBSCRIPTION_CLOSE_TO_EXPIRY',
  label: 'Subscription close to expiry',
  description: 'Triggered when a subscription is close to its expiry date.',
  category: 'SUBSCRIPTION',
};

export const CERTIFICATE_EXPIRY_HOOK: PortalNotificationHook = {
  id: 'CERTIFICATE_EXPIRY',
  label: 'Certificate expiry',
  description: 'Triggered when a client certificate expires.',
  category: 'CERTIFICATE',
};

export const CERTIFICATE_CLOSE_TO_EXPIRY_HOOK: PortalNotificationHook = {
  id: 'CERTIFICATE_CLOSE_TO_EXPIRY',
  label: 'Certificate close to expiry',
  description: 'Triggered when a client certificate is close to its expiry date.',
  category: 'CERTIFICATE',
};

export const API_STARTED_HOOK: PortalNotificationHook = {
  id: 'API_STARTED',
  label: 'API started',
  description: 'Triggered when an API is started.',
  category: 'LIFECYCLE',
};

export const API_STOPPED_HOOK: PortalNotificationHook = {
  id: 'API_STOPPED',
  label: 'API stopped',
  description: 'Triggered when an API is stopped.',
  category: 'LIFECYCLE',
};

export const API_UPDATED_HOOK: PortalNotificationHook = {
  id: 'API_UPDATED',
  label: 'API updated',
  description: 'Triggered when an API is updated.',
  category: 'LIFECYCLE',
};

export const API_DEPLOYED_HOOK: PortalNotificationHook = {
  id: 'API_DEPLOYED',
  label: 'API deployed',
  description: 'Triggered when an API is deployed.',
  category: 'LIFECYCLE',
};

export const API_DEPRECATED_HOOK: PortalNotificationHook = {
  id: 'API_DEPRECATED',
  label: 'API deprecated',
  description: 'Triggered when an API is deprecated.',
  category: 'LIFECYCLE',
};

export type PreferenceHookScope = 'APPLICATION' | 'API';

const HIDDEN_CATEGORIES = new Set(['SUPPORT', 'RATING', 'REVIEW']);
const HIDDEN_HOOK_IDS = new Set([
  'NEW_SUPPORT_TICKET',
  'NEW_RATING',
  'NEW_RATING_ANSWER',
  'ASK_FOR_REVIEW',
  'REVIEW_OK',
  'REQUEST_FOR_CHANGES',
]);

/** Categories shown for application preferences (matches Gamma application notifications). */
const APPLICATION_VISIBLE_CATEGORIES = new Set(['SUBSCRIPTION', 'CERTIFICATE']);

/** Categories shown for API preferences (matches Gamma API notifications, without certificate). */
const API_VISIBLE_CATEGORIES = new Set(['SUBSCRIPTION', 'APIKEY', 'LIFECYCLE']);

export const DEFAULT_CLOSE_TO_EXPIRY_DAYS = 30;
export const MIN_CLOSE_TO_EXPIRY_DAYS = 1;
export const MAX_CLOSE_TO_EXPIRY_DAYS = 366;

export const SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID = 'SUBSCRIPTION_CLOSE_TO_EXPIRY';
export const SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_';
export const CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID = 'CERTIFICATE_CLOSE_TO_EXPIRY';
export const CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_';
export const APIKEY_CLOSE_TO_EXPIRY_HOOK_ID = 'APIKEY_CLOSE_TO_EXPIRY';
export const APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX = 'APIKEY_CLOSE_TO_EXPIRY_DAYS_';

export const CLOSE_TO_EXPIRY_HOOK_IDS = new Set([
  SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
  CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
  APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
]);

const APPLICATION_EXTRA_HOOKS: readonly PortalNotificationHook[] = [
  SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK,
  CERTIFICATE_EXPIRY_HOOK,
  CERTIFICATE_CLOSE_TO_EXPIRY_HOOK,
];

const API_EXTRA_HOOKS: readonly PortalNotificationHook[] = [
  APIKEY_EXPIRED_HOOK,
  APIKEY_RENEWED_HOOK,
  APIKEY_REVOKED_HOOK,
  APIKEY_CLOSE_TO_EXPIRY_HOOK,
  SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK,
  API_STARTED_HOOK,
  API_STOPPED_HOOK,
  API_UPDATED_HOOK,
  API_DEPLOYED_HOOK,
  API_DEPRECATED_HOOK,
];

const CATEGORY_ORDER = ['SUBSCRIPTION', 'APIKEY', 'LIFECYCLE', 'CERTIFICATE'] as const;

const CATEGORY_LABELS: Record<string, string> = {
  SUBSCRIPTION: 'Subscription',
  APIKEY: 'API Key',
  LIFECYCLE: 'API Lifecycle',
  CERTIFICATE: 'Certificate',
};

function normalizeCategoryName(category: string): string {
  const upper = category.toUpperCase().trim();
  if (upper === 'API KEY' || upper === 'API_KEY') {
    return 'APIKEY';
  }
  return upper;
}

export function isCompanionDaysHook(hookId: string): boolean {
  return (
    hookId.startsWith(SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX) ||
    hookId.startsWith(CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX) ||
    hookId.startsWith(APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX)
  );
}

export function clampCloseToExpiryDays(days: number): number {
  if (!Number.isFinite(days)) {
    return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
  }
  return Math.min(MAX_CLOSE_TO_EXPIRY_DAYS, Math.max(MIN_CLOSE_TO_EXPIRY_DAYS, Math.round(days)));
}

export function parseCloseToExpiryDays(hooks: readonly string[], prefix: string): number {
  const found = hooks.find(hook => hook.startsWith(prefix));
  if (!found) {
    return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
  }
  return clampCloseToExpiryDays(Number(found.slice(prefix.length)));
}

function withCloseToExpiryDaysHook(hooks: readonly string[], eventId: string, prefix: string, days: number): string[] {
  const cleaned = hooks.filter(hook => !hook.startsWith(prefix));
  if (!cleaned.includes(eventId)) {
    return cleaned;
  }
  return [...cleaned, `${prefix}${clampCloseToExpiryDays(days)}`];
}

/** Strip companion/support hooks from the editable selection set. */
export function visibleNotificationHooks(hooks: readonly string[]): string[] {
  return hooks.filter(hook => !HIDDEN_HOOK_IDS.has(hook) && !isCompanionDaysHook(hook));
}

/** Persist selected hooks plus companion day hooks for close-to-expiry events. */
export function persistNotificationHooks(
  selectedHooks: readonly string[],
  subscriptionDays: number,
  certificateDays: number,
  apiKeyDays: number = DEFAULT_CLOSE_TO_EXPIRY_DAYS,
): string[] {
  const userHooks = visibleNotificationHooks(selectedHooks);
  return withCloseToExpiryDaysHook(
    withCloseToExpiryDaysHook(
      withCloseToExpiryDaysHook(userHooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX, subscriptionDays),
      CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
      CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX,
      certificateDays,
    ),
    APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
    APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX,
    apiKeyDays,
  );
}

function upsertHook(categories: PortalNotificationHookCategory[], hook: PortalNotificationHook): PortalNotificationHookCategory[] {
  const categoryName = normalizeCategoryName(hook.category ?? 'OTHER');
  const index = categories.findIndex(category => normalizeCategoryName(category.name) === categoryName);
  if (index === -1) {
    return [...categories, { name: categoryName, hooks: [hook] }];
  }
  const category = categories[index];
  if (category.hooks.some(existing => existing.id === hook.id)) {
    return categories;
  }
  const next = [...categories];
  next[index] = { ...category, hooks: [...category.hooks, hook] };
  return next;
}

function isCategoryVisibleForScope(category: string, scope: PreferenceHookScope): boolean {
  const normalized = normalizeCategoryName(category);
  if (HIDDEN_CATEGORIES.has(normalized)) {
    return false;
  }
  return scope === 'APPLICATION' ? APPLICATION_VISIBLE_CATEGORIES.has(normalized) : API_VISIBLE_CATEGORIES.has(normalized);
}

/** Group portal hooks for preferences UI — scope mirrors Gamma console App vs API notifications. */
export function buildPreferenceHookCategories(
  hooks: readonly PortalNotificationHook[],
  scope: PreferenceHookScope,
): PortalNotificationHookCategory[] {
  let categories: PortalNotificationHookCategory[] = [];

  for (const hook of hooks) {
    const category = normalizeCategoryName(hook.category ?? '');
    if (!category || !isCategoryVisibleForScope(category, scope) || HIDDEN_HOOK_IDS.has(hook.id) || isCompanionDaysHook(hook.id)) {
      continue;
    }
    categories = upsertHook(categories, {
      ...hook,
      category,
      label: hook.label ?? hook.id,
      description: hook.description ?? '',
    });
  }

  const extras = scope === 'APPLICATION' ? APPLICATION_EXTRA_HOOKS : API_EXTRA_HOOKS;
  for (const extra of extras) {
    categories = upsertHook(categories, extra);
  }

  return [...categories]
    .filter(category => isCategoryVisibleForScope(category.name, scope))
    .sort((left, right) => {
      const leftRank = CATEGORY_ORDER.indexOf(normalizeCategoryName(left.name) as (typeof CATEGORY_ORDER)[number]);
      const rightRank = CATEGORY_ORDER.indexOf(normalizeCategoryName(right.name) as (typeof CATEGORY_ORDER)[number]);
      const safeLeft = leftRank === -1 ? CATEGORY_ORDER.length : leftRank;
      const safeRight = rightRank === -1 ? CATEGORY_ORDER.length : rightRank;
      return safeLeft - safeRight || left.name.localeCompare(right.name);
    });
}

export function categoryDisplayName(category: string): string {
  return CATEGORY_LABELS[normalizeCategoryName(category)] ?? category;
}

/** All hook ids shown under API preferences (subscription, API key, lifecycle). */
export function allApiPreferenceHookIds(catalogueHooks: readonly PortalNotificationHook[] = []): string[] {
  return buildPreferenceHookCategories(catalogueHooks, 'API').flatMap(category => category.hooks.map(hook => hook.id));
}

/** Distinctive API-only hooks used to detect catalog bell subscription state. */
export function apiDistinctiveNotificationHookIds(): string[] {
  return API_EXTRA_HOOKS.filter(hook => hook.category === 'APIKEY' || hook.category === 'LIFECYCLE').map(hook => hook.id);
}

export function isApiCatalogNotificationSubscribed(savedHooks: readonly string[]): boolean {
  const saved = new Set(savedHooks);
  return apiDistinctiveNotificationHookIds().every(hookId => saved.has(hookId));
}

/** Merge all API preference hooks into an existing application notification set. */
export function withAllApiNotificationHooks(
  existingHooks: readonly string[],
  catalogueHooks: readonly PortalNotificationHook[] = [],
): string[] {
  const merged = new Set([...visibleNotificationHooks(existingHooks), ...allApiPreferenceHookIds(catalogueHooks)]);
  return persistNotificationHooks(
    [...merged],
    parseCloseToExpiryDays(existingHooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX),
    parseCloseToExpiryDays(existingHooks, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX),
    parseCloseToExpiryDays(existingHooks, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX),
  );
}

/** Remove API-key and lifecycle hooks while preserving application subscription/certificate prefs. */
export function withoutApiDistinctiveNotificationHooks(existingHooks: readonly string[]): string[] {
  const distinctive = new Set(apiDistinctiveNotificationHookIds());
  const remaining = visibleNotificationHooks(existingHooks).filter(hookId => !distinctive.has(hookId));
  return persistNotificationHooks(
    remaining,
    parseCloseToExpiryDays(existingHooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX),
    parseCloseToExpiryDays(existingHooks, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX),
    parseCloseToExpiryDays(existingHooks, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX),
  );
}
