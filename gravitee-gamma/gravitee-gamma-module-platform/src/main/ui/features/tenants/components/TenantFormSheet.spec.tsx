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

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { TenantFormSheet } from './TenantFormSheet';
import { ApimApiError } from '../../../shared/api/apimClient';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { Tenant } from '../types/tenant';

const EXISTING: Tenant[] = [{ id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia' }];

const EDIT_TENANT: Tenant = {
    id: 't-2',
    key: 'eu-west',
    name: 'EU West',
    description: 'Frankfurt',
};

function renderCreateSheet({
    open = true,
    isSaving = false,
    existingTenants = EXISTING,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    isSaving?: boolean;
    existingTenants?: Tenant[];
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(
        <TenantFormSheet
            open={open}
            mode="create"
            existingTenants={existingTenants}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

function renderEditSheet({
    open = true,
    tenant = EDIT_TENANT,
    isSaving = false,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    tenant?: Tenant;
    isSaving?: boolean;
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(
        <TenantFormSheet
            open={open}
            mode="edit"
            tenant={tenant}
            existingTenants={EXISTING}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

describe('TenantFormSheet', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = jest.fn();
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('does not show sheet content when closed', () => {
        renderCreateSheet({ open: false });
        expect(querySheetHeading('Create a tenant')).toBeNull();
    });

    it('explains that the key is generated from the name', () => {
        renderCreateSheet();
        expect(
            screen.getByText('The key is what you paste into gravitee.yml. It is generated from the name unless you type one.'),
        ).not.toBeNull();
    });

    it('generates the key from the name until the key field is typed', () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'AP South' } });
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('ap-south');
        expect((screen.getByRole('button', { name: 'Create tenant' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('stops generating the key after the user types in the key field', () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'AP South' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'custom-key' } });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'AP West' } });
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('custom-key');
    });

    it('keeps Create tenant disabled until name and key are valid', () => {
        renderCreateSheet();
        const createBtn = screen.getByRole('button', { name: 'Create tenant' }) as HTMLButtonElement;
        expect(createBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: '   ' } });
        expect((screen.getByRole('button', { name: 'Create tenant' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('explains why the name is rejected once the user has typed into it', () => {
        renderCreateSheet();
        expect(screen.queryByText('Name is required.')).toBeNull();

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: '   ' } });

        expect(screen.getByText('Name is required.')).not.toBeNull();
        expect(screen.getByLabelText(/^Name/).getAttribute('aria-describedby')).toBe('tenant-create-name-error');
    });

    it('explains why the key is rejected once the user has typed into it', () => {
        renderCreateSheet();
        expect(screen.queryByText('Key is required and must contain at least one letter or number.')).toBeNull();

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Lab' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: '---' } });

        expect(screen.getByText('Key is required and must contain at least one letter or number.')).not.toBeNull();
        expect(screen.getByLabelText(/^Key/).getAttribute('aria-describedby')).toBe('tenant-create-key-error');
    });

    it('submits create payload with slugified key and optional description', async () => {
        const { onSubmit } = renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'AP South' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'Custom Key!' } });
        fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: 'Singapore cluster' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'AP South',
                key: 'custom-key',
                description: 'Singapore cluster',
            });
        });
    });

    it('allows a duplicate name when the key differs, matching the API where only the key is unique', async () => {
        const { onSubmit } = renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'US East' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'us-east-2' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({ name: 'US East', key: 'us-east-2', description: undefined });
        });
    });

    it('shows inline error for duplicate key on submit', async () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'US West' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'us-east' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));

        await waitFor(() => {
            expect(screen.queryByText('The tenant key already exists.')).not.toBeNull();
        });
    });

    it('keeps existing key read-only in edit mode', () => {
        renderEditSheet();
        expect(screen.getByRole('heading', { name: 'Edit a tenant' })).not.toBeNull();
        expect(
            screen.getByText('Change the name or description. The key is already in use on gateways and cannot be renamed.'),
        ).not.toBeNull();
        const keyInput = screen.getByLabelText(/^Key/) as HTMLInputElement;
        expect(keyInput.value).toBe('eu-west');
        expect(keyInput.readOnly).toBe(true);
        expect(keyInput.disabled).toBe(true);
    });

    it('maps create-mode duplicate-key HTTP 400 to the key field error', async () => {
        const onSubmit = jest
            .fn()
            .mockRejectedValue(new ApimApiError(400, 'The tenant already exists.', { technicalCode: 'tenant.exists' }));
        renderCreateSheet({ onSubmit });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Lab' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));
        await waitFor(() => {
            expect(screen.queryByText('The tenant already exists.')).not.toBeNull();
            expect(screen.getByLabelText(/^Key/).getAttribute('aria-invalid')).toBe('true');
        });
    });

    it('maps other create-mode HTTP 400s to the form submit error, not the key field', async () => {
        const onSubmit = jest.fn().mockRejectedValue(new ApimApiError(400, 'size must be between 1 and 40'));
        renderCreateSheet({ onSubmit });
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Lab' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create tenant' }));
        await waitFor(() => {
            expect(screen.queryByText('size must be between 1 and 40')).not.toBeNull();
        });
        expect(screen.getByLabelText(/^Key/).getAttribute('aria-invalid')).not.toBe('true');
    });

    it('marks the create key as required and invalid when empty', () => {
        renderCreateSheet();
        const keyInput = screen.getByLabelText(/^Key/);
        expect(keyInput).toHaveProperty('required', true);
        expect(keyInput.getAttribute('aria-required')).toBe('true');
        expect(keyInput.getAttribute('aria-invalid')).toBe('true');
        expect(keyInput.getAttribute('maxLength')).toBe('40');
    });

    it('keeps Create tenant disabled when the key sanitizes to empty', () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Lab' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: '---' } });
        expect((screen.getByRole('button', { name: 'Create tenant' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('scopes the form and field ids per mode so both sheets can be mounted at once', () => {
        const { unmount } = render(
            <TenantFormSheet
                open
                mode="create"
                existingTenants={EXISTING}
                onClose={jest.fn()}
                onSubmit={jest.fn().mockResolvedValue(undefined)}
                isSaving={false}
            />,
        );
        expect(document.getElementById('tenant-create-form')).not.toBeNull();
        expect(document.getElementById('tenant-create-name')).not.toBeNull();
        expect(document.getElementById('tenant-create-key')).not.toBeNull();
        expect(document.getElementById('tenant-create-description')).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Create tenant' }).getAttribute('form')).toBe('tenant-create-form');
        unmount();

        renderEditSheet();
        expect(document.getElementById('tenant-edit-form')).not.toBeNull();
        expect(document.getElementById('tenant-edit-name')).not.toBeNull();
        expect(document.getElementById('tenant-edit-key')).not.toBeNull();
        expect(document.getElementById('tenant-edit-description')).not.toBeNull();
        expect(document.getElementById('tenant-form')).toBeNull();
        expect(document.getElementById('tenant-name')).toBeNull();
        expect(screen.getByRole('button', { name: 'Save' }).getAttribute('form')).toBe('tenant-edit-form');
    });

    it('shows character counters for name, key, and description', () => {
        renderCreateSheet();
        expect(screen.getAllByText('0/40')).toHaveLength(2);
        expect(screen.getByText('0/160')).not.toBeNull();
    });
});
