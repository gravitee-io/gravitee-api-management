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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { IntegrationDetailLayout } from './IntegrationDetailLayout';
import { useIntegrationPermissions } from '../hooks/useIntegrationPermissions';

jest.mock('../hooks/useIntegrationPermissions');

const mockUseIntegrationPermissions = jest.mocked(useIntegrationPermissions);

function grantIntegrationPermissions(permissions: string[]) {
    mockUseIntegrationPermissions.mockReturnValue({ data: permissions } as ReturnType<typeof useIntegrationPermissions>);
}

function renderDetailPage(path = '/integrations/integration-1') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="/integrations/:integrationId" element={<IntegrationDetailLayout />}>
                    <Route index element={<div>Overview content</div>} />
                    <Route path="configuration" element={<div>Configuration content</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('IntegrationDetailLayout', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('lists no Configuration entry on the detail page of an integration the user can read but neither update nor delete', () => {
        grantIntegrationPermissions(['integration-definition-r']);

        renderDetailPage();

        expect(screen.getByText('Overview content')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Overview' })).not.toBeNull();
        expect(screen.queryByText('Configuration')).toBeNull();
    });

    it.each([
        ['update', 'integration-definition-u'],
        ['delete', 'integration-definition-d'],
    ])('lists a Configuration entry linking to the Configuration page when the user can %s the integration', (_case, permission) => {
        grantIntegrationPermissions(['integration-definition-r', permission]);

        renderDetailPage();

        expect(screen.getByRole('link', { name: 'Configuration' }).getAttribute('href')).toBe('/integrations/integration-1/configuration');
    });

    it('links the Overview entry back to the integration detail page from its Configuration page', () => {
        grantIntegrationPermissions(['integration-definition-r', 'integration-definition-u']);

        renderDetailPage('/integrations/integration-1/configuration');

        expect(screen.getByText('Configuration content')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Overview' }).getAttribute('href')).toBe('/integrations/integration-1');
    });
});
