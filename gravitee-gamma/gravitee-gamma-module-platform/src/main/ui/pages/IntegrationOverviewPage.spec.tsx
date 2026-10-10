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

import { dataTableHarness } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { IntegrationOverviewPage } from './IntegrationOverviewPage';
import { getIntegration, listIngestedApis } from '../features/integrations/services/integrationDetail';
import type { IntegrationAgentStatus } from '../features/integrations/types/integration';
import { ApimApiError } from '../shared/api/apimClient';
import { copyTextToClipboardWithNotifyHandler } from '../shared/copyToClipboard';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({ useEnvironment: jest.fn(), useHasPermission: jest.fn() }));
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../features/users/testing/buildModuleNavPathForTests'));
jest.mock('../features/integrations/services/integrationDetail', () => ({ getIntegration: jest.fn(), listIngestedApis: jest.fn() }));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

jest.mock('../shared/copyToClipboard', () => ({ copyTextToClipboardWithNotifyHandler: jest.fn() }));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseHasPermission = jest.mocked(useHasPermission);
const mockCopyToClipboard = jest.mocked(copyTextToClipboardWithNotifyHandler);
const mockGetIntegration = jest.mocked(getIntegration);
const mockListIngestedApis = jest.mocked(listIngestedApis);
const mockNotifyError = jest.mocked(notify.error);
// The v2 DTO declares agentStatus nullable, so the wire can send an explicit null that the optional field type cannot express.
const WIRE_NULL_AGENT_STATUS = null as unknown as IntegrationAgentStatus;

const GATEWAY_INTEGRATION = { id: 'int-1', name: 'Gateway integration', provider: 'aws-api-gateway' };
const PENDING_INGESTION_JOB = { id: 'job-1', status: 'PENDING' as const };
const ORDERS_API = { id: 'api-1', name: 'Orders API', version: '1.0.0' };
const NO_INGESTED_APIS = { data: [], pagination: { page: 1, perPage: 10, pageCount: 0, pageItemsCount: 0, totalCount: 0 } };

function ingestedApisPage(apis: Array<{ id: string; name: string; version: string }>) {
    return { data: apis, pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: apis.length, totalCount: apis.length } };
}

function serveOrdersApiOnFirstPageAndPaymentsApiOnLaterPages() {
    const totalCount = 12;
    mockListIngestedApis.mockImplementation((_environmentId, _integrationId, { page, perPage }) => {
        const name = page === 1 ? 'Orders API' : 'Payments API';
        return Promise.resolve({
            data: [{ id: name, name, version: '1.0.0' }],
            pagination: { page, perPage, pageCount: Math.ceil(totalCount / perPage), pageItemsCount: 1, totalCount },
        });
    });
}

function grantIntegrationCreatePermission() {
    mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('environment-integration-c') ?? false);
}

function LocationProbe() {
    return <span data-testid="location">{useLocation().pathname}</span>;
}

