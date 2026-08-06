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

import { GroupAssociateDialog } from './GroupAssociateDialog';

function renderDialog(overrides: Partial<React.ComponentProps<typeof GroupAssociateDialog>> = {}) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(
        <GroupAssociateDialog
            open
            typeLabel="APIs"
            onClose={onClose}
            onConfirm={onConfirm}
            isAssociating={false}
            {...overrides}
        />,
    );
    return { onClose, onConfirm };
}

describe('GroupAssociateDialog', () => {
    it('does not render dialog content when closed', () => {
        renderDialog({ open: false });
        expect(screen.queryByRole('heading', { name: /Add group to existing/i })).toBeNull();
    });

    it('shows the type label in the title and body', () => {
        renderDialog({ typeLabel: 'API Products' });
        expect(screen.getByText('Add group to existing API Products')).not.toBeNull();
        expect(
            screen.getByText('You are trying to add the group to all the existing api products. Do you want to continue?'),
        ).not.toBeNull();
    });

    it('calls onConfirm when Continue is clicked', () => {
        const { onConfirm } = renderDialog();
        fireEvent.click(screen.getByRole('button', { name: 'Continue' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when Cancel is clicked', () => {
        const { onClose } = renderDialog();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables both buttons while associating', () => {
        renderDialog({ isAssociating: true });
        expect(screen.getByRole('button', { name: 'Cancel' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Adding…' })).toHaveProperty('disabled', true);
    });
});
