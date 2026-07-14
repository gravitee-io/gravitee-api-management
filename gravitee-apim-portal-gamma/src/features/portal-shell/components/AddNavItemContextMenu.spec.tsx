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

import { CustomizePanelContext } from '../../theming/components/CustomizePanelContext';
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

function renderMenu(
    props: Omit<React.ComponentProps<typeof AddNavItemContextMenu>, 'children'>,
    children: React.ReactNode = <button type="button">Guides</button>,
    customizePanel = { openCustomizePanel: jest.fn() },
) {
    return renderWithGraphene(
        <CustomizePanelContext.Provider value={customizePanel}>
            <AddNavItemContextMenu {...props}>{children}</AddNavItemContextMenu>
        </CustomizePanelContext.Provider>,
    );
}

async function openAddItemMenu(user: ReturnType<typeof userEvent.setup>, targetName = 'Guides') {
    await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: targetName }) });
    await user.click(screen.getByRole('menuitem', { name: 'Add item' }));
}

describe('AddNavItemContextMenu', () => {
    it('should render children without a menu when disabled', () => {
        renderMenu({
            parentId: 'folder-1',
            allItems,
            enabled: false,
            onAdd: jest.fn(),
            onRequestApi: jest.fn(),
            onRequestPage: jest.fn(),
        });

        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
    });

    it('should show Add item and Style in the primary context menu', async () => {
        const user = userEvent.setup();

        renderMenu({
            parentId: 'folder-1',
            allItems,
            enabled: true,
            onAdd: jest.fn(),
            onRequestApi: jest.fn(),
            onRequestPage: jest.fn(),
        });

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });

        expect(screen.getByRole('menuitem', { name: 'Add item' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Style' })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: 'Page' })).not.toBeInTheDocument();
    });

    it('should call onRequestPage when Page type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();
        const onRequestPage = jest.fn();

        renderMenu({
            parentId: 'folder-1',
            allItems,
            enabled: true,
            onAdd,
            onRequestApi,
            onRequestPage,
        });

        await openAddItemMenu(user);
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onRequestPage).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
        expect(onRequestApi).not.toHaveBeenCalled();
    });

    it('should call onRequestApi when API type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();

        renderMenu({
            parentId: 'folder-1',
            allItems,
            enabled: true,
            onAdd,
            onRequestApi,
            onRequestPage: jest.fn(),
        });

        await openAddItemMenu(user);
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        expect(onRequestApi).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
    });

    it('should not offer API when parent is an API item', async () => {
        const user = userEvent.setup();

        renderMenu(
            {
                parentId: 'api-1',
                allItems,
                enabled: true,
                onAdd: jest.fn(),
                onRequestApi: jest.fn(),
                onRequestPage: jest.fn(),
            },
            <button type="button">Payments API</button>,
        );

        await openAddItemMenu(user, 'Payments API');

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Page' })).toBeInTheDocument();
    });

    it('should not offer API when parent is a folder nested under an API', async () => {
        const user = userEvent.setup();

        renderMenu(
            {
                parentId: 'nested-folder',
                allItems,
                enabled: true,
                onAdd: jest.fn(),
                onRequestApi: jest.fn(),
                onRequestPage: jest.fn(),
            },
            <button type="button">Nested</button>,
        );

        await openAddItemMenu(user, 'Nested');

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Folder' })).toBeInTheDocument();
    });

    it('should open the style panel from the Style context menu item', async () => {
        const user = userEvent.setup();
        const openCustomizePanel = jest.fn();

        renderMenu(
            {
                parentId: 'folder-1',
                allItems,
                enabled: true,
                onAdd: jest.fn(),
                onRequestApi: jest.fn(),
                onRequestPage: jest.fn(),
            },
            <button type="button" data-style-target="nav-item">
                Guides
            </button>,
            { openCustomizePanel },
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });
        await user.click(screen.getByRole('menuitem', { name: 'Style' }));

        expect(openCustomizePanel).toHaveBeenCalledWith(
            screen.getByRole('button', { name: 'Guides' }),
            expect.objectContaining({ x: expect.any(Number), y: expect.any(Number) }),
        );
    });
});
