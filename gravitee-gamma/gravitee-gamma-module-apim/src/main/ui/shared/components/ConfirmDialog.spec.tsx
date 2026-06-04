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
import userEvent from '@testing-library/user-event';

import { ConfirmDialog } from './ConfirmDialog';

describe('ConfirmDialog', () => {
    it('confirms a simple action', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();

        render(
            <ConfirmDialog
                open
                onOpenChange={() => {}}
                title="Publish plan?"
                description="This will make the plan available for subscriptions."
                confirmLabel="Publish"
                onConfirm={onConfirm}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Publish' }));

        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('keeps the confirm button disabled until the keyword matches', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();

        render(
            <ConfirmDialog
                open
                onOpenChange={() => {}}
                title="Delete API permanently?"
                confirmLabel="Delete permanently"
                destructive
                confirmKeyword="payments-api"
                onConfirm={onConfirm}
            />,
        );

        const confirmButton = screen.getByRole('button', { name: 'Delete permanently' });
        expect(confirmButton).toBeDisabled();

        await user.type(screen.getByPlaceholderText('payments-api'), 'payments-api');
        expect(confirmButton).toBeEnabled();

        await user.click(confirmButton);
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('disables actions while pending and shows the pending label', () => {
        render(
            <ConfirmDialog
                open
                onOpenChange={() => {}}
                title="Close plan?"
                confirmLabel="Close"
                pendingLabel="Closing…"
                destructive
                isPending
                onConfirm={() => {}}
            />,
        );

        expect(screen.getByRole('button', { name: 'Closing…' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });
});
