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
    assessPassword,
    countSatisfiedPasswordRules,
    describablePasswordRules,
    evaluatePasswordPolicyRule,
    isPasswordPolicySatisfied,
    resolvePasswordStrengthLevel,
    type PasswordPolicyRule,
} from './index';

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

    it('treats empty rules as unsatisfied', () => {
        expect(isPasswordPolicySatisfied('any', [])).toBe(false);
        expect(isPasswordPolicySatisfied('', TEST_RULES)).toBe(false);
        expect(isPasswordPolicySatisfied('Short1!a', TEST_RULES)).toBe(false);
        expect(isPasswordPolicySatisfied('LongEnough1!a', TEST_RULES)).toBe(true);
    });
});

describe('assessPassword', () => {
    const DIGIT_RULE = { id: 'digit', label: 'Contains a number', pattern: '[0-9]' };

    it('refuses a password the derived rules accept but the configured pattern rejects', () => {
        // The parser reads a handful of regex shapes. A policy it read incompletely used to go
        // all-green here and be rejected by the server -- the false green this ticket exists for.
        const policy = { pattern: '^(?=.*[0-9])(?=.*[!@#$]).{8,}$', rules: [DIGIT_RULE] };

        expect(evaluatePasswordPolicyRule(DIGIT_RULE, 'password1')).toBe(true);
        expect(assessPassword('password1', policy)).toBe('unsatisfied');
    });

    it('accepts a password the configured pattern accepts', () => {
        const policy = { pattern: '^(?=.*[0-9])(?=.*[!@#$]).{8,}$', rules: [DIGIT_RULE] };

        expect(assessPassword('password1!', policy)).toBe('satisfied');
    });

    it('matches the configured pattern against the whole password, as the server does', () => {
        // Java's Matcher.matches() must consume the whole password; RegExp.test() settles for any
        // substring. Unanchored, the 20-character limit here would stop applying.
        const policy = { pattern: '(?=.*[0-9])(?=.*[A-Z]).{8,20}', rules: [DIGIT_RULE] };

        expect(assessPassword(`A1${'x'.repeat(28)}`, policy)).toBe('unsatisfied');
        expect(assessPassword('Abcdefg1', policy)).toBe('satisfied');
    });

    it('cannot decide when the browser refuses to compile the pattern', () => {
        // The server compiles with Java's engine and we compile with JavaScript's. A possessive
        // quantifier is valid Java and a syntax error here; reading that as "unsatisfied" would
        // block a password the server would accept, forever.
        const policy = { pattern: '^[a-z]++$', rules: [DIGIT_RULE] };

        expect(assessPassword('abcdef', policy)).toBe('undecidable');
    });

    it.each([
        ['a Unicode property class', '^\\p{Upper}.{7,}$', 'Abcdefgh'],
        ['a horizontal-whitespace class', '^\\h*[A-Z].{7,}$', 'Abcdefgh'],
        ['a quoted literal', '^\\QA.\\E.{6,}$', 'A.bcdefg'],
        ['input-boundary anchors', '\\A[A-Z].{7,}\\z', 'Abcdefgh'],
        ['a nested character-class intersection', '^[a-z&&[^x]]{8,}$', 'abcdefgh'],
        ['a character-class intersection', '^[a-z&&def]{8,}$', 'abcdefgh'],
    ])('cannot decide on %s, which JavaScript compiles to something else', (_, pattern, password) => {
        // Without the u flag an escape JavaScript does not know reads as its letter, and '&&' or a
        // nested '[' in a class read as literals. These compile here and demand something else,
        // which would block passwords the server accepts or accept ones it refuses.
        expect(assessPassword(password, { pattern, rules: [DIGIT_RULE] })).toBe('undecidable');
    });

    it('still decides on escapes both engines read alike', () => {
        const policy = { pattern: '^(?=.*\\d)(?=.*[\\-_])\\w[\\w\\-]{7,}$', rules: [DIGIT_RULE] };

        expect(assessPassword('abc-defg1', policy)).toBe('satisfied');
        expect(assessPassword('abc-defgh', policy)).toBe('unsatisfied');
    });

    it('cannot decide when there is no pattern and no rule to check', () => {
        expect(assessPassword('anything', { rules: [] })).toBe('undecidable');
    });

    it('falls back to the derived rules when no pattern was returned', () => {
        expect(assessPassword('password1', { rules: [DIGIT_RULE] })).toBe('satisfied');
        expect(assessPassword('password', { rules: [DIGIT_RULE] })).toBe('unsatisfied');
    });

    it('treats an empty password as unsatisfied rather than undecidable', () => {
        expect(assessPassword('', { pattern: '^.{8,}$', rules: [] })).toBe('unsatisfied');
    });
});

describe('describablePasswordRules', () => {
    it('drops the opaque whole-pattern fallback the server sends when it derives nothing', () => {
        const rules = [{ id: 'policyPattern', label: 'Matches the configured password policy', pattern: '^\\d{8,}$' }];

        // Nothing to tick off, and its pattern is the raw policy, which must not reach the DOM.
        expect(describablePasswordRules(rules)).toEqual([]);
    });

    it('keeps rules a reader can act on', () => {
        const rules = [{ id: 'minLength', label: 'At least 12 characters', pattern: '^.{12,}$' }];

        expect(describablePasswordRules(rules)).toEqual(rules);
    });
});
