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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { ApiHealthCheckPage } from './ApiHealthCheckPage';
import { apimFetchJsonV1Env, apimFetchJsonV2 } from '../shared/api/apimClient';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'env-1', hrids: ['default'] })),
}));
jest.mock('../shared/api/apimClient', () => ({ apimFetchJsonV2: jest.fn(), apimFetchJsonV1Env: jest.fn() }));
jest.mock('../features/api-health-check/utils/healthCheckDashboardPath', () => ({
    healthCheckDashboardPath: (_pathname: string, apiId: string) => `/apis/${apiId}/health-check-dashboard`,
}));

const mockSearch = jest.mocked(apimFetchJsonV2);
const mockAvailability = jest.mocked(apimFetchJsonV1Env);

const HC_GROUPS = [{ services: { healthCheck: { enabled: true } } }];

/** 10 APIs on the page, 5 with a health check -- the shape of Arpit's dev environment. */
const PAGE = Array.from({ length: 10 }, (_, i) => ({
    id: `api-${i}`,
    name: `API ${i}`,
    apiVersion: '1.0.0',
    state: 'STARTED',
    lifecycleState: 'PUBLISHED',
    ...(i % 2 === 1 ? { endpointGroups: HC_GROUPS } : {}),
}));

function searchPaths(): string[] {
    return mockSearch.mock.calls.map(([, path]) => String(path));
}
function availabilityPaths(): string[] {
    return mockAvailability.mock.calls.map(([, path]) => String(path));
}
function healthPaths(): string[] {
    return availabilityPaths().filter(p => p.endsWith('/health?type=availability'));
}
function averagePaths(): string[] {
    return availabilityPaths().filter(p => p.includes('/health/average?type=AVAILABILITY'));
}
/** The table page request; the report's pass is the perPage=100 one. */
function tableSearches(): string[] {
    return searchPaths().filter(p => p.includes('/apis/_search') && p.endsWith('perPage=10'));
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/api-health-check']}>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }
    return render(<ApiHealthCheckPage />, { wrapper: Wrapper });
}

describe('ApiHealthCheckPage network traffic', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockSearch.mockImplementation((_env, path) => {
            // The report's has_health_check:true pass matches nothing until the term is indexed for V4.
            const isReport = String(path).includes('perPage=100');
            const data = isReport ? [] : PAGE;
            return Promise.resolve({
                data,
                pagination: { page: 1, perPage: isReport ? 100 : 10, pageCount: 1, totalCount: data.length },
            } as never);
        });
        mockAvailability.mockImplementation((_env, path) =>
            String(path).includes('/health/average')
                ? (Promise.resolve({ values: [{ buckets: [{ name: 'default', data: [1, 1] }] }] }) as never)
                : (Promise.resolve({ global: { '1m': 99, '1h': 99, '1d': 99, '1w': 99, '1M': 99 } }) as never),
        );
    });

    it('asks for availability once per health-checked row, and not at all for the others', async () => {
        renderPage();

        await waitFor(() => expect(screen.getByText('API 1')).not.toBeNull());
        // Classic's pair per health-checked row: health?type=availability + health/average?type=AVAILABILITY.
        await waitFor(() => expect(healthPaths()).toHaveLength(5));
        await waitFor(() => expect(averagePaths()).toHaveLength(5));

        expect(searchPaths().filter(p => p.includes('/apis/_search'))).toHaveLength(2); // table page + report pass
        expect(averagePaths().every(p => /[?&]from=\d+&to=\d+&interval=\d+$/.test(p))).toBe(true);
        // The 5 rows without a health check must not cost a request.
        for (const i of [0, 2, 4, 6, 8]) {
            expect(availabilityPaths().filter(p => p.includes(`/apis/api-${i}/`))).toHaveLength(0);
        }
    });

    it('does not re-request anything when the page re-renders', async () => {
        const { rerender } = renderPage();

        await waitFor(() => expect(healthPaths()).toHaveLength(5));
        await waitFor(() => expect(averagePaths()).toHaveLength(5));

        // A re-render rebuilds the table's columns, which remounts every cell. Without memoised columns and a
        // staleTime the remount re-requested each row, doubling this page's traffic.
        rerender(<ApiHealthCheckPage />);
        rerender(<ApiHealthCheckPage />);

        await waitFor(() => expect(screen.getByText('API 1')).not.toBeNull());
        expect(healthPaths()).toHaveLength(5);
        expect(averagePaths()).toHaveLength(5);
    });

    it('refreshes without re-running the table search or duplicating availability', async () => {
        renderPage();
        await waitFor(() => expect(availabilityPaths()).toHaveLength(10));

        expect(tableSearches()).toHaveLength(1);
        mockSearch.mockClear();
        mockAvailability.mockClear();

        fireEvent.click(screen.getByRole('button', { name: /Refresh/i }));

        // Classic's onRefreshClicked re-runs the report and each row's availability pair -- and nothing else.
        await waitFor(() => expect(healthPaths()).toHaveLength(5));
        await waitFor(() => expect(averagePaths()).toHaveLength(5));
        expect(searchPaths().filter(p => p.includes('perPage=100'))).toHaveLength(1);
        expect(tableSearches()).toHaveLength(0);
    });
});
