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
    countSatisfiedPasswordRules,
    evaluatePasswordPolicyRule,
    isPasswordPolicySatisfied,
    resolvePasswordStrengthLevel,
    type PasswordPolicyRule,
} from '../../../../../gamma-ui-shared/src/passwordPolicy';

const TEST_RULES: PasswordPolicyRule[] = [
    { id: 'minLength', label: 'At least 12 characters', pattern: '.{12,}' },
    { id: 'uppercase', label: 'Contains uppercase letter', pattern: '[A-Z]' },
    { id: 'lowercase', label: 'Contains lowercase letter', pattern: '[a-z]' },
    { id: 'digit', label: 'Contains a number', pattern: '[0-9]' },
    { id: 'special', label: 'Contains a special character', pattern: '[!@#$]' },
    { id: 'noConsecutive', label: 'No more than 2 consecutive equal characters', pattern: '^(?!.*(.)\\1{2,}).+$' },
];

describe('passwordPolicyRules', () => {
    it('evaluates rules using patterns returned by the API', () => {
        expect(evaluatePasswordPolicyRule(TEST_RULES[0], 'Short1!a')).toBe(false);
        expect(evaluatePasswordPolicyRule(TEST_RULES[0], 'LongEnough1!a')).toBe(true);
        expect(evaluatePasswordPolicyRule(TEST_RULES[1], 'longenough1!a')).toBe(false);
        expect(evaluatePasswordPolicyRule(TEST_RULES[3], 'LongEnough!abc')).toBe(false);
        expect(evaluatePasswordPolicyRule(TEST_RULES[4], 'LongEnough1abc')).toBe(false);
        expect(evaluatePasswordPolicyRule(TEST_RULES[5], 'LongEnough111!a')).toBe(false);
    });

    it('resolves password strength from satisfied rule count', () => {
        expect(resolvePasswordStrengthLevel('', TEST_RULES)).toBe('weak');
        expect(resolvePasswordStrengthLevel('LongEnough1!a', TEST_RULES)).toBe('strong');
        expect(countSatisfiedPasswordRules('LongEnough1!a', TEST_RULES)).toBe(TEST_RULES.length);
    });

    it('treats empty rules as satisfied only when no rules are configured', () => {
        expect(isPasswordPolicySatisfied('any', [])).toBe(true);
        expect(isPasswordPolicySatisfied('Short1!a', TEST_RULES)).toBe(false);
        expect(isPasswordPolicySatisfied('LongEnough1!a', TEST_RULES)).toBe(true);
    });
});
