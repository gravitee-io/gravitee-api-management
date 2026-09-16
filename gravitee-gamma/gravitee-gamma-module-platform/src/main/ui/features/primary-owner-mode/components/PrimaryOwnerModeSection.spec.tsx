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

import { TooltipProvider } from '@gravitee/graphene-core';
import { radioGroupHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import type { ReactNode } from 'react';

import { PrimaryOwnerModeSection } from './PrimaryOwnerModeSection';
import type { PrimaryOwnerModeFormState } from '../utils/primaryOwnerMode';

function renderSection({
    value = { api: 'HYBRID', apiProduct: 'HYBRID' },
    disabled = false,
    readonly = { api: false, apiProduct: false },
    onChange = jest.fn(),
}: {
    value?: PrimaryOwnerModeFormState;
    disabled?: boolean;
    readonly?: { api: boolean; apiProduct: boolean };
    onChange?: (next: PrimaryOwnerModeFormState) => void;
} = {}) {
    function Wrapper({ children }: { children: ReactNode }) {
        return <TooltipProvider>{children}</TooltipProvider>;
    }
    renderWithGraphene(<PrimaryOwnerModeSection value={value} disabled={disabled} readonly={readonly} onChange={onChange} />, {
        wrapper: Wrapper,
    });
    return { onChange };
}

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

describe('PrimaryOwnerModeSection', () => {
    beforeEach(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
    });

    it('renders both resource cards with Hybrid, User, and Group', () => {
        renderSection();

        expect(screen.getByText('API Primary Owner mode')).not.toBeNull();
        expect(screen.getByText('API Product Primary Owner mode')).not.toBeNull();
        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getOptions()).toEqual(['HYBRID', 'USER', 'GROUP']);
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).getOptions()).toEqual(['HYBRID', 'USER', 'GROUP']);
    });

    it('shows the current mode and its implications', () => {
        renderSection({ value: { api: 'USER', apiProduct: 'GROUP' } });

        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getValue()).toBe('USER');
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).getValue()).toBe('GROUP');
        expect(screen.getAllByText('Only a person can be the primary owner. Groups cannot take that role.').length).toBeGreaterThan(0);
        expect(
            screen.getAllByText('Only a group can be the primary owner. Ownership stays with the team, not one person.').length,
        ).toBeGreaterThan(0);
    });

    it('renders an unselected radio group when the stored mode is unrecognized', () => {
        renderSection({ value: { api: null, apiProduct: 'HYBRID' } });

        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getValue() || '').toBe('');
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).getValue()).toBe('HYBRID');
    });

    it('emits the selected API mode', async () => {
        const { onChange } = renderSection();

        await radioGroupHarness({ name: 'API Primary Owner mode' }).select('USER');

        expect(onChange).toHaveBeenCalledWith({ api: 'USER', apiProduct: 'HYBRID' });
    });

    it('disables both groups when the viewer cannot edit', () => {
        renderSection({ disabled: true });

        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).isDisabled()).toBe(true);
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).isDisabled()).toBe(true);
    });

    it('marks a system-provided group as readonly', () => {
        renderSection({ readonly: { api: true, apiProduct: false } });

        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).getElement().closest('[data-system-readonly="true"]')).not.toBeNull();
        expect(radioGroupHarness({ name: 'API Primary Owner mode' }).isDisabled()).toBe(true);
        expect(radioGroupHarness({ name: 'API Product Primary Owner mode' }).isDisabled()).toBe(false);
    });
});
