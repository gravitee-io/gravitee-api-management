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
import { act, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { IntegrationOverviewPage } from './IntegrationOverviewPage';
import { getIntegration } from '../features/integrations/services/integrationDetail';
import type { IntegrationAgentStatus } from '../features/integrations/types/integration';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn() }));
jest.mock('../features/integrations/services/integrationDetail', () => ({ getIntegration: jest.fn() }));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetIntegration = jest.mocked(getIntegration);
const mockNotifyError = jest.mocked(notify.error);
// The v2 DTO declares agentStatus nullable, so the wire can send an explicit null that the optional field type cannot express.
const WIRE_NULL_AGENT_STATUS = null as unknown as IntegrationAgentStatus;

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderIntegrationOverviewPage(integrationId: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/integrations/${integrationId}`]}>
                <LocationProbe />
                <Routes>
                    <Route path="/integrations">
                        <Route index element={<p>Integrations list</p>} />
                        <Route path=":integrationId" element={<IntegrationOverviewPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('IntegrationOverviewPage', () => {
    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('shows neither integration details nor the load failure message while the integration is loading', async () => {
        mockGetIntegration.mockReturnValue(new Promise(() => {}));

        renderIntegrationOverviewPage('integration-a2a');

        await waitFor(() => expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-a2a'));
        const overview = screen.getByTestId('integration-overview-page');
        expect(within(overview).queryByRole('heading')).toBeNull();
        expect(overview.textContent).not.toContain('Integration could not be loaded');
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('sends no integration request and shows no details or error when there is no current environment', async () => {
        mockUseEnvironment.mockReturnValue(undefined);

        renderIntegrationOverviewPage('integration-a2a');

        const overview = await screen.findByTestId('integration-overview-page');
        await act(() => new Promise(resolve => setTimeout(resolve, 0)));
        expect(mockGetIntegration).not.toHaveBeenCalled();
        expect(within(overview).queryByRole('heading')).toBeNull();
        expect(overview.textContent).not.toContain('Integration could not be loaded');
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('raises a single error toast carrying the failure when the integration fails to load', async () => {
        const failure = new Error('integration unavailable');
        mockGetIntegration.mockRejectedValue(failure);

        renderIntegrationOverviewPage('integration-a2a');

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledWith(failure, expect.stringMatching(/\S/)));
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
        expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-a2a');
    });

    it('shows only the load failure message, with no integration details, when the integration fails to load', async () => {
        mockGetIntegration.mockRejectedValue(new Error('integration unavailable'));

        renderIntegrationOverviewPage('integration-a2a');

        const overview = screen.getByTestId('integration-overview-page');
        await waitFor(() => expect(overview.textContent).toBe('Integration could not be loaded. Please refresh and try again.'));
        expect(within(overview).queryByRole('heading')).toBeNull();
    });

    it('redirects to the Integrations list without a toast or load failure message when the integration request is forbidden', async () => {
        mockGetIntegration.mockRejectedValue(new ApimApiError(403, 'Forbidden'));

        renderIntegrationOverviewPage('integration-a2a');

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-overview-page')).toBeNull();
        expect(screen.queryByText(/Integration could not be loaded/)).toBeNull();
        expect(screen.queryByRole('heading')).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('warns once with the integration id when the integration request is forbidden', async () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        mockGetIntegration.mockRejectedValue(new ApimApiError(403, 'Forbidden'));

        renderIntegrationOverviewPage('integration-a2a');

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(warn).toHaveBeenCalledTimes(1);
        expect(String(warn.mock.calls[0][0])).toContain('integration-a2a');
        warn.mockRestore();
    });

    it.each(['aws-api-gateway', 'solace'])(
        'shows a Connected agent connection section without disconnected guidance for a connected %s integration',
        async provider => {
            mockGetIntegration.mockResolvedValue({ id: 'integration-gw', name: 'Gateway integration', provider, agentStatus: 'CONNECTED' });

            renderIntegrationOverviewPage('integration-gw');

            const section = await screen.findByTestId('integration-agent-connection');
            expect(within(section).getByRole('heading', { name: 'Agent connection' })).toBeInTheDocument();
            expect(within(section).getByText('Connected')).toBeInTheDocument();
            expect(within(section).queryByText('Disconnected')).toBeNull();
            expect(screen.queryByText(/Check your agent status/)).toBeNull();
        },
    );

    it('shows a Disconnected agent connection section with guidance to check the agent for a disconnected gateway-style integration', async () => {
        mockGetIntegration.mockResolvedValue({
            id: 'integration-gw',
            name: 'Gateway integration',
            provider: 'aws-api-gateway',
            agentStatus: 'DISCONNECTED',
        });

        renderIntegrationOverviewPage('integration-gw');

        const section = await screen.findByTestId('integration-agent-connection');
        expect(within(section).getByRole('heading', { name: 'Agent connection' })).toBeInTheDocument();
        expect(within(section).getByText('Disconnected')).toBeInTheDocument();
        expect(within(section).queryByText('Connected')).toBeNull();
        expect(
            within(section).getByText(
                'Check your agent status and ensure connectivity with the provider to start importing your APIs in Gravitee.',
            ),
        ).toBeInTheDocument();
    });

    it.each([
        ['omits the agent status', {}],
        ['carries a null agent status', { agentStatus: WIRE_NULL_AGENT_STATUS }],
    ])(
        'shows the agent connection section with no status badge and no guidance when a gateway-style integration response %s',
        async (_variant, agentStatusField) => {
            mockGetIntegration.mockResolvedValue({
                id: 'integration-gw',
                name: 'Gateway integration',
                provider: 'aws-api-gateway',
                ...agentStatusField,
            });

            renderIntegrationOverviewPage('integration-gw');

            const section = await screen.findByTestId('integration-agent-connection');
            expect(within(section).getByRole('heading', { name: 'Agent connection' })).toBeInTheDocument();
            expect(section.textContent).not.toMatch(/Connected|Disconnected/);
            expect(screen.queryByText(/Check your agent status/)).toBeNull();
        },
    );

    it.each([
        ['without an agent status', {}],
        ['even when the response carries a disconnected agent status', { agentStatus: 'DISCONNECTED' as const }],
    ])('shows no agent connection section for an A2A integration %s', async (_variant, agentStatusField) => {
        mockGetIntegration.mockResolvedValue({ id: 'integration-a2a', name: 'A2A integration', provider: 'A2A', ...agentStatusField });

        renderIntegrationOverviewPage('integration-a2a');

        expect(await screen.findByRole('heading', { name: 'A2A integration' })).toBeInTheDocument();
        expect(screen.getByText('A2A Protocol')).toBeInTheDocument();
        const overview = screen.getByTestId('integration-overview-page');
        expect(screen.queryByTestId('integration-agent-connection')).toBeNull();
        expect(within(overview).queryByRole('heading', { name: 'Agent connection' })).toBeNull();
        expect(overview.textContent).not.toMatch(/Connected|Disconnected/);
        expect(overview.textContent).not.toContain('Check your agent status');
    });

    it('shows the ingestion-in-progress indicator when a gateway-style integration has a pending ingestion job', async () => {
        mockGetIntegration.mockResolvedValue({
            id: 'integration-aws',
            name: 'AWS integration',
            provider: 'aws-api-gateway',
            pendingJob: { id: 'job-1', startedAt: '2026-09-26T10:00:00Z', status: 'PENDING' },
        });

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByTestId('integration-ingestion-in-progress')).toBeInTheDocument();
    });

    it.each([
        ['has no ingestion job', undefined],
        ['has a succeeded ingestion job', { id: 'job-1', status: 'SUCCESS' as const }],
        ['has a failed ingestion job', { id: 'job-1', status: 'ERROR' as const }],
        ['has a timed-out ingestion job', { id: 'job-1', status: 'TIMEOUT' as const }],
    ])('shows no ingestion-in-progress indicator when a gateway-style integration %s', async (_, pendingJob) => {
        mockGetIntegration.mockResolvedValue({
            id: 'integration-aws',
            name: 'AWS integration',
            provider: 'aws-api-gateway',
            pendingJob,
        });

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByRole('heading', { name: 'AWS integration' })).toBeInTheDocument();
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
    });

    it('shows no ingestion indicator or ingestion text on the overview of an A2A integration', async () => {
        mockGetIntegration.mockResolvedValue({ id: 'integration-a2a', name: 'A2A integration', provider: 'A2A' });

        renderIntegrationOverviewPage('integration-a2a');

        expect(await screen.findByRole('heading', { name: 'A2A integration' })).toBeInTheDocument();
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
        expect(screen.getByTestId('integration-overview-page').textContent).not.toMatch(/ingest/i);
    });

    it('shows no ingestion-in-progress indicator for an A2A integration even when its response carries a pending job', async () => {
        mockGetIntegration.mockResolvedValue({
            id: 'integration-a2a',
            name: 'A2A integration',
            provider: 'A2A',
            pendingJob: { id: 'job-1', status: 'PENDING' },
        });

        renderIntegrationOverviewPage('integration-a2a');

        expect(await screen.findByRole('heading', { name: 'A2A integration' })).toBeInTheDocument();
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
    });

    it.each([
        ['is no longer reported', undefined],
        ['has timed out', { id: 'job-1', status: 'TIMEOUT' as const }],
    ])('hides the ingestion-in-progress indicator and stops polling once the pending ingestion job %s', async (_, nextPendingJob) => {
        jest.useFakeTimers();
        const gatewayIntegration = { id: 'integration-aws', name: 'AWS integration', provider: 'aws-api-gateway' };
        mockGetIntegration
            .mockResolvedValueOnce({ ...gatewayIntegration, pendingJob: { id: 'job-1', status: 'PENDING' } })
            .mockResolvedValue({ ...gatewayIntegration, pendingJob: nextPendingJob });

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByTestId('integration-ingestion-in-progress')).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
        expect(mockGetIntegration).toHaveBeenCalledTimes(2);
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        expect(mockGetIntegration).toHaveBeenCalledTimes(2);
    });

    it('stops polling and raises a single error toast when refreshing an integration with a pending ingestion job keeps failing', async () => {
        jest.useFakeTimers();
        mockGetIntegration
            .mockResolvedValueOnce({
                id: 'integration-aws',
                name: 'AWS integration',
                provider: 'aws-api-gateway',
                pendingJob: { id: 'job-1', status: 'PENDING' },
            })
            .mockImplementation(() => Promise.reject(new Error('integration unavailable')));

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByTestId('integration-ingestion-in-progress')).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        await act(() => jest.advanceTimersByTimeAsync(15_000));
        expect(mockGetIntegration).toHaveBeenCalledTimes(2);
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
        expect(mockNotifyError).toHaveBeenCalledWith(expect.any(Error), expect.stringMatching(/\S/));
    });

    it.each([
        ['a gateway-style integration has no pending ingestion job', { provider: 'aws-api-gateway' }],
        [
            'an A2A integration response carries a pending ingestion job',
            { provider: 'A2A', pendingJob: { id: 'job-1', status: 'PENDING' as const } },
        ],
    ])('does not refresh the integration when %s', async (_, variant) => {
        jest.useFakeTimers();
        mockGetIntegration.mockResolvedValue({ id: 'integration-x', name: 'Some integration', ...variant });

        renderIntegrationOverviewPage('integration-x');

        expect(await screen.findByRole('heading', { name: 'Some integration' })).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(12_000));
        expect(mockGetIntegration).toHaveBeenCalledTimes(1);
    });

    it.each([
        ['while the integration is loading', () => new Promise<never>(() => {})],
        ['when the integration fails to load', () => Promise.reject(new Error('integration unavailable'))],
    ])('shows no ingestion-in-progress indicator %s', async (_, integrationResponse) => {
        mockGetIntegration.mockImplementation(integrationResponse);

        renderIntegrationOverviewPage('integration-aws');

        await waitFor(() => expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-aws'));
        await act(() => new Promise(resolve => setTimeout(resolve, 0)));
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
    });
});
