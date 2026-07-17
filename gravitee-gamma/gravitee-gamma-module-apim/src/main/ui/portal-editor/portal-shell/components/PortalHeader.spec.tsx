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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PortalHeader } from './PortalHeader';

jest.mock('../../editor/services/api.service', () => ({
    searchApis: jest.fn().mockResolvedValue({
        data: [
            {
                id: 'api-payments',
                name: 'Payments API',
                version: '1.0.0',
                definitionVersion: 'V4',
                description: 'Payments',
                entrypoints: [],
                owner: { id: 'u1', displayName: 'Team' },
            },
        ],
        metadata: { pagination: { total: 1, current_page: 1, size: 20, total_pages: 1 } },
    }),
}));

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
        userMenuRootItems: [],
        allNavItems: rootItems,
        hasUserMenuItems: false,
        onAddUserMenuNavItem: jest.fn().mockResolvedValue(undefined),
        onAddUserMenuLink: jest.fn().mockResolvedValue(undefined),
        onUpdateNavItem: jest.fn(),
        onRequestDeleteNavItem: jest.fn(),
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

    it('should offer API in the header add menu in edit mode', async () => {
        const user = userEvent.setup();

        renderWithGraphene(<PortalHeader {...defaultProps} mode="edit" />);

        await user.click(screen.getByLabelText('Add navigation item'));

        expect(screen.getByRole('menuitem', { name: 'API' })).toBeInTheDocument();
    });

    it('should call onAddApiNavItem with null parent when an API is selected from the header menu', async () => {
        const user = userEvent.setup();
        const onAddApiNavItem = jest.fn().mockResolvedValue(undefined);

        renderWithGraphene(
            <PortalHeader {...defaultProps} mode="edit" onAddApiNavItem={onAddApiNavItem} />,
        );

        await user.click(screen.getByLabelText('Add navigation item'));
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        await waitFor(() => {
            expect(screen.getByRole('option', { name: /Payments API/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole('option', { name: /Payments API/i }));

        expect(onAddApiNavItem).toHaveBeenCalledWith('api-payments', 'Payments API', null);
    });
});
