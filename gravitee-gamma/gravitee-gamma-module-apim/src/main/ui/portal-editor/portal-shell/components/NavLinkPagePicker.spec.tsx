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

import type { PortalNavigationPage } from '../../portals/types';
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { NavLinkPagePicker } from './NavLinkPagePicker';

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId: 'p1', title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

describe('NavLinkPagePicker', () => {
    it('should filter pages while searching', async () => {
        renderPortalUi(
            <NavLinkPagePicker pages={portalPages} onSelect={jest.fn()} onCancel={jest.fn()} />,
        );

        const searchInput = screen.getByRole('textbox', { name: 'Search for a page' });
        fireEvent.change(searchInput, { target: { value: 'home' } });

        await waitFor(() => {
            expect(screen.getByRole('option', { name: 'Home' })).toBeInTheDocument();
            expect(screen.queryByRole('option', { name: 'About' })).not.toBeInTheDocument();
        });
    });

    it('should call onSelect when a page is chosen', () => {
        const onSelect = jest.fn();

        renderPortalUi(
            <NavLinkPagePicker pages={portalPages} onSelect={onSelect} onCancel={jest.fn()} />,
        );

        fireEvent.click(screen.getByRole('option', { name: 'About' }));

        expect(onSelect).toHaveBeenCalledWith(portalPages[1]);
    });

    it('should call onCancel on Escape', () => {
        const onCancel = jest.fn();

        renderPortalUi(
            <NavLinkPagePicker pages={portalPages} onSelect={jest.fn()} onCancel={onCancel} />,
        );

        fireEvent.keyDown(screen.getByRole('textbox', { name: 'Search for a page' }), { key: 'Escape' });

        expect(onCancel).toHaveBeenCalled();
    });
});
