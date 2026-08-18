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
import { createPlatformAlert, listPlatformAlerts, updatePlatformAlertFromForm } from '../services/alerts';
import type { AlertTrigger } from '../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(config => {
        if (config.enabled === false) {
            return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
        }
        return { data: config.queryFnResult, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
    }),
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            const result = await config.mutationFn(args);
            config.onSuccess?.(result);
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../services/alerts', () => ({
    listPlatformAlerts: jest.fn(() => Promise.resolve([])),
    listPlatformAlertEvents: jest.fn(() => Promise.resolve({ content: [], totalElements: 0 })),
    createPlatformAlert: jest.fn(() => Promise.resolve({ id: 'new-id', name: 'My Alert' })),
    updatePlatformAlertFromForm: jest.fn(() => Promise.resolve({ id: 'alert-1', name: 'High Response Time' })),
    alertTriggerToFormData: jest.requireActual('../services/alerts').alertTriggerToFormData,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseQuery = useQuery as jest.Mock;
const mockListPlatformAlerts = listPlatformAlerts as jest.Mock;
const mockCreatePlatformAlert = createPlatformAlert as jest.Mock;
const mockUpdatePlatformAlertFromForm = updatePlatformAlertFromForm as jest.Mock;

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
        <MemoryRouter initialEntries={['/alerts/new']}>
            <Routes>
                <Route path="alerts/new" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function renderEditPage(alert: AlertTrigger = EXISTING_ALERT) {
    mockListPlatformAlerts.mockResolvedValue([alert]);
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) {
            return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
        }
        return { data: [alert], isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
    });

    render(
        <MemoryRouter initialEntries={['/alerts/alert-1']}>
            <Routes>
                <Route path="alerts/:alertId" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockCreatePlatformAlert.mockResolvedValue({ id: 'new-id', name: 'My Alert' });
    mockUpdatePlatformAlertFromForm.mockResolvedValue({ id: 'alert-1', name: 'High Response Time' });
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) {
            return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
        }
        return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
    });
});

it('renders create form with name field and Create button', () => {
    renderCreatePage();

    expect(screen.getByRole('heading', { name: /create new alert/i })).not.toBeNull();
    expect(screen.getByLabelText(/name/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /^create$/i })).not.toBeNull();
});

it('shows name validation error when saving without a name', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByRole('button', { name: /^create$/i }));

    expect(screen.getByText(/name is required/i)).not.toBeNull();
});

it('shows validation error when name is too short', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.type(screen.getByLabelText(/name/i), 'AB');
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    expect(screen.getByText(/at least 3 characters/i)).not.toBeNull();
});

it('calls createPlatformAlert with correct payload when form is valid and submitted', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.type(screen.getByLabelText(/name/i), 'My Alert');
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreatePlatformAlert).toHaveBeenCalledTimes(1));

    const [, sentData] = mockCreatePlatformAlert.mock.calls[0];
    expect(sentData.name).toBe('My Alert');
    expect(sentData.source).toBe('REQUEST');
    expect(sentData.type).toBe('METRICS_SIMPLE_CONDITION');
});

it('populates form with existing alert data in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('heading', { name: /update alert/i })).not.toBeNull();
    expect((screen.getByLabelText(/name/i) as HTMLInputElement).value).toBe('High Response Time');
});

it('hides Create button for read-only users', () => {
    mockUseHasPermission.mockReturnValue(false);
    renderCreatePage();

    expect(screen.queryByRole('button', { name: /^create$/i })).toBeNull();
});

it('renders Notifications and Alerts tabs', () => {
    renderCreatePage();

    expect(screen.getByRole('tab', { name: /alerts/i })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /notifications/i })).not.toBeNull();
});

it('adds a timeframe with start and end times including seconds', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByRole('button', { name: /add timeframe/i }));

    const start = screen.getByLabelText(/start time/i) as HTMLInputElement;
    const end = screen.getByLabelText(/end time/i) as HTMLInputElement;
    expect(start.type).toBe('time');
    expect(start.step).toBe('1');
    expect(start.value).toBe('09:00:00');
    expect(end.value).toBe('18:00:00');
});

it('opens the native time picker when any part of the time field is clicked', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByRole('button', { name: /add timeframe/i }));

    const start = screen.getByLabelText(/start time/i) as HTMLInputElement;
    const showPicker = jest.fn();
    start.showPicker = showPicker;

    await user.click(start);

    expect(showPicker).toHaveBeenCalled();
});

it('shows History tab in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('tab', { name: /history/i })).not.toBeNull();
});
