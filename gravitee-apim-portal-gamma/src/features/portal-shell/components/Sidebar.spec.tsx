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
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import type { PortalNavigationItem } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import { Sidebar } from './Sidebar';
import styles from './PortalIconEditor.module.scss';

const allItems: PortalNavigationItem[] = [
    { id: 'home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 1, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
];

const emptyUserMenuProps = {
    userMenuRootItems: [],
    allNavItems: [],
    hasUserMenuItems: false,
    onAddUserMenuNavItem: jest.fn().mockResolvedValue(undefined),
    onAddUserMenuLink: jest.fn().mockResolvedValue(undefined),
    onUpdateNavItem: jest.fn(),
    onRequestDeleteNavItem: jest.fn(),
    onSelectNavItem: jest.fn(),
};

const profileUserMenuLink = {
    id: 'menu-profile',
    portalId: 'p1',
    title: 'Profile',
    type: 'LINK' as const,
    parentId: null,
    order: 0,
    slug: 'profile-menu',
    url: '/profile',
    area: 'USER_MENU' as const,
};

const userMenuPropsWithProfile = {
    ...emptyUserMenuProps,
    hasUserMenuItems: true,
    userMenuRootItems: [profileUserMenuLink],
    allNavItems: [profileUserMenuLink],
};

describe('Sidebar', () => {
    it('should render folder subtree in folder scope', () => {
        renderPortalUi(
            <Sidebar
                scope="folder"
                rootFolder={allItems[1]}
                allItems={allItems}
                selectedNavItemId="quick-start"
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('navigation', { name: 'Portal navigation' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Guides' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Quick Start' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Home' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Back to main navigation' })).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Portal icon')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('User menu')).not.toBeInTheDocument();
    });

    it('should show sidebar chrome in folder scope when showSidebarChrome is enabled', () => {
        renderPortalUi(
            <Sidebar
                scope="folder"
                rootFolder={allItems[1]}
                allItems={allItems}
                selectedNavItemId="quick-start"
                mode="edit"
                showSidebarChrome
                userMenuProps={userMenuPropsWithProfile}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Change portal icon')).toBeInTheDocument();
        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
    });

    it('should return to main navigation when the sidebar chrome is clicked in folder scope', async () => {
        const user = userEvent.setup();
        const onBackToMainNavigation = jest.fn();

        renderPortalUi(
            <Sidebar
                scope="folder"
                rootFolder={allItems[1]}
                allItems={allItems}
                selectedNavItemId="quick-start"
                mode="preview"
                showSidebarChrome
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
                onBackToMainNavigation={onBackToMainNavigation}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Back to main navigation' }));

        expect(onBackToMainNavigation).toHaveBeenCalledTimes(1);
    });

    it('should render all root items in full scope', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0], allItems[1]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
    });

    it('should return null when there are no items to show in folder scope', () => {
        const { container } = renderPortalUi(
            <Sidebar
                scope="folder"
                rootFolder={null}
                allItems={allItems}
                selectedNavItemId={null}
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('should render empty folder sidebar with add button in edit mode', () => {
        renderPortalUi(
            <Sidebar
                scope="folder"
                rootFolder={allItems[1]}
                allItems={[allItems[1]]}
                selectedNavItemId="guides"
                mode="edit"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('navigation', { name: 'Portal navigation' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Guides' })).not.toBeInTheDocument();
        expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
    });

    it('should not render user menu in preview mode when there are no items', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0], allItems[1]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.queryByLabelText('User menu')).not.toBeInTheDocument();
    });

    it('should render user menu in preview mode when items exist', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0], allItems[1]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="preview"
                userMenuProps={userMenuPropsWithProfile}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
    });

    it('should render empty full scope sidebar with add button in edit mode', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[]}
                allItems={[]}
                selectedNavItemId={null}
                mode="edit"
                userMenuProps={emptyUserMenuProps}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
        expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
    });

    it('should show change portal icon button in edit mode for full scope', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="edit"
                onPortalIconChange={jest.fn()}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Change portal icon')).toBeInTheDocument();
    });

    it('should show reset to default when a custom icon is set in edit mode', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="edit"
                portalIconUrl="data:image/png;base64,abc"
                onPortalIconChange={jest.fn()}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Reset to default' })).toHaveClass(styles.resetOverlay);
    });

    it('should hide reset to default when using the default icon', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="edit"
                portalIconUrl=""
                onPortalIconChange={jest.fn()}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.queryByRole('button', { name: 'Reset to default' })).not.toBeInTheDocument();
    });

    it('should reset portal icon to default when reset is clicked', async () => {
        const user = userEvent.setup();
        const onPortalIconChange = jest.fn();

        const { container } = renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="edit"
                portalIconUrl="data:image/png;base64,abc"
                onPortalIconChange={onPortalIconChange}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        const iconWrapper = container.querySelector(`.${styles.wrapper}`);
        expect(iconWrapper).not.toBeNull();
        await user.hover(iconWrapper as Element);
        await user.click(screen.getByRole('button', { name: 'Reset to default' }));

        expect(onPortalIconChange).toHaveBeenCalledWith('');
    });

    it('should allow editing the portal label in edit mode', async () => {
        const user = userEvent.setup();
        const onPortalLabelChange = jest.fn();

        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="edit"
                portalLabel={DEFAULT_PORTAL_LABEL}
                onPortalLabelChange={onPortalLabelChange}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.dblClick(screen.getByLabelText('Portal label'));
        const input = screen.getByRole('textbox', { name: 'Portal label' });
        await user.clear(input);
        await user.type(input, 'Partner Portal{Enter}');

        expect(onPortalLabelChange).toHaveBeenCalledWith('Partner Portal');
    });

    it('should render portal label as read-only text in preview mode', () => {
        renderPortalUi(
            <Sidebar
                scope="full"
                rootItems={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="preview"
                portalLabel={DEFAULT_PORTAL_LABEL}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Portal label' })).not.toBeInTheDocument();
    });
});
