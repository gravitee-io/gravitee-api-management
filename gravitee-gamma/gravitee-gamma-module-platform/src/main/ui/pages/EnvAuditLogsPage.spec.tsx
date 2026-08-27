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

import { useEnvironment, useHasFeature } from '@gravitee/gamma-modules-sdk';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { EnvAuditLogsPage } from './EnvAuditLogsPage';
import { useAuditApis, useAuditApplications, useAuditEvents, useAuditLogs } from '../features/audit-logs/hooks/useAuditLogs';
import type { AuditEntity, AuditMetadataPage } from '../features/audit-logs/types/auditLog';
import { ApimApiError } from '../shared/api/apimClient';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasFeature: jest.fn(),
    useEnvironment: jest.fn(),
    permissionService: { load: jest.fn() },
}));

jest.mock('../features/audit-logs/hooks/useAuditLogs');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseHasFeature = jest.mocked(useHasFeature);
const mockUseEnvironment = jest.mocked(useEnvironment);
const mockUseAuditLogs = jest.mocked(useAuditLogs);
const mockUseAuditEvents = jest.mocked(useAuditEvents);
const mockUseAuditApplications = jest.mocked(useAuditApplications);
const mockUseAuditApis = jest.mocked(useAuditApis);

const PAGE: AuditMetadataPage = {
    content: [
        {
            id: 'a-1',
            referenceId: 'app-1',
            referenceType: 'APPLICATION',
            user: 'user-1',
            createdAt: 1_700_000_000_000,
            event: 'APPLICATION_UPDATED',
            properties: {},
            patch: '',
        } satisfies AuditEntity,
    ],
    pageNumber: 1,
    pageElements: 1,
    totalElements: 1,
    metadata: { 'USER:user-1:name': 'Grace Hopper', 'APPLICATION:app-1:name': 'Portal' },
};

function renderPage(permissions: string[] = ['environment-audit-r']) {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'prod'], permissions);
    const view = render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/environment-audit']}>
                <EnvAuditLogsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { ...view, queryClient };
}

describe('EnvAuditLogsPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseHasFeature.mockReturnValue(true);
        mockUseEnvironment.mockReturnValue({ id: 'prod' } as ReturnType<typeof useEnvironment>);
        mockUseAuditLogs.mockReturnValue({ data: PAGE, isLoading: false, isError: false, error: null } as ReturnType<typeof useAuditLogs>);
        mockUseAuditEvents.mockReturnValue({ data: ['APPLICATION_UPDATED'], isLoading: false } as ReturnType<typeof useAuditEvents>);
        mockUseAuditApplications.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useAuditApplications>);
        mockUseAuditApis.mockReturnValue({ data: [], isLoading: false } as ReturnType<typeof useAuditApis>);
    });

    it('loads environment-scoped audits for the current environment', () => {
        renderPage();
        expect(mockUseAuditLogs).toHaveBeenCalledWith('environment', expect.anything(), 'prod', true);
        expect(screen.getByText('Grace Hopper')).not.toBeNull();
        expect(screen.getByText('APPLICATION_UPDATED')).not.toBeNull();
        expect(screen.getByText('this environment', { exact: false })).not.toBeNull();
    });

    it('shows the license dialog when apim-audit-trail is missing', () => {
        mockUseHasFeature.mockReturnValue(false);
        renderPage();
        expect(screen.getByText(/part of Gravitee Enterprise/i)).not.toBeNull();
        expect(mockUseAuditLogs).toHaveBeenCalledWith('environment', expect.anything(), 'prod', false);
    });

    it('on 403 strips only environment-audit permissions so Organization Audit stays available', async () => {
        mockUseAuditLogs.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new ApimApiError(403, 'Forbidden'),
        } as ReturnType<typeof useAuditLogs>);

        const { queryClient } = renderPage(['organization-audit-r', 'environment-audit-r']);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
        expect(queryClient.getQueryData(['environment-permissions', 'prod'])).toEqual(['organization-audit-r']);
    });
});
