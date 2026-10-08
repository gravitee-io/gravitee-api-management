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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation } from 'react-router-dom';

import { useModuleBreadcrumbs } from './useModuleBreadcrumbs';

jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({ pathForKey: (key: string) => `/path/${key}` }),
    useToHref: () => (to: string) => `/href${to}`,
}));

function renderBreadcrumbs() {
    const wrapper = ({ children }: { readonly children: ReactNode }) => <MemoryRouter>{children}</MemoryRouter>;
    return renderHook(() => ({ breadcrumbs: useModuleBreadcrumbs('observe/logs'), location: useLocation() }), { wrapper });
}

describe('useModuleBreadcrumbs', () => {
    it('should link a parent crumb to the path its click navigates to', () => {
        const { result } = renderBreadcrumbs();
        const [observability] = result.current.breadcrumbs;

        act(() => observability.onClick?.());

        expect(observability.label).toBe('Observability');
        expect(result.current.location.pathname).toBe('/path/observe/dashboards');
        expect(observability.href).toBe('/href/path/observe/dashboards');
    });

    it('should leave the current page crumb without a link', () => {
        const { result } = renderBreadcrumbs();

        expect(result.current.breadcrumbs.at(-1)).toEqual({ label: 'Logs' });
    });
});
