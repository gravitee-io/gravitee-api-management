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
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import type { PortalNavigationPage } from '../../portals/types';
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { LinkUrlDropdown } from './LinkUrlDropdown';

const portalPages: PortalNavigationPage[] = [
    { id: 'page-home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
    { id: 'page-about', portalId: 'p1', title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
];

describe('LinkUrlDropdown', () => {
    it('should open on trigger click and update URL from custom input', async () => {
        const user = userEvent.setup();
        const onUrlChange = jest.fn();

        renderPortalUi(
            <LinkUrlDropdown
                url="https://example.com"
                portalPages={portalPages}
                portalId="p1"
                onUrlChange={onUrlChange}
            >
                <button type="button">Edit link</button>
            </LinkUrlDropdown>,
        );

        await user.click(screen.getByRole('button', { name: 'Edit link' }));

        const customInput = screen.getByRole('textbox', { name: 'Custom URL' });
        fireEvent.change(customInput, { target: { value: '/docs' } });
        await user.click(screen.getByRole('button', { name: 'Apply' }));

        expect(onUrlChange).toHaveBeenCalledWith('/docs');
    });

    it('should update URL when selecting a portal page', async () => {
        const user = userEvent.setup();
        const onUrlChange = jest.fn();

        renderPortalUi(
            <LinkUrlDropdown
                url="#"
                portalPages={portalPages}
                portalId="p1"
                onUrlChange={onUrlChange}
            >
                <button type="button">Edit link</button>
            </LinkUrlDropdown>,
        );

        await user.click(screen.getByRole('button', { name: 'Edit link' }));
        await user.click(screen.getByRole('option', { name: 'About' }));

        expect(onUrlChange).toHaveBeenCalledWith('about-def456');
    });

    it('should not open on trigger click when openOnClick is false', async () => {
        const user = userEvent.setup();

        renderPortalUi(
            <LinkUrlDropdown
                url="home-abc123"
                portalPages={portalPages}
                portalId="p1"
                onUrlChange={jest.fn()}
                openOnClick={false}
            >
                <button type="button">Button label</button>
            </LinkUrlDropdown>,
        );

        await user.click(screen.getByRole('button', { name: 'Button label' }));

        expect(screen.queryByRole('textbox', { name: 'Search for a page' })).not.toBeInTheDocument();
    });

    it('should open when controlled open is true even if openOnClick is false', () => {
        renderPortalUi(
            <LinkUrlDropdown
                url="home-abc123"
                portalPages={portalPages}
                portalId="p1"
                onUrlChange={jest.fn()}
                openOnClick={false}
                open
            >
                <button type="button">Button label</button>
            </LinkUrlDropdown>,
        );

        expect(screen.getByRole('textbox', { name: 'Search for a page' })).toBeInTheDocument();
    });
});
