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

import { RegistrationEnabledRoute } from './RegistrationEnabledRoute';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { seedBootstrap } from '../../../testing/helpers';

function renderSignUpRoute() {
    return render(
        <MemoryRouter initialEntries={['/sign-up']}>
            <Routes>
                <Route element={<RegistrationEnabledRoute />}>
                    <Route path="/sign-up" element={<div>Sign-up Page</div>} />
                </Route>
                <Route path="/login" element={<div>Login Page</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('RegistrationEnabledRoute', () => {
    it('should render the sign-up page when registration is enabled', () => {
        seedBootstrap({ registrationEnabled: true });

        renderSignUpRoute();

        expect(screen.getByText('Sign-up Page')).toBeTruthy();
    });

    it('should redirect to login when registration is disabled', () => {
        seedBootstrap({ registrationEnabled: false });

        renderSignUpRoute();

        expect(screen.getByText('Login Page')).toBeTruthy();
        expect(screen.queryByText('Sign-up Page')).toBeNull();
    });

    it('should redirect to login when the registration setting is unresolved', () => {
        useBootstrapStore.setState({ config: null });

        renderSignUpRoute();

        expect(screen.getByText('Login Page')).toBeTruthy();
    });
});
