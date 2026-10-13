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
import { MemoryRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { useAuthStore } from '../features/auth/auth.store';
import { buildUser, TEST_GAMMA_BASE } from '../testing/factories';
import { respondWith, respondWithError, seedEnvironments } from '../testing/helpers';
import { PathnameProbe } from '../testing/PathnameProbe';

function MockRemoteModule() {
    return <div>Remote module</div>;
}

const mockLoadRemote = jest.fn();

jest.mock('@module-federation/runtime', () => ({
    loadRemote: (...args: unknown[]) => mockLoadRemote(...args),
    registerRemotes: jest.fn(),
}));

const MODULE_PAGE = '/environments/env-1/aim/agents';
const PAGE_ERROR_TITLE = "Couldn't load your applications";

describe('AppRoutes, when the list of modules cannot be loaded', () => {
    let lastPath = '';

    beforeEach(() => {
        mockLoadRemote.mockResolvedValue({ default: MockRemoteModule });
        useAuthStore.setState({ user: buildUser(), initialized: true });
        seedEnvironments();
        respondWithError('get', `${TEST_GAMMA_BASE}/modules`, 503);
        lastPath = '';
    });

    function renderApp(initialPath: string) {
        return render(
            <MemoryRouter initialEntries={[initialPath]}>
                <PathnameProbe onPath={path => (lastPath = path)} />
                <AppRoutes />
            </MemoryRouter>,
        );
    }

    it('should keep the address of a direct link and say so, instead of sending the user to Home', async () => {
        renderApp(MODULE_PAGE);

        expect(await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeTruthy();
        expect(lastPath).toBe(MODULE_PAGE);
    });

    it('should open the page asked once a retry loads the list', async () => {
        const user = userEvent.setup();
        renderApp(MODULE_PAGE);
        await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE });
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [
            { id: 'aim', name: 'AIM', version: '1.0.0', mfManifest: { name: 'aim', exposes: [{ name: './App' }] } },
        ]);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(lastPath).toBe(MODULE_PAGE);
        expect(screen.queryByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeNull();
    });

    it('should show the same message on Home, as Home has nothing left to show without the list', async () => {
        renderApp('/environments/env-1/home');

        expect(await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeTruthy();
        expect(screen.queryByRole('heading', { level: 1, name: /Welcome back/ })).toBeNull();
        expect(lastPath).toBe('/environments/env-1/home');
    });

    it('should open Home once a retry loads the list', async () => {
        const user = userEvent.setup();
        renderApp('/environments/env-1/home');
        await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE });
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [
            { id: 'aim', name: 'AIM', version: '1.0.0', mfManifest: { name: 'aim', exposes: [{ name: './App' }] } },
        ]);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(await screen.findByRole('heading', { level: 1, name: /Welcome back/ })).toBeTruthy();
        expect(lastPath).toBe('/environments/env-1/home');
        expect(screen.queryByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeNull();
    });

    it('should keep the message and the address while a retry runs and once it fails again', async () => {
        const user = userEvent.setup();
        renderApp(MODULE_PAGE);
        await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE });

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(screen.getByRole('button', { name: 'Retrying…' })).toBeTruthy();
        expect(screen.getByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeTruthy();
        expect(await screen.findByRole('button', { name: 'Retry' }, { timeout: 2000 })).toBeTruthy();
        expect(screen.getByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeTruthy();
        expect(lastPath).toBe(MODULE_PAGE);
    });

    it('should still open Tasks, as it is not a page of a module', async () => {
        renderApp('/environments/env-1/tasks');

        expect(await screen.findByRole('heading', { level: 1, name: 'Tasks & Approvals' })).toBeTruthy();
        expect(screen.queryByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeNull();
        expect(lastPath).toBe('/environments/env-1/tasks');
    });
});
