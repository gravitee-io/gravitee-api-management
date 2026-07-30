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

import { ShardingTagFormSheet } from './ShardingTagFormSheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';
import type { ShardingTagRow } from '../types/entrypoint';

jest.mock('./ShardingTagGroupsField', () => ({
    ShardingTagGroupsField: () => <div data-testid="sharding-tag-groups-field" />,
}));

const EXISTING_TAGS: ShardingTagRow[] = [
    {
        id: 'tag-1',
        key: 'prod',
        name: 'Production',
        description: 'Prod tag',
        restrictedGroupIds: [],
        restrictedGroupNames: [],
    },
];

const EDIT_TAG: ShardingTagRow = {
    id: 'tag-2',
    key: 'internal',
    name: 'Internal Gateway',
    description: 'Internal routing',
    restrictedGroupIds: ['group-1'],
    restrictedGroupNames: ['Ops'],
};

function renderCreateSheet({
    open = true,
    isSaving = false,
    existingTags = EXISTING_TAGS,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    isSaving?: boolean;
    existingTags?: ShardingTagRow[];
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(
        <ShardingTagFormSheet
            open={open}
            mode="create"
            existingTags={existingTags}
            groups={[]}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

function renderEditSheet({
    open = true,
    tag = EDIT_TAG,
    isSaving = false,
    onSubmit = jest.fn().mockResolvedValue(undefined),
}: {
    open?: boolean;
    tag?: ShardingTagRow;
    isSaving?: boolean;
    onSubmit?: jest.Mock;
} = {}) {
    const onClose = jest.fn();
    render(
        <ShardingTagFormSheet
            open={open}
            mode="edit"
            tag={tag}
            existingTags={EXISTING_TAGS}
            groups={[]}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={isSaving}
        />,
    );
    return { onClose, onSubmit };
}

describe('ShardingTagFormSheet', () => {
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
        expect(querySheetHeading('Add Sharding Tag')).toBeNull();
    });

    it('keeps Add Tag disabled until name and key are valid', () => {
        renderCreateSheet();
        const addBtn = screen.getByRole('button', { name: 'Add Tag' }) as HTMLButtonElement;
        expect(addBtn.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: '   ' } });
        expect((screen.getByRole('button', { name: 'Add Tag' }) as HTMLButtonElement).disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Tag Key' } });
        expect((screen.getByRole('button', { name: 'Add Tag' }) as HTMLButtonElement).disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'my-tag-key' } });
        expect((screen.getByRole('button', { name: 'Add Tag' }) as HTMLButtonElement).disabled).toBe(false);
    });

    it('sanitizes key while typing and does not auto-fill from name', () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Tag Key' } });
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('');

        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'My Tag Key' } });
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('my-tag-key');
    });

    it('submits create payload with slugified key', async () => {
        const { onSubmit } = renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'My Tag Key' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'Custom Key!' } });
        fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: 'A test tag' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add Tag' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                name: 'My Tag Key',
                key: 'custom-key',
                description: 'A test tag',
                restricted_groups: undefined,
            });
        });
    });

    it('shows inline error for duplicate name on submit', async () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Production' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'prod-2' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add Tag' }));

        await waitFor(() => {
            expect(screen.queryByText('A sharding tag with this name already exists.')).not.toBeNull();
        });
    });

    it('shows inline error for duplicate key on submit', async () => {
        renderCreateSheet();
        fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: 'Staging' } });
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'prod' } });
        fireEvent.click(screen.getByRole('button', { name: 'Add Tag' }));

        await waitFor(() => {
            expect(screen.queryByText('The tag key already exists.')).not.toBeNull();
        });
    });

    it('keeps existing key read-only in edit mode', () => {
        renderEditSheet();
        expect(screen.getByRole('heading', { name: 'Edit Sharding Tag' })).not.toBeNull();
        const keyInput = screen.getByLabelText(/^Key/) as HTMLInputElement;
        expect(keyInput.value).toBe('internal');
        expect(keyInput.readOnly).toBe(true);
        expect(keyInput.disabled).toBe(true);
    });
});
