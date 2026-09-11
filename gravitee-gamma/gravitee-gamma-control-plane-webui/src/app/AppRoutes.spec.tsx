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
import { MemoryRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';
import { TEST_MANAGEMENT_BASE } from '../testing/factories';
import { seedBootstrap, trackHandler } from '../testing/helpers';

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
