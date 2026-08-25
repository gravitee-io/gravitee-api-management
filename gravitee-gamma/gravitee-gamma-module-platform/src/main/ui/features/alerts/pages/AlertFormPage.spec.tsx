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
import { useConsoleSettings } from '../../../shared/console-settings';
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

jest.mock('../../../shared/console-settings', () => ({
    useConsoleSettings: jest.fn(() => ({})),
}));

jest.mock('../hooks/useAlertLookupOptions', () => ({
    useAlertLookupOptions: () => ({ tenants: [], apis: [] }),
}));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

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
    useQueries: jest.fn(({ queries = [] }: { queries?: unknown[] }) =>
        queries.map(() => ({ data: undefined, isLoading: false, isFetching: false, isError: false })),
    ),
    useQueryClient: jest.fn(() => ({ invalidateQueries: jest.fn() })),
}));

jest.mock('../services/alerts', () => ({
    listPlatformAlerts: jest.fn(() => Promise.resolve([])),
    listPlatformAlertEvents: jest.fn(() => Promise.resolve({ content: [], totalElements: 0 })),
    createPlatformAlert: jest.fn(() => Promise.resolve({ id: 'new-id', name: 'My Alert' })),
    updatePlatformAlertFromForm: jest.fn(() => Promise.resolve({ id: 'alert-1', name: 'High Response Time' })),
    associatePlatformAlert: jest.fn(() => Promise.resolve()),
    alertTriggerToFormData: jest.requireActual('../services/alerts').alertTriggerToFormData,
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseQuery = useQuery as jest.Mock;
const mockListPlatformAlerts = listPlatformAlerts as jest.Mock;
const mockCreatePlatformAlert = createPlatformAlert as jest.Mock;
const mockUpdatePlatformAlertFromForm = updatePlatformAlertFromForm as jest.Mock;
const mockUseConsoleSettings = useConsoleSettings as jest.Mock;

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

async function selectRequestSimpleRule(user: ReturnType<typeof userEvent.setup>) {
    await user.click(screen.getByLabelText(/^rule/i));
    await user.click(await screen.findByRole('option', { name: /metric of the request validates a condition/i }));
}

async function clearAndTypeName(user: ReturnType<typeof userEvent.setup>, name: string) {
    await user.clear(screen.getByLabelText(/name/i));
    await user.type(screen.getByLabelText(/name/i), name);
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseConsoleSettings.mockReturnValue({});
    mockCreatePlatformAlert.mockResolvedValue({ id: 'new-id', name: 'My Alert' });
    mockUpdatePlatformAlertFromForm.mockResolvedValue({ id: 'alert-1', name: 'High Response Time' });
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) {
            return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
        }
        return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
    });
});

it('renders create form with Classic name, Enable off, and empty rule', () => {
    renderCreatePage();

    expect(screen.getByRole('heading', { name: /create new alert/i })).not.toBeNull();
    expect((screen.getByLabelText(/name/i) as HTMLInputElement).value).toBe('New alert');
    expect(screen.getByRole('switch', { name: /enable alert/i }).getAttribute('aria-checked')).toBe('false');
    expect(screen.getByText(/select a rule before setting the condition/i)).not.toBeNull();
    expect(screen.queryByText('When')).toBeNull();
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);
});

it('shows Classic template and When after a request rule is selected', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectRequestSimpleRule(user);

    expect(screen.getByLabelText(/define as template/i)).not.toBeNull();
    expect(screen.getByText('When')).not.toBeNull();
    expect(screen.getByText('Condition')).not.toBeNull();
    expect((screen.getByLabelText(/description/i) as HTMLTextAreaElement).value).toMatch(/metric of the request validates/i);
});

it('disables Create when name is empty or too short', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.clear(screen.getByLabelText(/name/i));
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);

    await user.type(screen.getByLabelText(/name/i), 'AB');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);
});

it('disables Create until a rule is selected', () => {
    renderCreatePage();

    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);
    expect(mockCreatePlatformAlert).not.toHaveBeenCalled();
});

