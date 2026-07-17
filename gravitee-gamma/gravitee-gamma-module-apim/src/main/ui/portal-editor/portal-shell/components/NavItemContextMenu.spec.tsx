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
import { NavItemContextMenu } from './NavItemContextMenu';

const folder: PortalNavigationItem = {
    id: 'folder-1',
    portalId: 'p1',
    title: 'Guides',
    type: 'FOLDER',
    parentId: null,
    order: 0,
    slug: 'guides',
};

const allItems: PortalNavigationItem[] = [
    folder,
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
    {
        id: 'page-1',
        portalId: 'p1',
        title: 'Draft Page',
        type: 'PAGE',
        parentId: 'folder-1',
        order: 1,
        slug: 'draft-page',
        published: false,
    },
];

function renderMenu(
    props: Omit<React.ComponentProps<typeof NavItemContextMenu>, 'children'>,
    children: React.ReactNode = <button type="button">Guides</button>,
    customizePanel = { openCustomizePanel: jest.fn() },
) {
    return renderWithGraphene(
        <CustomizePanelContext.Provider value={customizePanel}>
            <NavItemContextMenu {...props}>{children}</NavItemContextMenu>
        </CustomizePanelContext.Provider>,
    );
}

async function openAddItemMenu(user: ReturnType<typeof userEvent.setup>, targetName = 'Guides') {
    await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: targetName }) });
    await user.click(screen.getByRole('menuitem', { name: 'Add item' }));
}

describe('NavItemContextMenu', () => {
    it('should render children without a menu when disabled', () => {
        renderMenu({
            item: folder,
            allItems,
            enabled: false,
            isContainer: true,
            onAdd: jest.fn(),
            onRequestApi: jest.fn(),
            onRequestPage: jest.fn(),
            onTogglePublished: jest.fn(),
        });

        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
    });

    it('should show Add item and Style in the primary context menu for containers', async () => {
        const user = userEvent.setup();

        renderMenu({
            item: folder,
            allItems,
            enabled: true,
            isContainer: true,
            onAdd: jest.fn(),
            onRequestApi: jest.fn(),
            onRequestPage: jest.fn(),
            onTogglePublished: jest.fn(),
        });

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });

        expect(screen.getByRole('menuitem', { name: 'Add item' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Style' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Unpublish' })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: 'Page' })).not.toBeInTheDocument();
    });

    it('should show Publish for unpublished items', async () => {
        const user = userEvent.setup();
        const unpublishedPage = allItems[3];

        renderMenu(
            {
                item: unpublishedPage,
                allItems,
                enabled: true,
                isContainer: false,
                onTogglePublished: jest.fn(),
            },
            <button type="button">Draft Page</button>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Draft Page' }) });

        expect(screen.getByRole('menuitem', { name: 'Publish' })).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', { name: 'Add item' })).not.toBeInTheDocument();
    });

    it('should disable Publish when parent is unpublished', async () => {
        const user = userEvent.setup();
        const unpublishedFolder = { ...folder, published: false };
        const childPage = {
            id: 'child-page',
            portalId: 'p1',
            title: 'Child',
            type: 'PAGE' as const,
            parentId: 'folder-1',
            order: 0,
            slug: 'child',
            published: false,
        };
        const items = [unpublishedFolder, childPage];

        renderMenu(
            {
                item: childPage,
                allItems: items,
                enabled: true,
                isContainer: false,
                onTogglePublished: jest.fn(),
                publishDisabled: true,
                publishDisabledReason: 'A navigation item cannot be published within an unpublished folder',
            },
            <button type="button">Child</button>,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Child' }) });

        expect(screen.getByRole('menuitem', { name: 'Publish' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('should call onTogglePublished when Unpublish is selected', async () => {
        const user = userEvent.setup();
        const onTogglePublished = jest.fn();

        renderMenu({
            item: folder,
            allItems,
            enabled: true,
            isContainer: true,
            onAdd: jest.fn(),
            onRequestApi: jest.fn(),
            onRequestPage: jest.fn(),
            onTogglePublished,
        });

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });
        await user.click(screen.getByRole('menuitem', { name: 'Unpublish' }));

        expect(onTogglePublished).toHaveBeenCalledWith(folder);
    });

    it('should call onRequestPage when Page type is chosen', async () => {
        const user = userEvent.setup();
        const onAdd = jest.fn();
        const onRequestApi = jest.fn();
        const onRequestPage = jest.fn();

        renderMenu({
            item: folder,
            allItems,
            enabled: true,
            isContainer: true,
            onAdd,
            onRequestApi,
            onRequestPage,
            onTogglePublished: jest.fn(),
        });

        await openAddItemMenu(user);
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onRequestPage).toHaveBeenCalledWith('folder-1');
        expect(onAdd).not.toHaveBeenCalled();
        expect(onRequestApi).not.toHaveBeenCalled();
    });

    it('should open the context menu when children are wrapped in a component', async () => {
        const user = userEvent.setup();

        function NavItemWrapper({ label }: { readonly label: string }) {
            return (
                <div>
                    <button type="button">{label}</button>
                </div>
            );
        }

        renderMenu(
            {
                item: folder,
                allItems,
                enabled: true,
                isContainer: false,
                onTogglePublished: jest.fn(),
            },
            <NavItemWrapper label="Guides" />,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByRole('button', { name: 'Guides' }) });

        expect(screen.getByRole('menuitem', { name: 'Style' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Unpublish' })).toBeInTheDocument();
    });

    it('should open the style panel from the Style context menu item', async () => {
        const user = userEvent.setup();
        const openCustomizePanel = jest.fn();

        renderMenu(
            {
                item: folder,
                allItems,
                enabled: true,
                isContainer: true,
                onAdd: jest.fn(),
                onRequestApi: jest.fn(),
                onRequestPage: jest.fn(),
                onTogglePublished: jest.fn(),
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
