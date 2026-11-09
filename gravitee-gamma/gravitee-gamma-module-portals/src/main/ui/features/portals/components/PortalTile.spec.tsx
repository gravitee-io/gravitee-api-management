/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { PortalTile } from './PortalTile';
import { createPortal } from '../../../testing/factories';

describe('PortalTile', () => {
    it.each([
        ['https://partners.example.com/docs', 'https://partners.example.com/docs'],
        [null, 'No public URL yet'],
    ])('shows the portal address or its unavailable state (%s)', (url, address) => {
        render(
            <MemoryRouter>
                <PortalTile portal={createPortal({ url })} canConfigure={false} overviewPath="p1/overview" />
            </MemoryRouter>,
        );

        expect(screen.getByText(address)).toBeVisible();
    });

    it('shows the portal name, default badge and backend API count', () => {
        render(
            <MemoryRouter>
                <PortalTile
                    portal={createPortal({ name: 'Partners', publishedApisCount: 42 })}
                    canConfigure={false}
                    overviewPath="p1/overview"
                />
            </MemoryRouter>,
        );

        expect(screen.getByRole('heading', { level: 3, name: 'Partners' })).toBeVisible();
        expect(screen.getByRole('article', { name: 'Partners' })).toBeVisible();
        expect(screen.getByText('Default')).toBeVisible();
        expect(screen.getByText('42')).toBeVisible();
        expect(screen.getByText('Published APIs')).toBeVisible();
    });

    it('does not mark non-default portals as default', () => {
        render(
            <MemoryRouter>
                <PortalTile portal={createPortal({ default: false })} canConfigure={false} overviewPath="p1/overview" />
            </MemoryRouter>,
        );

        expect(screen.queryByText('Default')).not.toBeInTheDocument();
    });

    it('shows Configure only when the user can update documentation and respects the router basename', () => {
        const { rerender } = render(
            <MemoryRouter basename="/environments/DEFAULT/portals" initialEntries={['/environments/DEFAULT/portals/']}>
                <PortalTile portal={createPortal({ name: 'Partners' })} canConfigure={true} overviewPath="p1/overview" />
            </MemoryRouter>,
        );

        expect(screen.getByRole('link', { name: 'Configure Partners' })).toHaveAttribute(
            'href',
            '/environments/DEFAULT/portals/p1/overview',
        );

        rerender(
            <MemoryRouter basename="/environments/DEFAULT/portals" initialEntries={['/environments/DEFAULT/portals/']}>
                <PortalTile portal={createPortal({ name: 'Partners' })} canConfigure={false} overviewPath="p1/overview" />
            </MemoryRouter>,
        );

        expect(screen.queryByRole('link', { name: 'Configure Partners' })).not.toBeInTheDocument();
    });
});
