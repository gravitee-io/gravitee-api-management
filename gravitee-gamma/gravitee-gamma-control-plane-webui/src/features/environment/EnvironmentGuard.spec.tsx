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
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useEnvironmentStore } from './environment.store';
import { EnvironmentGuard } from './EnvironmentGuard';
import { TEST_ENVIRONMENTS } from '../../testing/factories';
import { resetAllStores, seedBootstrap, seedEnvironments } from '../../testing/helpers';
import { PathnameProbe } from '../../testing/PathnameProbe';

describe('EnvironmentGuard', () => {
    let lastPath = '';

    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        lastPath = '';
    });

    function renderGuard(initialPath: string) {
        return render(
            <MemoryRouter initialEntries={[initialPath]}>
                <PathnameProbe onPath={p => (lastPath = p)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<EnvironmentGuard />}>
                        <Route path="home" element={<div>Child</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    it('should render children when env segment matches a known hrid', async () => {
        seedEnvironments();
        renderGuard('/environments/env-1/home');

        await waitFor(() => expect(screen.getByText('Child')).toBeTruthy());
    });

    it('should set current environment in the store on match', async () => {
        seedEnvironments();
        renderGuard('/environments/env-2/home');

        await waitFor(() => {
            expect(useEnvironmentStore.getState().currentEnvironment?.id).toBe('env-2-id');
        });
    });

    it('should replace id in URL with primary hrid', async () => {
        useEnvironmentStore.setState({
            organizationId: 'test-org',
            environments: TEST_ENVIRONMENTS,
            currentEnvironment: null,
            environmentId: '',
            loading: false,
            error: null,
            initialized: true,
        });

        render(
            <MemoryRouter initialEntries={['/environments/env-1-id/home']}>
                <PathnameProbe onPath={p => (lastPath = p)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<EnvironmentGuard />}>
                        <Route path="home" element={<div>Child</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await waitFor(() => {
            expect(lastPath).toBe('/environments/env-1/home');
        });
    });

    it('should redirect to first environment when segment is unknown', async () => {
        seedEnvironments();
        render(
            <MemoryRouter initialEntries={['/environments/unknown-segment/home']}>
                <PathnameProbe onPath={p => (lastPath = p)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<EnvironmentGuard />}>
                        <Route path="home" element={<div>Child</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await waitFor(() => {
            expect(lastPath).toBe('/environments/env-1/home');
        });
    });

    it('should render nothing when environments list is empty', () => {
        useEnvironmentStore.setState({
            organizationId: 'x',
            environments: [],
            currentEnvironment: null,
            environmentId: '',
            loading: false,
            error: null,
            initialized: true,
        });

        const { container } = renderGuard('/environments/x/home');
        expect(container.textContent).not.toContain('Child');
    });
});
