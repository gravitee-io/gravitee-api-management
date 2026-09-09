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
import { Rule } from './rule.metrics';

import { Scope } from '../alert';

describe('Rule', () => {
  describe('findCategoriesByScope', () => {
    const initialRules = Rule.RULES;

    afterEach(() => {
      Rule.RULES = initialRules;
    });

    it.each([false, true])('should include health-check rules for API scope when cloud is %s', (cloudEnabled) => {
      expect(Rule.findCategoriesByScope(Scope.API, cloudEnabled)).toEqual(['API metrics', 'Health-check']);
    });

    it('should keep node categories first for environment scope', () => {
      expect(Rule.findCategoriesByScope(Scope.ENVIRONMENT, false)).toEqual(['Node', 'API metrics', 'Health-check']);
    });

    it('should exclude node categories for environment scope in cloud', () => {
      expect(Rule.findCategoriesByScope(Scope.ENVIRONMENT, true)).toEqual(['API metrics', 'Health-check']);
    });

    it.each([false, true])('should include application rules for application scope when cloud is %s', (cloudEnabled) => {
      expect(Rule.findCategoriesByScope(Scope.APPLICATION, cloudEnabled)).toEqual(['Application']);
    });

    it('should only include categories with rules available for the scope', () => {
      Rule.RULES = Rule.RULES.filter((rule) => rule.category !== 'Health-check');

      expect(Rule.findCategoriesByScope(Scope.API, false)).toEqual(['API metrics']);
    });
  });
});
