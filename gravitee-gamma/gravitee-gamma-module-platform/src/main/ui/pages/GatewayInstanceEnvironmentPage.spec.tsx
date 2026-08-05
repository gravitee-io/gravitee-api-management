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

import { GatewayInstanceEnvironmentPage } from './GatewayInstanceEnvironmentPage';
import { useGatewayInstanceDetail } from '../features/gateway-instances/hooks/useGatewayInstanceDetail';
import type { Instance } from '../features/gateway-instances/types/instance';

jest.mock('../features/gateway-instances/hooks/useGatewayInstanceDetail');

jest.mock('../features/gateway-instances/components/GatewayInstanceInformationTable', () => ({
    GatewayInstanceInformationTable: ({ rows }: { rows: { type: string }[] }) => (
        <div data-testid="information-table">{rows.map(r => r.type).join(',')}</div>
    ),
}));

jest.mock('../features/gateway-instances/components/GatewayInstancePluginsTable', () => ({
    GatewayInstancePluginsTable: ({ plugins }: { plugins: { id: string }[] }) => (
        <div data-testid="plugins-table">{plugins.map(p => p.id).join(',')}</div>
    ),
}));

jest.mock('../features/gateway-instances/components/GatewayInstanceSystemPropertiesTable', () => ({
    GatewayInstanceSystemPropertiesTable: ({ properties }: { properties: { name: string }[] }) => (
        <div data-testid="properties-table">{properties.map(p => p.name).join(',')}</div>
    ),
}));

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
    plugins: [{ id: 'policy-a', name: 'A', description: '', version: '1.0', plugin: 'policy-a', type: 'policy' }],
    systemProperties: { 'os.name': 'Linux' },
};

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/gateways/event-1/environment']}>
            <Routes>
                <Route path="/gateways/:instanceId/environment" element={<GatewayInstanceEnvironmentPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('GatewayInstanceEnvironmentPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders environment sections from the detail payload', () => {
        mockUseDetail.mockReturnValue({ data: INSTANCE, isLoading: false, isError: false } as never);
        renderPage();
        expect(screen.getByTestId('gateway-instance-environment')).not.toBeNull();
        expect(screen.getByTestId('information-table').textContent).toContain('Hostname');
        expect(screen.getByTestId('plugins-table').textContent).toContain('policy-a');
        expect(screen.getByTestId('properties-table').textContent).toContain('os.name');
    });

    it('shows an error message when the detail query fails', () => {
        mockUseDetail.mockReturnValue({ data: undefined, isLoading: false, isError: true } as never);
        renderPage();
        expect(screen.getByText(/Failed to load environment details/i)).not.toBeNull();
    });
});
