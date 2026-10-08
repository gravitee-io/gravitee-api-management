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

import { useModuleNavigation } from './useModuleNavigation';
import { NAV_GROUPS } from '../config/navigation';

const mockNavigateToKey = jest.fn();

jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({ hrefForKey: (key: string) => `/href/${key}`, navigateToKey: mockNavigateToKey }),
}));

describe('useModuleNavigation', () => {
    it('should link every side menu item to the key its click navigates to', () => {
        const { result } = renderHook(() => useModuleNavigation(NAV_GROUPS));
        const items = result.current.navGroups.flatMap(group => group.items);

        for (const item of items) {
            act(() => result.current.handleNavSelect(item.key));

            expect(item.href).toBe(`/href/${item.key}`);
            expect(mockNavigateToKey).toHaveBeenLastCalledWith(item.key);
        }
    });
});
