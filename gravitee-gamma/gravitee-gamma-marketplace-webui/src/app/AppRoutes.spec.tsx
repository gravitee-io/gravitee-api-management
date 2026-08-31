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
import { MemoryRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { useAuthStore } from './auth/auth.store';
import { buildUser } from '../testing/factories';
import { seedBootstrap } from '../testing/helpers';

function renderApp(path = '/') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
        </MemoryRouter>,
    );
}

describe('AppRoutes', () => {
    it('renders Hello World on the home route without logging in', () => {
        renderApp('/');

        expect(screen.getByRole('heading', { name: 'Hello World' })).toBeTruthy();
        expect(screen.getByRole('link', { name: 'Sign in' })).toBeTruthy();
    });

    it('renders catalog without logging in', () => {
        renderApp('/catalog');

        expect(screen.getByRole('heading', { name: 'Catalog' })).toBeTruthy();
    });

    it('redirects dashboard to login when unauthenticated', () => {
        renderApp('/dashboard');

        expect(screen.getByText('Sign in to continue')).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy();
    });

    it('renders dashboard when authenticated', () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderApp('/dashboard');

        expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeTruthy();
    });

    it('redirects home and catalog to login when forceLogin is enabled', () => {
        seedBootstrap({ forceLoginEnabled: true });
        renderApp('/catalog');

        expect(screen.getByText('Sign in to continue')).toBeTruthy();
    });
});
