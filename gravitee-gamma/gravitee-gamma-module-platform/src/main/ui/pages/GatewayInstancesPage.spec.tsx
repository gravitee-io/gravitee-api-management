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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { GatewayInstancesPage } from './GatewayInstancesPage';
import { useGatewayInstanceList } from '../features/gateway-instances/hooks/useGatewayInstanceList';
import type { GatewayInstanceRow } from '../features/gateway-instances/types/instance';
import { ApimApiError } from '../shared/api/apimClient';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: () => ({ id: 'env-1' }),
}));

jest.mock('../features/gateway-instances/hooks/useGatewayInstanceList');

jest.mock('../features/gateway-instances/components/GatewayInstancesTable', () => ({
    GatewayInstancesTable: ({
        rows,
        onPageChange,
        onPageSizeChange,
    }: {
        rows: GatewayInstanceRow[];
        onPageChange: (page: number) => void;
        onPageSizeChange: (size: number) => void;
    }) => (
        <div>
            {rows.map(row => (
                <span key={row.id}>{row.hostname}</span>
            ))}
            <button type="button" onClick={() => onPageChange(2)}>
                Next page
            </button>
            <button type="button" onClick={() => onPageSizeChange(25)}>
                Page size 25
            </button>
        </div>
    ),
}));

const mockUseGatewayInstanceList = jest.mocked(useGatewayInstanceList);

const ROWS: GatewayInstanceRow[] = [
    {
        id: 'event-1',
        hostname: 'apim-gateway',
        version: '4.12.13',
        state: 'STARTED',
        lastHeartbeat: new Date('2026-08-05T13:45:14Z'),
        os: 'Linux',
        ip: '172.18.0.3',
        port: '8082',
        tenant: '',
        tags: [],
    },
];

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter>
                <GatewayInstancesPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('GatewayInstancesPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseGatewayInstanceList.mockReturnValue({
            rows: ROWS,
            totalCount: 1,
            isLoading: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useGatewayInstanceList>);
    });

    it('renders the Gateways heading and rows', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'Gateways' })).not.toBeNull();
        expect(screen.getByText('apim-gateway')).not.toBeNull();
    });

    it('forwards pagination to the list query', () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
        expect(mockUseGatewayInstanceList).toHaveBeenCalledWith(expect.objectContaining({ page: 2 }));
        fireEvent.click(screen.getByRole('button', { name: 'Page size 25' }));
        expect(mockUseGatewayInstanceList).toHaveBeenCalledWith(expect.objectContaining({ pageSize: 25, page: 1 }));
    });

    it('shows an error message when the list query fails', () => {
        mockUseGatewayInstanceList.mockReturnValue({
            rows: [],
            totalCount: 0,
            isLoading: false,
            isError: true,
            error: new Error('boom'),
        } as ReturnType<typeof useGatewayInstanceList>);
        renderPage();
        expect(screen.getByText(/Failed to load gateway instances/i)).not.toBeNull();
    });

    it('renders nothing while redirecting on forbidden', () => {
        mockUseGatewayInstanceList.mockReturnValue({
            rows: [],
            totalCount: 0,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as ReturnType<typeof useGatewayInstanceList>);
        const { container } = renderPage();
        expect(container.textContent).toBe('');
        expect(screen.queryByRole('heading', { name: 'Gateways' })).toBeNull();
    });
});
