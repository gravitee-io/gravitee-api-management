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
import { useQuery } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { AlertFormPage } from './AlertFormPage';
import { createAlertTrigger, listAlerts, updateAlertTrigger } from '../../../services/alerts';
import type { AlertTrigger } from '../../../types/api';

// ─── SDK / context mocks ──────────────────────────────────────────────────────

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

// ─── React Query mock ─────────────────────────────────────────────────────────

jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false };
        return { data: config.queryFnResult, isLoading: false, isError: false };
    }),
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            await config.mutationFn(args);
            config.onSuccess?.();
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

// ─── Service mocks ────────────────────────────────────────────────────────────

jest.mock('../../../services/alerts', () => ({
    listAlerts: jest.fn(() => Promise.resolve([])),
    createAlertTrigger: jest.fn(() => Promise.resolve({ id: 'new-id' })),
    updateAlertTrigger: jest.fn(() => Promise.resolve({ id: 'alert-1' })),
    alertTriggerToFormData: jest.requireActual('../../../services/alerts').alertTriggerToFormData,
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseQuery = useQuery as jest.Mock;
const mockListAlerts = listAlerts as jest.Mock;
const mockCreateAlertTrigger = createAlertTrigger as jest.Mock;
const mockUpdateAlertTrigger = updateAlertTrigger as jest.Mock;

const EXISTING_ALERT: AlertTrigger = {
    id: 'alert-1',
    name: 'High Response Time',
    description: 'Alert on slow responses',
    severity: 'WARNING',
    enabled: true,
    source: 'REQUEST',
    type: 'METRICS_SIMPLE_CONDITION',
    conditions: [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 500 }],
    filters: [],
    notifications: [],
    notificationPeriods: [],
    dampening: { mode: 'STRICT_COUNT', trueEvaluations: 1 },
};

function renderCreatePage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/alerts/new']}>
            <Routes>
                <Route path="apis/:apiId/alerts/new" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function renderEditPage(alert: AlertTrigger = EXISTING_ALERT) {
    mockListAlerts.mockResolvedValue([alert]);
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false };
        return { data: [alert], isLoading: false, isError: false };
    });

    render(
        <MemoryRouter initialEntries={['/apis/api-1/alerts/alert-1']}>
            <Routes>
                <Route path="apis/:apiId/alerts/:alertId" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockCreateAlertTrigger.mockResolvedValue({ id: 'new-id' });
    mockUpdateAlertTrigger.mockResolvedValue({ id: 'alert-1' });
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false };
        return { data: undefined, isLoading: false, isError: false };
    });
});

// ─── 1. Create mode renders empty form with Create button ────────────────────

it('renders create form with name field and Create button', () => {
    renderCreatePage();

    expect(screen.getByRole('heading', { name: /create new alert/i })).not.toBeNull();
    expect(screen.getByLabelText(/name/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /^create$/i })).not.toBeNull();
});

// ─── 2. Validation: name required ────────────────────────────────────────────

it('shows name validation error when saving without a name', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByRole('button', { name: /^create$/i }));

    expect(screen.getByText(/name is required/i)).not.toBeNull();
});

// ─── 3. Validation: name too short ───────────────────────────────────────────

it('shows validation error when name is too short', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.type(screen.getByLabelText(/name/i), 'AB');
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    expect(screen.getByText(/at least 3 characters/i)).not.toBeNull();
});

// ─── 4. Create: calls createAlertTrigger with correct payload ────────────────

it('calls createAlertTrigger with correct payload when form is valid and submitted', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.type(screen.getByLabelText(/name/i), 'My Alert');
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreateAlertTrigger).toHaveBeenCalledTimes(1));

    const [, , sentData] = mockCreateAlertTrigger.mock.calls[0];
    expect(sentData.name).toBe('My Alert');
    expect(sentData.source).toBe('REQUEST');
    expect(sentData.type).toBe('METRICS_SIMPLE_CONDITION');
});

// ─── 5. Edit mode: loads existing alert into form ────────────────────────────

it('populates form with existing alert data in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('heading', { name: /update alert/i })).not.toBeNull();
    expect((screen.getByLabelText(/name/i) as HTMLInputElement).value).toBe('High Response Time');
});

// ─── 6. Permission: read-only user cannot see Create/Save buttons ─────────────

it('hides Create button for read-only users', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderCreatePage();

    expect(screen.queryByRole('button', { name: /^create$/i })).toBeNull();
});

// ─── 7. Tabs: Notifications tab is accessible ────────────────────────────────

it('renders Notifications and Alerts tabs', () => {
    renderCreatePage();

    expect(screen.getByRole('tab', { name: /alerts/i })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /notifications/i })).not.toBeNull();
});

// ─── 8. Edit mode shows History tab ──────────────────────────────────────────

it('shows History tab in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('tab', { name: /history/i })).not.toBeNull();
});
