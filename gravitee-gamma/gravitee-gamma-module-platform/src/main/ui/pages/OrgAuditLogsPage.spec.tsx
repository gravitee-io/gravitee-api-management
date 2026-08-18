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
    useAuditEnvironments,
    useAuditEvents,
    useAuditLogs,
    useOrgAuditApis,
    useOrgAuditApplications,
} from '../features/audit-logs/hooks/useAuditLogs';
import { exportOrgAudits } from '../features/audit-logs/services/auditLogs';
import type { AuditEntity, AuditMetadataPage } from '../features/audit-logs/types/auditLog';
import { notify } from '../shared/notify';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
}));

jest.mock('../features/audit-logs/hooks/useAuditLogs');
jest.mock('../features/audit-logs/services/auditLogs', () => ({
    ...jest.requireActual('../features/audit-logs/services/auditLogs'),
    exportOrgAudits: jest.fn(),
}));
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));
jest.mock('../features/audit-logs/utils/auditExport', () => ({
    ...jest.requireActual('../features/audit-logs/utils/auditExport'),
    downloadAuditExport: jest.fn(),
}));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockUseAuditLogs = jest.mocked(useAuditLogs);
const mockUseAuditEvents = jest.mocked(useAuditEvents);
const mockUseAuditEnvironments = jest.mocked(useAuditEnvironments);
const mockUseOrgAuditApplications = jest.mocked(useOrgAuditApplications);
const mockUseOrgAuditApis = jest.mocked(useOrgAuditApis);
const mockExportOrgAudits = jest.mocked(exportOrgAudits);

const AUDIT: AuditEntity = {
    id: 'a-1',
    referenceId: 'api-1',
    referenceType: 'API',
    user: 'user-1',
    createdAt: 1_700_000_000_000,
    event: 'API_UPDATED',
    properties: { API: 'api-1' },
    patch: '[]',
};

const PAGE: AuditMetadataPage = {
    content: [AUDIT],
    pageNumber: 1,
    pageElements: 1,
    totalElements: 1,
    metadata: { 'USER:user-1:name': 'Ada Lovelace', 'API:api-1:name': 'Pets' },
};

function renderPage() {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], ['organization-audit-r']);
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/organization-audit']}>
                <OrgAuditLogsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('OrgAuditLogsPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasFeature.mockReturnValue(true);
        mockUseAuditLogs.mockReturnValue({ data: PAGE, isLoading: false, isError: false, error: null } as ReturnType<typeof useAuditLogs>);
        mockUseAuditEvents.mockReturnValue({ data: ['API_UPDATED'], isLoading: false } as ReturnType<typeof useAuditEvents>);
        mockUseAuditEnvironments.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useAuditEnvironments>);
        mockUseOrgAuditApplications.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useOrgAuditApplications>);
        mockUseOrgAuditApis.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useOrgAuditApis>);
        mockExportOrgAudits.mockResolvedValue(PAGE);
        jest.mocked(notify.success).mockClear();
        jest.mocked(notify.error).mockClear();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('renders organization audit rows', () => {
        renderPage();
        expect(screen.getByRole('heading', { name: 'Audit' })).not.toBeNull();
        expect(screen.getByText('Ada Lovelace')).not.toBeNull();
        expect(screen.getByText('API_UPDATED')).not.toBeNull();
    });

    it('shows the license dialog and skips the table when apim-audit-trail is missing', () => {
        mockUseHasFeature.mockReturnValue(false);
        renderPage();
        expect(screen.getByText(/part of Gravitee Enterprise/i)).not.toBeNull();
        expect(screen.queryByText('Ada Lovelace')).toBeNull();
        expect(mockUseAuditLogs).toHaveBeenCalledWith('organization', expect.anything(), undefined, false);
    });

    it('exports filtered results and toasts success', async () => {
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'Export' }));
        fireEvent.click(screen.getByRole('button', { name: 'CSV' }));
        await waitFor(() => expect(mockExportOrgAudits).toHaveBeenCalled());
        expect(notify.success).toHaveBeenCalledWith('Audit logs exported.');
    });

    it('does not change relative time bounds on rerender after a date filter is selected', () => {
        let now = 1_800_000_000_000;
        jest.spyOn(Date, 'now').mockImplementation(() => {
            now += 1_000;
            return now;
        });

        const queryClient = new QueryClient();
        queryClient.setQueryData(['environment-permissions', 'env-1'], ['organization-audit-r']);
        const ui = (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/organization-audit']}>
                    <OrgAuditLogsPage />
                </MemoryRouter>
            </QueryClientProvider>
        );
        const { rerender } = render(ui);

        fireEvent.click(screen.getByLabelText('Filter by time period'));
        fireEvent.click(screen.getByRole('option', { name: 'Last 24 hours' }));

        const afterSelect = mockUseAuditLogs.mock.calls.at(-1)?.[1] as { from?: number; to?: number };
        expect(afterSelect.from).toEqual(expect.any(Number));
        expect(afterSelect.to).toEqual(expect.any(Number));

        rerender(ui);
        rerender(ui);

        const afterRerender = mockUseAuditLogs.mock.calls.at(-1)?.[1] as { from?: number; to?: number };
        expect(afterRerender.from).toBe(afterSelect.from);
        expect(afterRerender.to).toBe(afterSelect.to);
    });
});
