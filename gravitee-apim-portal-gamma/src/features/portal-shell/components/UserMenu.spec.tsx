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
import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import type { PortalNavigationItem, PortalNavigationLink, PortalNavigationPage } from '../../portals/types';
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { UserMenu } from './UserMenu';

const portalId = 'p1';

const sampleUserMenuLinks: PortalNavigationLink[] = [
    {
        id: 'menu-profile',
        portalId,
        title: 'Profile',
        type: 'LINK',
        parentId: null,
        order: 0,
        slug: 'profile-menu001',
        url: '/profile',
        area: 'USER_MENU',
    },
    {
        id: 'menu-logout',
        portalId,
        title: 'Log out',
        type: 'LINK',
        parentId: null,
        order: 1,
        slug: 'log-out-menu002',
        url: '/logout',
        area: 'USER_MENU',
    },
];

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId, title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId, title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

const getPagePath = (slug: string) => `/portals/${portalId}/edit/${slug}`;

const shellProps = {
    onAddUserMenuNavItem: jest.fn().mockResolvedValue(undefined),
    onAddUserMenuLink: jest.fn().mockResolvedValue(undefined),
    onUpdateNavItem: jest.fn(),
    onRequestDeleteNavItem: jest.fn(),
    onSelectNavItem: jest.fn(),
};

const baseProps = {
    portalId,
    portalPages,
    getPagePath,
    allNavItems: sampleUserMenuLinks,
    hasUserMenuItems: true,
    userMenuRootItems: sampleUserMenuLinks,
    ...shellProps,
};

