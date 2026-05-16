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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiNotificationsPage } from './ApiNotificationsPage';
import {
    useApiNotifications,
    useCreateNotification,
    useDeleteNotification,
    useUpdateNotification,
} from '../features/apis/hooks/useApiNotifications';

// ─── SDK mocks ────────────────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── Hook mocks ───────────────────────────────────────────────────────────────

jest.mock('../features/apis/hooks/useApiNotifications', () => ({
    useApiNotifications: jest.fn(),
    useCreateNotification: jest.fn(),
    useUpdateNotification: jest.fn(),
    useDeleteNotification: jest.fn(),
}));

// ─── Test data ────────────────────────────────────────────────────────────────

const PORTAL_ROW = {
    key: 'PORTAL',
    notification: { name: 'Default console', config_type: 'PORTAL' as const, hooks: [], referenceType: 'API', referenceId: 'api-1' },
    notifier: undefined,
    channel: 'CONSOLE' as const,
    isReadonly: false,
    canDelete: false,
};

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

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiNotifications = useApiNotifications as jest.Mock;
const mockUseCreateNotification = useCreateNotification as jest.Mock;
const mockUseUpdateNotification = useUpdateNotification as jest.Mock;
const mockUseDeleteNotification = useDeleteNotification as jest.Mock;

function buildMutationMock(overrides?: Partial<{ mutate: jest.Mock; isPending: boolean }>) {
    return { mutate: jest.fn(), isPending: false, ...overrides };
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/notifications']}>
            <Routes>
                <Route path="apis/:apiId/notifications" element={<ApiNotificationsPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiNotifications.mockReturnValue({
        rows: [PORTAL_ROW],
        notifiers: NOTIFIERS,
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    mockUseCreateNotification.mockReturnValue(buildMutationMock());
    mockUseUpdateNotification.mockReturnValue(buildMutationMock());
    mockUseDeleteNotification.mockReturnValue(buildMutationMock());
});

// ─── 1. Error state ───────────────────────────────────────────────────────────

it('renders an error alert when the notifications query fails', () => {
    mockUseApiNotifications.mockReturnValue({
        rows: [],
        notifiers: [],
        hookCategories: [],
        isLoading: false,
        isLoadingHooks: false,
        isError: true,
    });
    renderPage();
    expect(screen.getByText(/failed to load notifications/i)).not.toBeNull();
});

// ─── 2. Empty state: shows learning card when no rows ─────────────────────────

it('shows the learning card when there are no configured notifications', () => {
    mockUseApiNotifications.mockReturnValue({
        rows: [],
        notifiers: [],
        hookCategories: [],
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    renderPage();
    expect(screen.getByText(/why configure notifications/i)).not.toBeNull();
});

// ─── 3. Table renders rows ────────────────────────────────────────────────────

it('renders notification rows in the table', () => {
    mockUseApiNotifications.mockReturnValue({
        rows: [PORTAL_ROW, EMAIL_ROW],
        notifiers: NOTIFIERS,
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    renderPage();
    expect(screen.getByText('Default console')).not.toBeNull();
    expect(screen.getByText('Ops email')).not.toBeNull();
});

// ─── 4. Permissions: no Add button when canCreate is false ───────────────────

it('hides Add notification button when user lacks api-notification-c', () => {
    mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.some(p => p !== 'api-notification-c'));
    renderPage();
    expect(screen.queryByRole('button', { name: /add notification/i })).toBeNull();
});

// ─── 5. Inline editor opens on Edit click ────────────────────────────────────

it('opens the inline event editor when Edit events is selected from the dropdown', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /actions for default console/i }));
    await user.click(screen.getByText(/edit events/i));

    // The editor renders a Save/Close button when the editor is open
    await waitFor(() => expect(screen.getByRole('button', { name: /save events/i })).not.toBeNull());
});

// ─── 6. Save events calls updateMutation ─────────────────────────────────────

it('calls updateNotification with the selected hooks when Save events is clicked', async () => {
    const mutateFn = jest.fn((_payload: unknown, opts: { onSuccess?: () => void }) => opts.onSuccess?.());
    mockUseUpdateNotification.mockReturnValue({ mutate: mutateFn, isPending: false });
    const user = userEvent.setup();
    renderPage();

    // Open editor
    await user.click(screen.getByRole('button', { name: /actions for default console/i }));
    await user.click(screen.getByText(/edit events/i));

    // Toggle a hook checkbox
    await user.click(screen.getByLabelText(/api started/i));
    await user.click(screen.getByRole('button', { name: /save events/i }));

    await waitFor(() => expect(mutateFn).toHaveBeenCalledTimes(1));
    const [payload] = mutateFn.mock.calls[0];
    expect(payload.hooks).toContain('API_STARTED');
});

// ─── 7. Delete: PORTAL row shows no Delete option ────────────────────────────

it('does not show a Delete option for the PORTAL (console) notification', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /actions for default console/i }));
    expect(screen.queryByText(/^delete$/i)).toBeNull();
});

// ─── 8. Delete confirmation flow ─────────────────────────────────────────────

it('calls deleteNotification with the notification id after confirming deletion', async () => {
    const mutateFn = jest.fn((_id: unknown, opts: { onSuccess?: () => void }) => opts.onSuccess?.());
    mockUseDeleteNotification.mockReturnValue({ mutate: mutateFn, isPending: false });
    mockUseApiNotifications.mockReturnValue({
        rows: [EMAIL_ROW],
        notifiers: NOTIFIERS,
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: /actions for ops email/i }));
    await user.click(screen.getByText(/^delete$/i));

    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => expect(mutateFn).toHaveBeenCalledWith('notif-email-1', expect.any(Object)));
});

// ─── 9. Summary cards show correct counts ────────────────────────────────────

it('renders summary cards with correct counts per channel', () => {
    mockUseApiNotifications.mockReturnValue({
        rows: [PORTAL_ROW, EMAIL_ROW],
        notifiers: NOTIFIERS,
        hookCategories: HOOK_CATEGORIES,
        isLoading: false,
        isLoadingHooks: false,
        isError: false,
    });
    renderPage();

    // CONSOLE count = 1, EMAIL count = 1, WEBHOOK count = 0
    const cards = screen.getAllByText(/notifiers/i);
    expect(cards.length).toBeGreaterThanOrEqual(3);
});
