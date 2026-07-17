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
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { PortalTile } from './PortalTile';
import { createDefaultPortalScreenshot } from '../storage/dummy-portals';
import { DEFAULT_PORTAL_LABEL } from '../types';

const portalWithScreenshot = {
    id: 'portal-1',
    name: 'Test Portal',
    screenshotDataUrl: 'data:image/png;base64,abc',
    updatedAt: new Date().toISOString(),
    layout: 'header-content-footer' as const,
    showFooter: true,
    pageWidth: 'narrow' as const,
    portalIconUrl: '',
    portalLabel: DEFAULT_PORTAL_LABEL,
    footerLinks: [],
    userMenuItems: [],
};

const portalWithPlaceholder = {
    ...portalWithScreenshot,
    screenshotDataUrl: createDefaultPortalScreenshot('Test Portal'),
};

function renderTile(portal = portalWithScreenshot, initialEntry = '/', onRequestDelete = jest.fn()) {
    return renderWithGraphene(
        <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
                <Route path="/" element={<PortalTile portal={portal} onRequestDelete={onRequestDelete} />} />
                <Route path="/portals/:id" element={<div>View page</div>} />
                <Route path="/portals/:id/edit" element={<div>Edit page</div>} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('PortalTile', () => {
    it('should render a skeleton when the screenshot is a placeholder', () => {
        renderTile(portalWithPlaceholder);

        expect(screen.queryByRole('img')).not.toBeInTheDocument();
        expect(screen.getByText('Test Portal')).toBeInTheDocument();
    });

    it('should show action buttons on hover', () => {
        renderTile();

        expect(screen.queryByRole('link', { name: 'Open portal' })).not.toBeInTheDocument();

        fireEvent.mouseEnter(screen.getByText('Test Portal').closest('[tabindex="0"]')!);

        expect(screen.getByRole('link', { name: 'Open portal' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Edit portal' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Delete portal' })).toBeInTheDocument();
    });

    it('should navigate to view page when open is clicked', async () => {
        const user = userEvent.setup();
        renderTile();

        fireEvent.mouseEnter(screen.getByText('Test Portal').closest('[tabindex="0"]')!);
        await user.click(screen.getByRole('link', { name: 'Open portal' }));

        expect(screen.getByText('View page')).toBeInTheDocument();
    });

    it('should navigate to edit page when edit is clicked', async () => {
        const user = userEvent.setup();
        renderTile();

        fireEvent.mouseEnter(screen.getByText('Test Portal').closest('[tabindex="0"]')!);
        await user.click(screen.getByRole('link', { name: 'Edit portal' }));

        expect(screen.getByText('Edit page')).toBeInTheDocument();
    });

    it('should call onRequestDelete when delete is clicked', async () => {
        const user = userEvent.setup();
        const onRequestDelete = jest.fn();
        renderTile(portalWithScreenshot, '/', onRequestDelete);

        fireEvent.mouseEnter(screen.getByText('Test Portal').closest('[tabindex="0"]')!);
        await user.click(screen.getByRole('button', { name: 'Delete portal' }));

        expect(onRequestDelete).toHaveBeenCalledTimes(1);
    });
});
