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
import { render, screen } from '@testing-library/react';

import { EnvironmentNotificationsTable } from './EnvironmentNotificationsTable';
import type { ApplicationNotificationRow } from '../../applications/types/applicationNotification';

function buildRow(overrides: Partial<ApplicationNotificationRow> = {}): ApplicationNotificationRow {
    return {
        key: 'n-1',
        name: 'Email alerts',
        subscribedEvents: 2,
        notifierName: 'Default Email Notifier',
        notification: {
            id: 'n-1',
            name: 'Email alerts',
            referenceType: 'ENVIRONMENT',
            referenceId: 'env-1',
            notifier: 'default-email',
            config_type: 'GENERIC',
            config: 'ops@example.com',
            hooks: ['USER_REGISTERED', 'USER_CREATED'],
        },
        notifier: { id: 'default-email', type: 'EMAIL' },
        isReadonly: false,
        ...overrides,
    };
}

const PORTAL_ROW = buildRow({
    key: 'PORTAL',
    name: 'Console Notification',
    subscribedEvents: 0,
    notifierName: 'Console',
    notification: {
        name: 'Console Notification',
        referenceType: 'ENVIRONMENT',
        referenceId: 'env-1',
        config_type: 'PORTAL',
        hooks: [],
    },
    notifier: undefined,
});

describe('EnvironmentNotificationsTable', () => {
    const baseProps = {
        isLoading: false,
        isError: false,
        canDelete: true,
        onEdit: jest.fn(),
        onDelete: jest.fn(),
    };

    it('renders the Configured notifications heading and column headers', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[PORTAL_ROW]} canUpdate={() => true} />);

        expect(screen.getByText('Configured notifications')).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Name' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Channel' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Events' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Target' })).not.toBeNull();
        expect(screen.getByRole('columnheader', { name: 'Actions' })).not.toBeNull();
    });

    it('shows Graphene channel and event badges, and an em dash target on the Console row', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[PORTAL_ROW]} canUpdate={() => true} />);

        expect(screen.getByText('Console')).not.toBeNull();
        expect(screen.getByText('None')).not.toBeNull();
        expect(screen.getByText('—')).not.toBeNull();
    });

    it('shows the Email channel badge, event count, and config target on GENERIC rows', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[buildRow()]} canUpdate={() => true} />);

        expect(screen.getByText('Email')).not.toBeNull();
        expect(screen.getByText('2 events')).not.toBeNull();
        expect(screen.getByText('ops@example.com')).not.toBeNull();
    });

    it('puts row actions behind a three-dot menu, not inline edit/delete icons', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[PORTAL_ROW, buildRow()]} canUpdate={() => true} />);

        expect(screen.getByLabelText('Actions for Console Notification')).not.toBeNull();
        expect(screen.getByLabelText('Actions for Email alerts')).not.toBeNull();
        expect(screen.queryByLabelText('Edit Console Notification notification')).toBeNull();
        expect(screen.queryByLabelText('Delete Email alerts notification')).toBeNull();
    });

    it('omits the actions menu on the Console row when the caller can only delete', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[PORTAL_ROW, buildRow()]} canUpdate={() => false} canDelete={true} />);

        expect(screen.queryByLabelText('Actions for Console Notification')).toBeNull();
        expect(screen.getByLabelText('Actions for Email alerts')).not.toBeNull();
    });

    it('hides the three-dot menu on GENERIC rows the caller cannot edit or delete', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[buildRow()]} canUpdate={() => false} canDelete={false} />);

        expect(screen.queryByLabelText('Actions for Email alerts')).toBeNull();
    });

    it('omits the Actions column when every row is Kubernetes-origin readonly', () => {
        render(
            <EnvironmentNotificationsTable
                {...baseProps}
                rows={[buildRow({ isReadonly: true })]}
                canUpdate={() => true}
                canDelete={true}
            />,
        );

        expect(screen.queryByRole('columnheader', { name: 'Actions' })).toBeNull();
        expect(screen.queryByLabelText('Actions for Email alerts')).toBeNull();
    });

    it('shows an empty state when there are no configured notifications', () => {
        render(<EnvironmentNotificationsTable {...baseProps} rows={[]} canUpdate={() => true} />);

        expect(screen.getByText('No notifications configured.')).not.toBeNull();
    });
});
