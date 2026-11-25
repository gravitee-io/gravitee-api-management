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
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApplicationSharedApiKeysPage } from './ApplicationSharedApiKeysPage';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import {
    useApplicationSharedApiKeys,
    useRenewApplicationApiKey,
    useRevokeApplicationApiKey,
} from '../features/applications/hooks/useApplicationSharedApiKeys';
import { useApplicationSubscriptionPermissions } from '../features/applications/hooks/useApplicationSubscriptionPermissions';
import { useEnvironmentPortalConfiguration } from '../features/applications/hooks/useEnvironmentPortalConfiguration';
import { notify } from '../shared/notify';

jest.mock('../features/applications/context/ApplicationDetailContext');
jest.mock('../features/applications/hooks/useApplicationSharedApiKeys');
jest.mock('../features/applications/hooks/useApplicationSubscriptionPermissions');
jest.mock('../features/applications/hooks/useEnvironmentPortalConfiguration');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);
const mockUseApplicationSharedApiKeys = jest.mocked(useApplicationSharedApiKeys);
const mockUseRenewApplicationApiKey = jest.mocked(useRenewApplicationApiKey);
const mockUseRevokeApplicationApiKey = jest.mocked(useRevokeApplicationApiKey);
const mockUseApplicationSubscriptionPermissions = jest.mocked(useApplicationSubscriptionPermissions);
const mockUseEnvironmentPortalConfiguration = jest.mocked(useEnvironmentPortalConfiguration);

const sharedApplication = {
    id: 'app-1',
    name: 'Billing',
    status: 'ACTIVE' as const,
    type: 'SIMPLE' as const,
    api_key_mode: 'SHARED' as const,
    created_at: 0,
    updated_at: 0,
};

const validKey = {
    id: 'key-1',
    key: 'current-key-value',
    maskedKey: '••••••••alue',
    isValid: true,
    createdAt: 1,
};

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/applications/app-1/shared-api-keys']}>
            <Routes>
                <Route path="/applications/:applicationId/shared-api-keys" element={<ApplicationSharedApiKeysPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function givenSharedApplication() {
    mockUseApplicationDetailContext.mockReturnValue({
        application: sharedApplication,
        isLoading: false,
        permissionsReady: true,
        permissionsError: false,
        refetchPermissions: jest.fn(),
    });
    mockUseApplicationSubscriptionPermissions.mockReturnValue({
        permissionsReady: true,
        canRead: true,
        canCreate: false,
        canUpdate: true,
        canDelete: false,
        canViewDetail: true,
    });
    mockUseEnvironmentPortalConfiguration.mockReturnValue({
        data: { plan: { security: { customApiKey: { enabled: true } } } },
        isLoading: false,
        dataUpdatedAt: 1,
        isError: false,
        error: null,
    } as ReturnType<typeof useEnvironmentPortalConfiguration>);
    mockUseApplicationSharedApiKeys.mockReturnValue({
        data: [validKey],
        isLoading: false,
        isError: false,
    } as ReturnType<typeof useApplicationSharedApiKeys>);
    mockUseRenewApplicationApiKey.mockReturnValue({
        mutateAsync: jest.fn().mockResolvedValue(undefined),
        isPending: false,
    } as ReturnType<typeof useRenewApplicationApiKey>);
    mockUseRevokeApplicationApiKey.mockReturnValue({
        mutateAsync: jest.fn().mockResolvedValue(undefined),
        isPending: false,
    } as ReturnType<typeof useRevokeApplicationApiKey>);
}

describe('ApplicationSharedApiKeysPage', () => {
    beforeEach(() => {
        givenSharedApplication();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows an error notification and keeps the dialog open when renewal is rejected', async () => {
        const mutateAsync = jest.fn().mockRejectedValue(new Error('This API key cannot be used'));
        mockUseRenewApplicationApiKey.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as ReturnType<typeof useRenewApplicationApiKey>);

        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByTestId('shared-api-key-renew-value'), { target: { value: 'invalid-key' } });
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('invalid-key'));
        // Flush the rejection handler and the resulting React update before asserting the dialog stays open.
        await new Promise(resolve => setTimeout(resolve, 0));
        expect(notify.error).toHaveBeenCalled();
        expect(screen.getByTestId('shared-api-key-renew-notice')).toBeInTheDocument();
    });

    it('shows a success notification when renewal succeeds', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseRenewApplicationApiKey.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as ReturnType<typeof useRenewApplicationApiKey>);

        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith(undefined));
        expect(notify.success).toHaveBeenCalledWith('API Key renewed');
    });

    it('passes the typed value through to the renew mutation', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseRenewApplicationApiKey.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as ReturnType<typeof useRenewApplicationApiKey>);

        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByTestId('shared-api-key-renew-value'), { target: { value: 'chosen-key' } });
        fireEvent.click(screen.getByTestId('shared-api-key-renew-submit'));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('chosen-key'));
        expect(notify.success).toHaveBeenCalledWith('API Key renewed');
    });

    it('hides Renew and Revoke when the user does not have update permission', () => {
        mockUseApplicationSubscriptionPermissions.mockReturnValue({
            permissionsReady: true,
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canDelete: false,
            canViewDetail: true,
        });

        renderPage();

        expect(screen.queryByRole('button', { name: 'Renew' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Revoke' })).toBeNull();
    });

    it('hides Renew and Revoke for an archived application', () => {
        mockUseApplicationDetailContext.mockReturnValue({
            application: { ...sharedApplication, status: 'ARCHIVED' as const },
            isLoading: false,
            permissionsReady: true,
            permissionsError: false,
            refetchPermissions: jest.fn(),
        });

        renderPage();

        expect(screen.queryByRole('button', { name: 'Renew' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Revoke' })).toBeNull();
    });

    it('hides the custom value field when custom API keys are disabled', () => {
        mockUseEnvironmentPortalConfiguration.mockReturnValue({
            data: { plan: { security: { customApiKey: { enabled: false } } } },
            isLoading: false,
            dataUpdatedAt: 1,
            isError: false,
            error: null,
        } as ReturnType<typeof useEnvironmentPortalConfiguration>);

        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));

        expect(screen.queryByTestId('shared-api-key-renew-value')).toBeNull();
    });

    it('shows an error notification and keeps the revoke dialog open when revocation fails', async () => {
        const mutateAsync = jest.fn().mockRejectedValue(new Error('Forbidden'));
        mockUseRevokeApplicationApiKey.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as ReturnType<typeof useRevokeApplicationApiKey>);

        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Revoke' }));
        const dialog = screen.getByRole('dialog');
        fireEvent.click(within(dialog).getByRole('button', { name: 'Revoke' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('key-1'));
        // Flush the rejection handler and the resulting React update before asserting the dialog stays open.
        await new Promise(resolve => setTimeout(resolve, 0));
        expect(notify.error).toHaveBeenCalled();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
});
