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
import userEvent from '@testing-library/user-event';
import type { ComponentProps } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { HealthCheckApisTable } from './HealthCheckApisTable';
import { useEnvironmentHealthAvailability } from '../hooks/useEnvironmentHealthAvailability';
import type { EnvironmentHealthApi } from '../types';

jest.mock('../hooks/useEnvironmentHealthAvailability', () => ({
    useEnvironmentHealthAvailability: jest.fn(),
}));

const mockUseAvailability = jest.mocked(useEnvironmentHealthAvailability);

const HC_API: EnvironmentHealthApi = {
    id: 'api-inventory',
    name: 'Inventory Service',
    apiVersion: '1.2.0',
    state: 'STARTED',
    lifecycleState: 'PUBLISHED',
    healthcheckEnabled: true,
};

const NO_HC_API: EnvironmentHealthApi = {
    id: 'api-tunnel',
    name: 'Database Tunnel',
    apiVersion: '1.0.0',
    state: 'STARTED',
    lifecycleState: 'PUBLISHED',
    healthcheckEnabled: false,
};

function renderTable(apis: EnvironmentHealthApi[], overrides: Partial<ComponentProps<typeof HealthCheckApisTable>> = {}) {
    return render(
        <MemoryRouter>
            <HealthCheckApisTable
                apis={apis}
                totalCount={apis.length}
                loading={false}
                query=""
                page={1}
                pageSize={10}
                sorting={[]}
                from={1}
                to={2}
                reloadToken={0}
                dashboardHref={apiId => `/environments/default/apim/apis/${apiId}/endpoints/health-check-dashboard`}
                onSearchChange={jest.fn()}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                onSortingChange={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('HealthCheckApisTable', () => {
    beforeEach(() => {
        mockUseAvailability.mockReturnValue({
            availability: { type: 'configured', availabilityPct: 88 },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useEnvironmentHealthAvailability>);
    });

    afterEach(() => jest.clearAllMocks());

    it('links the kebab to the API Health Check Dashboard under endpoints', async () => {
        const user = userEvent.setup();
        renderTable([HC_API]);

        await user.click(screen.getByRole('button', { name: /Actions for Inventory Service/i }));
        const item = await screen.findByRole('menuitem', { name: /View API-level health check details/i });
        expect(item).toHaveAttribute('href', '/environments/default/apim/apis/api-inventory/endpoints/health-check-dashboard');
    });

    it('hides the kebab when health check is off, matching Classic', () => {
        renderTable([NO_HC_API]);
        expect(screen.queryByRole('button', { name: /Actions for Database Tunnel/i })).toBeNull();
    });

    it('shows the kebab when health check is on, matching Classic eye', () => {
        renderTable([HC_API]);
        expect(screen.getByRole('button', { name: /Actions for Inventory Service/i })).not.toBeNull();
    });

    it('shows not configured without requesting availability', () => {
        renderTable([NO_HC_API]);
        expect(screen.getByText('Health check has not been configured')).not.toBeNull();
        expect(mockUseAvailability).toHaveBeenCalledWith(expect.objectContaining({ enabled: false, apiId: 'api-tunnel' }));
    });

    it('shows no-data, failed, and configured availability states', () => {
        mockUseAvailability.mockReturnValue({
            availability: { type: 'no-data' },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useEnvironmentHealthAvailability>);
        const { unmount } = renderTable([HC_API]);
        expect(screen.getByText('No data to display')).not.toBeNull();
        unmount();

        mockUseAvailability.mockReturnValue({
            availability: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useEnvironmentHealthAvailability>);
        renderTable([HC_API]);
        expect(screen.getByText('Failed to load')).not.toBeNull();
    });

    it('shows a search empty message when the query matches nothing', () => {
        renderTable([], { query: 'Payment', totalCount: 0 });
        expect(screen.getByText('No APIs match your search.')).not.toBeNull();
    });

    it('keeps the search-result order and does not sort by name in the client', () => {
        renderTable([
            { ...NO_HC_API, id: 'api-z', name: 'Zebra API' },
            { ...NO_HC_API, id: 'api-a', name: 'Alpha API' },
        ]);

        const cells = screen.getAllByRole('cell').map(cell => cell.textContent);
        expect(cells.findIndex(text => text?.includes('Zebra API'))).toBeLessThan(cells.findIndex(text => text?.includes('Alpha API')));
    });
});
