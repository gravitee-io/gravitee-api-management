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

import { EntrypointDeleteSheet } from './EntrypointDeleteSheet';
import type { EntrypointMappingRow } from '../types/entrypoint';

const ENTRYPOINT: EntrypointMappingRow = {
    id: 'ep-1',
    value: 'https://api.example.com',
    target: 'HTTP',
    targetLabel: 'HTTP',
    tags: ['prod'],
    tagsName: ['Production'],
    environmentIds: [],
    environmentNames: [],
};

describe('EntrypointDeleteSheet', () => {
    it('renders nothing meaningful when closed', () => {
        render(<EntrypointDeleteSheet open={false} entrypoint={ENTRYPOINT} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.queryByText('Delete Entrypoint Mapping')).toBeNull();
    });

    it('shows the target label and value when open', () => {
        render(<EntrypointDeleteSheet open entrypoint={ENTRYPOINT} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect(screen.getByText('Delete Entrypoint Mapping')).not.toBeNull();
        expect(screen.getByText('HTTP')).not.toBeNull();
        expect(screen.getByText('https://api.example.com')).not.toBeNull();
    });

    it('calls onClose when Cancel is clicked', () => {
        const onClose = jest.fn();
        render(<EntrypointDeleteSheet open entrypoint={ENTRYPOINT} onClose={onClose} onConfirm={jest.fn()} isDeleting={false} />);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalled();
    });

    it('calls onConfirm when Delete is clicked', () => {
        const onConfirm = jest.fn();
        render(<EntrypointDeleteSheet open entrypoint={ENTRYPOINT} onClose={jest.fn()} onConfirm={onConfirm} isDeleting={false} />);
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onConfirm).toHaveBeenCalled();
    });

    it('disables both buttons and shows Deleting… label while isDeleting is true', () => {
        render(<EntrypointDeleteSheet open entrypoint={ENTRYPOINT} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting />);
        expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Deleting…' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('disables the Delete button when there is no entrypoint', () => {
        render(<EntrypointDeleteSheet open entrypoint={undefined} onClose={jest.fn()} onConfirm={jest.fn()} isDeleting={false} />);
        expect((screen.getByRole('button', { name: 'Delete' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
