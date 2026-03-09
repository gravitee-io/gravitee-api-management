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
import { ApplicationTypeTranslatePipe } from './application-type-translate.pipe';
import { ApplicationType } from '../entities/application/application';

type LocalizeFunction = (strings: TemplateStringsArray, ...values: string[]) => string;
type GlobalWithLocalize = typeof globalThis & { $localize?: LocalizeFunction };

describe('ApplicationTypeTranslatePipe', () => {
  const pipe = new ApplicationTypeTranslatePipe();
  const originalLocalize = (globalThis as GlobalWithLocalize).$localize;

  afterEach(() => {
    (globalThis as GlobalWithLocalize).$localize = originalLocalize;
  });

  it('returns empty string for null/undefined or missing id', () => {
    expect(pipe.transform(null, 'name')).toBe('');
    expect(pipe.transform(undefined, 'name')).toBe('');
    expect(pipe.transform({ name: 'X' } as ApplicationType, 'name')).toBe('');
  });

  describe('unknown type', () => {
    it('falls back to backend values', () => {
      const type: ApplicationType = { id: 'unknown', name: 'N', description: 'D' };
      expect(pipe.transform(type, 'name')).toBe('N');
      expect(pipe.transform(type, 'description')).toBe('D');
    });

    it('falls back to id when name missing', () => {
      const type: ApplicationType = { id: 'unknown', name: '' };
      expect(pipe.transform(type, 'name')).toBe('unknown');
      expect(pipe.transform(type, 'description')).toBe('');
    });
  });

  describe('when $localize is not available', () => {
    beforeEach(() => {
      delete (globalThis as GlobalWithLocalize).$localize;
    });

    it('returns backend value for known types', () => {
      const type: ApplicationType = {
        id: 'simple',
        name: 'Simple',
        description: 'Backend desc',
      };

      expect(pipe.transform(type, 'name')).toBe('Simple');
      expect(pipe.transform(type, 'description')).toBe('Backend desc');
    });
  });

  describe('when $localize is available', () => {
    beforeEach(() => {
      (globalThis as GlobalWithLocalize).$localize = (strings: TemplateStringsArray): string => {
        const full = strings.join('');
        const i = full.lastIndexOf(':');
        return i >= 0 ? full.slice(i + 1) : full;
      };
    });

    it('returns fallback EN from $localize for known type', () => {
      const type: ApplicationType = {
        id: 'simple',
        name: 'Simple',
        description: 'Backend desc',
      };

      expect(pipe.transform(type, 'name')).toBe('Simple');
      expect(pipe.transform(type, 'description')).toBe('A standalone client where you manage your own client_id. No DCR involved.');
    });

    it('returns translated value if $localize returns different text', () => {
      (globalThis as GlobalWithLocalize).$localize = () => '[TRANSLATED] Simple';

      const type: ApplicationType = { id: 'simple', name: 'Simple' };
      expect(pipe.transform(type, 'name')).toBe('[TRANSLATED] Simple');
    });

    it.each([
      ['browser', 'SPA'],
      ['web', 'Web'],
      ['native', 'Native'],
      ['backend_to_backend', 'Backend to backend'],
    ])('returns EN fallback for %s name', (id, expected) => {
      const type: ApplicationType = { id, name: expected };
      expect(pipe.transform(type, 'name')).toBe(expected);
    });
  });
});
