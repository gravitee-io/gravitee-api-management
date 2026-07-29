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

import { EntrypointDetailSheet } from './EntrypointDetailSheet';
import type { EntrypointMappingRow } from '../types/entrypoint';

const ROW: EntrypointMappingRow = {
    id: 'ep-1',
    value: 'https://api.example.com',
    target: 'HTTP',
    targetLabel: 'HTTP',
    tags: ['prod'],
    tagsName: ['Production'],
    environmentIds: ['env-1'],
    environmentNames: ['Default Environment'],
};

describe('EntrypointDetailSheet', () => {
    it('renders read-only fields when an entrypoint is selected', () => {
        render(<EntrypointDetailSheet entrypoint={ROW} onClose={jest.fn()} />);
        expect(screen.getByText('Entrypoint details')).not.toBeNull();
        expect(screen.getByText('https://api.example.com')).not.toBeNull();
        expect(screen.getByText('HTTP')).not.toBeNull();
        expect(screen.getByText('Production')).not.toBeNull();
        expect(screen.getByText('Default Environment')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /^Edit$/i })).toBeNull();
    });

    it('calls onClose when Close is clicked', () => {
        const onClose = jest.fn();
        render(<EntrypointDetailSheet entrypoint={ROW} onClose={onClose} />);
        const closeButtons = screen.getAllByRole('button', { name: 'Close' });
        fireEvent.click(closeButtons[closeButtons.length - 1]!);
        expect(onClose).toHaveBeenCalled();
    });

    it('calls onEdit when Edit is clicked', () => {
        const onEdit = jest.fn();
        render(<EntrypointDetailSheet entrypoint={ROW} canEdit onClose={jest.fn()} onEdit={onEdit} />);
        fireEvent.click(screen.getByRole('button', { name: 'Edit' }));
        expect(onEdit).toHaveBeenCalledWith(ROW);
    });
});
