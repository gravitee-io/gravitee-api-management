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

import type { PortalNavigationPage, UserMenuItem } from '../../portals/types';
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { UserMenu } from './UserMenu';

const portalId = 'p1';

const sampleItems: UserMenuItem[] = [
    { id: 'menu-profile', label: 'Profile', url: '/profile' },
    { id: 'menu-logout', label: 'Log out', url: '/logout' },
];

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId, title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId, title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

const getPagePath = (slug: string) => `/portals/${portalId}/edit/${slug}`;

const baseProps = {
    portalId,
    portalPages,
    getPagePath,
};

describe('UserMenu', () => {
    it('should open dropdown and render stored items in preview mode', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={sampleItems}
                mode="preview"
                onNavigate={jest.fn()}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByRole('menuitem', { name: 'Profile' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Log out' })).toBeInTheDocument();
    });

    it('should navigate via onNavigate for internal links in preview mode', async () => {
        const user = userEvent.setup();
        const onNavigate = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={[{ id: 'menu-about', label: 'About', url: 'about-def456' }]}
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
                items={sampleItems}
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

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={[{ id: 'menu-docs', label: 'Docs', url: 'https://docs.example.com' }]}
                mode="preview"
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByRole('menuitem', { name: 'Docs' })).toHaveAttribute('target', '_blank');
        expect(screen.getByRole('menuitem', { name: 'Docs' })).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should not render in preview mode when there are no items', () => {
        const { container } = renderPortalUi(
            <UserMenu {...baseProps} items={[]} mode="preview" />,
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('should add a menu item from the page picker in edit mode and keep menu open', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={sampleItems}
                mode="edit"
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'Add user menu item' }));
        expect(screen.getByRole('textbox', { name: 'Search for a page' })).toBeInTheDocument();
        await user.click(screen.getByRole('option', { name: 'About' }));

        expect(onChange).toHaveBeenCalledWith([
            ...sampleItems,
            expect.objectContaining({
                label: 'About',
                url: 'about-def456',
            }),
        ]);
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Add user menu item' })).toBeInTheDocument();
    });

    it('should filter pages while searching in the page picker', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu {...baseProps} items={[]} mode="edit" onChange={jest.fn()} />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'Add user menu item' }));

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
            <UserMenu {...baseProps} items={[]} mode="edit" onChange={jest.fn()} />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'Add user menu item' }));

        const searchInput = screen.getByRole('textbox', { name: 'Search for a page' });
        fireEvent.keyDown(searchInput, { key: 'Escape' });

        expect(screen.queryByRole('textbox', { name: 'Search for a page' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Add user menu item' })).toBeInTheDocument();
    });

    it('should remove a menu item in edit mode', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={sampleItems}
                mode="edit"
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'Remove Profile' }));

        expect(onChange).toHaveBeenCalledWith([sampleItems[1]]);
    });

    it('should persist label edits in edit mode', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={sampleItems}
                mode="edit"
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'User menu item label: Profile' }));
        const input = screen.getByRole('textbox', { name: 'User menu item label: Profile' });
        fireEvent.change(input, { target: { value: 'My Profile' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        expect(onChange).toHaveBeenCalledWith([
            { ...sampleItems[0], label: 'My Profile' },
            sampleItems[1],
        ]);
    });

    it('should persist URL edits in edit mode', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={sampleItems}
                mode="edit"
                onChange={onChange}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));
        await user.click(screen.getByRole('button', { name: 'User menu item URL: Profile' }));
        const input = screen.getByRole('textbox', { name: 'User menu item URL: Profile' });
        fireEvent.change(input, { target: { value: '/my-profile' } });
        fireEvent.keyDown(input, { key: 'Enter' });

        expect(onChange).toHaveBeenCalledWith([
            { ...sampleItems[0], url: '/my-profile' },
            sampleItems[1],
        ]);
    });

    it('should display page slug instead of full path for portal page links in edit mode', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <UserMenu
                {...baseProps}
                items={[{ id: 'menu-about', label: 'About', url: '/portals/p1/about-def456' }]}
                mode="edit"
                onChange={jest.fn()}
            />,
        );

        await user.click(screen.getByLabelText('User menu'));

        expect(screen.getByRole('button', { name: 'User menu item URL: About' })).toHaveTextContent('about-def456');
    });
});
