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

import { EditMetadataDialog } from './EditMetadataDialog';
import type { ApplicationMetadata } from '../../types/applicationNotification';

const metadata: ApplicationMetadata = {
    key: 'threshold',
    name: 'Threshold',
    format: 'NUMERIC',
    value: '1',
};

describe('EditMetadataDialog', () => {
    it('keeps Save enabled for a "0" value', () => {
        render(<EditMetadataDialog metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);

        const saveButton = screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
        expect(saveButton.disabled).toBe(true);

        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '0' } });

        expect(saveButton.disabled).toBe(false);
    });

    it('keeps Save disabled for blank values', () => {
        render(<EditMetadataDialog metadata={metadata} isSaving={false} onCancel={jest.fn()} onSave={jest.fn()} />);

        const saveButton = screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement;
        fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: '   ' } });

        expect(saveButton.disabled).toBe(true);
    });
});
