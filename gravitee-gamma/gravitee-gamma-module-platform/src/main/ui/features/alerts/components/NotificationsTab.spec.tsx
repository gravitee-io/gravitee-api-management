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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';

import { NotificationsTab } from './NotificationsTab';
import type { AlertFormNotification } from '../types';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual('@gravitee/graphene-core');
    return {
        ...actual,
        JsonSchemaForm: ({ schema }: { schema: { properties?: Record<string, { title?: string }> } }) => (
            <div>
                {Object.entries(schema.properties ?? {}).map(([key, prop]) => (
                    <label key={key}>
                        {prop.title ?? key}
                        <input aria-label={prop.title ?? key} />
                    </label>
                ))}
            </div>
        ),
    };
});

const DEFAULT_EMAIL_SCHEMA = {
    type: 'object',
    properties: {
        from: { title: 'From', type: 'string' },
        to: { title: 'Recipients', type: 'string' },
        subject: { title: 'Subject', type: 'string' },
        body: { title: 'Body', type: 'string' },
    },
};

const WEBHOOK_SCHEMA = {
    type: 'object',
    properties: {
        method: { title: 'HTTP Method', type: 'string', default: 'POST' },
        url: { title: 'URL', type: 'string' },
    },
    required: ['url', 'method'],
};

jest.mock('@tanstack/react-query', () => ({
    useQuery: jest.fn(),
}));

beforeAll(() => {
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
});

function mockNotifiersQuery(config: { queryKey: unknown[]; enabled?: boolean }) {
    const key = config.queryKey as unknown[];
    if (config.enabled === false) {
        return { data: undefined, isLoading: false, isError: false };
    }
    if (key.includes('notifier-schema')) {
        const notifierId = String(key[key.length - 1]);
        return {
            data: notifierId === 'webhook-notifier' ? WEBHOOK_SCHEMA : DEFAULT_EMAIL_SCHEMA,
            isLoading: false,
            isError: false,
        };
    }
    if (key.includes('notifiers')) {
        return {
            data: [
                { id: 'email-notifier', name: 'E-mail' },
                { id: 'slack-notifier', name: 'Slack' },
                { id: 'default-email', name: 'System e-mail' },
                { id: 'webhook-notifier', name: 'Webhook' },
            ],
            isLoading: false,
            isError: false,
        };
    }
    return { data: undefined, isLoading: false, isError: false };
}

function Harness({ initial }: { initial?: AlertFormNotification[] }) {
    const [notifications, setNotifications] = useState<AlertFormNotification[]>(initial ?? []);
    return (
        <NotificationsTab
            dampening={{ mode: 'STRICT_COUNT', trueEvaluations: 1 }}
            setDampening={() => undefined}
            notifications={notifications}
            addNotification={() => setNotifications(prev => [...prev, { type: '', configuration: {} }])}
            removeNotification={index => setNotifications(prev => prev.filter((_, i) => i !== index))}
            setNotificationType={(index, type) =>
                setNotifications(prev => prev.map((n, i) => (i === index ? { type, configuration: {} } : n)))
            }
            updateNotification={(index, configuration) =>
                setNotifications(prev => prev.map((n, i) => (i === index ? { ...n, configuration } : n)))
            }
            canEdit
            markDirty={() => undefined}
        />
    );
}

