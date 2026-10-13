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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { buildUser } from '../../../testing/factories';
import { useAuthStore } from '../auth.store';
import { ProtectedRoute, PublicOnlyRoute } from './ProtectedRoute';

function renderProtectedRoute(initialPath = '/') {
    return render(
        <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
                <Route element={<ProtectedRoute />}>
                    <Route path="/" element={<div>Home</div>} />
                    <Route path="/dashboard" element={<div>Dashboard</div>} />
                </Route>
                <Route path="/login" element={<div>Login Page</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

function renderPublicOnlyRoute(initialPath = '/login') {
    return render(
        <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
                <Route element={<PublicOnlyRoute />}>
                    <Route path="/login" element={<div>Login Page</div>} />
                </Route>
                <Route path="/" element={<div>Home</div>} />
                <Route path="/dashboard" element={<div>Dashboard</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ProtectedRoute', () => {
    it('should redirect to login when unauthenticated', () => {
        renderProtectedRoute('/');
        expect(screen.getByText('Login Page')).toBeTruthy();
    });

    it('should render children when authenticated', () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderProtectedRoute('/');
        expect(screen.getByText('Home')).toBeTruthy();
    });

    it('should include redirect param for non root paths', () => {
        renderProtectedRoute('/dashboard');
        expect(screen.getByText('Login Page')).toBeTruthy();
    });
});

describe('PublicOnlyRoute', () => {
    it('should render login when unauthenticated', () => {
        renderPublicOnlyRoute();
        expect(screen.getByText('Login Page')).toBeTruthy();
    });

    it('should redirect to home when authenticated', () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderPublicOnlyRoute();
        expect(screen.getByText('Home')).toBeTruthy();
    });

    it('should redirect to the page requested before the login when authenticated', () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderPublicOnlyRoute('/login?redirect=%2Fdashboard');
        expect(screen.getByText('Dashboard')).toBeTruthy();
    });

    it('should keep the query and the hash of the page requested', () => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderPublicOnlyRoute(`/login?redirect=${encodeURIComponent('/dashboard?tab=1#top')}`);
        expect(screen.getByText('Dashboard')).toBeTruthy();
    });

    it.each([
        ['a backslash after the slash', '/\\example.org'],
        ['a tab between two slashes', '/\t/example.org'],
        ['two slashes', '//example.org'],
        ['a full URL', 'https://example.org/dashboard'],
        ['a script', 'javascript:alert(1)'],
    ])('should redirect to home when the page requested is not one of the console: %s', (_case, redirect) => {
        useAuthStore.setState({ user: buildUser(), initialized: true });
        renderPublicOnlyRoute(`/login?redirect=${encodeURIComponent(redirect)}`);
        expect(screen.getByText('Home')).toBeTruthy();
    });
});
