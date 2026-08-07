/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { PasswordPolicyRule } from './types';

export function evaluatePasswordPolicyRule(rule: PasswordPolicyRule, password: string): boolean {
    if (!password || !rule.pattern) {
        return false;
    }

    try {
        return new RegExp(rule.pattern).test(password);
    } catch {
        return false;
    }
}

export function countSatisfiedPasswordRules(password: string, rules: PasswordPolicyRule[]): number {
    return rules.filter(rule => evaluatePasswordPolicyRule(rule, password)).length;
}

export function isPasswordPolicySatisfied(password: string, rules: PasswordPolicyRule[]): boolean {
    if (!password || rules.length === 0) {
        return false;
    }

    return countSatisfiedPasswordRules(password, rules) === rules.length;
}

const PASSWORD_STRENGTH_FAIR_THRESHOLD = 0.5;
const PASSWORD_STRENGTH_GOOD_THRESHOLD = 0.83;

export type PasswordStrengthLevel = 'weak' | 'fair' | 'good' | 'strong';

export function resolvePasswordStrengthLevel(password: string, rules: PasswordPolicyRule[]): PasswordStrengthLevel {
    if (!password || rules.length === 0) {
        return 'weak';
    }

    const satisfiedCount = countSatisfiedPasswordRules(password, rules);
    const ratio = satisfiedCount / rules.length;

    if (ratio >= 1) {
        return 'strong';
    }
    if (ratio >= PASSWORD_STRENGTH_GOOD_THRESHOLD) {
        return 'good';
    }
    if (ratio >= PASSWORD_STRENGTH_FAIR_THRESHOLD) {
        return 'fair';
    }
    return 'weak';
}

export function resolvePasswordStrengthLabel(level: PasswordStrengthLevel): string {
    switch (level) {
        case 'fair':
            return 'Fair';
        case 'good':
            return 'Good';
        case 'strong':
            return 'Strong';
        default:
            return 'Weak';
    }
}
