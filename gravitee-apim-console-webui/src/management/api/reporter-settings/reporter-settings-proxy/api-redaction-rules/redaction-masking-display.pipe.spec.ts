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

import { RedactionMaskingDisplayPipe } from './redaction-masking-display.pipe';

import { RedactionRule } from '../../../../../entities/management-api-v2';

describe('RedactionMaskingDisplayPipe', () => {
  const pipe = new RedactionMaskingDisplayPipe();

  describe('FULL masking', () => {
    it('should use explicit replacement text when provided', () => {
      const rule: RedactionRule = { attributeNamePattern: 'x', maskingStrategy: { type: 'FULL', replacement: '***' } };
      const result = pipe.transform(rule);
      expect(result.label).toBe('FULL');
      expect(result.badgeClass).toBe('gio-badge-neutral');
      expect(result.detail).toBe('→ "***"');
    });

    it('should default to [REDACTED] when no replacement is provided', () => {
      const rule: RedactionRule = { attributeNamePattern: 'x', maskingStrategy: { type: 'FULL' } };
      const result = pipe.transform(rule);
      expect(result.label).toBe('FULL');
      expect(result.detail).toBe('→ "[REDACTED]"');
    });
  });

  describe('PARTIAL masking', () => {
    it('should render prefix, suffix and char', () => {
      const rule: RedactionRule = {
        attributeNamePattern: 'x',
        maskingStrategy: { type: 'PARTIAL', prefixLength: 2, suffixLength: 3, replacement: '#' },
      };
      const result = pipe.transform(rule);
      expect(result.label).toBe('PARTIAL');
      expect(result.badgeClass).toBe('gio-badge-accent');
      expect(result.detail).toBe('prefix 2 · suffix 3 · char "#"');
    });

    it('should default prefix, suffix and char to 0 / 0 / * when not provided', () => {
      const rule: RedactionRule = { attributeNamePattern: 'x', maskingStrategy: { type: 'PARTIAL' } };
      const result = pipe.transform(rule);
      expect(result.detail).toBe('prefix 0 · suffix 0 · char "*"');
    });
  });
});
