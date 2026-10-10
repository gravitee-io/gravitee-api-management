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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Suspense, useEffect } from 'react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

import { RemoteModuleRoute } from './RemoteModuleRoute';
import { EnvironmentGuard } from '../../../features/environment';
import { resetAllStores, seedEnvironments } from '../../../testing/helpers';
import { RETRY_DELAYS_MS } from '../modules.remotes';
import type { GammaModule } from '../modules.types';

const mockMounts = { count: 0 };

function MockRemoteModule() {
    useEffect(() => {
        mockMounts.count += 1;
    }, []);
    return <div>Remote module</div>;
}

const mockRemoteExport = { default: MockRemoteModule };
const mockLoadRemote = jest.fn();

jest.mock('@module-federation/runtime', () => ({
    loadRemote: (...args: unknown[]) => mockLoadRemote(...args),
    registerRemotes: jest.fn(),
}));

const UNAVAILABLE_TITLE = "isn't available right now";
const ALL_RETRIES_MS = RETRY_DELAYS_MS.reduce((total, delay) => total + delay, 0);

function buildModule(id: string): GammaModule {
    return { id, name: id, version: '1.0.0', remoteName: id, exposedModule: 'App' };
}

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
        mockLoadRemote.mockReset();
        mockLoadRemote.mockResolvedValue(mockRemoteExport);
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

    describe('when a module cannot be shown', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            jest.spyOn(console, 'error').mockImplementation(() => undefined);
            jest.spyOn(console, 'warn').mockImplementation(() => undefined);
            jest.spyOn(console, 'info').mockImplementation(() => undefined);
        });

        afterEach(() => {
            jest.useRealTimers();
            jest.restoreAllMocks();
        });

        async function runAllRetries() {
            await act(async () => {
                await jest.advanceTimersByTimeAsync(ALL_RETRIES_MS);
            });
        }

        /** Two sibling module routes, as AppRoutes declares them: React Router renders both in the same slot. */
        function renderTwoModulesUnderGuard(first: GammaModule, second: GammaModule) {
            const router = createMemoryRouter(
                [
                    {
                        path: '/environments/:envHrid',
                        element: <EnvironmentGuard />,
                        children: [first, second].map(m => ({ path: `${m.id}/*`, element: <RemoteModuleRoute module={m} /> })),
                    },
                ],
                { initialEntries: [`/environments/env-1/${first.id}`] },
            );
            render(<RouterProvider router={router} />);
            return router;
        }

        async function openModule(router: ReturnType<typeof createMemoryRouter>, module: GammaModule) {
            await act(async () => {
                await router.navigate(`/environments/env-1/${module.id}`);
                await jest.advanceTimersByTimeAsync(0);
            });
        }

        it('should keep the loading skeleton, without any message, when a retry succeeds right after a failure', async () => {
            mockLoadRemote.mockRejectedValueOnce(new Error('Loading chunk 8201 failed.')).mockResolvedValue(mockRemoteExport);

            render(<RemoteModuleRoute module={buildModule('blip')} />);
            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] - 1);
            });

            expect(screen.getByLabelText('Loading content')).toBeTruthy();
            expect(screen.queryByText(/isn't ready yet/)).toBeNull();

            await act(async () => {
                await jest.advanceTimersByTimeAsync(1);
            });

            expect(await screen.findByText('Remote module')).toBeTruthy();
        });

        it("should tell the user the app isn't ready yet once failures last, then open it by itself", async () => {
            const chunkError = new Error('Loading chunk 8201 failed.');
            mockLoadRemote
                .mockRejectedValueOnce(chunkError)
                .mockRejectedValueOnce(chunkError)
                .mockRejectedValueOnce(chunkError)
                .mockResolvedValue(mockRemoteExport);

            render(<RemoteModuleRoute module={{ ...buildModule('aim'), remoteName: 'aim-restarting' }} />);
            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
            });

            expect(await screen.findByText("Agent Management isn't ready yet")).toBeTruthy();

            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[2]);
            });

            expect(await screen.findByText('Remote module')).toBeTruthy();
            expect(screen.queryByText(/isn't ready yet/)).toBeNull();
        });

        it('should announce that the app is not ready yet, then that it is ready', async () => {
            const chunkError = new Error('Loading chunk 8201 failed.');
            mockLoadRemote
                .mockRejectedValueOnce(chunkError)
                .mockRejectedValueOnce(chunkError)
                .mockRejectedValueOnce(chunkError)
                .mockResolvedValue(mockRemoteExport);

            render(<RemoteModuleRoute module={{ ...buildModule('aim'), remoteName: 'aim-announced' }} />);
            const announcement = screen.getByRole('status');
            expect(announcement.textContent).toBe('');

            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
            });
            expect(announcement.textContent).toContain("Agent Management isn't ready yet");

            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[2]);
            });
            await screen.findByText('Remote module');
            expect(screen.getByRole('status').textContent).toBe('Agent Management is ready.');
        });

        it('should move the focus to the module area when the module opens while the user was on Retry now', async () => {
            let platformIsBack = false;
            mockLoadRemote.mockImplementation(() =>
                platformIsBack ? Promise.resolve(mockRemoteExport) : Promise.reject(new Error('Loading chunk 8201 failed.')),
            );

            render(<RemoteModuleRoute module={buildModule('focus-return')} />);
            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
            });
            const retryNow = await screen.findByRole('button', { name: 'Retry now' });
            retryNow.focus();

            platformIsBack = true;
            await act(async () => {
                fireEvent.click(retryNow);
                await jest.advanceTimersByTimeAsync(0);
            });
            await screen.findByText('Remote module');

            await waitFor(() => expect(document.activeElement).toBe(screen.getByRole('status')));
        });

        it('should load the module at once when the user clicks Retry now', async () => {
            let platformIsBack = false;
            mockLoadRemote.mockImplementation(() =>
                platformIsBack ? Promise.resolve(mockRemoteExport) : Promise.reject(new Error('Loading chunk 8201 failed.')),
            );

            render(<RemoteModuleRoute module={buildModule('retry-now')} />);
            await act(async () => {
                await jest.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0] + RETRY_DELAYS_MS[1]);
            });
            const retryNow = await screen.findByRole('button', { name: 'Retry now' });

            platformIsBack = true;
            await act(async () => {
                fireEvent.click(retryNow);
                await jest.advanceTimersByTimeAsync(0);
            });

            expect(await screen.findByText('Remote module')).toBeTruthy();
        });

        it('should show a message in place of the module, and keep the rest of the page, once every attempt has failed', async () => {
            mockLoadRemote.mockRejectedValue(new Error('Loading chunk 8201 failed.'));

            render(
                <>
                    <nav>Console navigation</nav>
                    <Suspense fallback={<p>Loading</p>}>
                        <RemoteModuleRoute module={buildModule('unreachable')} />
                    </Suspense>
                </>,
            );
            await runAllRetries();

            expect((await screen.findByRole('alert')).textContent).toContain(UNAVAILABLE_TITLE);
            expect(screen.getByRole('button', { name: 'Reload page' })).toBeTruthy();
            expect(screen.queryByText(/Loading chunk/)).toBeNull();
            expect(screen.getByText('Console navigation')).toBeTruthy();
        });

        it('should name the app in the message', async () => {
            mockLoadRemote.mockRejectedValue(new Error('Loading chunk 8201 failed.'));

            render(<RemoteModuleRoute module={{ ...buildModule('aim'), remoteName: 'aim-unreachable' }} />);
            await runAllRetries();

            expect((await screen.findByRole('alert')).textContent).toContain("Agent Management isn't available right now");
        });

        it('should show the message without loading the module again when it fails to render', async () => {
            function CrashingModule(): never {
                throw new Error("Cannot read properties of undefined (reading 'id')");
            }
            mockLoadRemote.mockResolvedValue({ default: CrashingModule });

            render(<RemoteModuleRoute module={buildModule('crashing')} />);
            await runAllRetries();

            expect((await screen.findByRole('alert')).textContent).toContain(UNAVAILABLE_TITLE);
            expect(mockLoadRemote).toHaveBeenCalledTimes(1);
        });

        it('should show the next module opened after one that failed', async () => {
            seedEnvironments();
            const failing = buildModule('failing');
            const working = buildModule('working');
            mockLoadRemote.mockImplementation((id: string) =>
                id === 'failing/App' ? Promise.reject(new Error('Loading chunk 8201 failed.')) : Promise.resolve(mockRemoteExport),
            );

            const router = renderTwoModulesUnderGuard(failing, working);
            await runAllRetries();
            await screen.findByRole('alert');

            await openModule(router, working);

            expect(await screen.findByText('Remote module')).toBeTruthy();
            expect(screen.queryByRole('alert')).toBeNull();
        });

        it('should load a module again when it is opened after a failure', async () => {
            seedEnvironments();
            const flaky = buildModule('flaky');
            const other = buildModule('other');
            let flakyIsBack = false;
            mockLoadRemote.mockImplementation((id: string) =>
                id === 'flaky/App' && !flakyIsBack
                    ? Promise.reject(new Error('Loading chunk 8201 failed.'))
                    : Promise.resolve(mockRemoteExport),
            );

            const router = renderTwoModulesUnderGuard(flaky, other);
            await runAllRetries();
            await screen.findByRole('alert');

            flakyIsBack = true;
            await openModule(router, other);
            await openModule(router, flaky);

            expect(await screen.findByText('Remote module')).toBeTruthy();
            expect(screen.queryByRole('alert')).toBeNull();
        });

        it('should start with the loading skeleton when a module is opened again after a failure', async () => {
            seedEnvironments();
            const slow = buildModule('slow-after-failure');
            const neighbor = buildModule('neighbor');
            let slowIsBack = false;
            mockLoadRemote.mockImplementation((id: string) => {
                if (id !== 'slow-after-failure/App') return Promise.resolve(mockRemoteExport);
                return slowIsBack ? new Promise(() => undefined) : Promise.reject(new Error('Loading chunk 8201 failed.'));
            });

            const router = renderTwoModulesUnderGuard(slow, neighbor);
            await runAllRetries();
            await screen.findByRole('alert');

            slowIsBack = true;
            await openModule(router, neighbor);
            await openModule(router, slow);

            expect(screen.getByLabelText('Loading content')).toBeTruthy();
            expect(screen.queryByText(/isn't ready yet/)).toBeNull();
        });
    });
});
