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
import { useQuery } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';

import { AddMembersSheet } from './AddMembersSheet';
import { querySheetHeading } from '../test/sheetSpecHelpers';

jest.mock('@tanstack/react-query', () => ({
    ...jest.requireActual('@tanstack/react-query'),
    useQuery: jest.fn(),
}));

const mockUseQuery = jest.mocked(useQuery);

function renderSheet(open = true) {
    const onClose = jest.fn();
    const onAdd = jest.fn();
    render(<AddMembersSheet open={open} roles={['USER']} existingMembers={[]} onClose={onClose} onAdd={onAdd} isAdding={false} />);
    return { onClose, onAdd };
}

describe('AddMembersSheet', () => {
    beforeEach(() => {
        mockUseQuery.mockReturnValue({ data: [], isFetching: false } as ReturnType<typeof useQuery>);
    });

    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(querySheetHeading('Add Members')).toBeNull();
    });

    it('shows sheet title when open', () => {
        renderSheet(true);
        expect(screen.getByRole('heading', { name: 'Add Members' })).not.toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const { onClose } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
