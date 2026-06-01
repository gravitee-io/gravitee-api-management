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
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { resolveListHrefFromDetailBasePath, useDetailBasePath } from './useDetailBasePath';

describe('useDetailBasePath', () => {
    function hookAt(path: string, id = 'app-1') {
        const wrapper = ({ children }: { children: ReactNode }) => <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>;
        const { result } = renderHook(() => useDetailBasePath('applications', id), { wrapper });
        return result.current;
    }

    it('strips the sub-page suffix and returns the application root path', () => {
        expect(hookAt('/applications/app-1/general')).toBe('/applications/app-1');
    });

    it('handles deeply nested sub-pages', () => {
        expect(hookAt('/applications/app-1/subscriptions/sub-1')).toBe('/applications/app-1');
    });

    it('handles an MF host prefix — extracts only up to /applications/{id}', () => {
        expect(hookAt('/org/env/platform/applications/app-1/general')).toBe('/org/env/platform/applications/app-1');
    });
});

describe('resolveListHrefFromDetailBasePath', () => {
    it('strips the trailing resource id segment', () => {
        expect(resolveListHrefFromDetailBasePath('/applications/app-1')).toBe('/applications');
        expect(resolveListHrefFromDetailBasePath('/org/env/applications/app-1')).toBe('/org/env/applications');
    });
});
