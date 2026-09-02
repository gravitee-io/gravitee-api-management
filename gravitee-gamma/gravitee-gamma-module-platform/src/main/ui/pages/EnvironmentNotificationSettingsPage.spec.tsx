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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { EnvironmentNotificationSettingsPage } from './EnvironmentNotificationSettingsPage';
import type { ApplicationNotificationRow, ApplicationNotifier } from '../features/applications/types/applicationNotification';
import { useEnvironmentNotificationPermissions } from '../features/environment-notifications/hooks/useEnvironmentNotificationPermissions';
import {
    useCreateEnvironmentNotification,
    useDeleteEnvironmentNotification,
    useEnvironmentNotifications,
    useUpdateEnvironmentNotification,
} from '../features/environment-notifications/hooks/useEnvironmentNotifications';
import { ApimApiError } from '../shared/api/apimClient';
import { notify } from '../shared/notify';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: () => ({ id: 'env-1' }),
    permissionService: { load: jest.fn() },
}));

jest.mock('../features/environment-notifications/hooks/useEnvironmentNotifications');
jest.mock('../features/environment-notifications/hooks/useEnvironmentNotificationPermissions');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// The hook checkbox rendering is exercised by NotificationHookCategorySection's own tests.
jest.mock('../features/applications/components/notifications/NotificationHookCategorySection', () => ({
    NotificationHookCategorySection: () => null,
}));

jest.mock('../features/environment-notifications/components/EnvironmentNotificationsTable', () => ({
    EnvironmentNotificationsTable: ({
        rows,
        canUpdate,
        canDelete,
        onEdit,
        onDelete,
    }: {
        rows: ApplicationNotificationRow[];
        canUpdate: (row: ApplicationNotificationRow) => boolean;
        canDelete: boolean;
        onEdit: (row: ApplicationNotificationRow) => void;
        onDelete: (row: ApplicationNotificationRow) => void;
    }) => (
        <div>
            {rows.map(row => (
                <div key={row.key}>
                    <span>{row.name}</span>
                    {canUpdate(row) ? (
                        <button type="button" onClick={() => onEdit(row)}>
                            Edit {row.name} notification
                        </button>
                    ) : null}
                    {canDelete && row.notification.config_type !== 'PORTAL' && row.notification.id ? (
                        <button type="button" onClick={() => onDelete(row)}>
                            Delete {row.name} notification
                        </button>
                    ) : null}
                </div>
            ))}
        </div>
    ),
}));

const mockUseEnvironmentNotifications = jest.mocked(useEnvironmentNotifications);
const mockUseCreateEnvironmentNotification = jest.mocked(useCreateEnvironmentNotification);
const mockUseUpdateEnvironmentNotification = jest.mocked(useUpdateEnvironmentNotification);
const mockUseDeleteEnvironmentNotification = jest.mocked(useDeleteEnvironmentNotification);
const mockUseEnvironmentNotificationPermissions = jest.mocked(useEnvironmentNotificationPermissions);

const NOTIFIERS: ApplicationNotifier[] = [
    { id: 'default-email', type: 'EMAIL', name: 'Default Email Notifier' },
    { id: 'default-webhook', type: 'WEBHOOK', name: 'Default Webhook Notifier' },
];

const PORTAL_ROW: ApplicationNotificationRow = {
    key: 'PORTAL',
    name: 'Console Notification',
    subscribedEvents: 1,
    notifierName: 'Console',
    notification: {
        name: 'Console Notification',
        referenceType: 'ENVIRONMENT',
        referenceId: 'env-1',
        config_type: 'PORTAL',
        hooks: ['USER_REGISTERED'],
    },
    notifier: undefined,
    isReadonly: false,
};

const GENERIC_ROW: ApplicationNotificationRow = {
    key: 'n-1',
    name: 'Email alerts',
    subscribedEvents: 1,
    notifierName: 'Default Email Notifier',
    notification: {
        id: 'n-1',
        name: 'Email alerts',
        referenceType: 'ENVIRONMENT',
        referenceId: 'env-1',
        notifier: 'default-email',
        config_type: 'GENERIC',
        config: 'ops@example.com',
        hooks: ['USER_REGISTERED'],
    },
    notifier: NOTIFIERS[0],
    isReadonly: false,
};

function makeNotificationsResult(overrides: Partial<ReturnType<typeof useEnvironmentNotifications>> = {}) {
    return {
        rows: [PORTAL_ROW, GENERIC_ROW],
        notifiers: NOTIFIERS,
        hookCategories: [],
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
        error: null,
        ...overrides,
    } as ReturnType<typeof useEnvironmentNotifications>;
}

function makePermissions(overrides: Partial<ReturnType<typeof useEnvironmentNotificationPermissions>> = {}) {
    return {
        canCreate: true,
        canUpdateGeneric: true,
        canUpdatePortal: true,
        canDelete: true,
        ...overrides,
    };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn().mockResolvedValue({})): any {
    return { mutate: jest.fn((_vars, opts) => opts?.onSuccess?.({})), mutateAsync, isPending: false };
}

function renderPage(seedPermissions: string[] = []) {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['environment-permissions', 'env-1'], seedPermissions);
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    const result = render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/notification-settings']}>
                <EnvironmentNotificationSettingsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { ...result, queryClient, invalidateSpy };
}

describe('EnvironmentNotificationSettingsPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
        mockUseEnvironmentNotifications.mockReturnValue(makeNotificationsResult());
        mockUseEnvironmentNotificationPermissions.mockReturnValue(makePermissions());
        mockUseCreateEnvironmentNotification.mockReturnValue(makeMutation());
        mockUseUpdateEnvironmentNotification.mockReturnValue(makeMutation());
        mockUseDeleteEnvironmentNotification.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the page title', () => {
        renderPage();
        expect(screen.queryByRole('heading', { name: 'Notification settings' })).not.toBeNull();
    });

    it('renders both the Console Notification row and GENERIC rows', () => {
        renderPage();
        expect(screen.queryByText('Console Notification')).not.toBeNull();
        expect(screen.queryByText('Email alerts')).not.toBeNull();
    });

    it('redirects away and strips notification permissions from the cache on a 403', async () => {
        mockUseEnvironmentNotifications.mockReturnValue(
            makeNotificationsResult({ rows: [], isError: true, error: new ApimApiError(403, 'Forbidden') }),
        );

        const { queryClient, invalidateSpy } = renderPage([
            'environment-notification-r',
            'environment-notification-c',
            'environment-notification-u',
            'environment-notification-d',
        ]);

        await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('../applications', { replace: true }));
        expect(queryClient.getQueryData(['environment-permissions', 'env-1'])).toEqual([]);
        expect(invalidateSpy).not.toHaveBeenCalled();
    });

    describe('edit permission split (Console Notification vs GENERIC)', () => {
        it('shows edit on the Console Notification row even without update permission (read-only)', () => {
            mockUseEnvironmentNotificationPermissions.mockReturnValue(makePermissions({ canUpdateGeneric: false, canUpdatePortal: true }));
            renderPage();

            expect(screen.queryByRole('button', { name: 'Edit Console Notification notification' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Edit Email alerts notification' })).toBeNull();
        });

        it('hides edit on the Console Notification row when the caller lacks even read-derived portal permission', () => {
            mockUseEnvironmentNotificationPermissions.mockReturnValue(makePermissions({ canUpdatePortal: false, canUpdateGeneric: true }));
            renderPage();

            expect(screen.queryByRole('button', { name: 'Edit Console Notification notification' })).toBeNull();
            expect(screen.queryByRole('button', { name: 'Edit Email alerts notification' })).not.toBeNull();
        });
    });

    describe('create', () => {
        it('shows Add notification when the user can create and notifiers are configurable', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: /Add notification/i })).not.toBeNull();
        });

        it('hides Add notification when the user cannot create', () => {
            mockUseEnvironmentNotificationPermissions.mockReturnValue(makePermissions({ canCreate: false }));
            renderPage();
            expect(screen.queryByRole('button', { name: /Add notification/i })).toBeNull();
        });

        it('POSTs a GENERIC ENVIRONMENT notification on create', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({ id: 'new-1', name: 'New webhook', config_type: 'GENERIC' });
            mockUseCreateEnvironmentNotification.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /Add notification/i }));
            fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'New webhook' } });
            fireEvent.click(screen.getByRole('button', { name: 'Add notification' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith(
                    expect.objectContaining({
                        name: 'New webhook',
                        referenceType: 'ENVIRONMENT',
                        referenceId: 'env-1',
                        config_type: 'GENERIC',
                    }),
                );
                expect(notify.success).toHaveBeenCalledWith('Notification created successfully');
            });
        });

        it('tells the operator the row exists when POST succeeds but the follow-up config PUT fails', async () => {
            const createAsync = jest.fn().mockResolvedValue({ id: 'new-1', name: 'Ops email', config_type: 'GENERIC' });
            const updateAsync = jest.fn().mockRejectedValue(new Error('network'));
            mockUseCreateEnvironmentNotification.mockReturnValue(makeMutation(createAsync));
            mockUseUpdateEnvironmentNotification.mockReturnValue(makeMutation(updateAsync));
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
            expect(screen.queryByLabelText(/^Name/)).toBeNull();
        });
    });

    describe('delete', () => {
        it('shows delete on GENERIC rows and hides it on the Console Notification row', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: 'Delete Email alerts notification' })).not.toBeNull();
            expect(screen.queryByRole('button', { name: 'Delete Console Notification notification' })).toBeNull();
        });

        it('hides delete on GENERIC rows when the user lacks delete permission', () => {
            mockUseEnvironmentNotificationPermissions.mockReturnValue(makePermissions({ canDelete: false }));
            renderPage();
            expect(screen.queryByRole('button', { name: 'Delete Email alerts notification' })).toBeNull();
        });

        it('calls delete and shows success toast on confirm', async () => {
            const mutate = jest.fn((_id, opts) => opts?.onSuccess?.());
            mockUseDeleteEnvironmentNotification.mockReturnValue({ mutate, isPending: false } as never);
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: 'Delete Email alerts notification' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(mutate).toHaveBeenCalledWith('n-1', expect.anything());
                expect(notify.success).toHaveBeenCalledWith('"Email alerts" has been deleted');
            });
        });
    });
});
