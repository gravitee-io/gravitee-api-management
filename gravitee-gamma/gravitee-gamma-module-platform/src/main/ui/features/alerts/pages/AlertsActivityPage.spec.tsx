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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { AlertsActivityPage } from './AlertsActivityPage';
import { getPlatformAlertAnalytics } from '../services/alerts';
import type { AlertAnalytics } from '../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../services/alerts', () => ({
    getPlatformAlertAnalytics: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);
const mockGetPlatformAlertAnalytics = jest.mocked(getPlatformAlertAnalytics);

const ANALYTICS: AlertAnalytics = {
    bySeverity: { INFO: 114, WARNING: 0, CRITICAL: 0 },
    alerts: [
        { id: 'alert-1', name: 'Blackrock-okta-alert', severity: 'INFO', type: 'METRICS_SIMPLE_CONDITION', events_count: 51 },
        { id: 'alert-2', name: 'BlackRock-IDP-1-Down', severity: 'CRITICAL', type: 'NODE_HEALTHCHECK', events_count: 51 },
    ],
};

const NOW = 1_700_000_000_000;

function renderPage() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/alerts/activity']}>
                <Routes>
                    <Route path="alerts">
                        <Route path="activity" element={<AlertsActivityPage />} />
                        <Route path=":alertId" element={<div />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('AlertsActivityPage', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
    });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(Date, 'now').mockReturnValue(NOW);
        mockUseEnvironment.mockReturnValue({ id: 'env-1' } as ReturnType<typeof useEnvironment>);
        mockGetPlatformAlertAnalytics.mockResolvedValue(ANALYTICS);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('loads analytics for the last minute by default and shows summary cards and table', async () => {
        renderPage();

        await waitFor(() => expect(mockGetPlatformAlertAnalytics).toHaveBeenCalledWith('env-1', NOW - 60_000, NOW));
        expect(await screen.findByText('Total Alerts')).not.toBeNull();

        expect(screen.getByRole('heading', { name: 'Alerts board' })).not.toBeNull();
        expect(screen.getByText('All alert events in last minute')).not.toBeNull();
        expect(screen.getByText('Total Alerts').parentElement?.textContent).toContain('114');
        expect(screen.getByText('Info').parentElement?.textContent).toContain('114');
        expect(screen.getByText('Warning').parentElement?.textContent).toContain('0');
        expect(screen.getByText('Critical').parentElement?.textContent).toContain('0');
        expect(screen.getByText('Blackrock-okta-alert')).not.toBeNull();
        expect(screen.getByText('info')).not.toBeNull();
        expect(screen.getByText('critical')).not.toBeNull();
        expect(screen.getAllByText('51')).toHaveLength(2);
    });

    it('refetches analytics without replacing the board with a skeleton', async () => {
        const user = userEvent.setup();
        let resolveRefresh: (value: AlertAnalytics) => void = () => undefined;
        mockGetPlatformAlertAnalytics.mockResolvedValueOnce(ANALYTICS).mockImplementationOnce(
            () =>
                new Promise(resolve => {
                    resolveRefresh = resolve;
                }),
        );

        renderPage();
        expect(await screen.findByText('Blackrock-okta-alert')).not.toBeNull();

        jest.mocked(Date.now).mockReturnValue(NOW + 5_000);
        await user.click(screen.getByRole('button', { name: /^refresh$/i }));

        expect(screen.getByText('Blackrock-okta-alert')).not.toBeNull();
        expect(screen.getByText('Total Alerts')).not.toBeNull();

        resolveRefresh(ANALYTICS);
        await waitFor(() => expect(mockGetPlatformAlertAnalytics).toHaveBeenCalledWith('env-1', NOW + 5_000 - 60_000, NOW + 5_000));
    });

    it('refetches analytics for the last hour when the time range changes', async () => {
        const user = userEvent.setup();
        renderPage();
        await waitFor(() => expect(mockGetPlatformAlertAnalytics).toHaveBeenCalledTimes(1));
        expect(await screen.findByText('Blackrock-okta-alert')).not.toBeNull();

        await user.click(screen.getByRole('combobox', { name: 'Quick time range' }));
        await user.click(await screen.findByRole('option', { name: 'Last hour' }));

        await waitFor(() => expect(mockGetPlatformAlertAnalytics).toHaveBeenCalledWith('env-1', NOW - 3_600_000, NOW));
        expect(screen.getByText('All alert events in last hour')).not.toBeNull();
    });

    it('keeps the board visible when the time range changes', async () => {
        const user = userEvent.setup();
        let resolveHour: (value: AlertAnalytics) => void = () => undefined;
        mockGetPlatformAlertAnalytics.mockResolvedValueOnce(ANALYTICS).mockImplementationOnce(
            () =>
                new Promise(resolve => {
                    resolveHour = resolve;
                }),
        );

        renderPage();
        expect(await screen.findByText('Blackrock-okta-alert')).not.toBeNull();

        await user.click(screen.getByRole('combobox', { name: 'Quick time range' }));
        await user.click(await screen.findByRole('option', { name: 'Last hour' }));

        expect(screen.getByText('Blackrock-okta-alert')).not.toBeNull();
        expect(screen.getByText('Total Alerts')).not.toBeNull();

        resolveHour({ ...ANALYTICS, bySeverity: { INFO: 3, WARNING: 0, CRITICAL: 0 } });
        await waitFor(() => expect(screen.getByText('Total Alerts').parentElement?.textContent).toContain('3'));
    });

    it('links View history to the alert history tab with a unique accessible name', async () => {
        renderPage();
        await waitFor(() => expect(screen.getByText('Blackrock-okta-alert')).not.toBeNull());

        const historyLink = screen.getByRole('link', { name: 'View history for Blackrock-okta-alert' });
        expect(historyLink.getAttribute('href')).toBe('/alerts/alert-1?tab=history');
        expect(screen.getByRole('link', { name: 'View history for BlackRock-IDP-1-Down' })).not.toBeNull();
    });

    it('shows an empty message when there are no events in the window', async () => {
        mockGetPlatformAlertAnalytics.mockResolvedValue({ bySeverity: {}, alerts: [] });
        renderPage();

        await waitFor(() => expect(screen.getByText('No alert events')).not.toBeNull());
    });

    it('shows an error when analytics fail to load', async () => {
        mockGetPlatformAlertAnalytics.mockRejectedValue(new Error('boom'));
        renderPage();

        await waitFor(() => expect(screen.getByText(/failed to load alert activity/i)).not.toBeNull());
        expect(screen.queryByText('Total Alerts')).toBeNull();
        expect(screen.queryByText('Blackrock-okta-alert')).toBeNull();
    });

    it('keeps the board and shows an error banner when refresh of the current range fails', async () => {
        const user = userEvent.setup();
        mockGetPlatformAlertAnalytics.mockResolvedValueOnce(ANALYTICS).mockRejectedValueOnce(new Error('boom'));

        renderPage();
        expect(await screen.findByText('Blackrock-okta-alert')).not.toBeNull();

        await user.click(screen.getByRole('button', { name: /^refresh$/i }));

        expect(await screen.findByText(/failed to load alert activity/i)).not.toBeNull();
        expect(screen.getByText('Blackrock-okta-alert')).not.toBeNull();
        expect(screen.getByText('Total Alerts')).not.toBeNull();
    });

    it('shows a full error card when a new time range fails to load', async () => {
        const user = userEvent.setup();
        mockGetPlatformAlertAnalytics.mockResolvedValueOnce(ANALYTICS).mockRejectedValueOnce(new Error('boom'));

        renderPage();
        expect(await screen.findByText('Blackrock-okta-alert')).not.toBeNull();

        await user.click(screen.getByRole('combobox', { name: 'Quick time range' }));
        await user.click(await screen.findByRole('option', { name: 'Last hour' }));

        expect(await screen.findByText(/failed to load alert activity/i)).not.toBeNull();
        expect(screen.queryByText('Blackrock-okta-alert')).toBeNull();
        expect(screen.queryByText('Total Alerts')).toBeNull();
    });
});
