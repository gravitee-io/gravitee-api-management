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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation } from 'react-router-dom';

import { useModuleBreadcrumbs } from './useModuleBreadcrumbs';
import { observability } from '../config/observability';
import { APIM_ROUTE_CONFIG } from '../config/routes';

const BASENAME = '/console';

function renderBreadcrumbs() {
    const wrapper = ({ children }: { readonly children: ReactNode }) => (
        <MemoryRouter basename={BASENAME} initialEntries={[`${BASENAME}/environments/dev/apim/observe/logs`]}>
            {children}
        </MemoryRouter>
    );
    return renderHook(
        () => ({ breadcrumbs: useModuleBreadcrumbs('observe/logs', useModuleRouting(APIM_ROUTE_CONFIG)), location: useLocation() }),
        { wrapper },
    );
}

describe('useModuleBreadcrumbs', () => {
    afterEach(() => jest.restoreAllMocks());

    it('should link a parent crumb to the path its click navigates to', () => {
        const { result } = renderBreadcrumbs();
        const [parent] = result.current.breadcrumbs;

        act(() => parent.onClick?.());

        expect(parent.label).toBe('Observability');
        expect(result.current.location.pathname).toBe('/environments/dev/apim/observe/dashboards');
        expect(parent.href).toBe(`${BASENAME}/environments/dev/apim/observe/dashboards`);
    });

    it('should link a parent crumb whose route key the module does not declare', () => {
        jest.spyOn(observability, 'breadcrumbSegments').mockReturnValue([
            { label: 'Observability', routeKey: 'observe/new-page' },
            { label: 'Logs' },
        ]);
        const { result } = renderBreadcrumbs();
        const [parent] = result.current.breadcrumbs;

        act(() => parent.onClick?.());

        expect(result.current.location.pathname).toBe('/environments/dev/apim/observe/new-page');
        expect(parent.href).toBe(`${BASENAME}/environments/dev/apim/observe/new-page`);
    });

    it('should leave the current page crumb without a link', () => {
        const { result } = renderBreadcrumbs();

        expect(result.current.breadcrumbs.at(-1)).toEqual({ label: 'Logs' });
    });
});
