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

import { EntrypointSheet } from './EntrypointSheet';
import type { EntrypointMappingRow, OrgEnvironment, OrgTag } from '../types/entrypoint';

const TAGS: OrgTag[] = [{ id: 'tag-1', key: 'prod', name: 'Production' }];
const ENVIRONMENTS: OrgEnvironment[] = [{ id: 'env-1', name: 'Production' }];

const EXISTING_ROW: EntrypointMappingRow = {
    id: 'ep-1',
    value: 'https://api.example.com',
    target: 'HTTP',
    targetLabel: 'HTTP',
    tags: ['prod'],
    tagsName: ['Production'],
    environmentIds: [],
    environmentNames: [],
};

const EDIT_ROW: EntrypointMappingRow = {
    id: 'ep-2',
    value: 'https://api.example.com',
    target: 'HTTP',
    targetLabel: 'HTTP',
    tags: ['prod'],
    tagsName: ['Production'],
    environmentIds: [],
    environmentNames: [],
};

function renderSheet(overrides: Partial<Parameters<typeof EntrypointSheet>[0]> = {}) {
    const props: Parameters<typeof EntrypointSheet>[0] = {
        open: true,
        mode: 'create',
        target: 'HTTP',
        tags: TAGS,
        environments: ENVIRONMENTS,
        existingRows: [],
        onClose: jest.fn(),
        onSubmit: jest.fn(),
        isSaving: false,
        ...overrides,
    };
    render(<EntrypointSheet {...props} />);
    return props;
}

describe('EntrypointSheet', () => {
    it('does not render its content when closed', () => {
        renderSheet({ open: false });
        expect(screen.queryByText('Add HTTP Mapping')).toBeNull();
    });

    it('shows the create title for HTTP target', () => {
        renderSheet({ mode: 'create', target: 'HTTP' });
        expect(screen.getByText('Add HTTP Mapping')).not.toBeNull();
    });

    it('shows the edit title and prefills fields from the entrypoint', () => {
        renderSheet({ mode: 'edit', target: 'HTTP', entrypoint: EDIT_ROW });
        expect(screen.getByText('Edit HTTP Mapping')).not.toBeNull();
        expect(screen.getByDisplayValue('https://api.example.com')).not.toBeNull();
    });

    it('shows a validation error for an invalid HTTP URL', () => {
        renderSheet({ mode: 'create', target: 'HTTP' });
        const input = screen.getByLabelText(/Entrypoint URL/);
        fireEvent.change(input, { target: { value: 'not-a-url' } });
        expect(screen.getByText('Enter a valid URL (http:// or https://).')).not.toBeNull();
    });

    it('calls onClose when Cancel is clicked', () => {
        const props = renderSheet();
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(props.onClose).toHaveBeenCalled();
    });

    it('keeps the submit button disabled when no sharding tags are selected', () => {
        renderSheet({ mode: 'create', target: 'HTTP' });
        const input = screen.getByLabelText(/Entrypoint URL/);
        fireEvent.change(input, { target: { value: 'https://api.example.com' } });
        expect((screen.getByRole('button', { name: 'Add Mapping' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('shows a duplicate warning and disables submit when editing into an existing overlapping mapping', () => {
        renderSheet({ mode: 'edit', target: 'HTTP', entrypoint: EDIT_ROW, existingRows: [EXISTING_ROW, EDIT_ROW] });
        expect(screen.getByText(/already exists for an overlapping environment/)).not.toBeNull();
        expect((screen.getByRole('button', { name: 'Save Changes' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('disables all controls while isSaving is true', () => {
        renderSheet({ mode: 'create', target: 'HTTP', isSaving: true });
        expect((screen.getByLabelText(/Entrypoint URL/) as HTMLInputElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Cancel' }) as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Saving...' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
