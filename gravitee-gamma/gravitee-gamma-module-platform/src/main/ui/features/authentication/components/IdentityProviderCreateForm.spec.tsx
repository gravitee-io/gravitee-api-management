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

import { buttonHarness, inputHarness, passwordInputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { IdentityProviderCreateForm } from './IdentityProviderCreateForm';
import { notify } from '../../../shared/notify';
import { useCreateIdentityProvider } from '../hooks/useIdentityProviderMutations';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: () => true,
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../hooks/useIdentityProviderMutations');

const mockUseCreateIdentityProvider = jest.mocked(useCreateIdentityProvider);

function renderForm() {
    return renderWithGraphene(
        <MemoryRouter initialEntries={['/authentication/new']}>
            <IdentityProviderCreateForm />
        </MemoryRouter>,
    );
}

describe('IdentityProviderCreateForm', () => {
    let mutateAsync: jest.Mock;

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
    });

    beforeEach(() => {
        mutateAsync = jest.fn().mockResolvedValue({ id: 'google-sso' });
        mockNavigate.mockClear();
        mockUseCreateIdentityProvider.mockReturnValue({ mutateAsync, isPending: false } as ReturnType<typeof useCreateIdentityProvider>);
        Element.prototype.scrollIntoView = jest.fn();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('defaults to Gravitee.io AM and shows AM-specific fields', () => {
        renderForm();
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
        expect(screen.getByLabelText(/Server URL/).getAttribute('aria-required')).toBe('true');
        expect(screen.getByLabelText(/Security domain/).getAttribute('aria-required')).toBe('true');
        expect(screen.getByLabelText(/^Name/).getAttribute('aria-required')).toBe('true');
        expect(screen.getByLabelText(/^ID/).getAttribute('aria-required')).toBe('true');
    });

    it('shows OIDC endpoints when OpenID Connect is selected', () => {
        renderForm();
        fireEvent.click(screen.getByRole('radio', { name: /OpenID Connect/i }));
        expect(screen.getByLabelText(/^Token Endpoint/)).not.toBeNull();
        expect(screen.getByLabelText(/Authorize Endpoint/)).not.toBeNull();
        expect(screen.getByLabelText(/UserInfo Endpoint/)).not.toBeNull();
        expect(screen.getByLabelText(/^ID/)).not.toBeNull();
    });

    it('hides AM fields and user profile mapping when Google is selected', () => {
        renderForm();
        fireEvent.click(screen.getByRole('radio', { name: /^Google$/i }));
        expect(screen.queryByLabelText(/Server URL/)).toBeNull();
        expect(screen.queryByLabelText(/^ID/)).toBeNull();
        expect(screen.getByLabelText(/Client Id/)).not.toBeNull();
    });

    it('keeps AM configuration after clicking the selected type and arrowing', async () => {
        renderForm();
        await passwordInputHarness({ name: /Client Secret/ }).type('s3cret');
        await inputHarness({ name: /Server URL/ }).type('https://am.example.com');
        fireEvent.click(screen.getByRole('radio', { name: /Gravitee.io AM/i }));
        fireEvent.keyDown(screen.getByRole('radio', { name: /Gravitee.io AM/i }), { key: 'ArrowRight' });
        expect((screen.getByLabelText(/Client Secret/) as HTMLInputElement).value).toBe('s3cret');
        expect((screen.getByLabelText(/Server URL/) as HTMLInputElement).value).toBe('https://am.example.com');
        expect(screen.getByRole('radio', { name: /Gravitee.io AM/i }).getAttribute('aria-checked')).toBe('true');
    });

    it('shows validation errors when required fields are empty', async () => {
        renderForm();
        await buttonHarness({ name: 'Create' }).click();
        expect(await screen.findByText('Identity provider name is required.')).not.toBeNull();
        expect(screen.getByText('Client Id is required.')).not.toBeNull();
        expect(mutateAsync).not.toHaveBeenCalled();
    });

    it('creates a Google identity provider and returns to the list', async () => {
        renderForm();
        fireEvent.click(screen.getByRole('radio', { name: /^Google$/i }));
        await inputHarness({ name: /^Name/ }).type('Google SSO');
        await inputHarness({ name: /Client Id/ }).type('client-id');
        await passwordInputHarness({ name: /Client Secret/ }).type('client-secret');
        await buttonHarness({ name: 'Create' }).click();

        await waitFor(() => {
            expect(mutateAsync).toHaveBeenCalledWith({
                name: 'Google SSO',
                description: '',
                type: 'GOOGLE',
                enabled: true,
                emailRequired: true,
                syncMappings: false,
                configuration: { clientId: 'client-id', clientSecret: 'client-secret' },
            });
            expect(notify.success).toHaveBeenCalledWith('Identity provider successfully saved!');
            expect(mockNavigate).toHaveBeenCalledWith('..');
        });
    });

    it('wires the OpenID Connect scopes error to the scopes field', async () => {
        renderForm();
        fireEvent.click(screen.getByRole('radio', { name: /OpenID Connect/i }));
        await inputHarness({ name: /^Name/ }).type('Okta');
        await inputHarness({ name: /Client Id/ }).type('id');
        await passwordInputHarness({ name: /Client Secret/ }).type('secret');
        await inputHarness({ name: /^Token Endpoint/ }).type('https://example/token');
        await inputHarness({ name: /Authorize Endpoint/ }).type('https://example/authorize');
        await inputHarness({ name: /UserInfo Endpoint/ }).type('https://example/userinfo');
        await buttonHarness({ name: 'Remove openid' }).click();
        await buttonHarness({ name: 'Remove profile' }).click();
        await buttonHarness({ name: 'Remove email' }).click();
        await buttonHarness({ name: 'Create' }).click();

        expect(await screen.findByText('Scopes are required.')).not.toBeNull();
        const scopes = screen.getByLabelText(/Scopes/);
        expect(scopes.getAttribute('aria-invalid')).toBe('true');
        expect(scopes.getAttribute('aria-describedby')).toBe('idp-oidc-scopes-error');
        expect(scopes.getAttribute('aria-required')).toBe('true');
        expect(mutateAsync).not.toHaveBeenCalled();
    });
});
