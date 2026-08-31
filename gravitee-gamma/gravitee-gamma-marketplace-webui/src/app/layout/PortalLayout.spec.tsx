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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useSearchParams } from 'react-router-dom';

import { PortalLayout } from './PortalLayout';
import { useAuthStore } from '../auth/auth.store';
import { buildCategory, buildUser, TEST_PORTAL_API } from '../../testing/factories';
import { respondWith, respondWithError } from '../../testing/helpers';

function CatalogPage() {
    const [searchParams] = useSearchParams();
    const category = searchParams.get('category');
    const query = searchParams.get('query');
    const suffix = category ?? query;
    return <h1>{suffix ? `Catalog · ${suffix}` : 'Catalog'}</h1>;
}

function renderLayout(path = '/') {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route element={<PortalLayout />}>
                    <Route index element={<h1>Home</h1>} />
                    <Route path="/catalog" element={<CatalogPage />} />
                    <Route path="/dashboard" element={<h1>Dashboard</h1>} />
                    <Route path="/login" element={<h1>Login</h1>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('PortalLayout', () => {
    it('should render browse navigation and global search', async () => {
        renderLayout();

        expect(screen.getByRole('button', { name: 'Home' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Catalog' })).toBeTruthy();
        expect(screen.getByPlaceholderText('Search agents...')).toBeTruthy();
        expect(await screen.findByRole('heading', { name: 'Home' })).toBeTruthy();
    });

    it('should show categories from the portal API in the sidebar', async () => {
        respondWith('get', `${TEST_PORTAL_API}/apis/categories`, {
            data: [buildCategory(), buildCategory({ id: 'hr', name: 'HR', total_apis: 8 })],
        });
        renderLayout();

        expect(await screen.findByRole('button', { name: 'IT' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'HR' })).toBeTruthy();
    });

    it('should navigate to catalog with a category filter when a category is selected', async () => {
        const user = userEvent.setup();
        respondWith('get', `${TEST_PORTAL_API}/apis/categories`, { data: [buildCategory()] });
        renderLayout();

        await user.click(await screen.findByRole('button', { name: 'IT' }));

        expect(await screen.findByRole('heading', { name: 'Catalog · it' })).toBeTruthy();
    });

    it('should navigate to catalog search when the header search is submitted', async () => {
        const user = userEvent.setup();
        renderLayout();

        const search = screen.getByPlaceholderText('Search agents...');
        await user.type(search, 'ticket triage{enter}');

        expect(await screen.findByRole('heading', { name: 'Catalog · ticket triage' })).toBeTruthy();
    });

    it('should show a sign-in action when unauthenticated', () => {
        renderLayout();

        expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'User menu' })).toBeNull();
    });

    it('should show the user menu and sign out when authenticated', async () => {
        const user = userEvent.setup();
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderLayout();

        await user.click(screen.getByRole('button', { name: 'User menu' }));
        await user.click(await screen.findByRole('menuitem', { name: 'Sign out' }));

        await waitFor(() => expect(useAuthStore.getState().user).toBeNull());
        expect(screen.getByRole('button', { name: 'Sign in' })).toBeTruthy();
    });

    it('should keep browse navigation when categories fail to load', async () => {
        respondWithError('get', `${TEST_PORTAL_API}/apis/categories`, 500);
        renderLayout();

        expect(screen.getByRole('button', { name: 'Home' })).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Catalog' })).toBeTruthy();
        await waitFor(() => expect(screen.queryByRole('button', { name: 'IT' })).toBeNull());
    });

    it('should navigate home from catalog via the sidebar', async () => {
        const user = userEvent.setup();
        renderLayout('/catalog');

        expect(screen.getByRole('heading', { name: 'Catalog' })).toBeTruthy();
        const [sidebarHome] = screen.getAllByRole('button', { name: 'Home' });
        await user.click(sidebarHome);
        expect(await screen.findByRole('heading', { name: 'Home' })).toBeTruthy();
    });
});
