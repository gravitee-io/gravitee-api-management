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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';

import { PasswordRequirements } from './index';

const UNLISTED_REQUIREMENT = "This password doesn't meet the full policy yet. One of its requirements isn't listed here.";

const TEST_RULES = [
    { id: 'minLength', label: 'At least 12 characters', pattern: '.{12,}' },
    { id: 'uppercase', label: 'Contains uppercase letter', pattern: '[A-Z]' },
    { id: 'lowercase', label: 'Contains lowercase letter', pattern: '[a-z]' },
    { id: 'digit', label: 'Contains a number', pattern: '[0-9]' },
    { id: 'special', label: 'Contains a special character', pattern: '[!@#$]' },
    { id: 'noConsecutive', label: 'No more than 2 consecutive equal characters', pattern: '^(?!.*(.)\\1{2,}).+$' },
];

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
});

describe('PasswordRequirements', () => {
    it('renders policy rules and marks satisfied requirements when a password is provided', () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: TEST_RULES }} password="LongEnough1!a" showStrengthMeter />);

        expect(screen.getByText('Requirements')).toBeTruthy();
        expect(screen.getByText('Strong')).toBeTruthy();
        expect(screen.getByText('At least 12 characters')).toBeTruthy();
    });

    it('renders no heading when the policy could not be loaded', () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: [] }} password="" showStrengthMeter />);

        // A heading over an empty list reads as "no requirements" rather than "unknown".
        expect(screen.queryByText('Requirements')).toBeNull();
    });

    it('scores no strength when the policy could not be loaded', () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: [] }} password="LongEnough1!a" showStrengthMeter />);

        // Every password scores "Weak" against an empty rule set, which reads as a verdict on
        // the password rather than on the missing policy.
        expect(screen.queryByText('Weak')).toBeNull();
    });

    it('shows a weaker strength label when only some rules are satisfied', () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: TEST_RULES }} password="LongEnough1" showStrengthMeter />);

        expect(screen.getByText('Fair')).toBeTruthy();
        expect(screen.queryByText('Strong')).toBeNull();
    });

    it('announces the strength through a live region that is already mounted before the first keystroke', () => {
        // A region inserted together with its first message is usually not announced, so it has to
        // exist, empty, before there is anything to say.
        const { container, rerender } = renderWithGraphene(
            <PasswordRequirements policy={{ rules: TEST_RULES }} password="" showStrengthMeter />,
        );
        const region = container.querySelector('[aria-live="polite"]');
        expect(region?.textContent).toBe('');

        rerender(<PasswordRequirements policy={{ rules: TEST_RULES }} password="LongEnough1" showStrengthMeter />);

        expect(container.querySelector('[aria-live="polite"]')).toBe(region);
        expect(region?.textContent).toBe('Password strength: Fair');
    });

    it('hides the visual strength label from assistive technology, which hears the live region instead', () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: TEST_RULES }} password="LongEnough1" showStrengthMeter />);

        expect(screen.getByText('Fair').closest('[aria-hidden="true"]')).not.toBeNull();
    });

    it('mounts no live region where no meter is asked for', () => {
        const { container } = renderWithGraphene(<PasswordRequirements policy={{ rules: TEST_RULES }} password="LongEnough1" />);

        expect(container.querySelector('[aria-live]')).toBeNull();
    });
});

describe('PasswordRequirements, when the parser derived nothing usable', () => {
    const OPAQUE_RULE = { id: 'policyPattern', label: 'Matches the configured password policy', pattern: '^\\d{8,}$' };

    it("shows the operator's own words instead of the opaque fallback rule", () => {
        renderWithGraphene(<PasswordRequirements policy={{ rules: [OPAQUE_RULE], description: 'Ask IT for the house rules.' }} />);

        expect(screen.getByText('Ask IT for the house rules.')).toBeTruthy();
        expect(screen.queryByText('Matches the configured password policy')).toBeNull();
        expect(screen.queryByText('Requirements')).toBeNull();
    });

    it('never puts the raw policy pattern in the DOM', () => {
        const { container } = renderWithGraphene(
            <PasswordRequirements policy={{ rules: [OPAQUE_RULE], description: 'Ask IT for the house rules.' }} />,
        );

        expect(container.innerHTML).not.toContain('\\d{8,}');
    });

    it('renders nothing at all when there is no description either', () => {
        const { container } = renderWithGraphene(<PasswordRequirements policy={{ rules: [OPAQUE_RULE] }} />);

        expect(container.textContent?.trim()).toBe('');
    });

    it('does not repeat the description when there is a checklist to read', () => {
        const rules = [{ id: 'minLength', label: 'At least 12 characters', pattern: '^.{12,}$' }];

        renderWithGraphene(<PasswordRequirements policy={{ rules, description: 'Ask IT for the house rules.' }} />);

        expect(screen.getByText('At least 12 characters')).toBeTruthy();
        expect(screen.queryByText('Ask IT for the house rules.')).toBeNull();
    });
});

describe('PasswordRequirements, when the pattern holds a requirement no rule lists', () => {
    const DESCRIPTION = 'Use at least 8 characters, including a number.';
    // '\\d' defeats the parser, so only the length rule is derived from this pattern.
    const POLICY = {
        pattern: '^(?=.*\\d).{8,}$',
        rules: [{ id: 'minLength', label: 'At least 8 characters', pattern: '^.{8,}$' }],
    };

    it('says the password still falls short once every listed rule is met', () => {
        renderWithGraphene(<PasswordRequirements policy={POLICY} password="abcdefghij" showStrengthMeter />);

        expect(screen.getByText(UNLISTED_REQUIREMENT)).toBeTruthy();
    });

    it('does not call a password the policy refuses strong', () => {
        renderWithGraphene(<PasswordRequirements policy={POLICY} password="abcdefghij" showStrengthMeter />);

        expect(screen.queryByText('Strong')).toBeNull();
    });

    it('stays quiet while a listed rule is still unmet, since the list already explains it', () => {
        renderWithGraphene(<PasswordRequirements policy={POLICY} password="abc" showStrengthMeter />);

        expect(screen.queryByText(UNLISTED_REQUIREMENT)).toBeNull();
    });

    it("names what is missing in the operator's own words, where they wrote some", () => {
        renderWithGraphene(
            <PasswordRequirements policy={{ ...POLICY, description: DESCRIPTION }} password="abcdefghij" showStrengthMeter />,
        );

        // The one state where the checklist admits a gap is the one where the description is not a repeat.
        expect(screen.getByText(UNLISTED_REQUIREMENT)).toBeTruthy();
        expect(screen.getByText(DESCRIPTION)).toBeTruthy();
    });

    it('keeps the description back while the checklist still explains what is missing', () => {
        renderWithGraphene(<PasswordRequirements policy={{ ...POLICY, description: DESCRIPTION }} password="abc" showStrengthMeter />);

        expect(screen.queryByText(DESCRIPTION)).toBeNull();
    });

    it('stays quiet once the pattern accepts the password', () => {
        renderWithGraphene(<PasswordRequirements policy={POLICY} password="abcdefghi1" showStrengthMeter />);

        expect(screen.queryByText(UNLISTED_REQUIREMENT)).toBeNull();
        expect(screen.getByText('Strong')).toBeTruthy();
    });
});
