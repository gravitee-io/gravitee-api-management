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

import { installFakeIndexedDB, resetFakeIndexedDB } from '../../../testing/fake-indexeddb';
import { saveNavItem } from '../../portals/storage/navigation-items.storage';
import { savePageContent } from '../../portals/storage/page-contents.storage';
import type { DeveloperPortal } from '../../portals/types';
import { PortalShell } from './PortalShell';

installFakeIndexedDB();

jest.mock('../../editor/components/BlockEditor', () => ({
    BlockEditor: () => <div data-testid="block-editor">Editor</div>,
}));

jest.mock('../../editor/components/BlockViewer', () => ({
    BlockViewer: () => <div data-testid="block-viewer">Viewer</div>,
}));

const mockPortal: DeveloperPortal = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: '',
    updatedAt: new Date().toISOString(),
    layout: 'header-content-footer',
    portalIconUrl: '',
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
            expect(screen.getByTestId('block-editor')).toBeInTheDocument();
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
            expect(screen.getByTestId('block-viewer')).toBeInTheDocument();
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
});
