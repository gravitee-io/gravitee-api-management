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
import type { PasswordPolicy, PasswordPolicyRule } from './types';

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

/**
 * The rule the API sends when its parser could read nothing at all. Its label says only that the
 * password must match the policy, and its pattern is the raw policy itself, so it is neither
 * something a reader can act on nor something that may reach the DOM.
 */
const OPAQUE_RULE_ID = 'policyPattern';

/** Rules worth showing someone: the opaque whole-pattern fallback is not one. */
export function describablePasswordRules(rules: PasswordPolicyRule[]): PasswordPolicyRule[] {
    return rules.filter(rule => rule.id !== OPAQUE_RULE_ID);
}

export type PasswordPolicyVerdict = 'satisfied' | 'unsatisfied' | 'undecidable';

/**
 * Whether a password meets the policy, as far as this browser can tell.
 *
 * The configured pattern decides, because it is what the server enforces. The derived rules exist
 * to explain that pattern and the parser recognises only a handful of regex shapes, so a policy it
 * read incompletely would otherwise report every rule green on a password the server rejects.
 *
 * `undecidable` means this browser cannot read the pattern as the server does. The server compiles
 * with Java's engine and this runs on JavaScript's, so a construct valid there can be a syntax error
 * here, or compile to something else. Reading that as unsatisfied would permanently block a password
 * the server would accept, so the caller should let it through and let the server answer. Only a
 * difference visible in the pattern's text is caught; one that leaves no trace there, such as `\s`
 * matching a slightly different set of characters, is not.
 */
export function assessPassword(password: string, policy: PasswordPolicy): PasswordPolicyVerdict {
    if (!password) {
        return 'unsatisfied';
    }

    if (policy.pattern) {
        if (readsDifferentlyInJavaScript(policy.pattern)) {
            return 'undecidable';
        }
        let configured: RegExp;
        try {
            // Java's Matcher.matches() must consume the whole password, where test() settles for any
            // substring: unanchored, a pattern's length limit would stop applying here.
            configured = new RegExp(`^(?:${policy.pattern})$`);
        } catch {
            return 'undecidable';
        }
        return configured.test(password) ? 'satisfied' : 'unsatisfied';
    }

    const rules = describablePasswordRules(policy.rules);
    if (rules.length === 0) {
        return 'undecidable';
    }
    return isPasswordPolicySatisfied(password, rules) ? 'satisfied' : 'unsatisfied';
}

/** Letter escapes JavaScript, without the u flag, reads exactly as Java does. */
const SHARED_LETTER_ESCAPES = new Set(['d', 'D', 'w', 'W', 's', 'S', 'b', 'B', 't', 'n', 'r', 'f']);

function escapeReadAlike(escaped: string, following: string): boolean {
    if (!/[A-Za-z0-9]/.test(escaped)) {
        // An escaped symbol stands for that symbol in both engines.
        return true;
    }
    if (SHARED_LETTER_ESCAPES.has(escaped) || /[1-9]/.test(escaped)) {
        return true;
    }
    if (escaped === 'u') {
        return /^[0-9A-Fa-f]{4}/.test(following);
    }
    if (escaped === 'x') {
        return /^[0-9A-Fa-f]{2}/.test(following);
    }
    return false;
}

/**
 * Whether the pattern holds a construct that compiles here but means something else than on the
 * server. Without the u flag an escape JavaScript does not know (`\p{Upper}`, `\Q`, `\A`, `\z`...)
 * reads as its bare letter, and Java's class intersection (`&&`) or nested class (`[`) reads as
 * literals. An escape not known to read alike counts, so an unfamiliar one leaves the server to
 * decide. The u flag is no way out: it refuses escapes such as `\"` that the shipped default uses.
 */
function readsDifferentlyInJavaScript(pattern: string): boolean {
    let inClass = false;
    for (let i = 0; i < pattern.length; i++) {
        const char = pattern[i];
        if (char === '\\') {
            if (!escapeReadAlike(pattern[i + 1] ?? '', pattern.slice(i + 2))) {
                return true;
            }
            i++;
        } else if (char === '[') {
            if (inClass) {
                return true;
            }
            inClass = true;
        } else if (char === ']') {
            inClass = false;
        } else if (inClass && char === '&' && pattern[i + 1] === '&') {
            return true;
        }
    }
    return false;
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
