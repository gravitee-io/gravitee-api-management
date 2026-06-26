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

describe('SidebarLayout', () => {
    it('should render full navigation tree with portal icon and user menu', () => {
        renderPortalUi(
            <SidebarLayout
                portal={mockPortal}
                navItems={rootItems}
                rootItems={rootItems}
                selectedNavItemId="home"
                mode="preview"
                pageWidth="medium"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
                onPortalIconChange={jest.fn()}
                onPortalLabelChange={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByText(DEFAULT_PORTAL_LABEL)).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
        expect(screen.getByTestId('content-area')).toBeInTheDocument();
    });

    it('should not render portal header or footer', () => {
        renderPortalUi(
            <SidebarLayout
                portal={mockPortal}
                navItems={rootItems}
                rootItems={rootItems}
                selectedNavItemId="home"
                mode="preview"
                pageWidth="medium"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
                onPortalIconChange={jest.fn()}
                onPortalLabelChange={jest.fn()}
            />,
        );

        expect(screen.queryByLabelText('Add navigation item')).not.toBeInTheDocument();
        expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
    });

    it('should show editable portal icon in edit mode', () => {
        renderPortalUi(
            <SidebarLayout
                portal={mockPortal}
                navItems={rootItems}
                rootItems={rootItems}
                selectedNavItemId="home"
                mode="edit"
                pageWidth="medium"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
                onPortalIconChange={jest.fn()}
                onPortalLabelChange={jest.fn()}
            />,
        );

        expect(screen.getByLabelText('Change portal icon')).toBeInTheDocument();
        expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
    });
});
