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

import { EditDictionaryPropertySheet } from './EditDictionaryPropertySheet';
import { querySheetHeading } from '../../applications/components/test/sheetSpecHelpers';

jest.mock('../../../shared/notify', () => ({
    notify: { error: jest.fn(), success: jest.fn() },
}));

function renderSheet(
    overrides: Partial<{
        open: boolean;
        property: { key: string; value: string } | undefined;
        isSaving: boolean;
        existingKeys: string[];
        onSubmit: jest.Mock;
    }> = {},
) {
    const onClose = jest.fn();
    const onSubmit = overrides.onSubmit ?? jest.fn().mockResolvedValue(undefined);
    render(
        <EditDictionaryPropertySheet
            open={overrides.open ?? true}
            property={overrides.property ?? { key: 'MUC', value: 'Munich' }}
            onClose={onClose}
            onSubmit={onSubmit}
            isSaving={overrides.isSaving ?? false}
            existingKeys={overrides.existingKeys ?? ['MUC', 'FRA']}
        />,
    );
    return { onClose, onSubmit };
}

describe('EditDictionaryPropertySheet', () => {
    it('does not show sheet when closed', () => {
        renderSheet({ open: false });
        expect(querySheetHeading('Edit Property')).toBeNull();
    });

    it('prefills key and value', () => {
        renderSheet();
        expect(screen.getByRole('heading', { name: 'Edit Property' })).not.toBeNull();
        expect((screen.getByLabelText(/^Key/) as HTMLInputElement).value).toBe('MUC');
        expect((screen.getByLabelText(/^Value/) as HTMLInputElement).value).toBe('Munich');
    });

    it('submits updated value with original key', async () => {
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Value/), { target: { value: 'Munich Airport' } });
        fireEvent.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith({
                originalKey: 'MUC',
                key: 'MUC',
                value: 'Munich Airport',
            });
        });
    });

    it('blocks rename to an existing key', async () => {
        const { notify } = jest.requireMock('../../../shared/notify') as { notify: { error: jest.Mock } };
        const { onSubmit } = renderSheet();
        fireEvent.change(screen.getByLabelText(/^Key/), { target: { value: 'FRA' } });
        fireEvent.click(screen.getByRole('button', { name: 'Update' }));

        await waitFor(() => {
            expect(notify.error).toHaveBeenCalledWith('Property key "FRA" already exists');
        });
        expect(onSubmit).not.toHaveBeenCalled();
    });
});
