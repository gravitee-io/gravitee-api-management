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
import { act, render, waitFor } from '@testing-library/react';
import { useEffect } from 'react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { RemoteModuleRoute } from './RemoteModuleRoute';
import { EnvironmentGuard } from '../../../features/environment';
import { resetAllStores, seedEnvironments } from '../../../testing/helpers';
import type { GammaModule } from '../modules.types';

const mockMounts = { count: 0 };

function MockRemoteModule() {
    useEffect(() => {
        mockMounts.count += 1;
    }, []);
    return <div>Remote module</div>;
}

const mockRemoteExport = { default: MockRemoteModule };

jest.mock('@module-federation/runtime', () => ({
    loadRemote: jest.fn(() => Promise.resolve(mockRemoteExport)),
}));

const MODULE: GammaModule = {
    id: 'apim',
    name: 'API Management',
    version: '1.0.0',
    remoteName: 'apim',
    exposedModule: 'Module',
};

describe('RemoteModuleRoute', () => {
    beforeEach(() => {
        resetAllStores();
        mockMounts.count = 0;
    });

    /** Resolves the lazy remote; only the first mount is genuinely asynchronous. */
    async function renderMounted() {
        const result = render(<RemoteModuleRoute module={MODULE} />);
        await waitFor(() => expect(mockMounts.count).toBe(1));
        return result;
    }

    /** Mirrors AppRoutes: the module is only ever reached through EnvironmentGuard. */
    async function renderUnderGuard() {
        const router = createMemoryRouter(
            [
                {
                    path: '/environments/:envHrid',
                    element: <EnvironmentGuard />,
                    children: [{ path: 'apim/*', element: <RemoteModuleRoute module={MODULE} /> }],
                },
            ],
            { initialEntries: ['/environments/env-1/apim'] },
        );
        render(<RouterProvider router={router} />);
        await waitFor(() => expect(mockMounts.count).toBe(1));
        return router;
    }

    it('should remount the remote module when the environment changes in the real route tree', async () => {
        seedEnvironments();
        const router = await renderUnderGuard();

        await act(async () => {
            await router.navigate('/environments/env-2/apim');
        });

        await waitFor(() => expect(mockMounts.count).toBe(2));
    });

    it('should not remount on a re-render that leaves the environment unchanged', async () => {
        seedEnvironments();
        const { rerender } = await renderMounted();

        await act(async () => {
            rerender(<RemoteModuleRoute module={MODULE} />);
        });

        expect(mockMounts.count).toBe(1);
    });
});
