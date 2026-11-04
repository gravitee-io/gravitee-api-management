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
import { deriveNotificationTypeLabel } from './notification-type-label';

describe('deriveNotificationTypeLabel', () => {
  it.each([
    ['SUBSCRIPTION_ACCEPTED', 'Subscription accepted'],
    ['API_STOPPED', 'API stopped'],
    ['API_PRODUCT_DEPLOYED', 'API Product deployed'],
    ['API_PRODUCT_UPDATED', 'API Product updated'],
  ])('should label the %s hook', (hook, label) => {
    expect(deriveNotificationTypeLabel(hook)).toBe(label);
  });

  it.each([[undefined], ['A_HOOK_ADDED_LATER']])('should fall back to a generic label for %s', hook => {
    expect(deriveNotificationTypeLabel(hook)).toBe('Notification');
  });
});
