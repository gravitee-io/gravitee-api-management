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

import { CloudSettingsSidebarNav } from '../components/CloudSettingsSidebarNav';

describe('CloudSettingsSidebarNav', () => {
    it('renders Cockpit-style settings sidebar groups and items', () => {
        render(
            <MemoryRouter>
                <CloudSettingsSidebarNav basePath="/environments/default/cloud/settings" />
            </MemoryRouter>,
        );

        expect(screen.getByText('General settings')).toBeTruthy();
        expect(screen.getByText('Security')).toBeTruthy();
        expect(screen.getByText('User settings')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-general')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-custom-reporters')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-account-tokens')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-cloud-tokens')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-sso')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-private-networks')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-members')).toBeTruthy();
        expect(screen.getByTestId('cloud-settings-nav-invite-member')).toBeTruthy();
    });
});
