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
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { installFakeIndexedDB, resetFakeIndexedDB } from '../../../testing/fake-indexeddb';
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import { savePageContent } from '../../portals/storage/page-contents.storage';
import type { DeveloperPortal } from '../../portals/types';
import { DEFAULT_PORTAL_LABEL } from '../../portals/types';
import { PortalShell } from './PortalShell';

installFakeIndexedDB();

jest.mock('../../editor/components/BlockEditor', () => ({
    BlockEditor: () => <div data-testid="block-editor">Editor</div>,
}));

jest.mock('../../editor/components/BlockViewer', () => ({
    BlockViewer: () => <div data-testid="block-viewer">Viewer</div>,
}));

jest.mock('./ContentArea', () => ({
    ContentArea: () => <div data-testid="content-area">Content</div>,
}));

const mockPortal: DeveloperPortal = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'header-content-footer',
    portalIconUrl: '',
    portalLabel: DEFAULT_PORTAL_LABEL,
    footerLinks: [],
    userMenuItems: [],
};

describe('PortalShell', () => {
    beforeEach(async () => {
        resetFakeIndexedDB();
        await saveNavItem({
            id: 'nav-home',
            portalId: 'portal-1',
            title: 'Home',
            type: 'PAGE',
            parentId: null,
            order: 0,
            slug: 'home-abc123',
        });
        await saveNavItem({
            id: 'nav-about',
            portalId: 'portal-1',
            title: 'About',
            type: 'PAGE',
            parentId: null,
            order: 1,
            slug: 'about-def456',
        });
        await saveNavItem({
            id: 'footer-docs',
            portalId: 'portal-1',
            title: 'Docs',
            type: 'LINK',
            parentId: null,
            order: 0,
            slug: 'docs-footer001',
            url: 'https://docs.example.com',
            area: 'FOOTER',
        });
        await savePageContent({
            id: 'pc-home',
            portalId: 'portal-1',
            navigationItemId: 'nav-home',
            document: [{ type: 'paragraph', content: [{ type: 'text', text: 'Hello', styles: {} }], children: [] }],
        });
    });

    it('should render header layout with portal header and footer', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(screen.getByText('About')).toBeInTheDocument();
        expect(screen.getByText('Docs')).toBeInTheDocument();
        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
    });

    it('should hide user menu in preview mode when there are no items', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="preview"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(screen.queryByLabelText('User menu')).not.toBeInTheDocument();
    });

    it('should render user menu in edit mode even when there are no items', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByLabelText('User menu')).toBeInTheDocument();
        });
    });

    it('should render block editor in edit mode', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByTestId('content-area')).toBeInTheDocument();
        });
    });

    it('should render block viewer in preview mode', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="preview"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByTestId('content-area')).toBeInTheDocument();
        });
    });

    it('should show add nav item button in edit mode', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByLabelText('Add navigation item')).toBeInTheDocument();
        });
    });

    it('should hide add nav item button in preview mode', async () => {
        render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="preview"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(screen.queryByLabelText('Add navigation item')).not.toBeInTheDocument();
    });

    it('should render sidebar layout with full tree and no header or footer', async () => {
        render(
            <PortalShell
                portal={{ ...mockPortal, layout: 'sidebar-content' }}
                layout="sidebar-content"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(screen.getByText('About')).toBeInTheDocument();
        expect(screen.getByLabelText('Portal icon')).toBeInTheDocument();
        expect(screen.getByLabelText('User menu')).toBeInTheDocument();
        expect(screen.queryByText('Docs')).not.toBeInTheDocument();
        expect(screen.getByTestId('content-area')).toBeInTheDocument();
    });

    it('should apply edit-mode class in edit mode', async () => {
        const { container } = render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="edit"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(container.querySelector('.edit-mode')).toBeInTheDocument();
    });

    it('should not apply edit-mode class in preview mode', async () => {
        const { container } = render(
            <PortalShell
                portal={mockPortal}
                layout="header-content-footer"
                mode="preview"
                pageWidth="narrow"
                onPortalChange={jest.fn()}
            />,
        );

        await waitFor(() => {
            expect(screen.getByText('Home')).toBeInTheDocument();
        });

        expect(container.querySelector('.edit-mode')).not.toBeInTheDocument();
    });

    it('should show 404 page when slug does not match a page', async () => {
        render(
            <MemoryRouter>
                <PortalShell
                    portal={mockPortal}
                    layout="header-content-footer"
                    mode="preview"
                    pageWidth="narrow"
                    onPortalChange={jest.fn()}
                    slug="missing-page"
                    getPagePath={pageSlug => `/portals/portal-1/${pageSlug}`}
                    onNavigate={jest.fn()}
                />
            </MemoryRouter>,
        );

        await waitFor(() => {
            expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
        });

        expect(screen.getByRole('link', { name: 'Back to homepage' })).toHaveAttribute('href', '/portals/portal-1');
        expect(screen.queryByTestId('block-viewer')).not.toBeInTheDocument();
    });
});
