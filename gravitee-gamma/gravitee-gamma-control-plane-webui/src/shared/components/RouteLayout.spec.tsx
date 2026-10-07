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
import { render } from '@testing-library/react';

const mockPublished: Array<Record<string, unknown>> = [];
let mockLastDeps: unknown[] | null = null;
const mockNavigate = jest.fn();
let mockPathname = '/environments/env/apis/one';

jest.mock('@gravitee/graphene-core', () => ({
    buildLinearBreadcrumbs: (_navigate: unknown, segments: unknown) => segments,
    SidebarNavigation: () => null,
    useLayoutConfig: (config: Record<string, unknown>, deps: unknown[] = []) => {
        const unchanged =
            mockLastDeps !== null && mockLastDeps.length === deps.length && mockLastDeps.every((dep, index) => Object.is(dep, deps[index]));
        if (unchanged) return;
        mockLastDeps = deps;
        mockPublished.push(config);
    },
}));

jest.mock('react-router-dom', () => {
    const actual = jest.requireActual('react-router-dom');
    return {
        ...actual,
        useLocation: () => ({ pathname: mockPathname }),
        useNavigate: () => mockNavigate,
        useParams: () => ({ envHrid: 'env' }),
        Outlet: () => null,
    };
});

jest.mock('../config/routes', () => {
    const actual = jest.requireActual('../config/routes');
    return {
        ...actual,
        resolveHostRoute: () => ({
            activeNavKey: 'home',
            breadcrumbSegments: [{ label: 'Home', to: '/environments/env/home' }],
        }),
    };
});

import { RouteLayout } from './RouteLayout';

describe('RouteLayout breadcrumbs', () => {
    beforeEach(() => {
        mockPublished.length = 0;
        mockLastDeps = null;
        mockPathname = '/environments/env/apis/one';
    });

    it('keeps the same breadcrumb when the next page resolves to the same labels', () => {
        const view = render(<RouteLayout />);
        const first = mockPublished[0]?.breadcrumbs;

        mockPathname = '/environments/env/apis/one/plans';
        view.rerender(<RouteLayout />);

        expect(mockPublished.at(-1)?.breadcrumbs).toBe(first);
        expect(first).toEqual([{ label: 'Home', to: '/environments/env/home' }]);
    });
});
