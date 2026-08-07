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

import { PasswordRequirements } from '../../../../../gamma-ui-shared/src/passwordPolicy';

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
        renderWithGraphene(<PasswordRequirements rules={TEST_RULES} password="LongEnough1!a" showStrengthMeter />);

        expect(screen.getByText('Requirements')).toBeTruthy();
        expect(screen.getByText('Strong')).toBeTruthy();
        expect(screen.getByText('At least 12 characters')).toBeTruthy();
    });
});
