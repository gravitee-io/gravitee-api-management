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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiNotificationFormPage } from './ApiNotificationFormPage';
import { useApiNotifications, useCreateNotification, useUpdateNotification } from '../../../hooks/useApiNotifications';

// ─── Mocks ────────────────────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../hooks/useApiNotifications', () => ({
    useApiNotifications: jest.fn(),
    useCreateNotification: jest.fn(),
    useUpdateNotification: jest.fn(),
}));

// ─── Test data ────────────────────────────────────────────────────────────────

const EMAIL_ROW = {
    key: 'notif-email-1',
    notification: {
        id: 'notif-email-1',
        name: 'Ops email',
        config_type: 'GENERIC' as const,
        notifier: 'email-notifier',
        hooks: ['API_STARTED'],
        config: 'ops@example.com',
        referenceType: 'API',
        referenceId: 'api-1',
    },
    notifier: { id: 'email-notifier', type: 'EMAIL', name: 'Default email' },
    channel: 'EMAIL' as const,
    isReadonly: false,
    canDelete: true,
};

const NOTIFIERS = [{ id: 'email-notifier', type: 'EMAIL', name: 'Default email' }];

const HOOK_CATEGORIES = [
    {
        name: 'Lifecycle',
        hooks: [
            { id: 'API_STARTED', label: 'API started', description: '', scope: 'API', category: 'Lifecycle' },
            { id: 'API_STOPPED', label: 'API stopped', description: '', scope: 'API', category: 'Lifecycle' },
        ],
    },
];

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiNotifications = useApiNotifications as jest.Mock;
const mockUseCreateNotification = useCreateNotification as jest.Mock;
const mockUseUpdateNotification = useUpdateNotification as jest.Mock;

function renderForm(path: string) {
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="apis/:apiId/notifications">
                    <Route index element={<div data-testid="list">list</div>} />
                    <Route path="new" element={<ApiNotificationFormPage />} />
                    <Route path=":notificationKey" element={<ApiNotificationFormPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiNotifications.mockReturnValue({
        rows: [EMAIL_ROW],
        notifiers: NOTIFIERS,
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    mockUseCreateNotification.mockReturnValue({ mutate: jest.fn(), isPending: false });
    mockUseUpdateNotification.mockReturnValue({ mutate: jest.fn(), isPending: false });
});

// ─── Add: create then update (events + target) in one save ───────────────────

it('creates a GENERIC notification then persists the selected events and target on save', async () => {
    const created = {
        id: 'created-1',
        name: 'Ops hook',
        notifier: 'email-notifier',
        config_type: 'GENERIC' as const,
        hooks: [] as string[],
        referenceType: 'API',
        referenceId: 'api-1',
    };
    const createMutate = jest.fn((_payload: unknown, opts: { onSuccess?: (c: typeof created) => void }) => opts.onSuccess?.(created));
    const updateMutate = jest.fn((_payload: unknown, opts: { onSuccess?: () => void }) => opts.onSuccess?.());
    mockUseCreateNotification.mockReturnValue({ mutate: createMutate, isPending: false });
    mockUseUpdateNotification.mockReturnValue({ mutate: updateMutate, isPending: false });

    const user = userEvent.setup();
    renderForm('/apis/api-1/notifications/new');

    await user.type(screen.getByLabelText(/name/i), 'Ops hook');
    await user.type(screen.getByLabelText(/email address/i), 'team@example.com');
    await user.click(screen.getByLabelText(/api started/i));
    await user.click(screen.getByRole('button', { name: /add notification/i }));

    await waitFor(() => expect(createMutate).toHaveBeenCalledTimes(1));
    expect(createMutate.mock.calls[0][0]).toEqual({
        name: 'Ops hook',
        notifier: 'email-notifier',
        config_type: 'GENERIC',
        hooks: [],
        referenceType: 'API',
        referenceId: 'api-1',
    });

    await waitFor(() => expect(updateMutate).toHaveBeenCalledTimes(1));
    const updatePayload = updateMutate.mock.calls[0][0] as { hooks: string[]; config: string };
    expect(updatePayload.hooks).toContain('API_STARTED');
    expect(updatePayload.config).toBe('team@example.com');

    // Redirects back to the list on success
    await waitFor(() => expect(screen.getByTestId('list')).toBeInTheDocument());
});

// ─── Edit: update with selected hooks + target ───────────────────────────────

it('updates the notification with the edited events on save', async () => {
    const updateMutate = jest.fn((_payload: unknown, opts: { onSuccess?: () => void }) => opts.onSuccess?.());
    mockUseUpdateNotification.mockReturnValue({ mutate: updateMutate, isPending: false });

    const user = userEvent.setup();
    renderForm('/apis/api-1/notifications/notif-email-1');

    // Pre-filled from the existing notification
    expect((screen.getByLabelText(/email address/i) as HTMLInputElement).value).toBe('ops@example.com');

    // Add a second event and save
    await user.click(screen.getByLabelText(/api stopped/i));
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(updateMutate).toHaveBeenCalledTimes(1));
    const payload = updateMutate.mock.calls[0][0] as { hooks: string[]; config: string };
    expect(payload.hooks).toEqual(expect.arrayContaining(['API_STARTED', 'API_STOPPED']));
    expect(payload.config).toBe('ops@example.com');
});

// ─── Webhook: useSystemProxy is sent on save ─────────────────────────────────

it('includes useSystemProxy in the update payload for a webhook notification', async () => {
    const created = {
        id: 'created-2',
        name: 'Ops webhook',
        notifier: 'webhook-notifier',
        config_type: 'GENERIC' as const,
        hooks: [] as string[],
        referenceType: 'API',
        referenceId: 'api-1',
    };
    const createMutate = jest.fn((_payload: unknown, opts: { onSuccess?: (c: typeof created) => void }) => opts.onSuccess?.(created));
    const updateMutate = jest.fn((_payload: unknown, opts: { onSuccess?: () => void }) => opts.onSuccess?.());
    mockUseApiNotifications.mockReturnValue({
        rows: [],
        notifiers: [{ id: 'webhook-notifier', type: 'WEBHOOK', name: 'Default webhook' }],
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    mockUseCreateNotification.mockReturnValue({ mutate: createMutate, isPending: false });
    mockUseUpdateNotification.mockReturnValue({ mutate: updateMutate, isPending: false });

    const user = userEvent.setup();
    renderForm('/apis/api-1/notifications/new');

    await user.type(screen.getByLabelText(/name/i), 'Ops webhook');
    await user.type(screen.getByLabelText(/webhook url/i), 'https://hooks.example.com');
    await user.click(screen.getByRole('switch', { name: /use system proxy/i }));
    await user.click(screen.getByRole('button', { name: /add notification/i }));

    await waitFor(() => expect(updateMutate).toHaveBeenCalledTimes(1));
    expect((updateMutate.mock.calls[0][0] as { useSystemProxy: boolean }).useSystemProxy).toBe(true);
});

// ─── Guard: lacking permission redirects to the list ─────────────────────────

it('redirects to the list when the user lacks the create permission', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderForm('/apis/api-1/notifications/new');
    expect(screen.getByTestId('list')).toBeInTheDocument();
});
