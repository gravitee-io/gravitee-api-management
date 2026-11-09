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
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { PortalTable } from './PortalTable';
import { createPortal } from '../../../testing/factories';

describe('PortalTable', () => {
    const portals = [
        createPortal({ id: 'z', name: 'Zulu', default: false, publishedApisCount: 7, url: 'https://z.example.com' }),
        createPortal({ id: 'a', name: 'Alpha', default: true, publishedApisCount: 20, url: 'https://a.example.com' }),
    ];

    it('shows a URL column with an explicit unavailable state without adding another link', () => {
        render(
            <MemoryRouter>
                <PortalTable
                    portals={[...portals, createPortal({ id: 'missing', name: 'No address', url: null })]}
                    canConfigure={false}
                    getOverviewPath={id => `${id}/overview`}
                />
            </MemoryRouter>,
        );

        expect(screen.getByRole('columnheader', { name: 'URL' })).toBeVisible();
        expect(screen.getByText('https://z.example.com')).toBeVisible();
        expect(screen.getByText('https://a.example.com')).toBeVisible();
        expect(screen.getByText('No public URL yet')).toBeVisible();
        expect(screen.getAllByRole('link')).toHaveLength(2);
        expect(screen.getByRole('button', { name: 'View Portal: No address' })).toHaveAttribute('aria-disabled', 'true');
    });

    it('preserves the backend ordering, badges and API counts without adding pagination or sorting controls', () => {
        render(
            <MemoryRouter>
                <PortalTable portals={portals} canConfigure={false} getOverviewPath={id => `${id}/overview`} />
            </MemoryRouter>,
        );

        const table = screen.getByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows).toHaveLength(3);
        expect(within(rows[1]).getByText('Zulu')).toBeVisible();
        expect(within(rows[1]).getByText('7')).toBeVisible();
        expect(within(rows[1]).queryByText('Default')).not.toBeInTheDocument();
        expect(within(rows[2]).getByText('Alpha')).toBeVisible();
        expect(within(rows[2]).getByText('20')).toBeVisible();
        expect(within(rows[2]).getByText('Default')).toBeVisible();
        expect(screen.queryByRole('button', { name: /next page|previous page|sort/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /configure/i })).not.toBeInTheDocument();
    });

    it('links each portal to its own URL and overview when configuration is permitted', () => {
        render(
            <MemoryRouter>
                <PortalTable portals={portals} canConfigure={true} getOverviewPath={id => `${id}/overview`} />
            </MemoryRouter>,
        );

        expect(screen.getByRole('link', { name: 'View Portal: Zulu' })).toHaveAttribute('href', 'https://z.example.com');
        expect(screen.getByRole('link', { name: 'View Portal: Alpha' })).toHaveAttribute('href', 'https://a.example.com');
        expect(screen.getByRole('link', { name: 'Configure Zulu' })).toHaveAttribute('href', '/z/overview');
        expect(screen.getByRole('link', { name: 'Configure Alpha' })).toHaveAttribute('href', '/a/overview');
    });
});
