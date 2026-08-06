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
import userEvent from '@testing-library/user-event';

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
    it('renders entrypoint rows', () => {
        render(<EntrypointMappingsTable rows={ROWS} canCreate={false} canEdit={false} canDelete={false} />);
        expect(screen.getByText('https://api.example.com')).not.toBeNull();
        expect(screen.getByText('4082')).not.toBeNull();
        expect(screen.getByText('HTTP')).not.toBeNull();
    });

    it('filters rows by entrypoint value', () => {
        render(<EntrypointMappingsTable rows={ROWS} canCreate={false} canEdit={false} canDelete={false} />);
        fireEvent.change(screen.getByLabelText('Search entrypoints'), { target: { value: '4082' } });
        expect(screen.queryByText('https://api.example.com')).toBeNull();
        expect(screen.getByText('4082')).not.toBeNull();
    });

    it('shows create CTA in empty state when canCreate is true', () => {
        render(<EntrypointMappingsTable rows={[]} canCreate canEdit={false} canDelete={false} onCreate={jest.fn()} />);
        expect(screen.getByRole('button', { name: /Add a mapping/i })).not.toBeNull();
    });

    it('calls onCreate with the chosen target after opening the create dropdown', async () => {
        const user = userEvent.setup();
        const onCreate = jest.fn();
        render(<EntrypointMappingsTable rows={[]} canCreate canEdit={false} canDelete={false} onCreate={onCreate} />);
        // "Add a mapping" opens a target picker (HTTP / TCP / Kafka) rather than firing onCreate directly.
        await user.click(screen.getByRole('button', { name: /Add a mapping/i }));
        await user.click(await screen.findByRole('menuitem', { name: 'HTTP' }));
        expect(onCreate).toHaveBeenCalledWith('HTTP');
    });

    it('hides create CTA in empty state when canCreate is false', () => {
        render(<EntrypointMappingsTable rows={[]} canCreate={false} canEdit={false} canDelete={false} />);
        expect(screen.queryByRole('button', { name: /Add a mapping/i })).toBeNull();
        expect(screen.getByText('No entrypoints')).not.toBeNull();
    });
});
