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

import { AccountGuard } from './AccountGuard';
import { useEnvironmentStore } from './environment.store';
import { resetAllStores, seedBootstrap, seedEnvironments } from '../../testing/helpers';

describe('AccountGuard', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
    });

    function renderGuard() {
        return render(
            <MemoryRouter initialEntries={['/account/overview']}>
                <Routes>
                    <Route path="/account" element={<AccountGuard />}>
                        <Route path="*" element={<div>Child</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    it('should render children once the session has an environment', () => {
        seedEnvironments();

        renderGuard();

        expect(screen.getByText('Child')).toBeTruthy();
    });

    it('should wait while the environments load', () => {
        renderGuard();

        expect(screen.queryByText('Child')).toBeNull();
    });

    it('should stop waiting when the organization has no environment', () => {
        useEnvironmentStore.setState({ organizationId: 'x', initialized: true, error: new Error('No environment found!') });

        const { container } = renderGuard();

        expect(container.innerHTML).toBe('');
    });

    it('should say so when the environments cannot be loaded', () => {
        useEnvironmentStore.setState({ organizationId: 'x', loadFailed: true, initialized: true });

        renderGuard();

        expect(screen.getByRole('alert').textContent).toContain("Couldn't load your environments");
        expect(screen.queryByText('Child')).toBeNull();
    });
});
