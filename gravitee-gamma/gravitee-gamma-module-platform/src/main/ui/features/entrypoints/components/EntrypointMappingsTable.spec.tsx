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

import { EntrypointMappingsTable } from './EntrypointMappingsTable';
import type { EntrypointMappingRow } from '../types/entrypoint';

const ROWS: EntrypointMappingRow[] = [
    {
        id: 'ep-1',
        value: 'https://api.example.com',
        target: 'HTTP',
        targetLabel: 'HTTP',
        tags: ['prod'],
        tagsName: ['Production'],
        environmentIds: [],
        environmentNames: [],
    },
    {
        id: 'ep-2',
        value: '4082',
        target: 'TCP',
        targetLabel: 'TCP',
        tags: [],
        tagsName: [],
        environmentIds: ['env-1'],
        environmentNames: ['Production'],
    },
];

describe('EntrypointMappingsTable', () => {
    it('renders entrypoint rows and opens detail on value click', () => {
        const onOpenDetail = jest.fn();
        render(<EntrypointMappingsTable rows={ROWS} canCreate={false} onOpenDetail={onOpenDetail} />);
        expect(screen.getByText('https://api.example.com')).not.toBeNull();
        expect(screen.getByText('4082')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'https://api.example.com' }));
        expect(onOpenDetail).toHaveBeenCalledWith(ROWS[0]);
    });

    it('filters rows by entrypoint value', () => {
        render(<EntrypointMappingsTable rows={ROWS} canCreate={false} onOpenDetail={jest.fn()} />);
        fireEvent.change(screen.getByLabelText('Search entrypoints'), { target: { value: '4082' } });
        expect(screen.queryByText('https://api.example.com')).toBeNull();
        expect(screen.getByText('4082')).not.toBeNull();
    });

    it('shows create CTA in empty state when canCreate is true', () => {
        const onCreate = jest.fn();
        render(<EntrypointMappingsTable rows={[]} canCreate onOpenDetail={jest.fn()} onCreate={onCreate} />);
        fireEvent.click(screen.getByRole('button', { name: /Add a mapping/i }));
        expect(onCreate).toHaveBeenCalled();
    });

    it('hides create CTA in empty state when canCreate is false', () => {
        render(<EntrypointMappingsTable rows={[]} canCreate={false} onOpenDetail={jest.fn()} />);
        expect(screen.queryByRole('button', { name: /Add a mapping/i })).toBeNull();
        expect(screen.getByText('No entrypoints')).not.toBeNull();
    });
});
