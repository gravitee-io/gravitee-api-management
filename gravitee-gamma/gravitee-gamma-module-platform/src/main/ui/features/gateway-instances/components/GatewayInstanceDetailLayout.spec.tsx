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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { GatewayInstanceDetailLayout } from './GatewayInstanceDetailLayout';
import { ApimApiError } from '../../../shared/api/apimClient';
import { useGatewayInstanceDetail } from '../hooks/useGatewayInstanceDetail';
import type { Instance } from '../types/instance';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('../hooks/useGatewayInstanceDetail');

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseDetail = jest.mocked(useGatewayInstanceDetail);

const INSTANCE: Instance = {
    id: 'gw-1',
    event: 'event-1',
    hostname: 'apim-gateway',
    ip: '10.0.0.22',
    port: '8082',
    version: '4.13.0',
    state: 'STARTED',
    started_at: 1,
    last_heartbeat_at: 2,
};

function renderLayout(path = '/gateways/event-1/environment', seedPermissions: string[] = []) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(['environment-permissions', 'env-1'], seedPermissions);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route path="/gateways/:instanceId" element={<GatewayInstanceDetailLayout />}>
                        <Route path="environment" element={<div>Environment content</div>} />
                        <Route path="monitoring" element={<div>Monitoring content</div>} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { invalidateSpy, queryClient };
}

describe('GatewayInstanceDetailLayout', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
    });

    it('renders header, status badge, tabs, and outlet content', () => {
        mockUseDetail.mockReturnValue({ data: INSTANCE, isLoading: false, isError: false, error: null } as never);
        renderLayout();
        expect(screen.getByTestId('gateway-instance-detail')).not.toBeNull();
        expect(screen.getByRole('heading', { name: 'apim-gateway' })).not.toBeNull();
        expect(screen.getByText('Running')).not.toBeNull();
        expect(screen.getByTestId('instances-detail-environment')).not.toBeNull();
        expect(screen.getByTestId('instances-detail-monitoring')).not.toBeNull();
        expect(screen.getByText('Environment content')).not.toBeNull();
        expect(screen.getByTestId('gateway-instance-tags').textContent).toBe('No tag configured');
    });

    it('shows configured sharding tags in the header', () => {
        mockUseDetail.mockReturnValue({
            data: { ...INSTANCE, tags: ['external', 'partner'] },
            isLoading: false,
            isError: false,
            error: null,
        } as never);
        renderLayout();
        expect(screen.getByTestId('gateway-instance-tags').textContent).toBe('Tags: external, partner');
    });

    it('navigates back to the gateways list', () => {
        mockUseDetail.mockReturnValue({ data: INSTANCE, isLoading: false, isError: false, error: null } as never);
        renderLayout();
        fireEvent.click(screen.getByRole('button', { name: /Back to Gateways/i }));
        expect(mockNavigate).toHaveBeenCalledWith('../..');
    });

    it('shows not-found state when the detail query fails', () => {
        mockUseDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new Error('boom'),
        } as never);
        renderLayout();
        expect(screen.getByText(/Gateway instance not found or failed to load/i)).not.toBeNull();
    });

    it('redirects away and strips instance permissions from the cache on a 403', async () => {
        mockUseDetail.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as never);

        const { invalidateSpy, queryClient } = renderLayout('/gateways/event-1/environment', [
            'environment-metadata-r',
            'environment-instance-r',
        ]);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../../applications', { replace: true }));
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual(['environment-metadata-r']);
        expect(invalidateSpy).not.toHaveBeenCalled();
        expect(screen.queryByText(/Gateway instance not found or failed to load/i)).toBeNull();
    });
});
