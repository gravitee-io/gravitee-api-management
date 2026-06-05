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

import { EditMetadataSheet } from './EditMetadataSheet';
import type { ApplicationMetadata } from '../../types/applicationNotification';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const metadata: ApplicationMetadata = {
    key: 'threshold',
    name: 'Threshold',
    format: 'NUMERIC',
    value: '1',
};

describe('EditMetadataSheet', () => {
    it('does not show sheet content when metadata is null', () => {
        render(<EditMetadataSheet metadata={null} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);
        expect(querySheetHeading('Update Application metadata')).toBeNull();
    });

    it('shows sheet title and description when metadata is set', () => {
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);
        expect(screen.getByRole('heading', { name: 'Update Application metadata' })).not.toBeNull();
        expect(screen.getByText(/Update the display name and value for this metadata entry/i)).not.toBeNull();
    });

    it('invokes onCancel when Cancel is clicked', () => {
        const onCancel = jest.fn();
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={onCancel} onSave={jest.fn()} />);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('links footer Save to the metadata form via form attribute', () => {
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);
        const save = screen.getByRole('button', { name: 'Save' });
        expect(save.getAttribute('form')).toBe('edit-metadata-form');
        expect(save.getAttribute('type')).toBe('submit');
        expect(document.getElementById('edit-metadata-form')?.tagName).toBe('FORM');
    });

    it('submits changes through the linked form', () => {
        const onSave = jest.fn();
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={onSave} />);
        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '2' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ key: 'threshold', value: '2' }));
    });

    it('keeps Save enabled for a "0" value', () => {
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);

        const saveButton = screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
        expect(saveButton.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '0' } });

        expect(saveButton.disabled).toBe(false);
    });

    it('keeps Save disabled for blank values', () => {
        render(<EditMetadataSheet metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);

        const saveButton = screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '   ' } });

        expect(saveButton.disabled).toBe(true);
    });

    it('trims leading and trailing whitespace from saved values', () => {
        const stringMetadata: ApplicationMetadata = { key: 'owner', name: 'Owner', format: 'STRING', value: 'ops' };
        const onSave = jest.fn();
        render(<EditMetadataSheet metadata={stringMetadata} isSaving={false} onCancel={jest.fn()} onSave={onSave} />);
        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '  platform  ' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));
        expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ value: 'platform' }));
    });
});
