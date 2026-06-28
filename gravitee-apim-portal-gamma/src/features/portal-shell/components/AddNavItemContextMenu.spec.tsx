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
import { AddNavItemContextMenu } from './AddNavItemContextMenu';

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

describe('AddNavItemContextMenu', () => {
    it('should render children without a menu when disabled', () => {
        renderWithGraphene(
            <AddNavItemContextMenu
                parentId="folder-1"
                allItems={allItems}
                enabled={false}
                onAdd={jest.fn()}
                onRequestApi={jest.fn()}
                onRequestPage={jest.fn()}
            >
                <button type="button">Guides</button>
            </AddNavItemContextMenu>,
        );

        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
    });

    it('should call onRequestPage when Page type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();
        const onRequestPage = jest.fn();

        renderWithGraphene(
            <AddNavItemContextMenu
                parentId="folder-1"
                allItems={allItems}
                enabled
                onAdd={onAdd}
                onRequestApi={onRequestApi}
                onRequestPage={onRequestPage}
            >
                <button type="button">Guides</button>
            </AddNavItemContextMenu>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onRequestPage).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
        expect(onRequestApi).not.toHaveBeenCalled();
    });

    it('should call onRequestApi when API type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderWithGraphene(
            <AddNavItemContextMenu
                parentId="folder-1"
                allItems={allItems}
                enabled
                onAdd={onAdd}
                onRequestApi={onRequestApi}
                onRequestPage={jest.fn()}
            >
                <button type="button">Guides</button>
            </AddNavItemContextMenu>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        expect(onRequestApi).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('should not offer API when parent is an API item', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <AddNavItemContextMenu
                parentId="api-1"
                allItems={allItems}
                enabled
                onAdd={jest.fn()}
                onRequestApi={jest.fn()}
                onRequestPage={jest.fn()}
            >
                <button type="button">Payments API</button>
            </AddNavItemContextMenu>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Payments API' }) });

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Page' })).toBeInTheDocument();
    });

    it('should not offer API when parent is a folder nested under an API', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <AddNavItemContextMenu
                parentId="nested-folder"
                allItems={allItems}
                enabled
                onAdd={jest.fn()}
                onRequestApi={jest.fn()}
                onRequestPage={jest.fn()}
            >
                <button type="button">Nested</button>
            </AddNavItemContextMenu>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Nested' }) });

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Folder' })).toBeInTheDocument();
    });
});
