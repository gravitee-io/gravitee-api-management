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
import { catalogAccess, isAgent, protocolLabel, publishedLabel } from './catalog-item';
import { Plan } from '../../entities/plan/plan';

const plan = (overrides: Partial<Plan>): Plan =>
  ({ id: 'plan-1', name: 'Plan', security: 'API_KEY', validation: 'AUTO', order: 1, mode: 'STANDARD', ...overrides }) as Plan;

describe('catalog item', () => {
  describe('isAgent', () => {
    it.each([
      ['A2A_PROXY' as const, false, true],
      ['MCP_PROXY' as const, false, true],
      ['PROXY' as const, true, true],
      ['PROXY' as const, false, false],
      [undefined, false, false],
    ])('reads %s / mcp=%s as agent=%s', (apiType, hasMcpServer, expected) => {
      expect(isAgent(apiType, hasMcpServer)).toBe(expected);
    });
  });

  describe('protocolLabel', () => {
    it.each([
      ['A2A_PROXY' as const, false, 'A2A'],
      ['MCP_PROXY' as const, false, 'MCP'],
      ['PROXY' as const, true, 'MCP'],
      ['PROXY' as const, false, 'REST'],
    ])('names %s / mcp=%s as %s', (apiType, hasMcpServer, expected) => {
      expect(protocolLabel(apiType, hasMcpServer)).toBe(expected);
    });
  });

  describe('catalogAccess', () => {
    it('reports a subscription before anything the plans say', () => {
      expect(catalogAccess([plan({ validation: 'MANUAL' })], true)).toBe('SUBSCRIBED');
    });

    it('reports nothing when the plans are unknown', () => {
      expect(catalogAccess([], false)).toBeUndefined();
    });

    it('reports the keyless plan even when other plans need a key', () => {
      expect(catalogAccess([plan({ security: 'API_KEY' }), plan({ security: 'KEY_LESS' })], false)).toBe('NO_KEY');
    });

    it('reports approval only when every plan is validated by hand', () => {
      expect(catalogAccess([plan({ validation: 'MANUAL' }), plan({ validation: 'MANUAL' })], false)).toBe('APPROVAL');
      expect(catalogAccess([plan({ validation: 'MANUAL' }), plan({ validation: 'AUTO' })], false)).toBe('CREDENTIALS');
    });
  });

  describe('publishedLabel', () => {
    const now = new Date('2026-09-02T12:00:00Z');
    const daysBefore = (days: number) => new Date(now.getTime() - days * 24 * 60 * 60 * 1000);

    it.each([
      [0, 'today'],
      [3, '3 days ago'],
      [12, '1 week ago'],
      [21, '3 weeks ago'],
      [95, '3 months ago'],
      [400, '1 year ago'],
      [800, '2 years ago'],
    ])('says %s days back as "%s"', (days, expected) => {
      expect(publishedLabel(daysBefore(days), 'en', now)).toBe(expected);
    });

    it('speaks the locale it is given', () => {
      expect(publishedLabel(daysBefore(95), 'fr', now)).toBe('il y a 3 mois');
    });

    it('says nothing without a date', () => {
      expect(publishedLabel(undefined, 'en', now)).toBeUndefined();
    });
  });
});