describe('NotificationsTab', () => {
    beforeEach(() => {
        const { useQuery } = jest.requireMock('@tanstack/react-query') as { useQuery: jest.Mock };
        useQuery.mockImplementation(mockNotifiersQuery);
    });

    it('shows a Notifications header and Add button without an empty-state placeholder', () => {
        render(<Harness />);

        expect(screen.getByText('Notifications')).not.toBeNull();
        expect(screen.getByText(/via email, Slack, or webhooks/i)).not.toBeNull();
        expect(screen.getByRole('button', { name: /^add$/i })).not.toBeNull();
        expect(screen.queryByText('No data to display.')).toBeNull();
        expect(screen.queryByText(/channel/i)).toBeNull();
    });

    it('adds a notification card with a required channel select', async () => {
        const user = userEvent.setup();
        render(<Harness />);

        await user.click(screen.getByRole('button', { name: /^add$/i }));

        expect(screen.getAllByText(/channel/i).length).toBeGreaterThan(0);
        expect(screen.queryByText('Notification')).toBeNull();
    });

    it('renders system email schema fields from the notifier schema', () => {
        render(<Harness initial={[{ type: 'default-email', configuration: {} }]} />);

        expect(screen.getByLabelText('From')).not.toBeNull();
        expect(screen.getByLabelText('Recipients')).not.toBeNull();
        expect(screen.getByLabelText('Subject')).not.toBeNull();
        expect(screen.getByLabelText('Body')).not.toBeNull();
    });

    it('renders webhook schema fields from the notifier schema', () => {
        render(<Harness initial={[{ type: 'webhook-notifier', configuration: {} }]} />);

        expect(screen.getByLabelText('HTTP Method')).not.toBeNull();
        expect(screen.getByLabelText('URL')).not.toBeNull();
    });

    it('pushes schema defaults into parent configuration without an edit', async () => {
        const updateNotification = jest.fn();
        render(
            <NotificationsTab
                dampening={{ mode: 'STRICT_COUNT', trueEvaluations: 1 }}
                setDampening={() => undefined}
                notifications={[{ type: 'webhook-notifier', configuration: {} }]}
                addNotification={() => undefined}
                removeNotification={() => undefined}
                setNotificationType={() => undefined}
                updateNotification={updateNotification}
                canEdit
                markDirty={() => undefined}
            />,
        );

        await waitFor(() => expect(updateNotification).toHaveBeenCalledWith(0, expect.objectContaining({ method: 'POST' })));
    });

    it('does not invent channel plugins when the notifier list is empty', async () => {
        const { useQuery } = jest.requireMock('@tanstack/react-query') as { useQuery: jest.Mock };
        useQuery.mockImplementation((config: { queryKey: unknown[]; enabled?: boolean }) => {
            const key = config.queryKey as unknown[];
            if (config.enabled === false) {
                return { data: undefined, isLoading: false, isError: false };
            }
            if (key.includes('notifiers')) {
                return { data: [], isLoading: false, isError: false };
            }
            return { data: undefined, isLoading: false, isError: false };
        });

        const user = userEvent.setup();
        render(<Harness />);
        await user.click(screen.getByRole('button', { name: /^add$/i }));
        const channelSelect = screen.getAllByRole('combobox').find(el => el.textContent?.includes('Channel'));
        expect(channelSelect).toBeDefined();
        await user.click(channelSelect!);

        expect(screen.queryByRole('option', { name: 'E-mail' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'System e-mail' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'Slack' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'Webhook' })).toBeNull();
    });

    it('does not invent channel plugins when the notifier list request fails', async () => {
        const { useQuery } = jest.requireMock('@tanstack/react-query') as { useQuery: jest.Mock };
        useQuery.mockImplementation((config: { queryKey: unknown[]; enabled?: boolean }) => {
            const key = config.queryKey as unknown[];
            if (config.enabled === false) {
                return { data: undefined, isLoading: false, isError: false };
            }
            if (key.includes('notifiers')) {
                return { data: undefined, isLoading: false, isError: true };
            }
            return { data: undefined, isLoading: false, isError: false };
        });

        const user = userEvent.setup();
        render(<Harness />);
        await user.click(screen.getByRole('button', { name: /^add$/i }));

        expect(screen.getByText(/failed to load notification channels/i)).not.toBeNull();
        const channelSelect = screen.getAllByRole('combobox').find(el => el.textContent?.includes('Channel'));
        expect(channelSelect).toBeDefined();
        await user.click(channelSelect!);

        expect(screen.queryByRole('option', { name: 'E-mail' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'System e-mail' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'Slack' })).toBeNull();
        expect(screen.queryByRole('option', { name: 'Webhook' })).toBeNull();
    });

    it('shows an error when RELAXED_COUNT total evaluations is below true evaluations', () => {
        render(
            <NotificationsTab
                dampening={{ mode: 'RELAXED_COUNT', trueEvaluations: 5, totalEvaluations: 2 }}
                setDampening={() => undefined}
                notifications={[]}
                addNotification={() => undefined}
                removeNotification={() => undefined}
                setNotificationType={() => undefined}
                updateNotification={() => undefined}
                canEdit
                markDirty={() => undefined}
            />,
        );

        expect(screen.getByText('Number of total evaluations must be at least as high as the number of true evaluations.')).not.toBeNull();
    });
});
