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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import { useRef } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { PortalPageProvider } from '../portal-shell/context/PortalPageContext';
import { HtmlSlotHydrator } from './hydrate-slots';

jest.mock('../../blocks/ApiCatalogBlock/CatalogView', () => {
    const { useLocation } = require('react-router-dom');
    return {
        CatalogView: () => {
            const location = useLocation();
            return <div>Catalog at {location.pathname}</div>;
        },
    };
});

function TestHarness({ html }: { readonly html: string }) {
    const htmlContentRef = useRef<HTMLDivElement>(null);

    return (
        <div>
            <div ref={htmlContentRef} dangerouslySetInnerHTML={{ __html: html }} />
            <HtmlSlotHydrator containerRef={htmlContentRef} enabled htmlRevision={html} />
        </div>
    );
}

function renderHydrator(html: string) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });

    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/portals/portal-1/edit/home']}>
                <PortalPageProvider portalId="portal-1" selectedNavItemId={null} navItems={[]}>
                    <TestHarness html={html} />
                </PortalPageProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('HtmlSlotHydrator', () => {
    it('should hydrate api-catalog slot without Router context error', async () => {
        renderHydrator('<div data-gravitee-component="api-catalog"></div>');

        await waitFor(() => {
            expect(screen.getByText('Catalog at /portals/portal-1/edit/home')).toBeInTheDocument();
        });
    });

    it('should ignore unknown slot components', () => {
        renderHydrator('<div data-gravitee-component="unknown-widget">placeholder</div>');

        expect(screen.getByText('placeholder')).toBeInTheDocument();
        expect(screen.queryByText(/Catalog at/)).not.toBeInTheDocument();
    });
});
