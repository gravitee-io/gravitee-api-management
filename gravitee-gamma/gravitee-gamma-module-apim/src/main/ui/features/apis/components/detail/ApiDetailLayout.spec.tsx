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
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../hooks/useApiDetail', () => ({
    useApiDetail: jest.fn(() => ({ data: null, isLoading: false })),
}));

jest.mock('../../hooks/useApiPermissions', () => ({
    useApiPermissions: jest.fn(() => ({ permissionsReady: false })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Skeleton: () => <div />,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./ApiDetailSidebarNav', () => ({
    API_PROXY_NAV_GROUPS: [],
    ApiDetailSidebarNav: ({ basePath }: { basePath: string }) => <div data-testid="sidebar" data-basepath={basePath} />,
}));

import { ApiDetailIndexRedirect, ApiDetailLayout } from './ApiDetailLayout';

function renderAt(path: string, routePattern = 'apis/:apiId') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path={routePattern} element={<ApiDetailLayout />}>
                    <Route index element={<ApiDetailIndexRedirect />} />
                    <Route path="overview" element={<div />} />
                    <Route path="plans" element={<div />} />
                    <Route path="endpoints/list" element={<div />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
    return screen.getByTestId('sidebar').getAttribute('data-basepath') ?? '';
}

// ─── useApiBasePath ───────────────────────────────────────────────────────────

describe('useApiBasePath', () => {
    it('strips the sub-page suffix and returns the API root path', () => {
        expect(renderAt('/apis/abc-123/overview')).toBe('/apis/abc-123');
    });

    it('produces the same basePath regardless of which sub-page is active', () => {
        expect(renderAt('/apis/abc-123/plans')).toBe('/apis/abc-123');
    });

    it('handles deeply nested sub-pages', () => {
        expect(renderAt('/apis/abc-123/endpoints/list')).toBe('/apis/abc-123');
    });

    it('handles an MF host prefix — extracts only up to /apis/{id}', () => {
        expect(renderAt('/org/env/apis/abc-123/overview', 'org/env/apis/:apiId')).toBe('/org/env/apis/abc-123');
    });
});

// ─── ApiDetailIndexRedirect ───────────────────────────────────────────────────

describe('ApiDetailIndexRedirect', () => {
    it('redirects the index route to overview', () => {
        render(
            <MemoryRouter initialEntries={['/apis/abc-123']}>
                <Routes>
                    <Route path="apis/:apiId" element={<ApiDetailLayout />}>
                        <Route index element={<ApiDetailIndexRedirect />} />
                        <Route path="overview" element={<div data-testid="overview-page" />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByTestId('overview-page')).toBeInTheDocument();
    });
});
