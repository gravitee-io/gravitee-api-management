/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { DEFAULT_SUBSCRIPTION_FILTER_STATUSES, SUBSCRIPTION_STATUS_DISPLAY, SubscriptionStatus } from './subscription';

describe('subscription entity', () => {
  describe('SUBSCRIPTION_STATUS_DISPLAY', () => {
    it('should contain all subscription statuses with display metadata', () => {
      const expectedStatuses: SubscriptionStatus[] = ['ACCEPTED', 'CLOSED', 'PAUSED', 'PENDING', 'REJECTED', 'RESUMED'];
      expect(SUBSCRIPTION_STATUS_DISPLAY.map(s => s.id)).toEqual(expectedStatuses);
      SUBSCRIPTION_STATUS_DISPLAY.forEach(entry => {
        expect(entry).toHaveProperty('id');
        expect(entry).toHaveProperty('name');
        expect(entry).toHaveProperty('badge');
        expect(typeof entry.name).toBe('string');
        expect(entry.name.length).toBeGreaterThan(0);
        expect(['success', 'neutral', 'accent', 'warning']).toContain(entry.badge);
      });
    });
  });

  describe('DEFAULT_SUBSCRIPTION_FILTER_STATUSES', () => {
    it('should be ACCEPTED, PAUSED, PENDING', () => {
      expect(DEFAULT_SUBSCRIPTION_FILTER_STATUSES).toEqual(['ACCEPTED', 'PAUSED', 'PENDING']);
    });

    it('should only contain valid SubscriptionStatus values', () => {
      const validStatusSet = new Set(SUBSCRIPTION_STATUS_DISPLAY.map(s => s.id));
      DEFAULT_SUBSCRIPTION_FILTER_STATUSES.forEach(status => {
        expect(validStatusSet.has(status)).toBe(true);
      });
    });
  });
});
