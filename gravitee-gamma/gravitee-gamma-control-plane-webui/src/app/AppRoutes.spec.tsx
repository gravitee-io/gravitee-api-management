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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, useNavigate, type NavigateFunction } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { useAuthStore } from '../features/auth/auth.store';
import { buildUser, TEST_GAMMA_BASE, TEST_MANAGEMENT_BASE } from '../testing/factories';
import { respondWith, respondWithError, seedBootstrap, seedEnvironments, seedUser, trackHandler } from '../testing/helpers';
import { PathnameProbe } from '../testing/PathnameProbe';
import { server } from '../testing/server';

function MockRemoteModule() {
    return <div>Remote module</div>;
}

function NavigateHandle({ onReady }: { readonly onReady: (navigate: NavigateFunction) => void }) {
    onReady(useNavigate());
    return null;
}

const mockLoadRemote = jest.fn();

jest.mock('@module-federation/runtime', () => ({
    loadRemote: (...args: unknown[]) => mockLoadRemote(...args),
    registerRemotes: jest.fn(),
}));

const MODULE_PAGE = '/environments/env-1/aim/agents';
const PAGE_ERROR_TITLE = "Couldn't load your applications";
const ACCOUNT_MODULE = {
    id: 'account',
    name: 'Account Management',
    version: '1.0.0',
    scope: 'account',
    mfManifest: { name: 'cloud', exposes: [{ name: './App' }] },
};

function renderAt(path: string) {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
        </MemoryRouter>,
    );
}

describe('AppRoutes', () => {
    describe('/sign-up', () => {
        it('should send a hand-typed sign-up URL to login when registration is disabled', async () => {
            seedBootstrap({ registrationEnabled: false });
            const customUserFields = trackHandler('get', `${TEST_MANAGEMENT_BASE}/configuration/custom-user-fields`, []);

            renderAt('/sign-up');

            expect(await screen.findByRole('button', { name: 'Sign in' })).toBeTruthy();
            expect(screen.queryByRole('button', { name: 'Request account' })).toBeNull();
            // Every anonymous load renders the loading route table, then the main one. Without a
            // guard in either, the sign-up page mounts on the way to /login and this request fires.
            expect(customUserFields.callCount).toBe(0);
        });

        it('should render the sign-up page when registration is enabled', async () => {
            seedBootstrap({ registrationEnabled: true });

            renderAt('/sign-up');

            expect(await screen.findByRole('button', { name: 'Request account' })).toBeTruthy();
        });
    });

    describe('/environments/:envHrid/my-account', () => {
        it('should render My Account and the breadcrumb for a signed-in operator', async () => {
            seedUser();
            seedEnvironments();

            renderAt('/environments/env-1/my-account');

            expect(await screen.findByRole('heading', { name: 'My Account' })).toBeTruthy();
            expect(screen.getAllByText('My Account').length).toBeGreaterThan(0);
        });
    });

    describe('/registration/:token', () => {
        it('should reach the activation page while registration is disabled', async () => {
            // The email predates the setting: someone holding the link still gets the page's answer.
            seedBootstrap({ registrationEnabled: false });

            renderAt('/registration/not-a-token');

            expect(await screen.findByText('Activate your account')).toBeTruthy();
            expect(screen.queryByRole('button', { name: 'Sign in' })).toBeNull();
        });
    });
});

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

    it('should keep the address of a module above the environments and open it once a retry loads the list', async () => {
        const user = userEvent.setup();
        renderApp('/account/overview');
        expect(await screen.findByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeTruthy();
        expect(lastPath).toBe('/account/overview');
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [ACCOUNT_MODULE]);

        await user.click(screen.getByRole('button', { name: 'Retry' }));

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(lastPath).toBe('/account/overview');
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

    it('should still open My Account, as it is not a page of a module', async () => {
        renderApp('/environments/env-1/my-account');

        expect(await screen.findByRole('heading', { level: 1, name: 'My Account' })).toBeTruthy();
        expect(screen.queryByRole('heading', { level: 1, name: PAGE_ERROR_TITLE })).toBeNull();
        expect(lastPath).toBe('/environments/env-1/my-account');
    });
});

describe('AppRoutes, Account Management', () => {
    let lastPath = '';

    beforeEach(() => {
        mockLoadRemote.mockResolvedValue({ default: MockRemoteModule });
        useAuthStore.setState({ user: buildUser(), initialized: true });
        seedEnvironments();
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [ACCOUNT_MODULE]);
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

    it('should open without an environment in the address', async () => {
        renderApp('/account/overview');

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(lastPath).toBe('/account/overview');
    });

    it('should keep its address while the list of modules loads, as on a reload', async () => {
        let releaseModules: () => void = () => undefined;
        const modulesLoaded = new Promise<void>(resolve => (releaseModules = resolve));
        server.use(
            http.get(`${TEST_GAMMA_BASE}/modules`, async () => {
                await modulesLoaded;
                return HttpResponse.json([ACCOUNT_MODULE]);
            }),
        );

        renderApp('/account/overview');

        expect(await screen.findByLabelText('Loading content')).toBeTruthy();
        expect(lastPath).toBe('/account/overview');

        releaseModules();

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(lastPath).toBe('/account/overview');
    });

    it('should leave its bare address to the module, which picks its first page', async () => {
        renderApp('/account');

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(lastPath).toBe('/account');
    });

    it('should not be served under an environment', async () => {
        renderApp('/environments/env-1/account/overview');

        await waitFor(() => expect(lastPath).toBe('/environments/env-1/home'));
    });

    it('should keep the shell when moving from an environment to Account Management', async () => {
        let navigate: NavigateFunction = () => undefined;
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <NavigateHandle onReady={n => (navigate = n)} />
                <AppRoutes />
            </MemoryRouter>,
        );
        const accountMenu = await screen.findByRole('button', { name: 'Account menu' });

        act(() => {
            void navigate('/account/overview');
        });

        expect(await screen.findByText('Remote module')).toBeTruthy();
        expect(accountMenu.isConnected).toBe(true);
    });

    it('should send its address to Home when the module is not deployed', async () => {
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [
            { id: 'aim', name: 'AIM', version: '1.0.0', mfManifest: { name: 'aim', exposes: [{ name: './App' }] } },
        ]);

        renderApp('/account/overview');

        await waitFor(() => expect(lastPath).toBe('/environments/env-1/home'));
    });
});