function renderIntegrationOverviewPage(integrationId: string, pathPrefix = '') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`${pathPrefix}/integrations/${integrationId}`]}>
                <LocationProbe />
                <Routes>
                    <Route path={`${pathPrefix}/integrations`}>
                        <Route index element={<p>Integrations list</p>} />
                        <Route path=":integrationId" element={<IntegrationOverviewPage />} />
                    </Route>
                    <Route path="*" element={<p>Another page</p>} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('IntegrationOverviewPage', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;

        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    beforeEach(() => {
        mockUseEnvironment.mockReturnValue({ id: 'env-1' });
        mockUseHasPermission.mockReturnValue(false);
        mockListIngestedApis.mockResolvedValue(NO_INGESTED_APIS);
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.useRealTimers();
    });

    it('shows neither integration details, the Discover APIs action, nor the load failure message while the integration is loading', async () => {
        grantIntegrationCreatePermission();
        mockGetIntegration.mockReturnValue(new Promise(() => {}));

        renderIntegrationOverviewPage('integration-1');

        await waitFor(() => expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-1'));
        const overview = screen.getByTestId('integration-overview-page');
        expect(within(overview).queryByRole('heading')).toBeNull();
        expect(within(overview).queryByRole('button', { name: 'Discover APIs' })).toBeNull();
        expect(overview.textContent).not.toContain('Integration could not be loaded');
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it('sends no integration request and shows no details or error when there is no current environment', async () => {
        mockUseEnvironment.mockReturnValue(undefined);

        renderIntegrationOverviewPage('integration-1');

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

        renderIntegrationOverviewPage('integration-1');

        await waitFor(() => expect(mockNotifyError).toHaveBeenCalledWith(failure, expect.stringMatching(/\S/)));
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
        expect(mockGetIntegration).toHaveBeenCalledWith('env-1', 'integration-1');
    });

    it('shows only the load failure message, with no integration details and no Discover APIs action, when the integration fails to load', async () => {
        grantIntegrationCreatePermission();
        mockGetIntegration.mockRejectedValue(new Error('integration unavailable'));

        renderIntegrationOverviewPage('integration-1');

        const overview = screen.getByTestId('integration-overview-page');
        await waitFor(() => expect(overview.textContent).toBe('Integration could not be loaded. Please refresh and try again.'));
        expect(within(overview).queryByRole('button', { name: 'Discover APIs' })).toBeNull();
        expect(within(overview).queryByRole('heading')).toBeNull();
    });

    it('redirects to the Integrations list without a toast or load failure message when the integration request is forbidden', async () => {
        mockGetIntegration.mockRejectedValue(new ApimApiError(403, 'Forbidden'));

        renderIntegrationOverviewPage('integration-1');

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

        renderIntegrationOverviewPage('integration-1');

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(warn).toHaveBeenCalledTimes(1);
        expect(String(warn.mock.calls[0][0])).toContain('integration-1');
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

    it('redirects to the Integrations list without a toast when the integration is an A2A one', async () => {
        mockGetIntegration.mockResolvedValue({ id: 'classic-a2a', name: 'Classic A2A integration', provider: 'A2A' });

        renderIntegrationOverviewPage('classic-a2a');

        await waitFor(() => expect(screen.getByTestId('location').textContent).toBe('/integrations'));
        expect(screen.getByText('Integrations list')).toBeInTheDocument();
        expect(screen.queryByTestId('integration-overview-page')).toBeNull();
        expect(screen.queryByRole('heading', { name: 'Classic A2A integration' })).toBeNull();
        expect(mockNotifyError).not.toHaveBeenCalled();
    });

    it.each([
        ['connected', 'CONNECTED' as const],
        ['disconnected', 'DISCONNECTED' as const],
        ['of unknown agent status', undefined],
    ])('shows the integration id of a %s gateway-style integration', async (_variant, agentStatus) => {
        mockGetIntegration.mockResolvedValue({
            id: 'd12619e5-b7e4-4a99-a619-e5b7e45a9999',
            name: 'Gateway integration',
            provider: 'aws-api-gateway',
            agentStatus,
        });

        renderIntegrationOverviewPage('d12619e5-b7e4-4a99-a619-e5b7e45a9999');

        const section = await screen.findByTestId('integration-id');
        expect(within(section).getByRole('heading', { name: 'Integration ID' })).toBeInTheDocument();
        expect(within(section).getByText('d12619e5-b7e4-4a99-a619-e5b7e45a9999')).toBeInTheDocument();
    });

    it('copies the integration id to the clipboard when its copy button is clicked', async () => {
        mockGetIntegration.mockResolvedValue({
            id: 'integration-gw',
            name: 'Gateway integration',
            provider: 'aws-api-gateway',
            agentStatus: 'CONNECTED',
        });

        renderIntegrationOverviewPage('integration-gw');

        const section = await screen.findByTestId('integration-id');
        fireEvent.click(within(section).getByRole('button', { name: 'Copy integration ID' }));
        expect(mockCopyToClipboard).toHaveBeenCalledTimes(1);
        expect(mockCopyToClipboard).toHaveBeenCalledWith('integration-gw', 'Copied to clipboard');
    });

    it('shows the ingestion-in-progress banner when a gateway-style integration with ingested APIs has a pending ingestion job', async () => {
        mockGetIntegration.mockResolvedValue({
            ...GATEWAY_INTEGRATION,
            pendingJob: { ...PENDING_INGESTION_JOB, startedAt: '2026-09-26T10:00:00Z' },
        });
        mockListIngestedApis.mockResolvedValue(ingestedApisPage([ORDERS_API]));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        const banner = await screen.findByTestId('integration-ingestion-in-progress');
        expect(banner.textContent).toBe(
            'APIs are currently being ingested and will appear below once completed. This may take some time depending on volume.',
        );
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
        mockListIngestedApis.mockResolvedValue(ingestedApisPage([ORDERS_API]));

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByText(ORDERS_API.name)).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'AWS integration' })).toBeInTheDocument();
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
        mockListIngestedApis.mockResolvedValue(ingestedApisPage([ORDERS_API]));

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
        mockListIngestedApis.mockResolvedValue(ingestedApisPage([ORDERS_API]));

        renderIntegrationOverviewPage('integration-aws');

        expect(await screen.findByTestId('integration-ingestion-in-progress')).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        await act(() => jest.advanceTimersByTimeAsync(15_000));
        expect(mockGetIntegration).toHaveBeenCalledTimes(2);
        expect(mockNotifyError).toHaveBeenCalledTimes(1);
        expect(mockNotifyError).toHaveBeenCalledWith(expect.any(Error), expect.stringMatching(/\S/));
    });

    it('does not refresh the integration when a gateway-style integration has no pending ingestion job', async () => {
        jest.useFakeTimers();
        mockGetIntegration.mockResolvedValue({ id: 'integration-x', name: 'Some integration', provider: 'aws-api-gateway' });

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

    it.each([
        ['APIs are being ingested', 'No APIs created', 'has a pending ingestion job', PENDING_INGESTION_JOB],
        ['No APIs created', 'APIs are being ingested', 'has no pending ingestion job', undefined],
    ])(
        'shows the %s heading, and not the %s heading, when a gateway-style integration with no ingested APIs %s',
        async (shownHeading, hiddenHeading, _variant, pendingJob) => {
            mockGetIntegration.mockResolvedValue({ ...GATEWAY_INTEGRATION, pendingJob });

            renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

            expect(await screen.findByRole('heading', { name: shownHeading })).toBeInTheDocument();
            expect(screen.queryByRole('heading', { name: hiddenHeading })).toBeNull();
            expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
        },
    );

    it('shows the ingested APIs load failure message, with the integration name and no empty-state heading, when the ingested APIs request fails', async () => {
        mockGetIntegration.mockResolvedValue(GATEWAY_INTEGRATION);
        mockListIngestedApis.mockRejectedValue(new ApimApiError(500, 'Internal Server Error'));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByText('Ingested APIs could not be loaded. Please refresh and try again.')).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: GATEWAY_INTEGRATION.name })).toBeInTheDocument();
        expect(screen.queryByRole('heading', { name: 'No APIs created' })).toBeNull();
    });

    it.each([
        ['a single ingested API', [ORDERS_API]],
        [
            'several ingested APIs',
            [
                { id: 'api-2', name: 'Payments', version: '2.3.1' },
                { id: 'api-3', name: 'Inventory Service', version: 'v1' },
                { id: 'api-4', name: 'customer-events', version: '0.4.2' },
            ],
        ],
    ])('lists %s with the name and version of each', async (_variant, apis) => {
        mockGetIntegration.mockResolvedValue(GATEWAY_INTEGRATION);
        mockListIngestedApis.mockResolvedValue(ingestedApisPage(apis));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        await screen.findByText(apis[0].name);
        const ingestedApisTable = dataTableHarness({ within: screen.getByTestId('integration-overview-page') });
        expect(ingestedApisTable.getRows().map(row => [row.getCellText('Name'), row.getCellText('Version')])).toEqual(
            apis.map(({ name, version }) => [name, version]),
        );
    });

    it('opens an ingested API in the APIM module of the environment taken from the page path, not from the API response', async () => {
        const ordersApiFromAnotherEnvironment = { ...ORDERS_API, environmentId: 'env-b' };
        mockGetIntegration.mockResolvedValue(GATEWAY_INTEGRATION);
        mockListIngestedApis.mockResolvedValue(ingestedApisPage([ordersApiFromAnotherEnvironment]));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id, '/environments/env-a/platform');

        fireEvent.click(await screen.findByRole('link', { name: ORDERS_API.name }));
        expect(screen.getByTestId('location').textContent).toBe('/environments/env-a/apim/apis/api-1');
    });

    it('replaces the APIs being ingested heading with the No APIs created heading once the status poll no longer reports a pending job', async () => {
        jest.useFakeTimers();
        mockGetIntegration
            .mockResolvedValueOnce({ ...GATEWAY_INTEGRATION, pendingJob: PENDING_INGESTION_JOB })
            .mockResolvedValue(GATEWAY_INTEGRATION);

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByRole('heading', { name: 'APIs are being ingested' })).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        expect(await screen.findByRole('heading', { name: 'No APIs created' })).toBeInTheDocument();
        expect(screen.queryByRole('heading', { name: 'APIs are being ingested' })).toBeNull();
    });

    it('lists an API ingested by a job that ends while the overview is open, without a reload', async () => {
        jest.useFakeTimers();
        mockGetIntegration
            .mockResolvedValueOnce({ ...GATEWAY_INTEGRATION, pendingJob: PENDING_INGESTION_JOB })
            .mockResolvedValue(GATEWAY_INTEGRATION);
        mockListIngestedApis.mockResolvedValueOnce(NO_INGESTED_APIS).mockResolvedValue(ingestedApisPage([ORDERS_API]));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByRole('heading', { name: 'APIs are being ingested' })).toBeInTheDocument();
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        expect(await screen.findByText(ORDERS_API.name)).toBeInTheDocument();
    });

    it('shows neither empty-state heading nor the ingestion banner while the ingested APIs are loading', async () => {
        mockGetIntegration.mockResolvedValue({ ...GATEWAY_INTEGRATION, pendingJob: PENDING_INGESTION_JOB });
        mockListIngestedApis.mockReturnValue(new Promise(() => {}));

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByRole('heading', { name: GATEWAY_INTEGRATION.name })).toBeInTheDocument();
        await waitFor(() => expect(mockListIngestedApis).toHaveBeenCalled());
        expect(screen.queryByRole('heading', { name: 'APIs are being ingested' })).toBeNull();
        expect(screen.queryByRole('heading', { name: 'No APIs created' })).toBeNull();
        expect(screen.queryByTestId('integration-ingestion-in-progress')).toBeNull();
        expect(screen.queryByText('Ingested APIs could not be loaded. Please refresh and try again.')).toBeNull();
    });

    it('lists the next page of ingested APIs when the user moves to the next page', async () => {
        mockGetIntegration.mockResolvedValue(GATEWAY_INTEGRATION);
        serveOrdersApiOnFirstPageAndPaymentsApiOnLaterPages();

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByText('Orders API')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));

        expect(await screen.findByText('Payments API')).toBeInTheDocument();
        expect(mockListIngestedApis).toHaveBeenCalledWith('env-1', GATEWAY_INTEGRATION.id, { page: 2, perPage: 10 });
        expect(screen.queryByText('Orders API')).toBeNull();
    });

    it('returns to the first page of ingested APIs when the user changes the page size', async () => {
        mockGetIntegration.mockResolvedValue(GATEWAY_INTEGRATION);
        serveOrdersApiOnFirstPageAndPaymentsApiOnLaterPages();

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByText('Orders API')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
        expect(await screen.findByText('Payments API')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', { name: 'Items per page' }));
        await userEvent.click(screen.getByRole('option', { name: '25' }));

        await waitFor(() => expect(mockListIngestedApis).toHaveBeenCalledWith('env-1', GATEWAY_INTEGRATION.id, { page: 1, perPage: 25 }));
        expect(mockListIngestedApis).not.toHaveBeenCalledWith('env-1', GATEWAY_INTEGRATION.id, { page: 2, perPage: 25 });
    });

    it('shows the integration name and no Discover APIs action to a user without the environment integration create permission', async () => {
        mockGetIntegration.mockResolvedValue({ ...GATEWAY_INTEGRATION, agentStatus: 'CONNECTED' });

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByRole('heading', { name: GATEWAY_INTEGRATION.name })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Discover APIs' })).toBeNull();
    });

    it('opens the discovery preview of the integration when a user allowed to create integrations clicks Discover APIs on a connected integration', async () => {
        grantIntegrationCreatePermission();
        mockGetIntegration.mockResolvedValue({ ...GATEWAY_INTEGRATION, agentStatus: 'CONNECTED' });

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        await userEvent.click(await screen.findByRole('button', { name: 'Discover APIs' }));

        expect(screen.getByTestId('location').textContent).toBe('/integrations/int-1/discover');
    });

    it('enables Discover APIs, without a reload, once the status poll no longer reports a pending ingestion job for a connected integration', async () => {
        jest.useFakeTimers();
        grantIntegrationCreatePermission();
        const connectedIntegration = { ...GATEWAY_INTEGRATION, agentStatus: 'CONNECTED' as const };
        mockGetIntegration
            .mockResolvedValueOnce({ ...connectedIntegration, pendingJob: PENDING_INGESTION_JOB })
            .mockResolvedValue(connectedIntegration);

        renderIntegrationOverviewPage(GATEWAY_INTEGRATION.id);

        expect(await screen.findByRole('button', { name: 'Discover APIs' })).toHaveProperty('disabled', true);
        await act(() => jest.advanceTimersByTimeAsync(5_000));
        expect(screen.getByRole('button', { name: 'Discover APIs' })).toHaveProperty('disabled', false);
    });
});
