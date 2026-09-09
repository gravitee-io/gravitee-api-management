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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildBootstrapConfig, buildUser, TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { respondWith, respondWithError, trackHandler } from '../../../testing/helpers';
import { server } from '../../../testing/server';
import { useAuthStore } from '../auth.store';
import type { SocialIdentityProvider } from '../auth.types';
import { LoginPage } from './LoginPage';

function renderLoginPage(initialPath = '/login') {
    return render(
        <MemoryRouter initialEntries={[initialPath]}>
            <LoginPage />
        </MemoryRouter>,
    );
}

const googleProvider: SocialIdentityProvider = {
    id: 'google-idp',
    name: 'Google',
    clientId: 'google-client-id',
    type: 'GOOGLE',
    authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
    scopes: ['openid', 'profile', 'email'],
    color: '#4285F4',
};

const githubProvider: SocialIdentityProvider = {
    id: 'github-idp',
    name: 'GitHub',
    clientId: 'github-client-id',
    type: 'GITHUB',
    authorizationEndpoint: 'https://github.com/login/oauth/authorize',
    scopes: ['user:email'],
    color: '#444444',
};

const noColorProvider: SocialIdentityProvider = {
    id: 'oidc-idp',
    name: 'Corporate SSO',
    clientId: 'oidc-client-id',
    type: 'OIDC',
    authorizationEndpoint: 'https://sso.example.com/authorize',
};

function seedWithProviders(providers: SocialIdentityProvider[], localLoginEnabled = true) {
    useBootstrapStore.setState({
        config: buildBootstrapConfig({ identityProviders: providers, localLoginEnabled }),
        loading: false,
        error: null,
    });
}

function stubLoginMethods(providers: SocialIdentityProvider[] = [], localLoginEnabled = true) {
    respondWith('get', `${TEST_MANAGEMENT_BASE}/social-identities`, providers);
    respondWith('get', `${TEST_MANAGEMENT_BASE}/console`, {
        authentication: { localLogin: { enabled: localLoginEnabled } },
        reCaptcha: { enabled: false },
    });
}

