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

import { sanitizeKeyBase, sanitizeKeyFinal } from './key-sanitizer.util';

describe('Key Sanitizer Utils', () => {
  describe('sanitizeKeyBase', () => {
    it.each`
      input                                | expected
      ${'My Tag Key'}                      | ${'my-tag-key'}
      ${'Tâg Spécîal @#$ Nàme!'}           | ${'tag-special-name'}
      ${'Tag   With    Multiple---Spaces'} | ${'tag-with-multiple-spaces'}
      ${'UPPERCASE KEY'}                   | ${'uppercase-key'}
      ${'Key 123 Value 456'}               | ${'key-123-value-456'}
      ${'eu east 1! @#$%'}                 | ${'eu-east-1'}
      ${'café'}                            | ${'cafe'}
      ${'  spaces-around  '}               | ${'spaces-around'}
    `('should sanitize "$input" to "$expected"', ({ input, expected }) => {
      expect(sanitizeKeyBase(input)).toBe(expected);
    });
  });

  describe('sanitizeKeyFinal', () => {
    it.each`
      input                    | expected
      ${'My Tag Key'}          | ${'my-tag-key'}
      ${'Tag Key-'}            | ${'tag-key'}
      ${'UPPERCASE KEY'}       | ${'uppercase-key'}
      ${'Key 123 Value 456'}   | ${'key-123-value-456'}
      ${'eu east 1! @#$%'}     | ${'eu-east-1'}
      ${'trailing-hyphens---'} | ${'trailing-hyphens'}
    `('should sanitize "$input" to "$expected"', ({ input, expected }) => {
      expect(sanitizeKeyFinal(input)).toBe(expected);
    });
  });
});