describe('UserMenu', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should open dropdown and render stored items in preview mode', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                mode="preview"
                onNavigate={jest.fn()}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByRole('menuitem', { name: 'Profile' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Log out' })).toBeInTheDocument();
    });

    it('should navigate via onNavigate for portal page links in preview mode', async () => {
        const user = userEvent.setup();
        const onNavigate = jest.fn();
        const aboutLink: PortalNavigationLink = {
            id: 'menu-about',
            portalId,
            title: 'About',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'about-menu',
            url: 'about-def456',
            area: 'USER_MENU',
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[aboutLink]}
                allNavItems={[aboutLink]}
                mode="preview"
                onNavigate={onNavigate}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('menuitem', { name: 'About' }));

        expect(onNavigate).toHaveBeenCalledWith('/portals/p1/edit/about-def456');
    });

    it('should navigate custom relative paths via onNavigate in preview mode', async () => {
        const user = userEvent.setup();
        const onNavigate = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                mode="preview"
                onNavigate={onNavigate}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('menuitem', { name: 'Profile' }));

        expect(onNavigate).toHaveBeenCalledWith('/profile');
    });

    it('should open external links in a new tab in preview mode', async () => {
        const user = userEvent.setup();
        const docsLink: PortalNavigationLink = {
            id: 'menu-docs',
            portalId,
            title: 'Docs',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'docs-menu',
            url: 'https://docs.example.com',
            area: 'USER_MENU',
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[docsLink]}
                allNavItems={[docsLink]}
                mode="preview"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByRole('menuitem', { name: 'Docs' })).toHaveAttribute('target', '_blank');
        expect(screen.getByRole('menuitem', { name: 'Docs' })).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should not render in preview mode when there are no items', () => {
        const { container } = renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="preview"
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('should keep hook order stable when switching from edit to preview with no items', () => {
        const { container, rerender } = renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
            />,
        );

        expect(screen.getByLabelText('User menu')).toBeInTheDocument();

        rerender(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="preview"
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('should add a link from the page picker in edit mode', async () => {
        const user = userEvent.setup();
        const onAddUserMenuLink = jest.fn().mockResolvedValue(undefined);

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
                onAddUserMenuLink={onAddUserMenuLink}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Link' }));
        expect(screen.getByRole('textbox', { name: 'Search for a page' })).toBeInTheDocument();
        await user.click(screen.getByRole('option', { name: 'About' }));

        expect(onAddUserMenuLink).toHaveBeenCalledWith(portalPages[1], null);
    });

    it('should filter pages while searching in the page picker', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Link' }));

        const searchInput = screen.getByRole('textbox', { name: 'Search for a page' });
        fireEvent.change(searchInput, { target: { value: 'home' } });

        await waitFor(() => {
            expect(screen.getByRole('option', { name: 'Home' })).toBeInTheDocument();
            expect(screen.queryByRole('option', { name: 'About' })).not.toBeInTheDocument();
        });
    });

    it('should cancel page picker on Escape', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Link' }));

        const searchInput = screen.getByRole('textbox', { name: 'Search for a page' });
        fireEvent.keyDown(searchInput, { key: 'Escape' });

        expect(screen.queryByRole('textbox', { name: 'Search for a page' })).not.toBeInTheDocument();
        expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
    });

    it('should request delete for a menu item in edit mode', async () => {
        const user = userEvent.setup();
        const onRequestDeleteNavItem = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                mode="edit"
                onRequestDeleteNavItem={onRequestDeleteNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'Remove Profile' }));

        expect(onRequestDeleteNavItem).toHaveBeenCalledWith(sampleUserMenuLinks[0]);
    });

    it('should call onSelectNavItem when a page is clicked in edit mode', async () => {
        const user = userEvent.setup();
        const onSelectNavItem = jest.fn();
        const pageItem = {
            id: 'menu-page',
            portalId,
            title: 'Settings',
            type: 'PAGE' as const,
            parentId: null,
            order: 0,
            slug: 'settings-menu',
            area: 'USER_MENU' as const,
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[pageItem]}
                allNavItems={[pageItem]}
                mode="edit"
                onSelectNavItem={onSelectNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Edit Settings'));

        expect(onSelectNavItem).toHaveBeenCalledWith('menu-page');
    });

    it('should call onSelectNavItem when a folder is clicked in edit mode', async () => {
        const user = userEvent.setup();
        const onSelectNavItem = jest.fn();
        const folderItem = {
            id: 'menu-folder',
            portalId,
            title: 'Account',
            type: 'FOLDER' as const,
            parentId: null,
            order: 0,
            slug: 'account-menu',
            area: 'USER_MENU' as const,
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[folderItem]}
                allNavItems={[folderItem]}
                mode="edit"
                onSelectNavItem={onSelectNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Edit Account'));

        await waitFor(() => {
            expect(onSelectNavItem).toHaveBeenCalledWith('menu-folder');
        }, { timeout: 500 });
    });

    it('should rename a folder on double-click without opening it in edit mode', async () => {
        const user = userEvent.setup();
        const onSelectNavItem = jest.fn();
        const onUpdateNavItem = jest.fn();
        const folderItem = {
            id: 'menu-folder',
            portalId,
            title: 'Account',
            type: 'FOLDER' as const,
            parentId: null,
            order: 0,
            slug: 'account-menu',
            area: 'USER_MENU' as const,
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[folderItem]}
                allNavItems={[folderItem]}
                mode="edit"
                onSelectNavItem={onSelectNavItem}
                onUpdateNavItem={onUpdateNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.dblClick(screen.getByLabelText('Edit Account'));
        const input = screen.getByRole('textbox', { name: 'Edit Account' });
        fireEvent.change(input, { target: { value: 'My Account' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        expect(onUpdateNavItem).toHaveBeenCalledWith('menu-folder', { title: 'My Account' });
        expect(onSelectNavItem).not.toHaveBeenCalled();
    });

    it('should call onUpdateNavItem for label edits on double-click in edit mode', async () => {
        const user = userEvent.setup();
        const onUpdateNavItem = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                mode="edit"
                onUpdateNavItem={onUpdateNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.dblClick(screen.getByLabelText('Edit Profile'));
        const input = screen.getByRole('textbox', { name: 'Edit Profile' });
        fireEvent.change(input, { target: { value: 'My Profile' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        expect(onUpdateNavItem).toHaveBeenCalledWith('menu-profile', { title: 'My Profile' });
    });

    it('should call onUpdateNavItem for URL edits on double-click in edit mode', async () => {
        const user = userEvent.setup();
        const onUpdateNavItem = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                mode="edit"
                onUpdateNavItem={onUpdateNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.dblClick(screen.getByLabelText('Edit URL for Profile'));
        const input = screen.getByRole('textbox', { name: 'Edit URL for Profile' });
        fireEvent.change(input, { target: { value: '/my-profile' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        expect(onUpdateNavItem).toHaveBeenCalledWith('menu-profile', { url: '/my-profile' });
    });

    it('should display page slug instead of full path for portal page links in edit mode', async () => {
        const user = userEvent.setup();
        const aboutLink: PortalNavigationLink = {
            id: 'menu-about',
            portalId,
            title: 'About',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'about-menu',
            url: '/portals/p1/about-def456',
            area: 'USER_MENU',
        };

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[aboutLink]}
                allNavItems={[aboutLink]}
                mode="edit"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByLabelText('Edit URL for About')).toHaveTextContent('about-def456');
    });

    it('should call onAddUserMenuNavItem when adding a page from the type dialog', async () => {
        const user = userEvent.setup();
        const onAddUserMenuNavItem = jest.fn().mockResolvedValue(undefined);

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
                onAddUserMenuNavItem={onAddUserMenuNavItem}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));
        await user.click(screen.getByRole('option', { name: /Block/i }));

        expect(onAddUserMenuNavItem).toHaveBeenCalledWith('PAGE', null, { contentType: 'BLOCK' });
    });

    it('should only offer block and HTML page types in the user menu', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                userMenuRootItems={[]}
                allNavItems={[]}
                hasUserMenuItems={false}
                mode="edit"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(screen.getByRole('option', { name: /Block/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /HTML/i })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /OpenAPI/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /AsyncAPI/i })).not.toBeInTheDocument();
    });
});
