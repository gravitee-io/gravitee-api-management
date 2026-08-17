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

import { TenantDeleteDialog } from './TenantDeleteDialog';
import type { Tenant } from '../types/tenant';

const TENANT: Tenant = { id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia' };

function renderDialog(open: boolean, tenant: Tenant | undefined = TENANT) {
    const onClose = jest.fn();
    const onConfirm = jest.fn();
    render(<TenantDeleteDialog open={open} tenant={tenant} onClose={onClose} onConfirm={onConfirm} isDeleting={false} />);
    return { onClose, onConfirm };
}

describe('TenantDeleteDialog', () => {
    it('does not show dialog content when closed', () => {
        renderDialog(false);
        expect(screen.queryByRole('heading', { name: 'Delete a tenant' })).toBeNull();
    });

    it('confirms deletion using the tenant name, matching Classic console', () => {
        renderDialog(true);
        expect(screen.getByRole('heading', { name: 'Delete a tenant' })).not.toBeNull();
        expect(screen.getByText('US East', { selector: 'strong' })).not.toBeNull();
        expect(screen.getByText(/Gateways still tagged with this key will no longer match any endpoint that used it\./)).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Delete' })).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderDialog(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('invokes onConfirm when Delete is clicked', () => {
        const { onConfirm } = renderDialog(true);
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it('shows deleting label while the mutation is in progress', () => {
        render(<TenantDeleteDialog open tenant={TENANT} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);

        expect(screen.getByRole('button', { name: 'Deleting…' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Delete' })).toBeNull();
        expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Deleting…' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
