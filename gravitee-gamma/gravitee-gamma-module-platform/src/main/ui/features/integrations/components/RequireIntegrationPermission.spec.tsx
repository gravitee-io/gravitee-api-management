/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { RequireIntegrationPermission } from './RequireIntegrationPermission';
import { useIntegrationPermissions } from '../hooks/useIntegrationPermissions';
import {
    INTEGRATION_CONFIGURATION_PERMISSIONS,
    INTEGRATION_DEFINITION_DELETE_PERMISSION,
    INTEGRATION_DEFINITION_READ_PERMISSION,
    INTEGRATION_DEFINITION_UPDATE_PERMISSION,
} from '../utils/integrationPermissions';

jest.mock('../hooks/useIntegrationPermissions');

const mockUseIntegrationPermissions = jest.mocked(useIntegrationPermissions);

const CONFIGURATION_PATH = '/integrations/integration-1/configuration';

function mockPermissionsResult(result: Partial<ReturnType<typeof useIntegrationPermissions>>) {
    mockUseIntegrationPermissions.mockReturnValue(result as ReturnType<typeof useIntegrationPermissions>);
}

function LocationDisplay() {
    return <div data-testid="location">{useLocation().pathname}</div>;
}

function renderGuardedConfigurationPage() {
    render(
        <MemoryRouter initialEntries={[CONFIGURATION_PATH]}>
            <Routes>
                <Route path="/integrations" element={<div data-testid="integrations-page" />} />
                <Route
                    path="/integrations/:integrationId/configuration"
                    element={
                        <RequireIntegrationPermission anyOf={INTEGRATION_CONFIGURATION_PERMISSIONS}>
                            <div data-testid="configuration-page" />
                        </RequireIntegrationPermission>
                    }
                />
            </Routes>
            <LocationDisplay />
        </MemoryRouter>,
    );
}

describe('RequireIntegrationPermission', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it.each([
        ['update', INTEGRATION_DEFINITION_UPDATE_PERMISSION],
        ['delete', INTEGRATION_DEFINITION_DELETE_PERMISSION],
    ])('renders the guarded page when the user can only %s the integration', (_case, permission) => {
        mockPermissionsResult({ data: [permission], isError: false });

        renderGuardedConfigurationPage();

        expect(screen.getByTestId('configuration-page')).not.toBeNull();
        expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
    });

    it.each<[string, Partial<ReturnType<typeof useIntegrationPermissions>>]>([
        ['the user can neither update nor delete the integration', { data: [INTEGRATION_DEFINITION_READ_PERMISSION], isError: false }],
        ['the permissions request fails', { data: undefined, isError: true, error: new Error('permissions request failed') }],
    ])('redirects to the Integrations list when %s', (_case, permissionsResult) => {
        mockPermissionsResult(permissionsResult);

        renderGuardedConfigurationPage();

        expect(screen.getByTestId('location').textContent).toBe('/integrations');
        expect(screen.getByTestId('integrations-page')).not.toBeNull();
        expect(screen.queryByTestId('configuration-page')).toBeNull();
    });

    it('renders nothing and stays on the page while the permissions are loading', () => {
        mockPermissionsResult({ data: undefined, isError: false, error: null });

        renderGuardedConfigurationPage();

        expect(screen.getByTestId('location').textContent).toBe(CONFIGURATION_PATH);
        expect(screen.queryByTestId('configuration-page')).toBeNull();
        expect(screen.queryByTestId('integrations-page')).toBeNull();
    });
});
