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

import { useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { OrgAuditLogsPage } from './OrgAuditLogsPage';
import {
    listAuditEnvironments,
    listOrgAuditApisByEnvironment,
    listOrgAuditApplicationsByEnvironment,
    listOrgAuditEvents,
    searchOrgAudits,
} from '../features/audit-logs/services/auditLogs';
import type { AuditMetadataPage } from '../features/audit-logs/types/auditLog';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
}));

jest.mock('../features/audit-logs/services/auditLogs', () => ({
    ...jest.requireActual('../features/audit-logs/services/auditLogs'),
    searchOrgAudits: jest.fn(),
    listOrgAuditEvents: jest.fn(),
    listAuditEnvironments: jest.fn(),
    listOrgAuditApplicationsByEnvironment: jest.fn(),
    listOrgAuditApisByEnvironment: jest.fn(),
}));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockSearchOrgAudits = jest.mocked(searchOrgAudits);
const mockListOrgAuditEvents = jest.mocked(listOrgAuditEvents);
const mockListAuditEnvironments = jest.mocked(listAuditEnvironments);
const mockListOrgAuditApplications = jest.mocked(listOrgAuditApplicationsByEnvironment);
const mockListOrgAuditApis = jest.mocked(listOrgAuditApisByEnvironment);

const PAGE: AuditMetadataPage = {
    content: [],
    pageNumber: 1,
    pageElements: 0,
    totalElements: 0,
    metadata: {},
};

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(['environment-permissions', 'env-1'], ['organization-audit-r']);
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/organization-audit']}>
                <OrgAuditLogsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('OrgAuditLogsPage time filter', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasFeature.mockReturnValue(true);
        mockSearchOrgAudits.mockResolvedValue(PAGE);
        mockListOrgAuditEvents.mockResolvedValue([]);
        mockListAuditEnvironments.mockResolvedValue([]);
        mockListOrgAuditApplications.mockResolvedValue([]);
        mockListOrgAuditApis.mockResolvedValue([]);
    });

    it('sends from/to on /audit when a relative time preset is selected', async () => {
        renderPage();

        await waitFor(() => expect(mockSearchOrgAudits).toHaveBeenCalled());
        expect(mockSearchOrgAudits.mock.calls[0][0].from).toBeUndefined();
        expect(mockSearchOrgAudits.mock.calls[0][0].to).toBeUndefined();

        fireEvent.click(screen.getByLabelText('Filter by time period'));
        fireEvent.click(screen.getByRole('option', { name: 'Last 24 hours' }));

        await waitFor(() => {
            const last = mockSearchOrgAudits.mock.calls.at(-1)?.[0];
            expect(last?.from).toEqual(expect.any(Number));
            expect(last?.to).toEqual(expect.any(Number));
            expect(last!.to! - last!.from!).toBe(24 * 60 * 60 * 1000);
        });
    });

    it('sends a 7-day from/to window when Last 7 days is selected', async () => {
        renderPage();
        await waitFor(() => expect(mockSearchOrgAudits).toHaveBeenCalled());

        fireEvent.click(screen.getByLabelText('Filter by time period'));
        fireEvent.click(screen.getByRole('option', { name: 'Last 7 days' }));

        await waitFor(() => {
            const last = mockSearchOrgAudits.mock.calls.at(-1)?.[0];
            expect(last!.to! - last!.from!).toBe(7 * 24 * 60 * 60 * 1000);
        });
    });
});