describe('LoginPage', () => {
    describe('username/password form', () => {
        it('should render the sign-in form', async () => {
            renderLoginPage();

            expect(await screen.findByLabelText('Username')).toBeTruthy();
            expect(screen.getByLabelText('Password')).toBeTruthy();
            expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy();
        });

        it('should disable submit button when fields are empty', async () => {
            renderLoginPage();

            const button = (await screen.findByRole('button', { name: 'Sign in' })) as HTMLButtonElement;
            expect(button.disabled).toBe(true);
        });

        it('should enable submit button when fields are filled', async () => {
            const user = userEvent.setup();
            renderLoginPage();

            await user.type(await screen.findByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');

            const button = screen.getByRole('button', { name: 'Sign in' }) as HTMLButtonElement;
            expect(button.disabled).toBe(false);
        });

        it('should call login and show error on failure', async () => {
            const user = userEvent.setup();
            respondWithError('post', `${TEST_MANAGEMENT_BASE}/user/login`, 401);
            renderLoginPage();

            await user.type(await screen.findByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'wrong');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(await screen.findByRole('alert')).toBeTruthy();
            expect(screen.getByText("That username and password don't match. Try again.")).toBeTruthy();
        });

        it('should call login successfully', async () => {
            const user = userEvent.setup();
            const loginTracker = trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
            trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser());
            renderLoginPage();

            await user.type(await screen.findByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(loginTracker.callCount).toBe(1);
            expect(useAuthStore.getState().user?.displayName).toBe('Test User');
        });

        it('should hide username and password when local login is disabled', async () => {
            stubLoginMethods([], false);
            renderLoginPage();

            expect(await screen.findByText('No sign-in method is configured. Contact your administrator.')).toBeTruthy();
            expect(screen.queryByLabelText('Username')).toBeNull();
            expect(screen.queryByLabelText('Password')).toBeNull();
            expect(screen.queryByRole('button', { name: 'Sign in' })).toBeNull();
        });

        it('should show cached login methods without waiting for a refresh', async () => {
            seedWithProviders([], true);
            let release: () => void = () => {};
            const gate = new Promise<void>(resolve => {
                release = resolve;
            });
            server.use(
                http.get(`${TEST_MANAGEMENT_BASE}/social-identities`, async () => {
                    await gate;
                    return HttpResponse.json([]);
                }),
                http.get(`${TEST_MANAGEMENT_BASE}/console`, async () => {
                    await gate;
                    return HttpResponse.json({ authentication: { localLogin: { enabled: false } } });
                }),
            );

            renderLoginPage();

            expect(screen.getByLabelText('Username')).toBeTruthy();
            expect(screen.queryByLabelText('Loading sign-in options')).toBeNull();

            release();
            expect(await screen.findByText('No sign-in method is configured. Contact your administrator.')).toBeTruthy();
            expect(screen.queryByLabelText('Username')).toBeNull();
        });

        it('should skip a duplicate login-methods fetch when bootstrap data is still fresh', () => {
            useBootstrapStore.setState({
                config: buildBootstrapConfig({ localLoginEnabled: true }),
                loading: false,
                error: null,
                loginMethodsFetchedAt: Date.now(),
            });
            const tracker = trackHandler('get', `${TEST_MANAGEMENT_BASE}/console`, {
                authentication: { localLogin: { enabled: false } },
            });

            renderLoginPage();

            expect(screen.getByLabelText('Username')).toBeTruthy();
            expect(tracker.callCount).toBe(0);
        });
    });

    describe('identity provider buttons', () => {
        it('should not render IdP section when no providers configured', async () => {
            stubLoginMethods([]);
            renderLoginPage();

            await screen.findByLabelText('Username');
            expect(screen.queryByText('or')).toBeNull();
        });

        it('should render IdP buttons from the current social-identities response', async () => {
            seedWithProviders([githubProvider]);
            stubLoginMethods([googleProvider, githubProvider]);
            renderLoginPage();

            expect(await screen.findByText('Google')).toBeTruthy();
            expect(screen.getByText('or')).toBeTruthy();
            expect(screen.getByText('GitHub')).toBeTruthy();
        });

        it('should drop identity providers that are no longer returned for login', async () => {
            seedWithProviders([googleProvider]);
            stubLoginMethods([]);
            renderLoginPage();

            await screen.findByLabelText('Username');
            expect(screen.queryByText('Google')).toBeNull();
        });

        it('should keep previous identity providers when social-identities cannot be loaded', async () => {
            seedWithProviders([googleProvider]);
            respondWithError('get', `${TEST_MANAGEMENT_BASE}/social-identities`, 500);
            renderLoginPage();

            expect(await screen.findByText('Google')).toBeTruthy();
        });

        it('should explain the identity provider route when it is the only one', async () => {
            stubLoginMethods([googleProvider], false);
            renderLoginPage();

            expect(await screen.findByText('Continue with your identity provider.')).toBeTruthy();
        });

        it('should not describe the card when the labelled fields already do', async () => {
            stubLoginMethods([googleProvider]);
            const { container } = renderLoginPage();

            await screen.findByLabelText('Username');
            expect(container.querySelector('[data-slot="card-description"]')).toBeNull();
        });

        it('should not describe the card when no sign-in method is configured', async () => {
            stubLoginMethods([], false);
            const { container } = renderLoginPage();

            await screen.findByText('No sign-in method is configured. Contact your administrator.');
            expect(container.querySelector('[data-slot="card-description"]')).toBeNull();
        });

        it('should show identity providers without the password form when local login is disabled', async () => {
            stubLoginMethods([googleProvider], false);
            renderLoginPage();

            expect(await screen.findByText('Google')).toBeTruthy();
            expect(screen.queryByLabelText('Username')).toBeNull();
            expect(screen.queryByText('or')).toBeNull();
        });

        it('should edge the chip with the provider color rather than filling it', async () => {
            stubLoginMethods([googleProvider]);
            renderLoginPage();

            const button = (await screen.findByText('Google')).closest('button')!;
            const chip = button.querySelector('[aria-hidden]') as HTMLElement;

            expect(chip.style.borderColor.toLowerCase()).toBe('#4285f4');
            // A fill would put a multi-colour glyph on an administrator-chosen colour it cannot
            // adapt to, so the chip surface stays neutral and the glyph stays legible.
            expect(chip.style.backgroundColor).toBe('');
            expect(chip.className).toContain('bg-muted');
            expect(button.style.backgroundColor).toBe('');
        });

        it('should leave the chip on its neutral border when provider has no color', async () => {
            stubLoginMethods([noColorProvider]);
            renderLoginPage();

            const button = (await screen.findByText('Corporate SSO')).closest('button')!;
            const chip = button.querySelector('[aria-hidden]') as HTMLElement;

            // The provider is still named and still has its icon.
            expect(chip.style.borderColor).toBe('');
            expect(chip.className).toContain('bg-muted');
            // `Button`'s outline variant hovers to bg-muted; the border is what keeps the chip
            // visible under the pointer whether or not a colour was configured.
            expect(chip.className).toContain('border');
        });

        it('should keep the neutral border when the provider color is not a color', async () => {
            // `border-color` takes per-side lists and var() references; an administrator's value
            // is a colour or it is nothing.
            stubLoginMethods([{ ...googleProvider, color: 'red blue green yellow' }]);
            renderLoginPage();

            const button = (await screen.findByText('Google')).closest('button')!;
            const chip = button.querySelector('[aria-hidden]') as HTMLElement;

            expect(chip.style.borderColor).toBe('');
        });

        it('should name the provider for assistive technology', async () => {
            stubLoginMethods([googleProvider]);
            renderLoginPage();

            // The visible label is just the name so a list of providers stays scannable; the
            // accessible name still carries the action.
            expect(await screen.findByRole('button', { name: 'Continue with Google' })).toBeTruthy();
        });

        it('should call loginWithProvider on IdP button click', async () => {
            const user = userEvent.setup();
            stubLoginMethods([googleProvider]);
            const loginWithProviderSpy = jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockResolvedValue();
            renderLoginPage();

            await user.click(await screen.findByText('Google'));

            expect(loginWithProviderSpy).toHaveBeenCalledWith('google-idp', '/');
            loginWithProviderSpy.mockRestore();
        });

        it('should show error when IdP login fails', async () => {
            const user = userEvent.setup();
            stubLoginMethods([googleProvider]);
            jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockRejectedValue(new Error('IdP error'));
            renderLoginPage();

            await user.click(await screen.findByText('Google'));

            expect(await screen.findByRole('alert')).toBeTruthy();
            expect(screen.getByText("Couldn't reach that identity provider. Try again.")).toBeTruthy();
        });

        it('should pass redirect param to loginWithProvider', async () => {
            const user = userEvent.setup();
            stubLoginMethods([googleProvider]);
            const loginWithProviderSpy = jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockResolvedValue();

            render(
                <MemoryRouter initialEntries={['/login?redirect=/dashboard']}>
                    <LoginPage />
                </MemoryRouter>,
            );

            await user.click(await screen.findByText('Google'));

            expect(loginWithProviderSpy).toHaveBeenCalledWith('google-idp', '/dashboard');
            loginWithProviderSpy.mockRestore();
        });
    });
});
