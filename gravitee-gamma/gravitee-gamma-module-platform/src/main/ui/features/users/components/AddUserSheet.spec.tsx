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
import { buttonHarness, inputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AddUserSheet } from './AddUserSheet';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { useIdentityProviders } from '../hooks/useOrganizationUsers';
import { GRAVITEE_IDP } from '../types/user';

jest.mock('../hooks/useOrganizationUsers', () => ({
    useIdentityProviders: jest.fn(),
}));

const mockUseIdentityProviders = jest.mocked(useIdentityProviders);

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
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.scrollIntoView = jest.fn();
});

describe('AddUserSheet', () => {
    beforeEach(() => {
        mockUseIdentityProviders.mockReturnValue({
            data: [GRAVITEE_IDP],
            isLoading: false,
        } as ReturnType<typeof useIdentityProviders>);
    });

    it('renders at the standard sheet width instead of the narrower graphene default', () => {
        renderWithGraphene(<AddUserSheet open onClose={jest.fn()} onSubmit={jest.fn()} isPending={false} />);

        const content = document.querySelector('[data-slot="sheet-content"]') as HTMLElement | null;
        expect(content).not.toBeNull();
        expect(content?.style.maxWidth).toBe(STANDARD_SHEET_WIDTH);
    });

    it('submits a service account payload with firstname null', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        renderWithGraphene(<AddUserSheet open onClose={jest.fn()} onSubmit={onSubmit} isPending={false} />);

        await user.click(screen.getByRole('radio', { name: 'Service Account' }));
        await inputHarness({ name: /Service Name/i }).type('DevOps Bot');
        await inputHarness({ name: /^Email/i }).type('bot@company.com');
        await buttonHarness({ name: /^Add User$/ }).click();

        await waitFor(() => expect(onSubmit).toHaveBeenCalled());
        expect(onSubmit.mock.calls[0]?.[0]).toEqual({
            firstname: null,
            lastname: 'DevOps Bot',
            email: 'bot@company.com',
            source: 'gravitee',
            sourceId: '',
            service: true,
        });
    });

    it('shows identity provider fields when multiple providers are configured', async () => {
        mockUseIdentityProviders.mockReturnValue({
            data: [GRAVITEE_IDP, { id: 'ldap', name: 'LDAP' }],
            isLoading: false,
        } as ReturnType<typeof useIdentityProviders>);

        renderWithGraphene(<AddUserSheet open onClose={jest.fn()} onSubmit={jest.fn()} isPending={false} />);

        expect(await screen.findByLabelText('Identity Provider')).toBeTruthy();
    });

    it('requires an identifier for non-gravitee identity providers', async () => {
        mockUseIdentityProviders.mockReturnValue({
            data: [GRAVITEE_IDP, { id: 'ldap', name: 'LDAP' }],
            isLoading: false,
        } as ReturnType<typeof useIdentityProviders>);

        renderWithGraphene(<AddUserSheet open onClose={jest.fn()} onSubmit={jest.fn()} isPending={false} />);

        fireEvent.click(screen.getByLabelText('Identity Provider'));
        fireEvent.click(screen.getByRole('option', { name: 'LDAP' }));

        expect(await screen.findByLabelText(/Identifier/i)).toBeTruthy();

        await inputHarness({ name: /First Name/i }).type('Jane');
        await inputHarness({ name: /Last Name/i }).type('Doe');
        await inputHarness({ name: /^Email/i }).type('jane@company.com');

        const addUserButton = screen.getByRole('button', { name: /^Add User$/ }) as HTMLButtonElement;
        expect(addUserButton.disabled).toBe(true);
    });
});
