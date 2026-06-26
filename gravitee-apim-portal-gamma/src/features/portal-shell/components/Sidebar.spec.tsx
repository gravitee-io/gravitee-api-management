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
import { screen } from '@testing-library/react';

import type { PortalNavigationItem } from '../../portals/types';
import { Sidebar } from './Sidebar';

const allItems: PortalNavigationItem[] = [
    { id: 'home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home' },
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 1, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
];

describe('Sidebar', () => {
    it('should render folder subtree in folder scope', () => {
        renderWithGraphene(
            <Sidebar
                scope="folder"
                rootFolder={allItems[1]}
                allItems={allItems}
                selectedNavItemId="quick-start"
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('navigation', { name: 'Portal navigation' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Quick Start' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Home' })).not.toBeInTheDocument();
    });

    it('should render all root items in full scope', () => {
        renderWithGraphene(
            <Sidebar
                scope="full"
                rootItems={[allItems[0], allItems[1]]}
                allItems={allItems}
                selectedNavItemId="home"
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Guides' })).toBeInTheDocument();
    });

    it('should return null when there are no items to show', () => {
        const { container } = renderWithGraphene(
            <Sidebar
                scope="folder"
                rootFolder={null}
                allItems={allItems}
                selectedNavItemId={null}
                mode="preview"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(container).toBeEmptyDOMElement();
    });
});
