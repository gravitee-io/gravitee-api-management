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

import type { DeveloperPortal, PortalNavigationItem } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import { getPortalPages } from '../utils/portal-pages';
import { SidebarLayout } from './SidebarLayout';

jest.mock('./ContentArea', () => ({
    ContentArea: () => <div data-testid="content-area">Content</div>,
}));

const mockPortal: DeveloperPortal = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'sidebar-content',
    portalIconUrl: '',
    portalLabel: DEFAULT_PORTAL_LABEL,
    footerLinks: [],
    userMenuItems: [],
};

const rootItems: PortalNavigationItem[] = [
    { id: 'home', portalId: 'portal-1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
    { id: 'guides', portalId: 'portal-1', title: 'Guides', type: 'FOLDER', parentId: null, order: 1, slug: 'guides' },
];

const portalPages = getPortalPages(rootItems);
const getPagePath = (slug: string) => `/portals/portal-1/${slug}`;

const baseProps = {
    portal: mockPortal,
    navItems: rootItems,
    rootItems,
    selectedNavItemId: 'home',
    pageWidth: 'medium' as const,
    portalPages,
    getPagePath,
    onSelectNavItem: jest.fn(),
    onAddNavItem: jest.fn(),
    onAddApiNavItem: jest.fn().mockResolvedValue(undefined),
    onUpdateNavItem: jest.fn(),
    onRequestDeleteNavItem: jest.fn(),
    onPortalIconChange: jest.fn(),
    onPortalLabelChange: jest.fn(),
    onUserMenuChange: jest.fn(),
};

describe('SidebarLayout', () => {
    it('should render full navigation tree with portal icon and no user menu when empty in preview', () => {
        renderPortalUi(<SidebarLayout {...baseProps} mode="preview" />);

        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.queryByLabelText('User menu')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
        expect(screen.getByTestId('content-area')).toBeInTheDocument();
    });

    it('should render user menu in preview mode when items exist', () => {
        renderPortalUi(
            <SidebarLayout
                {...baseProps}
                mode="preview"
                portal={{
                    ...mockPortal,
                    userMenuItems: [{ id: 'menu-profile', label: 'Profile', url: '/profile' }],
                }}
            />,
        );

        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
    });

    it('should not render portal header or footer', () => {
        renderPortalUi(<SidebarLayout {...baseProps} mode="preview" />);

        expect(screen.queryByLabelText('Add navigation item')).not.toBeInTheDocument();
        expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
    });

    it('should show editable portal icon and user menu in edit mode', () => {
        renderPortalUi(<SidebarLayout {...baseProps} mode="edit" />);

        expect(screen.getByLabelText('Change portal icon')).toBeInTheDocument();
        expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
    });
});
