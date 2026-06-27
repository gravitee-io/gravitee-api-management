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

import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { TreeAddButton } from './TreeAddButton';

const allItems: PortalNavigationItem[] = [
    { id: 'folder-1', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
    {
        id: 'api-1',
        portalId: 'p1',
        title: 'Payments API',
        type: 'API',
        apiId: 'api-payments',
        parentId: 'folder-1',
        order: 0,
        slug: 'payments-api',
    } as PortalNavigationApi,
    {
        id: 'nested-folder',
        portalId: 'p1',
        title: 'Nested',
        type: 'FOLDER',
        parentId: 'api-1',
        order: 0,
        slug: 'nested',
    },
];

describe('TreeAddButton', () => {
    it('should call onAdd for non-API types', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderWithGraphene(
            <TreeAddButton
                parentId="folder-1"
                allItems={allItems}
                depth={2}
                onAdd={onAdd}
                onRequestApi={onRequestApi}
            />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onAdd).toHaveBeenCalledWith('PAGE', 'folder-1');
        expect(onRequestApi).not.toHaveBeenCalled();
    });

    it('should call onRequestApi when API type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderWithGraphene(
            <TreeAddButton
                parentId="folder-1"
                allItems={allItems}
                depth={1}
                onAdd={onAdd}
                onRequestApi={onRequestApi}
            />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        expect(onRequestApi).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('should not offer API when parent is an API item', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <TreeAddButton
                parentId="api-1"
                allItems={allItems}
                depth={2}
                onAdd={jest.fn()}
                onRequestApi={jest.fn()}
            />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Page' })).toBeInTheDocument();
    });

    it('should not offer API when parent is a folder nested under an API', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <TreeAddButton
                parentId="nested-folder"
                allItems={allItems}
                depth={3}
                onAdd={jest.fn()}
                onRequestApi={jest.fn()}
            />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Folder' })).toBeInTheDocument();
    });

    it('should indent the add button based on depth', () => {
        const { container } = renderWithGraphene(
            <TreeAddButton parentId={null} allItems={allItems} depth={3} onAdd={jest.fn()} onRequestApi={jest.fn()} />,
        );

        const row = container.querySelector('[class*="addButtonRow"]');
        expect(row).toHaveStyle({ '--tree-depth': '3' });
    });

    it('should align the add button with nav item icons using a chevron spacer', () => {
        const { container } = renderWithGraphene(
            <TreeAddButton parentId="folder-1" allItems={allItems} depth={1} onAdd={jest.fn()} onRequestApi={jest.fn()} />,
        );

        const row = container.querySelector('[class*="addButtonRow"]');
        expect(row?.querySelector('[class*="chevronSpacer"]')).toBeInTheDocument();
    });
});