it('calls createPlatformAlert with correct payload when form is valid and submitted', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await clearAndTypeName(user, 'My Alert');
    await selectRequestSimpleRule(user);
    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreatePlatformAlert).toHaveBeenCalledTimes(1));

    const [, sentData] = mockCreatePlatformAlert.mock.calls[0];
    expect(sentData.name).toBe('My Alert');
    expect(sentData.source).toBe('REQUEST');
    expect(sentData.type).toBe('METRICS_SIMPLE_CONDITION');
    expect(sentData.enabled).toBe(false);
    expect(sentData.conditions[0].threshold).toBe(500);
    expect(sentData.notifications).toEqual([]);
    expect(sentData.event_rules).toEqual([]);
});

it('POSTs API_CREATE event_rules when auto-create on new APIs is checked', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await clearAndTypeName(user, 'My Alert');
    await selectRequestSimpleRule(user);
    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    await user.click(screen.getByLabelText(/define as template/i));
    await user.click(screen.getByLabelText(/automatically create this alert for every new api/i));
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreatePlatformAlert).toHaveBeenCalledTimes(1));
    expect(mockCreatePlatformAlert.mock.calls[0][1].event_rules).toEqual([{ event: 'API_CREATE' }]);
});

it('does not POST API_CREATE event_rules when the template is not auto-created on new APIs', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await clearAndTypeName(user, 'My Alert');
    await selectRequestSimpleRule(user);
    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    await user.click(screen.getByLabelText(/define as template/i));
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreatePlatformAlert).toHaveBeenCalledTimes(1));
    expect(mockCreatePlatformAlert.mock.calls[0][1].event_rules).toEqual([]);
});

it('disables Create until the condition threshold is filled', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectRequestSimpleRule(user);
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);

    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);
});

it('disables Create after adding an incomplete filter and enables it when the filter is filled', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectRequestSimpleRule(user);
    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);

    await user.click(screen.getByRole('button', { name: /add filter/i }));
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);

    await user.type(screen.getAllByPlaceholderText('e.g. 500')[1]!, '200');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);
});

it('disables Create for aggregation until threshold and duration are filled', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByLabelText(/^rule/i));
    await user.click(await screen.findByRole('option', { name: /aggregated value of a request metric/i }));
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);

    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);

    await user.type(screen.getByPlaceholderText('e.g. 1'), '1');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);
});

it('sends Classic hidden STRING conditions when creating a node lifecycle alert', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByLabelText(/^rule/i));
    await user.click(await screen.findByRole('option', { name: /lifecycle status of a node has changed/i }));
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);
    await user.click(screen.getByRole('button', { name: /^create$/i }));

    await waitFor(() => expect(mockCreatePlatformAlert).toHaveBeenCalledTimes(1));
    const [, sentData] = mockCreatePlatformAlert.mock.calls[0];
    expect(sentData.source).toBe('NODE_LIFECYCLE');
    expect(sentData.conditions).toEqual([
        {
            type: 'STRING',
            operator: 'MATCHES',
            property: 'node.event',
            pattern: 'NODE_START|NODE_STOP',
        },
    ]);
});

it('hides Node rules when the organization is cloud-hosted', async () => {
    mockUseConsoleSettings.mockReturnValue({ cloudHosted: { enabled: true } });
    const user = userEvent.setup();
    renderCreatePage();

    await user.click(screen.getByLabelText(/^rule/i));

    expect(screen.queryByRole('option', { name: /lifecycle status of a node/i })).toBeNull();
    expect(screen.getByRole('option', { name: /metric of the request validates/i })).not.toBeNull();
});

it('disables Create after a notification is added until a channel and required schema fields are set', async () => {
    const user = userEvent.setup();
    renderCreatePage();

    await selectRequestSimpleRule(user);
    await user.type(screen.getByPlaceholderText('e.g. 500'), '500');
    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', false);

    await user.click(screen.getByRole('tab', { name: /notifications/i }));
    await user.click(screen.getByRole('button', { name: /^add$/i }));

    expect(screen.getByRole('button', { name: /^create$/i })).toHaveProperty('disabled', true);
    expect(screen.getByText(/select a channel for each notification/i)).not.toBeNull();
    expect(mockCreatePlatformAlert).not.toHaveBeenCalled();
});

it('populates form with existing alert data in edit mode', () => {
    renderEditPage();

    expect(screen.getByRole('heading', { name: /update alert/i })).not.toBeNull();
    expect((screen.getByLabelText(/name/i) as HTMLInputElement).value).toBe('High Response Time');
});

