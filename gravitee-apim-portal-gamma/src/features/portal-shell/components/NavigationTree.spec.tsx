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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { NavigationTree } from './NavigationTree';

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

const allItems: PortalNavigationItem[] = [
    { id: 'guides', portalId: 'p1', title: 'Guides', type: 'FOLDER', parentId: null, order: 0, slug: 'guides' },
    { id: 'quick-start', portalId: 'p1', title: 'Quick Start', type: 'PAGE', parentId: 'guides', order: 0, slug: 'quick-start' },
];

describe('NavigationTree', () => {
    it('should render nested items and call onSelectNavItem', async () => {
        const user = userEvent.setup();
        const onSelectNavItem = jest.fn();

        renderWithGraphene(
            <NavigationTree
                items={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId="quick-start"
                mode="preview"
                onSelectNavItem={onSelectNavItem}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.click(screen.getByRole('button', { name: 'Quick Start' }));
        expect(onSelectNavItem).toHaveBeenCalledWith('quick-start');
    });

    it('should open API dialog when API is chosen from context menu', async () => {
        const user = userEvent.setup();

        renderWithGraphene(
            <NavigationTree
                items={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="edit"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByLabelText('Edit Guides') });
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        expect(screen.getByRole('dialog', { name: 'Select an API' })).toBeInTheDocument();
    });

    it('should call onAddApiNavItem when an API is selected from the dialog', async () => {
        const user = userEvent.setup();
        const onAddApiNavItem = jest.fn().mockResolvedValue(undefined);

        renderWithGraphene(
            <NavigationTree
                items={[allItems[0]]}
                allItems={allItems}
                selectedNavItemId={null}
                mode="edit"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={onAddApiNavItem}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByLabelText('Edit Guides') });
        await user.click(screen.getByRole('menuitem', { name: 'API' }));

        await waitFor(() => {
            expect(screen.getByRole('option', { name: /Payments API/i })).toBeInTheDocument();
        });

        await user.click(screen.getByRole('option', { name: /Payments API/i }));

        expect(onAddApiNavItem).toHaveBeenCalledWith('api-payments', 'Payments API', 'guides');
    });

    it('should show root add button in full-tree mode', () => {
        renderWithGraphene(
            <NavigationTree
                items={allItems}
                allItems={allItems}
                selectedNavItemId={null}
                mode="edit"
                showRootAddButton
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        expect(screen.getAllByLabelText('Add navigation item').length).toBeGreaterThan(0);
    });

    it('should not offer API in context menu under an API item', async () => {
        const user = userEvent.setup();
        const itemsWithApi: PortalNavigationItem[] = [
            allItems[0],
            {
                id: 'payments-api',
                portalId: 'p1',
                title: 'Payments API',
                type: 'API',
                apiId: 'api-payments',
                parentId: 'guides',
                order: 1,
                slug: 'payments-api',
            } as PortalNavigationApi,
            {
                id: 'overview',
                portalId: 'p1',
                title: 'Overview',
                type: 'PAGE',
                parentId: 'payments-api',
                order: 0,
                slug: 'overview',
            },
        ];

        renderWithGraphene(
            <NavigationTree
                items={[itemsWithApi[0]]}
                allItems={itemsWithApi}
                selectedNavItemId="overview"
                mode="edit"
                onSelectNavItem={jest.fn()}
                onAddNavItem={jest.fn()}
                onAddApiNavItem={jest.fn().mockResolvedValue(undefined)}
                onUpdateNavItem={jest.fn()}
                onRequestDeleteNavItem={jest.fn()}
            />,
        );

        await user.pointer({ keys: '[MouseRight>]', target: screen.getByLabelText('Edit Payments API') });

        expect(screen.queryByRole('menuitem', { name: 'API' })).not.toBeInTheDocument();
    });
});
