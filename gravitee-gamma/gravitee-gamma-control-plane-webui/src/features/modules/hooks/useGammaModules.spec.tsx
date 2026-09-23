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
import { renderHook, waitFor } from '@testing-library/react';

import { useGammaModules } from './useGammaModules';
import { buildUser, TEST_GAMMA_BASE } from '../../../testing/factories';
import { respondWith } from '../../../testing/helpers';
import { useAuthStore } from '../../auth/auth.store';

const mockRegisterRemotes = jest.fn();
jest.mock('@module-federation/runtime', () => ({
    registerRemotes: (...args: unknown[]) => mockRegisterRemotes(...args),
}));

describe('useGammaModules', () => {
    beforeEach(() => {
        mockRegisterRemotes.mockClear();
        useAuthStore.setState({ user: buildUser() });
    });

    it('skips backend-only modules without an mfManifest and still loads UI modules', async () => {
        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [
            { id: 'apim', name: 'APIM', version: '1.0.0', mfManifest: { name: 'apim', exposes: [{ name: './Module' }] } },
            // backend-only module (e.g. authz): no UI, hence no mfManifest
            { id: 'authz', name: 'Authorization', version: '1.0.0' },
        ]);

        const { result } = renderHook(() => useGammaModules());

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.error).toBeNull();
        expect(result.current.modules).toEqual([
            { id: 'apim', name: 'APIM', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' },
        ]);
        expect(mockRegisterRemotes).toHaveBeenCalledWith(
            [{ name: 'apim', entry: `${TEST_GAMMA_BASE}/modules/apim/assets/mf-manifest.json` }],
            { force: true },
        );
    });

    it('loads a backend-only module from DEV_MODULE_ENTRIES when the packaged UI is missing', async () => {
        const previous = process.env.DEV_MODULE_ENTRIES;
        process.env.DEV_MODULE_ENTRIES = 'apim=http://localhost:3001/mf-manifest.json';

        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [{ id: 'apim', name: 'APIM', version: '1.0.0' }]);
        respondWith('get', 'http://localhost:3001/mf-manifest.json', {
            name: 'gravitee_gamma_module_apim',
            exposes: [{ name: './App' }],
        });

        try {
            const { result } = renderHook(() => useGammaModules());

            await waitFor(() => expect(result.current.loading).toBe(false));

            expect(result.current.error).toBeNull();
            expect(result.current.modules).toEqual([
                {
                    id: 'apim',
                    name: 'APIM',
                    version: '1.0.0',
                    remoteName: 'gravitee_gamma_module_apim',
                    exposedModule: 'App',
                },
            ]);
            expect(mockRegisterRemotes).toHaveBeenCalledWith(
                [{ name: 'gravitee_gamma_module_apim', entry: 'http://localhost:3001/mf-manifest.json' }],
                { force: true },
            );
        } finally {
            if (previous === undefined) {
                delete process.env.DEV_MODULE_ENTRIES;
            } else {
                process.env.DEV_MODULE_ENTRIES = previous;
            }
        }
    });

    it('loads Platform from the local fallback when only APIM is listed in DEV_MODULE_ENTRIES', async () => {
        const previous = process.env.DEV_MODULE_ENTRIES;
        process.env.DEV_MODULE_ENTRIES = 'apim=http://localhost:3001/mf-manifest.json';

        respondWith('get', `${TEST_GAMMA_BASE}/modules`, [
            { id: 'apim', name: 'APIM', version: '1.0.0' },
            { id: 'platform', name: 'Platform', version: '1.0.0' },
        ]);
        respondWith('get', 'http://localhost:3001/mf-manifest.json', {
            name: 'gravitee_gamma_module_apim',
            exposes: [{ name: './App' }],
        });
        respondWith('get', 'http://localhost:3002/mf-manifest.json', {
            name: 'gravitee_gamma_module_platform',
            exposes: [{ name: './App' }],
        });

        try {
            const { result } = renderHook(() => useGammaModules());

            await waitFor(() => expect(result.current.loading).toBe(false));

            expect(result.current.error).toBeNull();
            expect(result.current.modules.map(m => m.id)).toEqual(['apim', 'platform']);
            expect(mockRegisterRemotes).toHaveBeenCalledWith(
                [
                    { name: 'gravitee_gamma_module_apim', entry: 'http://localhost:3001/mf-manifest.json' },
                    { name: 'gravitee_gamma_module_platform', entry: 'http://localhost:3002/mf-manifest.json' },
                ],
                { force: true },
            );
        } finally {
            if (previous === undefined) {
                delete process.env.DEV_MODULE_ENTRIES;
            } else {
                process.env.DEV_MODULE_ENTRIES = previous;
            }
        }
    });
});
