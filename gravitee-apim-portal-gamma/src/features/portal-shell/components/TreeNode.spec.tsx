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
import userEvent from '@testing-library/user-event';

import type { PortalNavigationItem } from '../../portals/types';
import { TreeNode } from './TreeNode';

const allItems: PortalNavigationItem[] = [
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
];

describe('TreeNode', () => {
    it('should call onSelectNavItem when a page is clicked', async () => {
        const user = userEvent.setup();
        const onSelectNavItem = jest.fn();

        renderWithGraphene(
            <TreeNode
                item={allItems[1]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="preview"
                depth={1}
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={jest.fn()}
                onRequestApi={jest.fn()}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Quick Start' }));
        expect(onSelectNavItem).toHaveBeenCalledWith('quick-start');
    });

    it('should toggle folder children visibility', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <TreeNode
                item={allItems[0]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="preview"
                depth={0}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onRequestApi={jest.fn()}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: 'Quick Start' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Collapse Guides' }));
        expect(screen.queryByRole('button', { name: 'Quick Start' })).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Expand Guides' }));
        expect(screen.getByRole('button', { name: 'Quick Start' })).toBeInTheDocument();
    });

    it('should not render nested add button under expanded folder in edit mode', () => {
        renderWithGraphene(
            <TreeNode
                item={allItems[0]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="edit"
                depth={0}
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onRequestApi={jest.fn()}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.queryByLabelText('Add navigation item')).not.toBeInTheDocument();
    });

    it('should add a child via context menu on folder in edit mode', async () => {
        const user = userEvent.setup();
        const onAddNavItem = jest.fn();

        renderWithGraphene(
            <TreeNode
                item={allItems[0]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="edit"
                depth={0}
                onSelectNavItem={jest.fn()}
                onAddNavItem={onAddNavItem}
                onRequestApi={jest.fn()}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByLabelText('Edit Guides') });
        await user.click(screen.getByRole('menuitem', { name: 'Page' }));

        expect(onAddNavItem).toHaveBeenCalledWith('PAGE', 'guides');
    });
});