it('keeps classic group-by projections when saving an existing alert', async () => {
    const user = userEvent.setup();
    const grouped = [{ type: 'PROPERTY', property: 'api' }];
    renderEditPage({
        ...EXISTING_ALERT,
        projections: grouped,
        conditions: [{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 500, projections: grouped }],
    });

    await user.type(screen.getByLabelText(/name/i), '!');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(mockUpdatePlatformAlertFromForm).toHaveBeenCalledTimes(1));
    const [, , sentData, preserved] = mockUpdatePlatformAlertFromForm.mock.calls[0];
    expect(preserved.projections).toEqual(grouped);
    expect(sentData.conditions[0].projections).toEqual(grouped);
});

it('stays on the Alerts tab in edit mode after save instead of returning to the list', async () => {
    const user = userEvent.setup();
    renderEditPage();

    await user.type(screen.getByLabelText(/name/i), '!');
    await user.click(screen.getByRole('tab', { name: /notifications/i }));
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(mockUpdatePlatformAlertFromForm).toHaveBeenCalledTimes(1));
    expect(screen.getByRole('heading', { name: /update alert/i })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /alerts/i }).getAttribute('aria-selected')).toBe('true');
    expect(screen.queryByRole('button', { name: /^save$/i })).toBeNull();
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

it('shows a not-found state for an unknown alert id', () => {
    mockUseQuery.mockImplementation(config => {
        if (config.enabled === false) {
            return { data: undefined, isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
        }
        return { data: [], isLoading: false, isError: false, isFetching: false, refetch: jest.fn() };
    });

    render(
        <MemoryRouter initialEntries={['/alerts/missing-id']}>
            <Routes>
                <Route path="alerts/:alertId" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /alert not found/i })).not.toBeNull();
    expect(screen.queryByRole('heading', { name: /update alert/i })).toBeNull();
});

it('does not allow saving a template alert opened by id', () => {
    renderEditPage({ ...EXISTING_ALERT, template: true, event_rules: [{ event: 'API_CREATE' }] });

    expect(screen.getByRole('heading', { name: /update alert/i })).not.toBeNull();
    expect(screen.queryByRole('button', { name: /^save$/i })).toBeNull();
    expect(screen.queryByRole('tab', { name: /history/i })).toBeNull();
    expect(screen.getByRole('button', { name: /associate the alert to existing apis/i })).not.toBeNull();
});

it('keeps the original source and type when the rule is unrecognized', async () => {
    const user = userEvent.setup();
    renderEditPage({ ...EXISTING_ALERT, source: 'CUSTOM', type: 'UNKNOWN_TYPE' });

    await user.type(screen.getByLabelText(/name/i), '!');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(mockUpdatePlatformAlertFromForm).toHaveBeenCalledTimes(1));
    const [, , sentData] = mockUpdatePlatformAlertFromForm.mock.calls[0];
    expect(sentData.source).toBe('CUSTOM');
    expect(sentData.type).toBe('UNKNOWN_TYPE');
});

it('falls back to the Alerts tab when creating with ?tab=history', () => {
    render(
        <MemoryRouter initialEntries={['/alerts/new?tab=history']}>
            <Routes>
                <Route path="alerts/new" element={<AlertFormPage />} />
            </Routes>
        </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /create new alert/i })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /alerts/i }).getAttribute('aria-selected')).toBe('true');
    expect(screen.queryByRole('tab', { name: /history/i })).toBeNull();
});

it('keeps Save enabled when notifier schema fetch failed', async () => {
    const user = userEvent.setup();
    const { useQueries } = jest.requireMock('@tanstack/react-query') as { useQueries: jest.Mock };
    useQueries.mockImplementation(({ queries = [] }: { queries?: unknown[] }) =>
        queries.map(() => ({ data: undefined, isLoading: false, isFetching: false, isError: true })),
    );

    renderEditPage({
        ...EXISTING_ALERT,
        notifications: [{ type: 'webhook-notifier', configuration: { url: 'https://example.com' } }],
    });

    await user.type(screen.getByLabelText(/name/i), '!');
    expect(screen.getByRole('button', { name: /^save$/i })).toHaveProperty('disabled', false);
});

it('shows failed-to-load when notification configuration JSON is invalid', () => {
    renderEditPage({
        ...EXISTING_ALERT,
        notifications: [{ type: 'webhook-notifier', configuration: 'not-json' }],
    });

    expect(screen.getByText(/failed to load this alert/i)).not.toBeNull();
    expect(screen.queryByRole('heading', { name: /update alert/i })).toBeNull();
});
