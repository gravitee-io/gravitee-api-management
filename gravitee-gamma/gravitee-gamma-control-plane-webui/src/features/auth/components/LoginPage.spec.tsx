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
import { delay, http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';

import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildBootstrapConfig, buildUser, TEST_MANAGEMENT_BASE } from '../../../testing/factories';
import { respondWithError, trackHandler } from '../../../testing/helpers';
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

function seedWithProviders(providers: SocialIdentityProvider[]) {
    useBootstrapStore.setState({
        config: buildBootstrapConfig({ identityProviders: providers }),
        loading: false,
        error: null,
    });
}

describe('LoginPage', () => {
    describe('username/password form', () => {
        it('should render the sign-in form', () => {
            renderLoginPage();

            expect(screen.getByLabelText('Username')).toBeTruthy();
            expect(screen.getByLabelText('Password')).toBeTruthy();
            expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy();
        });

        it('should disable submit button when fields are empty', () => {
            renderLoginPage();

            const button = screen.getByRole('button', { name: 'Sign in' }) as HTMLButtonElement;
            expect(button.disabled).toBe(true);
        });

        it('should enable submit button when fields are filled', async () => {
            const user = userEvent.setup();
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');

            const button = screen.getByRole('button', { name: 'Sign in' }) as HTMLButtonElement;
            expect(button.disabled).toBe(false);
        });

        it('should call login and show error on failure', async () => {
            const user = userEvent.setup();
            respondWithError('post', `${TEST_MANAGEMENT_BASE}/user/login`, 401);
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'wrong');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(await screen.findByRole('alert')).toBeTruthy();
            expect(screen.getByText('Login failed! Check username and password.')).toBeTruthy();
        });

        it('should keep the usual message when the platform refuses the sign-in, as waiting would not change it', async () => {
            const user = userEvent.setup();
            respondWithError('post', `${TEST_MANAGEMENT_BASE}/user/login`, 400);
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            const alert = await screen.findByRole('alert');
            expect(alert.textContent).toContain('Login failed! Check username and password.');
            expect(alert.textContent).not.toContain('The platform may be updating');
        });

        it('should call login successfully', async () => {
            const user = userEvent.setup();
            const loginTracker = trackHandler('post', `${TEST_MANAGEMENT_BASE}/user/login`, null, 200);
            trackHandler('get', `${TEST_MANAGEMENT_BASE}/user`, buildUser());
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(loginTracker.callCount).toBe(1);
            expect(useAuthStore.getState().user?.displayName).toBe('Test User');
        });
    });

    describe('session that could not be checked', () => {
        it('should show no alert to a user who is simply signed out', () => {
            renderLoginPage();

            expect(screen.queryByRole('alert')).toBeNull();
        });

        it('should say that the session could not be checked and what to do', () => {
            useAuthStore.setState({ sessionCheckFailed: true });
            renderLoginPage();

            const alert = screen.getByRole('alert');
            expect(alert.textContent).toContain('Could not check your session');
            expect(alert.textContent).toContain('Reload the page in a minute, or sign in again.');
        });

        it('should be read with the field that takes the focus, as the alert is there when the page opens', () => {
            useAuthStore.setState({ sessionCheckFailed: true });
            renderLoginPage();

            const username = screen.getByLabelText('Username');
            expect(document.activeElement).toBe(username);
            expect(username.getAttribute('aria-describedby')).toBe(screen.getByRole('alert').id);
        });

        it('should leave the field alone when there is no session alert', () => {
            renderLoginPage();

            expect(screen.getByLabelText('Username').hasAttribute('aria-describedby')).toBe(false);
        });

        it('should not look like a failed sign-in: a warning, with an icon', () => {
            useAuthStore.setState({ sessionCheckFailed: true });
            renderLoginPage();

            const alert = screen.getByRole('alert');
            expect(alert.getAttribute('data-variant')).toBe('warning');
            expect(alert.querySelector('svg')).toBeTruthy();
        });

        it('should show the sign-in error in its place once a sign-in fails', async () => {
            const user = userEvent.setup();
            useAuthStore.setState({ sessionCheckFailed: true });
            respondWithError('post', `${TEST_MANAGEMENT_BASE}/user/login`, 401);
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'wrong');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(await screen.findByText('Login failed! Check username and password.')).toBeTruthy();
            expect(screen.getAllByRole('alert')).toHaveLength(1);
        });
    });

    describe('sign-in while the platform cannot answer', () => {
        it.each([
            ['answers with an error', () => HttpResponse.json({ message: 'Error 503' }, { status: 503 })],
            ['does not answer', () => HttpResponse.error()],
        ])('should say that the platform may be updating, not blame the credentials, when it %s', async (_case, respond) => {
            const user = userEvent.setup();
            server.use(http.post(`${TEST_MANAGEMENT_BASE}/user/login`, respond));
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            const alert = await screen.findByRole('alert');
            expect(alert.textContent).toContain('The platform may be updating. Reload the page in a minute, or sign in again.');
            expect(alert.textContent).not.toContain('Check username and password');
        });

        it('should not bring the session alert back while a new sign-in runs', async () => {
            const user = userEvent.setup();
            useAuthStore.setState({ sessionCheckFailed: true });
            server.use(
                http.post(`${TEST_MANAGEMENT_BASE}/user/login`, async () => {
                    await delay(50);
                    return HttpResponse.json({ message: 'Error 503' }, { status: 503 });
                }),
            );
            renderLoginPage();

            await user.type(screen.getByLabelText('Username'), 'admin');
            await user.type(screen.getByLabelText('Password'), 'password');
            await user.click(screen.getByRole('button', { name: 'Sign in' }));

            expect(screen.queryByText('Could not check your session')).toBeNull();
            expect(await screen.findByText('The platform may be updating. Reload the page in a minute, or sign in again.')).toBeTruthy();
            expect(screen.queryByText('Could not check your session')).toBeNull();
        });
    });

    describe('identity provider buttons', () => {
        it('should not render IdP section when no providers configured', () => {
            seedWithProviders([]);
            renderLoginPage();

            expect(screen.queryByText('or')).toBeNull();
        });

        it('should render IdP buttons when providers are configured', () => {
            seedWithProviders([googleProvider, githubProvider]);
            renderLoginPage();

            expect(screen.getByText('or')).toBeTruthy();
            expect(screen.getByText('Sign in with Google')).toBeTruthy();
            expect(screen.getByText('Sign in with GitHub')).toBeTruthy();
        });

        it('should apply provider color as background', () => {
            seedWithProviders([googleProvider]);
            renderLoginPage();

            const button = screen.getByText('Sign in with Google').closest('button')!;
            expect(button.style.backgroundColor).toBe('rgb(66, 133, 244)');
            // #4285F4 has luminance ~0.24 (above 0.179 threshold), so text is black
            expect(button.style.color).toBe('black');
        });

        it('should not apply inline color when provider has no color', () => {
            seedWithProviders([noColorProvider]);
            renderLoginPage();

            const button = screen.getByText('Sign in with Corporate SSO').closest('button')!;
            // No inline styles — uses Button variant="outline" default styling
            expect(button.style.backgroundColor).toBe('');
            expect(button.style.color).toBe('');
        });

        it('should use white text on dark provider color', () => {
            const darkProvider: SocialIdentityProvider = { ...githubProvider, color: '#111111' };
            seedWithProviders([darkProvider]);
            renderLoginPage();

            const button = screen.getByText('Sign in with GitHub').closest('button')!;
            expect(button.style.color).toBe('white');
        });

        it('should call loginWithProvider on IdP button click', async () => {
            const user = userEvent.setup();
            seedWithProviders([googleProvider]);
            const loginWithProviderSpy = jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockResolvedValue();
            renderLoginPage();

            await user.click(screen.getByText('Sign in with Google'));

            expect(loginWithProviderSpy).toHaveBeenCalledWith('google-idp', '/');
            loginWithProviderSpy.mockRestore();
        });

        it('should show error when IdP login fails', async () => {
            const user = userEvent.setup();
            seedWithProviders([googleProvider]);
            jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockRejectedValue(new Error('IdP error'));
            renderLoginPage();

            await user.click(screen.getByText('Sign in with Google'));

            expect(await screen.findByRole('alert')).toBeTruthy();
            expect(screen.getByText('Failed to start identity provider authentication.')).toBeTruthy();
        });

        it('should pass redirect param to loginWithProvider', async () => {
            const user = userEvent.setup();
            seedWithProviders([googleProvider]);
            const loginWithProviderSpy = jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockResolvedValue();

            render(
                <MemoryRouter initialEntries={['/login?redirect=/dashboard']}>
                    <LoginPage />
                </MemoryRouter>,
            );

            await user.click(screen.getByText('Sign in with Google'));

            expect(loginWithProviderSpy).toHaveBeenCalledWith('google-idp', '/dashboard');
            loginWithProviderSpy.mockRestore();
        });
    });
});
