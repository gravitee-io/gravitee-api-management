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
import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import { EditNotificationSheet } from './EditNotificationSheet';
import { buildNewNotificationRow } from './notificationHelpers';
import type { ApplicationNotificationRow, ApplicationNotifier } from '../../types/applicationNotification';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const notifiers: ApplicationNotifier[] = [{ id: 'email-notifier', type: 'EMAIL', name: 'Email' }];

jest.mock('./NotificationHookCategorySection', () => ({
    NotificationHookCategorySection: () => null,
}));

const row: ApplicationNotificationRow = {
    key: 'n1',
    name: 'Subscription alerts',
    subscribedEvents: 1,
    notifierName: 'Email',
    notification: {
        id: 'n1',
        name: 'Subscription alerts',
        referenceType: 'APPLICATION',
        referenceId: 'app-1',
        config_type: 'GENERIC',
        config: 'ops@example.com',
        hooks: ['API_STARTED'],
    },
    notifier: { type: 'EMAIL', name: 'Email' },
    isReadonly: false,
};

function renderSheet(overrides: Partial<Parameters<typeof EditNotificationSheet>[0]> = {}) {
    const onCancel = jest.fn();
    const onSave = jest.fn();
    const onCreate = jest.fn();
    render(
        <EditNotificationSheet
            row={row}
            notifiers={notifiers}
            hookCategories={[]}
            isLoadingHooks={false}
            isSaving={false}
            onCancel={onCancel}
            onSave={onSave}
            onCreate={onCreate}
            {...overrides}
        />,
    );
    return { onCancel, onSave, onCreate };
}

describe('EditNotificationSheet', () => {
    it('does not show sheet content when row is null', () => {
        render(
            <EditNotificationSheet
                row={null}
                notifiers={notifiers}
                hookCategories={[]}
                isLoadingHooks={false}
                isSaving={false}
                onCancel={jest.fn()}
                onSave={jest.fn()}
                onCreate={jest.fn()}
            />,
        );
        expect(querySheetHeading('Edit Console Notification')).toBeNull();
    });

    it('shows sheet title and description when row is set', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Edit Console Notification' })).not.toBeNull();
        expect(screen.getByText(/Configure notifier settings and subscribed events for Subscription alerts/i)).not.toBeNull();
    });

    it('invokes onCancel when Cancel is clicked', () => {
        const { onCancel } = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('links footer Save to the notification form via form attribute', () => {
        renderSheet();
        const save = screen.getByRole('button', { name: 'Save' });
        expect(save.getAttribute('form')).toBe('edit-notification-form');
        expect(save.getAttribute('type')).toBe('submit');
        expect(document.getElementById('edit-notification-form')?.tagName).toBe('FORM');
    });

    it('submits updated config through the linked form', () => {
        const { onSave } = renderSheet();
        fireEvent.change(screen.getByLabelText('Email list'), { target: { value: 'new@example.com' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ config: 'new@example.com' }));
    });

    it('restores server config after cancel and reopen', () => {
        function ControlledSheet() {
            const [editRow, setEditRow] = useState<ApplicationNotificationRow | null>(row);
            return (
                <>
                    <button type="button" onClick={() => setEditRow(row)}>
                        Reopen
                    </button>
                    <EditNotificationSheet
                        row={editRow}
                        notifiers={notifiers}
                        hookCategories={[]}
                        isLoadingHooks={false}
                        isSaving={false}
                        onCancel={() => setEditRow(null)}
                        onSave={jest.fn()}
                        onCreate={jest.fn()}
                    />
                </>
            );
        }

        render(<ControlledSheet />);
        const configInput = screen.getByLabelText('Email list') as HTMLInputElement;

        fireEvent.change(configInput, { target: { value: 'draft@example.com' } });
        expect(configInput.value).toBe('draft@example.com');

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        fireEvent.click(screen.getByRole('button', { name: 'Reopen' }));

        expect((screen.getByLabelText('Email list') as HTMLInputElement).value).toBe('ops@example.com');
    });

    it('shows name and notifier fields in create mode', () => {
        renderSheet({ row: buildNewNotificationRow('app-1', notifiers) });
        expect(screen.getByLabelText(/^Name/)).not.toBeNull();
        expect(screen.getByLabelText(/^Notifier/)).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Add notification' })).not.toBeNull();
    });

    it('submits create payload through onCreate', () => {
        const { onCreate } = renderSheet({ row: buildNewNotificationRow('app-1', notifiers) });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'New alerts' } });
        fireEvent.change(screen.getByLabelText('Email list'), { target: { value: 'ops@example.com' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add notification' }));
        expect(onCreate).toHaveBeenCalledWith(
            expect.objectContaining({
                name: 'New alerts',
                notifier: 'email-notifier',
                config: 'ops@example.com',
            }),
        );
    });

    it('resets create form after cancel and reopen', () => {
        const createRow = buildNewNotificationRow('app-1', notifiers);
        function ControlledSheet() {
            const [editRow, setEditRow] = useState<ApplicationNotificationRow | null>(createRow);
            return (
                <>
                    <button type="button" onClick={() => setEditRow(createRow)}>
                        Reopen
                    </button>
                    <EditNotificationSheet
                        row={editRow}
                        notifiers={notifiers}
                        hookCategories={[]}
                        isLoadingHooks={false}
                        isSaving={false}
                        onCancel={() => setEditRow(null)}
                        onSave={jest.fn()}
                        onCreate={jest.fn()}
                    />
                </>
            );
        }

        render(<ControlledSheet />);
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Draft name' } });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        fireEvent.click(screen.getByRole('button', { name: 'Reopen' }));

        expect((screen.getByLabelText(/^Name/) as HTMLInputElement).value).toBe('');
        expect(screen.getByRole('button', { name: 'Add notification' })).not.toBeNull();
    });
});
