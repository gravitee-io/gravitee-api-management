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

import { RoleDeleteDialog } from './RoleDeleteDialog';
import type { Role } from '../types/role';

describe('RoleDeleteDialog', () => {
    const role: Role = { name: 'CUSTOM', scope: 'API', permissions: {} };

    it('shows the role name and confirms deletion', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();
        render(<RoleDeleteDialog open role={role} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);

        expect(screen.getByText('Delete a Role')).toBeInTheDocument();
        expect(screen.getByText('CUSTOM')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        expect(onConfirm).toHaveBeenCalled();
    });

    it('disables actions while deleting', () => {
        render(<RoleDeleteDialog open role={role} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);

        expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
    });
});
