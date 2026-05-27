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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { renderHook } from '@testing-library/react';

import { useApplicationMemberPermissions } from './useApplicationMemberPermissions';
import { useApplicationDetailContext } from '../context/ApplicationDetailContext';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('../context/ApplicationDetailContext', () => ({
    useApplicationDetailContext: jest.fn(),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);

describe('useApplicationMemberPermissions', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseApplicationDetailContext.mockReturnValue({
            permissionsReady: true,
        } as ReturnType<typeof useApplicationDetailContext>);
    });

    it('gates CRUD flags until permissions are ready', () => {
        mockUseApplicationDetailContext.mockReturnValue({
            permissionsReady: false,
        } as ReturnType<typeof useApplicationDetailContext>);
        mockUseHasPermission.mockReturnValue(true);

        const { result } = renderHook(() => useApplicationMemberPermissions());

        expect(result.current.permissionsReady).toBe(false);
        expect(result.current.canCreate).toBe(false);
        expect(result.current.canUpdate).toBe(false);
        expect(result.current.canDelete).toBe(false);
    });

    it('exposes member permissions when ready', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('application-member-c') ?? false);

        const { result, rerender } = renderHook(() => useApplicationMemberPermissions());

        expect(result.current.canCreate).toBe(true);
        expect(result.current.canUpdate).toBe(false);
        expect(result.current.canDelete).toBe(false);

        mockUseHasPermission.mockImplementation(({ anyOf }) => anyOf?.includes('application-member-d') ?? false);
        rerender();

        expect(result.current.canDelete).toBe(true);
    });
});
