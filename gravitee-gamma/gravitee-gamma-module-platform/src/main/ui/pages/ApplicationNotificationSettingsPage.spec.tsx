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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApplicationNotificationSettingsPage } from './ApplicationNotificationSettingsPage';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationNotificationPermissions } from '../features/applications/hooks/useApplicationNotificationPermissions';
import {
    useApplicationMetadata,
    useApplicationNotifications,
    useCreateApplicationMetadata,
    useCreateApplicationNotification,
    useDeleteApplicationMetadata,
    useDeleteApplicationNotification,
    useUpdateApplicationMetadata,
    useUpdateApplicationNotification,
} from '../features/applications/hooks/useApplicationNotifications';
import type { ApplicationNotifier } from '../features/applications/types/applicationNotification';
import { notify } from '../shared/notify';

jest.mock('../features/applications/context/ApplicationDetailContext');
jest.mock('../features/applications/hooks/useApplicationNotificationPermissions');
jest.mock('../features/applications/hooks/useApplicationNotifications');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));
jest.mock('../features/applications/components/notifications/NotificationHookCategorySection', () => ({
    NotificationHookCategorySection: () => null,
}));
jest.mock('../features/applications/components/metadata/ApplicationMetadataSection', () => ({
    ApplicationMetadataSection: () => null,
}));

const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);
const mockUseApplicationNotificationPermissions = jest.mocked(useApplicationNotificationPermissions);
const mockUseApplicationNotifications = jest.mocked(useApplicationNotifications);
const mockUseApplicationMetadata = jest.mocked(useApplicationMetadata);
const mockUseCreateApplicationNotification = jest.mocked(useCreateApplicationNotification);
const mockUseUpdateApplicationNotification = jest.mocked(useUpdateApplicationNotification);
const mockUseDeleteApplicationNotification = jest.mocked(useDeleteApplicationNotification);
const mockUseCreateApplicationMetadata = jest.mocked(useCreateApplicationMetadata);
const mockUseUpdateApplicationMetadata = jest.mocked(useUpdateApplicationMetadata);
const mockUseDeleteApplicationMetadata = jest.mocked(useDeleteApplicationMetadata);

const NOTIFIERS: ApplicationNotifier[] = [{ id: 'default-email', type: 'EMAIL', name: 'Default Email Notifier' }];

function makeMutation(mutateAsync = jest.fn().mockResolvedValue({})) {
    return { mutate: jest.fn(), mutateAsync, isPending: false, error: null };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/applications/app-1/notifications']}>
            <Routes>
                <Route path="/applications/:applicationId/notifications" element={<ApplicationNotificationSettingsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApplicationNotificationSettingsPage create follow-up', () => {
    beforeEach(() => {
        mockUseApplicationDetailContext.mockReturnValue({
            application: { id: 'app-1', name: 'Billing', origin: 'MANAGEMENT' },
            isLoading: false,
            permissionsReady: true,
            refetchPermissions: jest.fn(),
        } as ReturnType<typeof useApplicationDetailContext>);
        mockUseApplicationNotificationPermissions.mockReturnValue({
            permissionsReady: true,
            canCreateNotification: true,
            canUpdateNotification: true,
            canDeleteNotification: true,
            canCreateMetadata: false,
            canUpdateMetadata: false,
            canDeleteMetadata: false,
        });
        mockUseApplicationNotifications.mockReturnValue({
            rows: [],
            notifiers: NOTIFIERS,
            hookCategories: [],
            isLoading: false,
            isLoadingHooks: false,
            isError: false,
            error: null,
        } as ReturnType<typeof useApplicationNotifications>);
        mockUseApplicationMetadata.mockReturnValue({ data: [], isLoading: false, isError: false } as ReturnType<
            typeof useApplicationMetadata
        >);
        mockUseCreateApplicationNotification.mockReturnValue(makeMutation() as never);
        mockUseUpdateApplicationNotification.mockReturnValue(makeMutation() as never);
        mockUseDeleteApplicationNotification.mockReturnValue(makeMutation() as never);
        mockUseCreateApplicationMetadata.mockReturnValue(makeMutation() as never);
        mockUseUpdateApplicationMetadata.mockReturnValue(makeMutation() as never);
        mockUseDeleteApplicationMetadata.mockReturnValue(makeMutation() as never);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('tells the operator the row exists when POST succeeds but the follow-up config PUT fails', async () => {
        const createAsync = jest.fn().mockResolvedValue({ id: 'new-1', name: 'Ops email', config_type: 'GENERIC' });
        const updateAsync = jest.fn().mockRejectedValue(new Error('network'));
        mockUseCreateApplicationNotification.mockReturnValue(makeMutation(createAsync) as never);
        mockUseUpdateApplicationNotification.mockReturnValue(makeMutation(updateAsync) as never);
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: /Add notification/i }));
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Ops email' } });
        fireEvent.change(screen.getByLabelText(/^Email list/), { target: { value: 'ops@example.com' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add notification' }));

        await waitFor(() => {
            expect(createAsync).toHaveBeenCalled();
            expect(updateAsync).toHaveBeenCalled();
            expect(notify.error).toHaveBeenCalledWith(
                expect.any(Error),
                '"Ops email" was created, but saving its configuration failed — edit it to finish setup.',
            );
            expect(notify.success).not.toHaveBeenCalled();
        });
    });
});
