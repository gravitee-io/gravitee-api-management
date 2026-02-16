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
import { FormControl } from '@angular/forms';

import { CorsUtil } from './cors.util';

describe('CorsUtil', () => {
  describe('allowOriginValidator', () => {
    const validator = CorsUtil.allowOriginValidator();

    const allowOriginValidTestCases = [[[]], [['*']], [['X-foo', 'X-bar']]];

    it.each(allowOriginValidTestCases)('should validate %p origin', origin => {
      const result = validator(new FormControl(origin));
      expect(result).toEqual(null);
    });

    const allowOriginInvalidTestCases = [
      [["('"], { allowOrigin: '"(\'" Regex is invalid' }],
      [["('", 'X-foo'], { allowOrigin: '"(\'" Regex is invalid' }],
      [['(', '['], { allowOrigin: '"(", "[" Regex is invalid' }],
    ];
    it.each(allowOriginInvalidTestCases)('should validate %p origin', (origin, error) => {
      const result = validator(new FormControl(origin));
      expect(result).toEqual(error);
    });
  });
});
