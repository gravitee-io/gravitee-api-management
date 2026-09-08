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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApiHealthCheckPage } from './ApiHealthCheckPage';
import { useEnvironmentHealthApis } from '../features/api-health-check/hooks/useEnvironmentHealthApis';
import { useEnvironmentHealthReport } from '../features/api-health-check/hooks/useEnvironmentHealthReport';
import { HEALTH_CHECK_FILTER_QUERY } from '../features/api-health-check/utils/healthCheckQuery';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'env-1', hrids: ['default'] })),
}));

jest.mock('../features/api-health-check/utils/healthCheckDashboardPath', () => ({
    healthCheckDashboardPath: (_pathname: string, apiId: string) =>
        `/environments/default/apim/apis/${apiId}/endpoints/health-check-dashboard`,
}));

jest.mock('../features/api-health-check/hooks/useEnvironmentHealthApis');
jest.mock('../features/api-health-check/hooks/useEnvironmentHealthReport');
jest.mock('../features/api-health-check/hooks/useEnvironmentHealthAvailability', () => ({
    useEnvironmentHealthAvailability: () => ({
        availability: { type: 'configured', availabilityPct: 99 },
        isLoading: false,
        isError: false,
    }),
}));

const mockUseEnvironmentHealthApis = jest.mocked(useEnvironmentHealthApis);
const mockUseEnvironmentHealthReport = jest.mocked(useEnvironmentHealthReport);

const PETSTORE = {
    id: 'api-petstore',
    name: 'Petstore API',
    apiVersion: '1.0.0',
    state: 'STARTED' as const,
    lifecycleState: 'PUBLISHED' as const,
    healthcheckEnabled: true,
};

function listResult(overrides: Partial<ReturnType<typeof useEnvironmentHealthApis>> = {}): ReturnType<typeof useEnvironmentHealthApis> {
    return {
        apis: [],
        totalCount: 0,
        isLoading: false,
        isFetching: false,
        isError: false,
        refetch: jest.fn(),
        ...overrides,
    } as ReturnType<typeof useEnvironmentHealthApis>;
}

function renderPage(path = '/api-health-check') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <ApiHealthCheckPage />
        </MemoryRouter>,
    );
}

describe('ApiHealthCheckPage', () => {
    beforeEach(() => {
        mockUseEnvironmentHealthReport.mockReturnValue({
            report: { operational: 1, inWarning: 0, inError: 0 },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useEnvironmentHealthReport>);
        mockUseEnvironmentHealthApis.mockReturnValue(listResult());
    });

    afterEach(() => jest.clearAllMocks());

    it('shows the educational landing when the environment has no APIs and the query is blank', () => {
        renderPage();

        expect(screen.getByText('Watch API backends before consumers feel an outage')).not.toBeNull();
        expect(screen.queryByPlaceholderText(/Search APIs/)).toBeNull();
        expect(screen.queryByRole('button', { name: /Filter to APIs with Health Check enabled/i })).toBeNull();
    });

    it('shows the list, report, and filter when APIs exist', () => {
        mockUseEnvironmentHealthApis.mockReturnValue(listResult({ apis: [PETSTORE], totalCount: 1 }));
        renderPage();

        expect(screen.queryByText('Watch API backends before consumers feel an outage')).toBeNull();
        expect(screen.getByPlaceholderText(/Search APIs/)).not.toBeNull();
        expect(screen.getByText('Petstore API')).not.toBeNull();
        expect(screen.getByText('API Health Check Report')).not.toBeNull();
        expect(mockUseEnvironmentHealthApis).toHaveBeenCalledWith(expect.objectContaining({ sortBy: undefined }));
    });

    it('sorts by name only when the order query param is set, matching Classic', () => {
        mockUseEnvironmentHealthApis.mockReturnValue(listResult({ apis: [PETSTORE], totalCount: 1 }));
        renderPage('/api-health-check?order=name');

        expect(mockUseEnvironmentHealthApis).toHaveBeenCalledWith(expect.objectContaining({ sortBy: 'name' }));
    });

    it('filters to APIs with health check enabled', () => {
        mockUseEnvironmentHealthApis.mockReturnValue(listResult({ apis: [PETSTORE], totalCount: 1 }));
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /Filter to APIs with Health Check enabled/i }));

        expect(screen.getByPlaceholderText(/Search APIs/)).toHaveValue(HEALTH_CHECK_FILTER_QUERY);
        expect(mockUseEnvironmentHealthApis).toHaveBeenCalledWith(expect.objectContaining({ query: HEALTH_CHECK_FILTER_QUERY }));
    });

    it('shows a list error with Try again', () => {
        const refetch = jest.fn();
        mockUseEnvironmentHealthApis.mockReturnValue(listResult({ isError: true, refetch }));
        renderPage();

        expect(screen.getByText('Failed to load APIs.')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: /Try again/i }));
        expect(refetch).toHaveBeenCalled();
    });
});
