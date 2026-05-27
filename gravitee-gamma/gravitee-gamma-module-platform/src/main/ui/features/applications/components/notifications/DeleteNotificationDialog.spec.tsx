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

import { DeleteNotificationDialog } from './DeleteNotificationDialog';
import type { ApplicationNotificationRow } from '../../types/applicationNotification';

const row: ApplicationNotificationRow = {
    key: 'n1',
    name: 'Subscription alerts',
    subscribedEvents: 2,
    notifierName: 'Email',
    notification: { id: 'n1', name: 'Subscription alerts', config_type: 'DEFAULT' },
    isReadonly: false,
};

describe('DeleteNotificationDialog', () => {
    it('shows the notification name in the confirmation message', () => {
        render(<DeleteNotificationDialog row={row} isDeleting={false} onCancel={jest.fn()} onConfirm={jest.fn()} />);

        expect(screen.getByText(/Subscription alerts/)).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Delete' })).not.toBeNull();
    });

    it('shows deleting label while the mutation is in progress', () => {
        render(<DeleteNotificationDialog row={row} isDeleting onCancel={jest.fn()} onConfirm={jest.fn()} />);

        expect(screen.getByRole('button', { name: 'Deleting…' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
    });
});
