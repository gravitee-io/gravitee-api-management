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
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { useAuthStore } from './auth.store';
import { LoginPage } from './LoginPage';
import { buildIdentityProvider, buildUser, TEST_PORTAL_API } from '../../testing/factories';
import { respondWithError, seedBootstrap, trackHandler } from '../../testing/helpers';

function renderLoginPage(initialPath = '/login') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[initialPath]}>
            <LoginPage />
        </MemoryRouter>,
    );
}

describe('LoginPage', () => {
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

    it('should show an error when login fails', async () => {
        const user = userEvent.setup();
        respondWithError('post', `${TEST_PORTAL_API}/auth/login`, 401);
        renderLoginPage();

        await user.type(screen.getByLabelText('Username'), 'admin');
        await user.type(screen.getByLabelText('Password'), 'wrong');
        await user.click(screen.getByRole('button', { name: 'Sign in' }));

        expect(await screen.findByRole('alert')).toBeTruthy();
        expect(screen.getByText('The username or password you entered is incorrect, please try again.')).toBeTruthy();
    });

    it('should call login successfully', async () => {
        const user = userEvent.setup();
        const loginTracker = trackHandler('post', `${TEST_PORTAL_API}/auth/login`, null, 200);
        trackHandler('get', `${TEST_PORTAL_API}/user`, buildUser());
        renderLoginPage();

        await user.type(screen.getByLabelText('Username'), 'admin');
        await user.type(screen.getByLabelText('Password'), 'password');
        await user.click(screen.getByRole('button', { name: 'Sign in' }));

        expect(loginTracker.callCount).toBe(1);
        expect(useAuthStore.getState().user?.display_name).toBe('Jane Doe');
    });

    it('should hide username and password when local login is disabled', () => {
        seedBootstrap({ localLoginEnabled: false });
        renderLoginPage();

        expect(screen.getByText('No login method available. Please contact your administrator.')).toBeTruthy();
        expect(screen.queryByLabelText('Username')).toBeNull();
        expect(screen.queryByRole('button', { name: 'Sign in' })).toBeNull();
    });

    it('should render SSO buttons when identity providers are configured', () => {
        seedBootstrap({
            identityProviders: [buildIdentityProvider(), buildIdentityProvider({ id: 'github-idp', name: 'GitHub', type: 'GITHUB' })],
        });
        renderLoginPage();

        expect(screen.getByText('Continue with Google')).toBeTruthy();
        expect(screen.getByText('or continue with')).toBeTruthy();
        expect(screen.getByText('Continue with GitHub')).toBeTruthy();
    });

    it('should call loginWithProvider on SSO button click', async () => {
        const user = userEvent.setup();
        seedBootstrap({ identityProviders: [buildIdentityProvider()] });
        const loginWithProviderSpy = jest.spyOn(useAuthStore.getState(), 'loginWithProvider').mockResolvedValue();
        renderLoginPage();

        await user.click(screen.getByText('Continue with Google'));

        expect(loginWithProviderSpy).toHaveBeenCalledWith('google-idp', '/');
        loginWithProviderSpy.mockRestore();
    });
});
