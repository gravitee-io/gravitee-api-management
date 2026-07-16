/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DeleteNavItemDialog } from './DeleteNavItemDialog';

const folderItem = {
    id: 'guides',
    portalId: 'p1',
    title: 'Guides',
    type: 'FOLDER' as const,
    parentId: null,
    order: 0,
    slug: 'guides',
};

const allItems = [
    folderItem,
    {
        id: 'page-1',
        portalId: 'p1',
        title: 'Quick Start',
        type: 'PAGE' as const,
        parentId: 'guides',
        order: 0,
        slug: 'quick-start',
    },
];

describe('DeleteNavItemDialog', () => {
    it('should prefill the item name and enable delete immediately', async () => {
        const user = userEvent.setup();
        const onConfirm = jest.fn();

        renderWithGraphene(
            <DeleteNavItemDialog
                item={folderItem}
                allItems={allItems}
                open
                isPending={false}
                onOpenChange={jest.fn()}
                onConfirm={onConfirm}
            />,
        );

        expect(screen.getByRole('textbox')).toHaveValue('Guides');
        expect(screen.getByRole('button', { name: 'Delete' })).toBeEnabled();
        expect(screen.getByText(/nested items will be permanently deleted/i)).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        expect(onConfirm).toHaveBeenCalled();
    });
});
