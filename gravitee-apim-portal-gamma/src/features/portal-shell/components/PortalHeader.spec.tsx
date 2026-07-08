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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PortalHeader } from './PortalHeader';

const rootItems = [
    {
        id: 'nav-home',
        portalId: 'portal-1',
        title: 'Home',
        type: 'PAGE' as const,
        parentId: null,
        order: 0,
        slug: 'home',
    },
    {
        id: 'nav-about',
        portalId: 'portal-1',
        title: 'About',
        type: 'PAGE' as const,
        parentId: null,
        order: 1,
        slug: 'about',
    },
];

const defaultProps = {
    portalId: 'portal-1',
    portalIconUrl: '',
    rootItems,
    allNavItems: rootItems,
    selectedNavItemId: 'nav-home',
    mode: 'preview' as const,
    onSelectNavItem: jest.fn(),
    onAddNavItem: jest.fn(),
    onAddLinkFromPage: jest.fn(),
    onUpdateNavItem: jest.fn(),
    onPortalIconChange: jest.fn(),
    onRequestDeleteNavItem: jest.fn(),
    userMenuProps: {
        userMenuItems: [],
        hasUserMenuItems: false,
        onAddUserMenuNavItem: jest.fn(),
        onAddUserMenuLinkFromPage: jest.fn(),
        onUpdateUserMenuNavItem: jest.fn(),
        onRequestDeleteUserMenuNavItem: jest.fn(),
    },
    portalPages: [],
    getPagePath: (slug: string) => `/portals/portal-1/${slug}`,
};

describe('PortalHeader', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should render root navigation items in preview mode', () => {
        render(<PortalHeader {...defaultProps} />);

        expect(screen.getByText('Home')).toBeInTheDocument();
        expect(screen.getByText('About')).toBeInTheDocument();
    });

    it('should open mobile navigation drawer when menu button is clicked', async () => {
        const user = userEvent.setup();

        render(<PortalHeader {...defaultProps} />);

        await user.click(screen.getByRole('button', { name: 'Open navigation menu' }));

        expect(screen.getByRole('navigation', { name: 'Navigation' })).toBeInTheDocument();
        expect(screen.getAllByText('Home').length).toBeGreaterThan(1);
    });

    it('should close mobile navigation drawer when close button is clicked', async () => {
        const user = userEvent.setup();

        render(<PortalHeader {...defaultProps} />);

        await user.click(screen.getByRole('button', { name: 'Open navigation menu' }));
        await user.click(screen.getByRole('button', { name: 'Close menu' }));

        expect(screen.queryByRole('navigation', { name: 'Navigation' })).not.toBeInTheDocument();
    });
});
