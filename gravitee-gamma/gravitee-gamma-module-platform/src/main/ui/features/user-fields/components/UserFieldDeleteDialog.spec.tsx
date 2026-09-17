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

import { UserFieldDeleteDialog } from './UserFieldDeleteDialog';
import type { UserField } from '../types/userField';

const FIELD: UserField = { key: 'department', label: 'Department', required: false, values: [] };

describe('UserFieldDeleteDialog', () => {
    it('does not render when closed', () => {
        render(<UserFieldDeleteDialog open={false} field={FIELD} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.queryByText('Delete custom user field')).toBeNull();
    });

    it('names the field key, warns about the cascade, and confirms deletion', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();
        render(<UserFieldDeleteDialog open field={FIELD} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);

        expect(screen.getByText('Delete custom user field')).toBeInTheDocument();
        expect(screen.getByText('department')).toBeInTheDocument();
        expect(screen.getByText(/existing answers on user profiles will be removed/)).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('closes on Cancel', async () => {
        const user = userEvent.setup();
        const onClose = jest.fn();
        render(<UserFieldDeleteDialog open field={FIELD} onClose={onClose} onConfirm={jest.fn()} isDeleting={false} />);

        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables actions while deleting', () => {
        render(<UserFieldDeleteDialog open field={FIELD} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);

        expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });
});
