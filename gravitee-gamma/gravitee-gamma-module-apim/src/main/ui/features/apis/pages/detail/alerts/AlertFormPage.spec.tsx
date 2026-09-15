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
import { createAlertTrigger, getAlertStatus, listAlerts } from '../../../services/alerts';
import type { AlertTrigger } from '../../../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false, isFetching: false };
        return { data: config.queryFnResult, isLoading: false, isError: false, isFetching: false };
    }),
    useQueries: jest.fn(() => []),
    useMutation: jest.fn(config => ({
        mutate: jest.fn(async args => {
            await config.mutationFn(args);
            config.onSuccess?.();
        }),
        isPending: false,
    })),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../../../services/alerts', () => ({
    listAlerts: jest.fn(() => Promise.resolve([])),
    createAlertTrigger: jest.fn(() => Promise.resolve({ id: 'new-id' })),
    updateAlertTrigger: jest.fn(() => Promise.resolve({ id: 'alert-1' })),
    getAlertStatus: jest.fn(() => Promise.resolve({ available_plugins: 1, enabled: true })),
    getAlertHistory: jest.fn(() => Promise.resolve({ content: [], totalElements: 0 })),
    alertTriggerToFormData: jest.requireActual('../../../services/alerts').alertTriggerToFormData,
}));

jest.mock('../../../services/alertNotifiers', () => ({
    listNotifiers: jest.fn(() => Promise.resolve([])),
    getNotifierSchema: jest.fn(() => Promise.resolve({})),
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseQuery = useQuery as jest.Mock;
const mockListAlerts = listAlerts as jest.Mock;
const mockCreateAlertTrigger = createAlertTrigger as jest.Mock;
const mockGetAlertStatus = getAlertStatus as jest.Mock;

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
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false, isFetching: false };
        if (config.queryKey?.includes('status')) {
            return { data: { available_plugins: 1, enabled: true }, isLoading: false, isError: false, isFetching: false };
        }
        return { data: [alert], isLoading: false, isError: false, isFetching: false };
    });

    render(
        <MemoryRouter initialEntries={['/apis/api-1/alerts/alert-1']}>
            <Routes>
                <Route path="apis/:apiId/alerts/:alertId" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockCreateAlertTrigger.mockResolvedValue({ id: 'new-id' });
    mockGetAlertStatus.mockResolvedValue({ available_plugins: 1, enabled: true });
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false, isFetching: false };
        if (config.queryKey?.includes('status')) {
            return { data: { available_plugins: 1, enabled: true }, isLoading: false, isError: false, isFetching: false };
        }
        return { data: undefined, isLoading: false, isError: false, isFetching: false };
    });
});

async function selectHealthCheckRule(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByRole('combobox', { name: /rule/i }));
    await user.click(
        screen.getByRole('option', {
            name: /alert when the health status of an endpoint has changed/i,
        }),
    );
}

it('renders create form with default name and disabled Create until a rule is selected', () => {
    renderCreatePage();

    expect(screen.getByRole('heading', { name: /create new alert/i })).not.toBeNull();
    expect((screen.getByLabelText(/name/i) as HTMLInputElement).value).toBe('New alert');
    expect(screen.getByText(/select a rule before setting the condition/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /^create$/i })).toBeDisabled();
});

it('keeps Create disabled when no rule is selected', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.clear(screen.getByLabelText(/name/i));
    await user.type(screen.getByLabelText(/name/i), 'My Alert');

    expect(screen.getByRole('button', { name: /^create$/i })).toBeDisabled();
});

it('enables Create after selecting a rule that needs no extra conditions', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectHealthCheckRule(user);

    expect(screen.getByRole('button', { name: /^create$/i })).toBeEnabled();
});

it('calls createAlertTrigger with correct payload when form is valid and submitted', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectHealthCheckRule(user);
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreateAlertTrigger).toHaveBeenCalledTimes(1));

    const [, , sentData] = mockCreateAlertTrigger.mock.calls[0];
    expect(sentData.name).toBe('New alert');
    expect(sentData.source).toBe('ENDPOINT_HEALTH_CHECK');
    expect(sentData.type).toBe('API_HC_ENDPOINT_STATUS_CHANGED');
    expect(sentData.enabled).toBe(false);
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

it('shows History tab in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('tab', { name: /history/i })).not.toBeNull();
});

it('shows plugin banner when no alert plugins are installed', () => {
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) return { data: undefined, isLoading: false, isError: false, isFetching: false };
        if (config.queryKey?.includes('status')) {
            return { data: { available_plugins: 0, enabled: false }, isLoading: false, isError: false, isFetching: false };
        }
        return { data: undefined, isLoading: false, isError: false, isFetching: false };
    });
    renderCreatePage();

    expect(screen.getByText(/no alert plugin is installed/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /^create$/i })).toBeDisabled();
});
