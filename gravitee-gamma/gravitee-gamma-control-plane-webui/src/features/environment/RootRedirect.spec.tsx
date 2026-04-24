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
import { RootRedirect } from './RootRedirect';
import { resetAllStores, seedBootstrap, seedEnvironments } from '../../testing/helpers';
import { PathnameProbe } from '../../testing/PathnameProbe';

describe('RootRedirect', () => {
    let lastPath = '';

    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        lastPath = '';
    });

    it('should redirect / to /environments/:firstHrid/home', async () => {
        seedEnvironments();
        render(
            <MemoryRouter initialEntries={['/']}>
                <PathnameProbe onPath={p => (lastPath = p)} />
                <Routes>
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="/environments/:envHrid/home" element={<div data-testid="landed">ok</div>} />
                </Routes>
            </MemoryRouter>,
        );

        await waitFor(() => {
            expect(lastPath).toBe('/environments/env-1/home');
        });
        expect(await screen.findByTestId('landed')).toBeTruthy();
    });

    it('should show error when store has an error and no environments', () => {
        useEnvironmentStore.setState({
            organizationId: '',
            environments: [],
            currentEnvironment: null,
            environmentId: '',
            loading: false,
            error: new Error('boom'),
            initialized: true,
        });
        render(
            <MemoryRouter>
                <RootRedirect />
            </MemoryRouter>,
        );
        expect(screen.getByRole('alert').textContent).toContain('boom');
    });
});
