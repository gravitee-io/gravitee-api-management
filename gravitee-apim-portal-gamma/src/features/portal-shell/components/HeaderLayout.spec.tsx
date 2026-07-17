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

import type { DeveloperPortal, PortalNavigationItem, PortalNavigationLink } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import { getPortalPages } from '../utils/portal-pages';
import { HeaderLayout } from './HeaderLayout';

jest.mock('./ContentArea', () => ({
    ContentArea: () => <div data-testid="content-area">Content</div>,
}));

const mockPortal: DeveloperPortal = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'header-content-footer',
    showFooter: true,
    pageWidth: 'narrow',
    portalIconUrl: '',
    portalLabel: DEFAULT_PORTAL_LABEL,
    footerLinks: [],
    userMenuItems: [],
};

const rootItems: PortalNavigationItem[] = [
    { id: 'home', portalId: 'portal-1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
];

const footerItems: PortalNavigationLink[] = [
    {
        id: 'footer-docs',
        portalId: 'portal-1',
        title: 'Docs',
        type: 'LINK',
        parentId: null,
        order: 0,
        slug: 'docs-footer001',
        url: 'https://docs.example.com',
        area: 'FOOTER',
    },
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

const portalPages = getPortalPages(rootItems);
const getPagePath = (slug: string) => `/portals/portal-1/${slug}`;

const baseProps = {
    portal: mockPortal,
    navItems: [...rootItems, ...footerItems],
    rootItems,
    footerItems,
    selectedNavItemId: 'home',
    pageWidth: 'medium' as const,
    portalPages,
    getPagePath,
    onSelectNavItem: jest.fn(),
    onAddNavItem: jest.fn(),
    onAddApiNavItem: jest.fn().mockResolvedValue(undefined),
    onAddApiProductNavItem: jest.fn().mockResolvedValue(undefined),
    onAddLinkFromPage: jest.fn(),
    onAddFooterLinkFromPage: jest.fn(),
    onUpdateNavItem: jest.fn(),
    onRequestDeleteNavItem: jest.fn(),
    onPortalIconChange: jest.fn(),
    userMenuProps: emptyUserMenuProps,
};

describe('HeaderLayout', () => {
    it('should render footer when showFooter is true', () => {
        renderPortalUi(<HeaderLayout {...baseProps} showFooter mode="preview" />);

        expect(screen.getByRole('contentinfo')).toBeInTheDocument();
        expect(screen.getByText('Docs')).toBeInTheDocument();
    });

    it('should hide footer when showFooter is false', () => {
        renderPortalUi(<HeaderLayout {...baseProps} showFooter={false} mode="preview" />);

        expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
        expect(screen.queryByText('Docs')).not.toBeInTheDocument();
    });
});
