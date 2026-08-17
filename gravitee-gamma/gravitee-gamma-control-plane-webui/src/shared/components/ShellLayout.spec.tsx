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
import { MemoryRouter, Route, Routes, useNavigate, type NavigateFunction } from 'react-router-dom';

import { ShellLayout } from './ShellLayout';
import { EnvironmentGuard } from '../../features/environment';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import type { GammaModule } from '../../features/modules';
import { EnvironmentRenderProbe } from '../../testing/EnvironmentRenderProbe';
import { resetAllStores, seedBootstrap, seedEnvironments } from '../../testing/helpers';

const MODULES: GammaModule[] = [{ id: 'apim', name: 'API Management', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' }];

function NavigateHandle({ onReady }: { readonly onReady: (navigate: NavigateFunction) => void }) {
    onReady(useNavigate());
    return null;
}

describe('ShellLayout environment switching', () => {
    let renders: Array<{ pathname: string; environmentId: string }> = [];
    let navigate: NavigateFunction;

    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
        renders = [];
    });

    /** Mirrors AppRoutes: the guard sits between the shell and the module area. */
    function renderShell(initialPath: string) {
        return render(
            <MemoryRouter initialEntries={[initialPath]}>
                <NavigateHandle onReady={n => (navigate = n)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route element={<EnvironmentGuard />}>
                            <Route
                                path="*"
                                element={
                                    <EnvironmentRenderProbe
                                        onRender={(pathname, environmentId) => renders.push({ pathname, environmentId })}
                                    />
                                }
                            />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    function mismatchedRenders() {
        return renders.filter(r => {
            const hrid = /^\/environments\/([^/]+)/.exec(r.pathname)?.[1];
            return hrid === 'env-1' ? r.environmentId !== 'env-1-id' : hrid === 'env-2' && r.environmentId !== 'env-2-id';
        });
    }

    async function switchToEnvironment2() {
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /Switch environment/ }));
        await user.click(await screen.findByText('Environment 2'));
    }

    it('should land on the module root when switching from a deep module page', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));
    });

    it('should land on the host area root when switching from a deep host page', async () => {
        renderShell('/environments/env-1/tasks/task-1');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/tasks'));
    });

    it('should never render the new environment path while the store still holds the previous environment', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should never pair a restored environment path with the environment left behind, on back navigation', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');
        await switchToEnvironment2();
        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));

        await act(async () => {
            navigate(-1);
        });

        await waitFor(() => expect(useEnvironmentStore.getState().environmentId).toBe('env-1-id'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should never pair a deep-linked environment path with the environment seeded at startup', async () => {
        renderShell('/environments/env-2/apim/apis/api-1/endpoints');

        await waitFor(() => expect(useEnvironmentStore.getState().environmentId).toBe('env-2-id'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should update the environment store when switching', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(useEnvironmentStore.getState().currentEnvironment?.id).toBe('env-2-id'));
    });
});
