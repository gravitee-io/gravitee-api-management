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
import {
  API_STARTED_HOOK,
  APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX,
  APIKEY_CLOSE_TO_EXPIRY_HOOK,
  buildPreferenceHookCategories,
  CERTIFICATE_CLOSE_TO_EXPIRY_HOOK,
  isApiCatalogNotificationSubscribed,
  parseCloseToExpiryDays,
  persistNotificationHooks,
  SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX,
  SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK,
  visibleNotificationHooks,
  withAllApiNotificationHooks,
  withoutApiDistinctiveNotificationHooks,
} from './portal-notification-hook';

describe('portal-notification-hook', () => {
  const catalogueHooks = [
    {
      id: 'SUBSCRIPTION_ACCEPTED',
      label: 'Subscription accepted',
      category: 'SUBSCRIPTION',
    },
    {
      id: 'NEW_SUPPORT_TICKET',
      label: 'Support',
      category: 'SUPPORT',
    },
  ];

  it('builds application categories without API Key and with Certificate', () => {
    const categories = buildPreferenceHookCategories(catalogueHooks, 'APPLICATION');

    expect(categories.map(category => category.name)).toEqual(['SUBSCRIPTION', 'CERTIFICATE']);
    expect(categories.find(category => category.name === 'SUBSCRIPTION')?.hooks.map(hook => hook.id)).toContain(
      SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK.id,
    );
    expect(categories.find(category => category.name === 'CERTIFICATE')?.hooks.map(hook => hook.id)).toEqual([
      'CERTIFICATE_EXPIRY',
      CERTIFICATE_CLOSE_TO_EXPIRY_HOOK.id,
    ]);
    expect(categories.some(category => category.name === 'APIKEY')).toBe(false);
    expect(categories.some(category => category.hooks.some(hook => hook.id === 'NEW_SUPPORT_TICKET'))).toBe(false);
  });

  it('builds API categories with Lifecycle and without Certificate', () => {
    const categories = buildPreferenceHookCategories(catalogueHooks, 'API');

    expect(categories.map(category => category.name)).toEqual(['SUBSCRIPTION', 'APIKEY', 'LIFECYCLE']);
    expect(categories.find(category => category.name === 'LIFECYCLE')?.hooks.map(hook => hook.id)).toEqual([
      API_STARTED_HOOK.id,
      'API_STOPPED',
      'API_UPDATED',
      'API_DEPLOYED',
      'API_DEPRECATED',
    ]);
    expect(categories.some(category => category.name === 'CERTIFICATE')).toBe(false);
    expect(categories.find(category => category.name === 'APIKEY')?.hooks.map(hook => hook.id)).toContain(
      APIKEY_CLOSE_TO_EXPIRY_HOOK.id,
    );
  });

  it('persists companion day hooks for close-to-expiry events', () => {
    const hooks = persistNotificationHooks(
      ['SUBSCRIPTION_ACCEPTED', 'SUBSCRIPTION_CLOSE_TO_EXPIRY', 'CERTIFICATE_CLOSE_TO_EXPIRY', 'APIKEY_CLOSE_TO_EXPIRY'],
      14,
      7,
      21,
    );

    expect(visibleNotificationHooks(hooks)).toEqual([
      'SUBSCRIPTION_ACCEPTED',
      'SUBSCRIPTION_CLOSE_TO_EXPIRY',
      'CERTIFICATE_CLOSE_TO_EXPIRY',
      'APIKEY_CLOSE_TO_EXPIRY',
    ]);
    expect(parseCloseToExpiryDays(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX)).toBe(14);
    expect(hooks).toContain('CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_7');
    expect(parseCloseToExpiryDays(hooks, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX)).toBe(21);
  });

  it('merges and detects catalog API notification subscription state', () => {
    const merged = withAllApiNotificationHooks(['SUBSCRIPTION_ACCEPTED']);
    expect(isApiCatalogNotificationSubscribed(merged)).toBe(true);
    expect(merged).toEqual(expect.arrayContaining(['API_STARTED', 'APIKEY_EXPIRED', 'SUBSCRIPTION_CLOSE_TO_EXPIRY']));

    const cleared = withoutApiDistinctiveNotificationHooks(merged);
    expect(isApiCatalogNotificationSubscribed(cleared)).toBe(false);
    expect(cleared).toContain('SUBSCRIPTION_ACCEPTED');
    expect(cleared).not.toContain('API_STARTED');
  });
});
